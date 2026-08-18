package app.axis.coach.ui.session

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.axis.coach.domain.model.CueSeverity
import app.axis.coach.domain.model.Exercise
import app.axis.coach.pose.PoseDetector
import app.axis.coach.ui.theme.Amber
import app.axis.coach.ui.theme.AxisTypography
import app.axis.coach.ui.theme.Coral
import app.axis.coach.ui.theme.Fog
import app.axis.coach.ui.theme.FogDim
import app.axis.coach.ui.theme.Ink
import app.axis.coach.ui.theme.Lime
import app.axis.coach.ui.theme.accent
import app.axis.coach.ui.theme.axisSpring

@Composable
fun SessionRoute(
    viewModel: SessionViewModel,
    onFinished: (Long) -> Unit,
    onClose: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.finishedId) {
        state.finishedId?.let(onFinished)
    }
    SessionScreen(
        state = state,
        onLandmarks = viewModel::onLandmarks,
        onDetectorError = viewModel::onDetectorError,
        onFlip = viewModel::flipCamera,
        onPause = viewModel::togglePause,
        onEnd = viewModel::endSession,
        onClose = onClose,
    )
}

@Composable
fun SessionScreen(
    state: SessionUiState,
    onLandmarks: (app.axis.coach.domain.model.PoseFrame) -> Unit,
    onDetectorError: (String) -> Unit,
    onFlip: () -> Unit,
    onPause: () -> Unit,
    onEnd: () -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    var hasCamera by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasCamera = granted }

    LaunchedEffect(Unit) {
        if (!hasCamera) launcher.launch(Manifest.permission.CAMERA)
    }

    val detector = remember {
        PoseDetector(
            context = context.applicationContext,
            onFrame = onLandmarks,
            onError = onDetectorError,
        )
    }
    DisposableEffect(Unit) {
        onDispose { detector.close() }
    }

    val accent = state.exercise.accent()
    val cueColor by animateColorAsState(
        when (state.severity) {
            CueSeverity.WARN -> Coral
            CueSeverity.COACH -> Amber
            CueSeverity.GOOD -> Lime
            CueSeverity.NONE -> Fog
        },
        label = "cue",
    )

    Box(modifier = Modifier.fillMaxSize().background(Ink)) {
        if (hasCamera) {
            CameraPreview(
                detector = detector,
                isFront = state.isFrontCamera,
                modifier = Modifier.fillMaxSize(),
            )
            PoseOverlay(
                landmarks = state.landmarks,
                imageWidth = state.imageWidth,
                imageHeight = state.imageHeight,
                highlighted = state.highlightedJoints,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Camera permission needed", color = FogDim)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Ink.copy(alpha = 0.55f),
                        0.22f to Color.Transparent,
                        0.62f to Color.Transparent,
                        1f to Ink.copy(alpha = 0.88f),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoundIcon(Icons.Outlined.Close, onClose)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(state.exercise.title.uppercase(), style = AxisTypography.labelSmall, color = accent)
                    Text(formatElapsed(state.elapsedMs), style = AxisTypography.titleMedium, color = Fog)
                }
                RoundIcon(Icons.Outlined.Cameraswitch, onFlip)
                Spacer(Modifier.width(8.dp))
                RoundIcon(
                    if (state.isPaused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause,
                    onPause,
                )
            }

            Spacer(Modifier.height(18.dp))
            AnimatedContent(
                targetState = state.cue ?: if (state.personDetected) "Hold the line" else "Step into frame",
                transitionSpec = { fadeIn(axisSpring()) togetherWith fadeOut() },
                label = "cueText",
            ) { text ->
                Text(
                    text = text,
                    style = AxisTypography.headlineMedium,
                    color = cueColor,
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(Ink.copy(alpha = 0.45f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }

            Spacer(Modifier.weight(1f))

            if (!state.personDetected) {
                Text(
                    state.exercise.setupHint,
                    style = AxisTypography.bodyMedium,
                    color = FogDim,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }

            SessionHud(state = state, accent = accent, onEnd = onEnd)
        }
    }
}

@Composable
private fun SessionHud(
    state: SessionUiState,
    accent: androidx.compose.ui.graphics.Color,
    onEnd: () -> Unit,
) {
    val metric = if (state.exercise == Exercise.PLANK) state.holdSeconds else state.reps
    val label = state.exercise.metricLabel
    val score = animateFloatAsState(state.formScore / 100f, label = "score").value

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Ink.copy(alpha = 0.72f))
            .border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(28.dp))
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = metric.toString(),
                style = AxisTypography.displayMedium,
                color = Fog,
                fontSize = 64.sp,
            )
            Spacer(Modifier.width(8.dp))
            Text(label, style = AxisTypography.labelLarge, color = FogDim, modifier = Modifier.padding(bottom = 12.dp))
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text("FORM", style = AxisTypography.labelSmall, color = FogDim)
                Text("${state.formScore}", style = AxisTypography.headlineLarge, color = accent)
            }
        }
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(score.coerceIn(0.04f, 1f))
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(accent),
            )
        }
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(accent)
                .clickable(onClick = onEnd),
            contentAlignment = Alignment.Center,
        ) {
            Text("End set", style = AxisTypography.titleMedium, color = Ink)
        }
    }
}

@Composable
private fun RoundIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Ink.copy(alpha = 0.55f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Fog, modifier = Modifier.size(20.dp))
    }
}

private fun formatElapsed(ms: Long): String {
    val total = (ms / 1000).toInt()
    val m = total / 60
    val s = total % 60
    return "%d:%02d".format(m, s)
}
