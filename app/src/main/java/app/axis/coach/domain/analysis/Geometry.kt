package app.axis.coach.domain.analysis

import app.axis.coach.domain.model.Landmark
import kotlin.math.acos
import kotlin.math.hypot

object Geometry {
    fun angle(a: Landmark, vertex: Landmark, c: Landmark): Float {
        val abx = a.x - vertex.x
        val aby = a.y - vertex.y
        val cbx = c.x - vertex.x
        val cby = c.y - vertex.y
        val magAb = hypot(abx.toDouble(), aby.toDouble())
        val magCb = hypot(cbx.toDouble(), cby.toDouble())
        if (magAb < 1e-6 || magCb < 1e-6) return 180f
        val cos = ((abx * cbx + aby * cby) / (magAb * magCb)).coerceIn(-1.0, 1.0)
        return Math.toDegrees(acos(cos)).toFloat()
    }

    fun deviationFromStraight(a: Landmark, b: Landmark, c: Landmark): Float {
        return 180f - angle(a, b, c)
    }

    fun midpoint(a: Landmark, b: Landmark): Landmark = Landmark(
        x = (a.x + b.x) / 2f,
        y = (a.y + b.y) / 2f,
        z = (a.z + b.z) / 2f,
        visibility = minOf(a.visibility, b.visibility),
    )

    fun distance(a: Landmark, b: Landmark): Float = hypot(a.x - b.x, a.y - b.y)

    fun verticalAlignmentError(a: Landmark, b: Landmark, c: Landmark): Float {
        val abx = b.x - a.x
        val aby = b.y - a.y
        val acx = c.x - a.x
        val acy = c.y - a.y
        val abLen = hypot(abx, aby)
        if (abLen < 1e-5f) return 180f
        val t = ((acx * abx + acy * aby) / (abLen * abLen)).coerceIn(0f, 1f)
        val px = a.x + t * abx
        val py = a.y + t * aby
        val offset = hypot(c.x - px, c.y - py)
        val total = hypot(c.x - a.x, c.y - a.y).coerceAtLeast(1e-5f)
        return (offset / total) * 180f
    }

    fun clampScore(value: Int): Int = value.coerceIn(0, 100)

    fun lerp(from: Float, to: Float, t: Float): Float = from + (to - from) * t

    fun almostEqual(a: Float, b: Float, epsilon: Float = 0.5f): Boolean =
        kotlin.math.abs(a - b) <= epsilon
}

data class BodySide(
    val shoulder: Landmark,
    val elbow: Landmark,
    val wrist: Landmark,
    val hip: Landmark,
    val knee: Landmark,
    val ankle: Landmark,
    val visibility: Float,
)
