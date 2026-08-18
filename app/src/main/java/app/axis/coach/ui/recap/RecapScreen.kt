package app.axis.coach.ui.recap

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.axis.coach.domain.model.Exercise
import app.axis.coach.domain.model.FinishedSession
import app.axis.coach.ui.components.AxisPrimaryButton
import app.axis.coach.ui.components.StatChip
import app.axis.coach.ui.theme.AxisTypography
import app.axis.coach.ui.theme.Fog
import app.axis.coach.ui.theme.FogDim
import app.axis.coach.ui.theme.FogMute
import app.axis.coach.ui.theme.Ink
import app.axis.coach.ui.theme.Line
import app.axis.coach.ui.theme.accent
import app.axis.coach.ui.theme.axisSpring

@Composable
fun RecapRoute(
    viewModel: RecapViewModel,
    onAgain: (Exercise) -> Unit,
    onHome: () -> Unit,
) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    session?.let {
        RecapScreen(session = it, onAgain = { onAgain(it.exercise) }, onHome = onHome)
    } ?: Box(
        Modifier.fillMaxSize().background(Ink),
        contentAlignment = Alignment.Center,
    ) {
        Text("Сохраняю сет…", color = FogDim)
    }
}

@Composable
fun RecapScreen(
    session: FinishedSession,
    onAgain: () -> Unit,
    onHome: () -> Unit,
) {
    val accent = session.exercise.accent()
    var armed by remember { mutableStateOf(false) }
    LaunchedEffect(session.id) { armed = true }
    val sweep by animateFloatAsState(
        targetValue = if (armed) session.formScore / 100f else 0f,
        animationSpec = axisSpring(),
        label = "ring",
    )
    val minutes = session.durationMs / 1000 / 60
    val seconds = (session.durationMs / 1000) % 60

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("СЕТ ЗАКРЫТ", style = AxisTypography.labelSmall, color = FogMute)
        Spacer(Modifier.height(8.dp))
        Text(session.exercise.title, style = AxisTypography.displayMedium, color = Fog)
        Spacer(Modifier.height(28.dp))

        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                drawArc(
                    color = Line,
                    startAngle = -210f,
                    sweepAngle = 240f,
                    useCenter = false,
                    style = stroke,
                )
                drawArc(
                    color = accent,
                    startAngle = -210f,
                    sweepAngle = 240f * sweep,
                    useCenter = false,
                    style = stroke,
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${session.formScore}", style = AxisTypography.displayMedium, color = Fog)
                Text("ТЕХНИКА", style = AxisTypography.labelSmall, color = FogMute)
            }
        }

        Spacer(Modifier.height(28.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val work = if (session.exercise == Exercise.PLANK) "${session.holdSeconds}s" else "${session.reps}"
            val workLabel = if (session.exercise == Exercise.PLANK) "удерж." else "повт"
            StatChip(workLabel, work, Modifier.weight(1f))
            StatChip("время", "%d:%02d".format(minutes, seconds), Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        session.topCue?.let { cue ->
            StatChip("частая подсказка", cue, Modifier.fillMaxWidth())
        }
        Spacer(Modifier.weight(1f))
        AxisPrimaryButton("Ещё раз", onAgain, color = accent)
        Spacer(Modifier.height(12.dp))
        AxisPrimaryButton("На главную", onHome, color = Fog)
    }
}
