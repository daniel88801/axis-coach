package app.axis.coach.ui.splash

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.axis.coach.ui.components.AxisBrand
import app.axis.coach.ui.theme.AxisTypography
import app.axis.coach.ui.theme.Cyan
import app.axis.coach.ui.theme.CyanHot
import app.axis.coach.ui.theme.Fog
import app.axis.coach.ui.theme.NavyDeep
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AxisSplash() {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    val intro by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "intro",
    )
    val infinite = rememberInfiniteTransition(label = "splash")
    val orbit by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(9_000, easing = LinearEasing)),
        label = "orbit",
    )
    val orbitBack by infinite.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(13_000, easing = LinearEasing)),
        label = "orbitBack",
    )
    val pulse by infinite.animateFloat(
        initialValue = 0.86f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )
    val sweep by infinite.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "sweep",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF101A4A), NavyDeep, Color(0xFF02030C)),
                    radius = 1100f,
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            val min = size.minDimension
            drawCircle(Cyan.copy(alpha = 0.10f * pulse), radius = min * 0.28f * pulse, center = c)
            drawCircle(CyanHot.copy(alpha = 0.06f), radius = min * 0.42f, center = c)
            rotate(orbit, c) {
                drawCircle(
                    color = Cyan.copy(alpha = 0.55f),
                    radius = min * 0.30f,
                    center = c,
                    style = Stroke(width = 2.2f),
                )
                drawArc(
                    color = CyanHot,
                    startAngle = 12f,
                    sweepAngle = 54f,
                    useCenter = false,
                    topLeft = Offset(c.x - min * 0.30f, c.y - min * 0.30f),
                    size = Size(min * 0.60f, min * 0.60f),
                    style = Stroke(width = 5.5f, cap = StrokeCap.Round),
                )
            }
            rotate(orbitBack, c) {
                drawCircle(
                    color = Cyan.copy(alpha = 0.22f),
                    radius = min * 0.38f,
                    center = c,
                    style = Stroke(width = 1.4f),
                )
                repeat(8) { i ->
                    val a = Math.toRadians(i * 45.0)
                    val r = min * 0.38f
                    val p = Offset(c.x + (cos(a) * r).toFloat(), c.y + (sin(a) * r).toFloat())
                    drawCircle(CyanHot, radius = 4.2f, center = p)
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .scale(0.86f + 0.14f * intro)
                .alpha(intro),
        ) {
            AxisBrand(size = 156.dp)
            Spacer(Modifier.height(28.dp))
            Text(
                text = "AXIS",
                style = AxisTypography.displayMedium.copy(
                    letterSpacing = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                ),
                color = Fog,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "ТЕХНИКА В РЕАЛЬНОМ ВРЕМЕНИ",
                style = AxisTypography.labelSmall,
                color = Cyan,
            )
            Spacer(Modifier.height(36.dp))
            Box(
                modifier = Modifier
                    .width(148.dp)
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(Cyan.copy(alpha = 0.16f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(sweep)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(listOf(CyanHot, Cyan, Color.White)),
                        ),
                )
            }
        }
    }
}
