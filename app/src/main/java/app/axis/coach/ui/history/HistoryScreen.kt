package app.axis.coach.ui.history

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.axis.coach.domain.model.Exercise
import app.axis.coach.domain.model.FinishedSession
import app.axis.coach.ui.theme.AxisTypography
import app.axis.coach.ui.theme.Fog
import app.axis.coach.ui.theme.FogDim
import app.axis.coach.ui.theme.FogMute
import app.axis.coach.ui.theme.Ink
import app.axis.coach.ui.theme.InkCard
import app.axis.coach.ui.theme.Lime
import app.axis.coach.ui.theme.accent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryRoute(
    viewModel: HistoryViewModel,
    onBack: () -> Unit,
) {
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    HistoryScreen(sessions = sessions, onBack = onBack)
}

@Composable
fun HistoryScreen(
    sessions: List<FinishedSession>,
    onBack: () -> Unit,
) {
    val formatter = SimpleDateFormat("d MMM  ·  HH:mm", Locale.getDefault())
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = Fog)
            }
            Text("History", style = AxisTypography.headlineMedium, color = Fog)
        }

        if (sessions.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No sets yet. Film one.", color = FogDim)
            }
            return
        }

        WeeklyBars(sessions = sessions, modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(sessions, key = { it.id }) { session ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(InkCard)
                        .padding(16.dp),
                ) {
                    Text(
                        session.exercise.title.uppercase(),
                        style = AxisTypography.labelSmall,
                        color = session.exercise.accent(),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (session.exercise == Exercise.PLANK) {
                            "${session.holdSeconds}s  ·  form ${session.formScore}"
                        } else {
                            "${session.reps} reps  ·  form ${session.formScore}"
                        },
                        style = AxisTypography.titleMedium,
                        color = Fog,
                    )
                    Text(formatter.format(Date(session.endedAt)), style = AxisTypography.bodyMedium, color = FogMute)
                    session.topCue?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(it, style = AxisTypography.bodyMedium, color = FogDim)
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun WeeklyBars(
    sessions: List<FinishedSession>,
    modifier: Modifier = Modifier,
) {
    val now = System.currentTimeMillis()
    val dayMs = 86_400_000L
    val counts = (0..6).map { offset ->
        val start = now - (6 - offset) * dayMs
        val end = start + dayMs
        sessions.count { it.endedAt in start until end }
    }
    val max = counts.maxOrNull()?.coerceAtLeast(1) ?: 1
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(92.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(InkCard)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        counts.forEach { count ->
            val fraction = (count.toFloat() / max).coerceIn(0.08f, 1f)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height((68 * fraction).dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (count == 0) FogMute.copy(alpha = 0.25f) else Lime),
            )
        }
    }
}
