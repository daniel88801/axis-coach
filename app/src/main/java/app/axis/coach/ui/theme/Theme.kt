package app.axis.coach.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import app.axis.coach.domain.model.Exercise

val LocalExerciseAccent = staticCompositionLocalOf { Lime }

private val AxisColors = darkColorScheme(
    primary = Lime,
    onPrimary = Ink,
    secondary = Mint,
    onSecondary = Ink,
    tertiary = Amber,
    background = Ink,
    onBackground = Fog,
    surface = InkElevated,
    onSurface = Fog,
    surfaceVariant = InkCard,
    onSurfaceVariant = FogDim,
    outline = Line,
    error = Coral,
    onError = Fog,
)

@Composable
fun AxisTheme(
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalExerciseAccent provides Lime) {
        MaterialTheme(
            colorScheme = AxisColors,
            typography = AxisTypography,
            content = content,
        )
    }
}

fun Exercise.accent(): Color = when (this) {
    Exercise.SQUAT -> Lime
    Exercise.PUSH_UP -> Mint
    Exercise.PLANK -> Amber
}
