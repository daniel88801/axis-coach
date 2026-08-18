package app.axis.coach.ui.session

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import app.axis.coach.domain.model.Landmark
import app.axis.coach.pose.LandmarkIndex
import app.axis.coach.ui.theme.Coral
import app.axis.coach.ui.theme.Lime
import kotlin.math.max

@Composable
fun PoseOverlay(
    landmarks: List<Landmark>,
    imageWidth: Int,
    imageHeight: Int,
    highlighted: Set<Int>,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        if (landmarks.isEmpty() || imageWidth <= 0 || imageHeight <= 0) return@Canvas
        val scale = max(size.width / imageWidth, size.height / imageHeight)
        val offsetX = (size.width - imageWidth * scale) / 2f
        val offsetY = (size.height - imageHeight * scale) / 2f

        fun point(index: Int): Offset? {
            val lm = landmarks.getOrNull(index) ?: return null
            if (lm.visibility < 0.35f) return null
            return Offset(
                lm.x * imageWidth * scale + offsetX,
                lm.y * imageHeight * scale + offsetY,
            )
        }

        LandmarkIndex.CONNECTIONS.forEach { (start, end) ->
            val a = point(start) ?: return@forEach
            val b = point(end) ?: return@forEach
            val hot = start in highlighted || end in highlighted
            val color = if (hot) Coral else Lime
            drawLine(
                color = color.copy(alpha = 0.22f),
                start = a,
                end = b,
                strokeWidth = 14f,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = color,
                start = a,
                end = b,
                strokeWidth = 5.5f,
                cap = StrokeCap.Round,
            )
        }

        landmarks.forEachIndexed { index, lm ->
            val p = point(index) ?: return@forEachIndexed
            val hot = index in highlighted
            val color = if (hot) Coral else Lime
            drawCircle(color.copy(alpha = 0.25f), radius = 14f, center = p)
            drawCircle(color, radius = 6.5f, center = p)
            drawCircle(Color.Black.copy(alpha = 0.55f), radius = 6.5f, center = p, style = Stroke(width = 1.4f))
        }
    }
}
