package app.axis.coach.data.repo

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "axis_prefs")

@Singleton
class PreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val onboarding = booleanPreferencesKey("onboarding_done")
    private val reminders = booleanPreferencesKey("reminders_on")

    val onboardingDone: Flow<Boolean> = context.dataStore.data.map { it[onboarding] == true }
    val remindersOn: Flow<Boolean> = context.dataStore.data.map { it[reminders] == true }

    suspend fun setOnboardingDone() {
        context.dataStore.edit { it[onboarding] = true }
    }

    suspend fun setReminders(enabled: Boolean) {
        context.dataStore.edit { it[reminders] = enabled }
    }
}
