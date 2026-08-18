package app.axis.coach.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.axis.coach.domain.model.Exercise
import app.axis.coach.ui.components.AxisBrand
import app.axis.coach.ui.components.AxisPill
import app.axis.coach.ui.components.ExerciseCard
import app.axis.coach.ui.theme.AxisTypography
import app.axis.coach.ui.theme.Fog
import app.axis.coach.ui.theme.FogDim
import app.axis.coach.ui.theme.FogMute
import app.axis.coach.ui.theme.Ink
import app.axis.coach.ui.theme.InkCard
import app.axis.coach.ui.theme.Lime
import app.axis.coach.ui.theme.Line

@Composable
fun HomeRoute(
    viewModel: HomeViewModel,
    onStart: (Exercise) -> Unit,
    onHistory: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onStart = onStart,
        onHistory = onHistory,
        onToggleReminders = viewModel::setReminders,
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onStart: (Exercise) -> Unit,
    onHistory: () -> Unit,
    onToggleReminders: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AxisBrand(size = 52.dp)
            Spacer(Modifier.weight(1f))
            AxisPill(if (state.sessionCount == 0) "first session" else "${state.sessionCount} logged")
        }
        Spacer(Modifier.height(28.dp))
        Text("AXIS", style = AxisTypography.displayLarge, color = Fog)
        Text("COACH", style = AxisTypography.labelLarge, color = Lime)
        Spacer(Modifier.height(10.dp))
        Text(
            "On-device pose. Live cues. No cloud.",
            style = AxisTypography.bodyLarge,
            color = FogDim,
        )
        Spacer(Modifier.height(28.dp))

        state.lastSession?.let { last ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .border(1.dp, Line, RoundedCornerShape(22.dp))
                    .clickable(onClick = onHistory)
                    .padding(16.dp),
            ) {
                Column {
                    Text("LAST SET", style = AxisTypography.labelSmall, color = FogMute)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${last.exercise.title}  ·  score ${last.formScore}",
                        style = AxisTypography.titleMedium,
                        color = Fog,
                    )
                    Text(
                        if (last.exercise == Exercise.PLANK) "${last.holdSeconds}s hold"
                        else "${last.reps} ${last.exercise.metricLabel}",
                        style = AxisTypography.bodyMedium,
                        color = FogDim,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        Text("CHOOSE A LIFT", style = AxisTypography.labelSmall, color = FogMute)
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Exercise.entries.forEach { exercise ->
                ExerciseCard(exercise = exercise, onClick = { onStart(exercise) })
            }
        }
        Spacer(Modifier.height(28.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(InkCard)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Evening reminder", style = AxisTypography.titleMedium, color = Fog)
                Text("19:00 · stay consistent", style = AxisTypography.bodyMedium, color = FogDim)
            }
            Switch(
                checked = state.remindersOn,
                onCheckedChange = onToggleReminders,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Ink,
                    checkedTrackColor = Lime,
                    uncheckedTrackColor = Line,
                ),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "History →",
            style = AxisTypography.titleMedium,
            color = Lime,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onHistory)
                .padding(vertical = 8.dp),
        )
    }
}
