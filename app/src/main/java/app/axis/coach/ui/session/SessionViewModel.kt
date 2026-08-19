package app.axis.coach.ui.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.axis.coach.audio.CoachVoice
import app.axis.coach.data.repo.SessionRepository
import app.axis.coach.domain.analysis.AnalyzerFactory
import app.axis.coach.domain.model.CueSeverity
import app.axis.coach.domain.model.Exercise
import app.axis.coach.domain.model.FrameVerdict
import app.axis.coach.domain.model.Landmark
import app.axis.coach.domain.model.MovementPhase
import app.axis.coach.domain.model.PoseFrame
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class SessionUiState(
    val exercise: Exercise,
    val isFrontCamera: Boolean = false,
    val isPaused: Boolean = false,
    val personDetected: Boolean = false,
    val landmarks: List<Landmark> = emptyList(),
    val imageWidth: Int = 1,
    val imageHeight: Int = 1,
    val reps: Int = 0,
    val holdSeconds: Int = 0,
    val formScore: Int = 100,
    val cue: String? = null,
    val severity: CueSeverity = CueSeverity.NONE,
    val phase: MovementPhase = MovementPhase.IDLE,
    val elapsedMs: Long = 0L,
    val highlightedJoints: Set<Int> = emptySet(),
    val finishedId: Long? = null,
    val detectorError: String? = null,
)

@HiltViewModel
class SessionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionRepository: SessionRepository,
    private val coachVoice: CoachVoice,
) : ViewModel() {

    private val exercise = Exercise.fromId(savedStateHandle.get<String>("exerciseId") ?: Exercise.SQUAT.id)
    private val analyzer = AnalyzerFactory.create(exercise)
    private val startedAt = System.currentTimeMillis()
    private var ticker: Job? = null
    private var topCue: String? = null
    private val cueHits = mutableMapOf<String, Int>()

    private val _uiState = MutableStateFlow(SessionUiState(exercise = exercise))
    val uiState: StateFlow<SessionUiState> = _uiState

    init {
        ticker = viewModelScope.launch {
            while (isActive) {
                delay(250)
                _uiState.update { state ->
                    if (state.isPaused || state.finishedId != null) state
                    else state.copy(elapsedMs = System.currentTimeMillis() - startedAt)
                }
            }
        }
    }

    fun onLandmarks(frame: PoseFrame) {
        val state = _uiState.value
        if (state.isPaused || state.finishedId != null) return
        val verdict: FrameVerdict = analyzer.analyze(frame)
        if (verdict.newRep) coachVoice.onRep()
        coachVoice.onCue(verdict.cue, verdict.severity, frame.timestampMs)
        if (verdict.severity == CueSeverity.COACH || verdict.severity == CueSeverity.WARN) {
            verdict.cue?.let { cue ->
                cueHits[cue] = (cueHits[cue] ?: 0) + 1
                topCue = cueHits.maxByOrNull { it.value }?.key
            }
        }
        _uiState.update {
            it.copy(
                personDetected = verdict.personDetected,
                landmarks = frame.landmarks,
                imageWidth = frame.imageWidth,
                imageHeight = frame.imageHeight,
                reps = verdict.reps,
                holdSeconds = (verdict.holdMillis / 1000L).toInt(),
                formScore = verdict.formScore,
                cue = verdict.cue,
                severity = verdict.severity,
                phase = verdict.phase,
                highlightedJoints = verdict.highlightedJoints,
            )
        }
    }

    fun onDetectorError(message: String) {
        _uiState.update { it.copy(detectorError = message) }
    }

    fun flipCamera() {
        _uiState.update { it.copy(isFrontCamera = !it.isFrontCamera) }
    }

    fun togglePause() {
        _uiState.update { it.copy(isPaused = !it.isPaused) }
    }

    fun endSession() {
        val snapshot = _uiState.value
        if (snapshot.finishedId != null) return
        viewModelScope.launch {
            val id = sessionRepository.save(
                exercise = exercise,
                startedAt = startedAt,
                endedAt = System.currentTimeMillis(),
                reps = snapshot.reps,
                holdSeconds = snapshot.holdSeconds,
                formScore = snapshot.formScore,
                topCue = topCue,
            )
            _uiState.update { it.copy(finishedId = id) }
        }
    }

    override fun onCleared() {
        ticker?.cancel()
        super.onCleared()
    }
}
