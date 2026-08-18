package app.axis.coach.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.axis.coach.R
import app.axis.coach.domain.model.Exercise
import app.axis.coach.ui.theme.AxisTypography
import app.axis.coach.ui.theme.Fog
import app.axis.coach.ui.theme.FogDim
import app.axis.coach.ui.theme.FogMute
import app.axis.coach.ui.theme.Ink
import app.axis.coach.ui.theme.InkCard
import app.axis.coach.ui.theme.Lime
import app.axis.coach.ui.theme.accent

@Composable
fun AxisBrand(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 56.dp,
) {
    Image(
        painter = painterResource(R.drawable.ic_axis_brand),
        contentDescription = "AXIS",
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(16.dp)),
    )
}

@Composable
fun AxisMark(modifier: Modifier = Modifier, color: Color = Lime) {
    Canvas(modifier = modifier.size(28.dp)) {
        val stroke = Stroke(width = size.minDimension * 0.08f, cap = StrokeCap.Round)
        val c = Offset(size.width / 2f, size.height / 2f)
        val r = size.minDimension * 0.32f
        drawLine(color, Offset(c.x, c.y - r * 1.35f), Offset(c.x, c.y + r * 1.35f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(color, Offset(c.x - r * 1.35f, c.y), Offset(c.x + r * 1.35f, c.y), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawCircle(color, radius = r, style = Stroke(width = stroke.width))
        drawCircle(color, radius = size.minDimension * 0.055f)
    }
}

@Composable
fun AxisPill(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Lime,
) {
    Text(
        text = text.uppercase(),
        style = AxisTypography.labelSmall,
        color = color,
        modifier = modifier
            .border(1.dp, color.copy(alpha = 0.45f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
fun AxisPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Lime,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = AxisTypography.titleMedium,
            color = Ink,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun ExerciseCard(
    exercise: Exercise,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = exercise.accent()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(InkCard)
            .clickable(onClick = onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ExerciseGlyph(exercise = exercise, accent = accent)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = exercise.indexLabel,
                style = AxisTypography.labelSmall,
                color = accent,
            )
            Text(
                text = exercise.title,
                style = AxisTypography.headlineMedium,
                color = Fog,
            )
            Text(
                text = exercise.blurb,
                style = AxisTypography.bodyMedium,
                color = FogDim,
            )
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(accent),
            contentAlignment = Alignment.Center,
        ) {
            Text("→", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ExerciseGlyph(
    exercise: Exercise,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .size(64.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(accent.copy(alpha = 0.12f)),
    ) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 4.2f, cap = StrokeCap.Round)
        when (exercise) {
            Exercise.SQUAT -> {
                drawCircle(accent, radius = 6f, center = Offset(w * 0.50f, h * 0.22f))
                drawLine(accent, Offset(w * 0.50f, h * 0.30f), Offset(w * 0.50f, h * 0.48f), 4.2f, StrokeCap.Round)
                drawLine(accent, Offset(w * 0.50f, h * 0.48f), Offset(w * 0.30f, h * 0.62f), 4.2f, StrokeCap.Round)
                drawLine(accent, Offset(w * 0.30f, h * 0.62f), Offset(w * 0.34f, h * 0.82f), 4.2f, StrokeCap.Round)
                drawLine(accent, Offset(w * 0.50f, h * 0.48f), Offset(w * 0.72f, h * 0.58f), 4.2f, StrokeCap.Round)
                drawLine(accent, Offset(w * 0.72f, h * 0.58f), Offset(w * 0.70f, h * 0.80f), 4.2f, StrokeCap.Round)
            }
            Exercise.PUSH_UP -> {
                drawCircle(accent, radius = 5.5f, center = Offset(w * 0.22f, h * 0.38f))
                drawLine(accent, Offset(w * 0.28f, h * 0.42f), Offset(w * 0.78f, h * 0.50f), 4.2f, StrokeCap.Round)
                drawLine(accent, Offset(w * 0.34f, h * 0.44f), Offset(w * 0.28f, h * 0.68f), 4.2f, StrokeCap.Round)
                drawLine(accent, Offset(w * 0.70f, h * 0.50f), Offset(w * 0.82f, h * 0.70f), 4.2f, StrokeCap.Round)
            }
            Exercise.PLANK -> {
                drawCircle(accent, radius = 5.5f, center = Offset(w * 0.20f, h * 0.42f))
                drawLine(accent, Offset(w * 0.26f, h * 0.46f), Offset(w * 0.82f, h * 0.46f), 4.2f, StrokeCap.Round)
                drawLine(accent, Offset(w * 0.30f, h * 0.46f), Offset(w * 0.26f, h * 0.70f), 4.2f, StrokeCap.Round)
                drawLine(accent, Offset(w * 0.82f, h * 0.46f), Offset(w * 0.82f, h * 0.70f), 4.2f, StrokeCap.Round)
            }
        }
    }
}

@Composable
fun StatChip(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(InkCard)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label.uppercase(), style = AxisTypography.labelSmall, color = FogMute)
        Text(value, style = AxisTypography.titleLarge, color = Fog)
    }
}
