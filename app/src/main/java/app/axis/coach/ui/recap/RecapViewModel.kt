package app.axis.coach.ui.recap

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.axis.coach.data.repo.SessionRepository
import app.axis.coach.domain.model.FinishedSession
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class RecapViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    sessionRepository: SessionRepository,
) : ViewModel() {
    private val _session = MutableStateFlow<FinishedSession?>(null)
    val session: StateFlow<FinishedSession?> = _session

    init {
        val id = savedStateHandle.get<String>("sessionId")?.toLongOrNull() ?: -1L
        viewModelScope.launch {
            _session.value = sessionRepository.get(id)
        }
    }
}
