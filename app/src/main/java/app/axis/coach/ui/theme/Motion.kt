package app.axis.coach.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

fun <T> axisSpring() = spring<T>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMediumLow,
)
