package app.axis.coach.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.axis.coach.data.repo.SessionRepository
import app.axis.coach.domain.model.FinishedSession
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class HistoryViewModel @Inject constructor(
    sessionRepository: SessionRepository,
) : ViewModel() {
    val sessions = sessionRepository.observeSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<FinishedSession>())
}
