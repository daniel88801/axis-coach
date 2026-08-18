package app.axis.coach.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import app.axis.coach.ui.splash.AxisSplash
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.axis.coach.data.repo.PreferencesRepository
import app.axis.coach.ui.history.HistoryRoute
import app.axis.coach.ui.home.HomeRoute
import app.axis.coach.ui.onboarding.OnboardingScreen
import app.axis.coach.ui.onboarding.OnboardingViewModel
import app.axis.coach.ui.recap.RecapRoute
import app.axis.coach.ui.session.SessionRoute
import app.axis.coach.ui.theme.Ink
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import androidx.compose.ui.platform.LocalContext

private object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val SESSION = "session/{exerciseId}"
    const val RECAP = "recap/{sessionId}"
    const val HISTORY = "history"

    fun session(id: String) = "session/$id"
    fun recap(id: Long) = "recap/$id"
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PrefsEntryPoint {
    fun preferences(): PreferencesRepository
}

@Composable
fun AxisRoot() {
    val context = LocalContext.current
    val prefs = EntryPointAccessors.fromApplication(
        context.applicationContext,
        PrefsEntryPoint::class.java,
    ).preferences()
    var ready by remember { mutableStateOf(false) }
    var onboarded by remember { mutableStateOf(false) }
    var showSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        val started = System.currentTimeMillis()
        onboarded = prefs.onboardingDone.first()
        val remaining = 2_200L - (System.currentTimeMillis() - started)
        if (remaining > 0) delay(remaining)
        ready = true
        delay(80)
        showSplash = false
    }

    val start = if (onboarded) Routes.HOME else Routes.ONBOARDING
    val nav = rememberNavController()

    Box(Modifier.fillMaxSize().background(Ink)) {
        if (ready) {
        NavHost(navController = nav, startDestination = start) {
            composable(Routes.ONBOARDING) {
                val vm: OnboardingViewModel = hiltViewModel()
                OnboardingScreen(
                    onComplete = vm::complete,
                    onFinished = {
                        nav.navigate(Routes.HOME) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.HOME) {
                HomeRoute(
                    viewModel = hiltViewModel(),
                    onStart = { exercise -> nav.navigate(Routes.session(exercise.id)) },
                    onHistory = { nav.navigate(Routes.HISTORY) },
                )
            }
            composable(
                Routes.SESSION,
                arguments = listOf(navArgument("exerciseId") { type = NavType.StringType }),
            ) {
                SessionRoute(
                    viewModel = hiltViewModel(),
                    onFinished = { id ->
                        nav.navigate(Routes.recap(id)) {
                            popUpTo(Routes.HOME)
                        }
                    },
                    onClose = { nav.popBackStack() },
                )
            }
            composable(
                Routes.RECAP,
                arguments = listOf(navArgument("sessionId") { type = NavType.StringType }),
            ) {
                RecapRoute(
                    viewModel = hiltViewModel(),
                    onAgain = { exercise ->
                        nav.navigate(Routes.session(exercise.id)) {
                            popUpTo(Routes.HOME)
                        }
                    },
                    onHome = {
                        nav.navigate(Routes.HOME) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.HISTORY) {
                HistoryRoute(
                    viewModel = hiltViewModel(),
                    onBack = { nav.popBackStack() },
                )
            }
        }
        }

        AnimatedVisibility(
            visible = showSplash,
            exit = fadeOut(tween(420)) + scaleOut(targetScale = 1.06f, animationSpec = tween(420)),
        ) {
            AxisSplash()
        }
    }
}
