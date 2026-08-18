package app.axis.coach.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.axis.coach.data.repo.PreferencesRepository
import app.axis.coach.data.repo.SessionRepository
import app.axis.coach.domain.model.FinishedSession
import app.axis.coach.notify.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val lastSession: FinishedSession? = null,
    val sessionCount: Int = 0,
    val remindersOn: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    sessionRepository: SessionRepository,
    private val preferences: PreferencesRepository,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    val uiState = combine(
        sessionRepository.observeLatest(),
        sessionRepository.observeCount(),
        preferences.remindersOn,
    ) { last, count, reminders ->
        HomeUiState(lastSession = last, sessionCount = count, remindersOn = reminders)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun setReminders(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setReminders(enabled)
            reminderScheduler.setEnabled(enabled)
        }
    }
}
