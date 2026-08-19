package app.axis.coach.domain.analysis

import app.axis.coach.domain.model.Landmark
import app.axis.coach.domain.model.MovementPhase
import app.axis.coach.domain.model.PoseFrame
import app.axis.coach.pose.LandmarkIndex
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Counts a push-up only after a real descent (both elbows + shoulder drop)
 * and a return to lockout. Smooths MediaPipe jitter so reps are not skipped
 * or double-counted.
 */
class PushUpTracker {
    var reps: Int = 0
        private set
    var phase: MovementPhase = MovementPhase.IDLE
        private set
    var lastDepth: Float = 180f
        private set
    var lastFeat: Feat? = null
        private set

    private var elbowEma: Float? = null
    private var depthEma: Float? = null
    private var topDepth: Float? = null
    private var reachedBottom: Boolean = false
    private var lastRepAt: Long = 0L
    private var lowestElbow: Float = 180f
    private var highestElbow: Float = 0f
    private var maxDrop: Float = 0f
    private var downAt: Long = 0L
    private var seenTop: Boolean = false
    private var warm: Int = 0

    data class Feat(
        val elbow: Float,
        val depth: Float,
        val torso: Float,
        val front: Boolean,
        val wristBelow: Boolean,
        val align: Float,
    )

    fun onFrame(frame: PoseFrame): Boolean {
        val feat = measure(frame) ?: run {
            lastFeat = null
            return false
        }
        lastFeat = feat
        if (!feat.wristBelow) return false

        elbowEma = ema(elbowEma, feat.elbow, 0.30f)
        depthEma = ema(depthEma, feat.depth, 0.26f)
        if (warm < 8) {
            warm += 1
            val e = elbowEma ?: return false
            val d = depthEma ?: return false
            if (e >= 148f) topDepth = min(topDepth ?: d, d)
            return false
        }

        val elbow = elbowEma ?: return false
        val depth = depthEma ?: return false
        lowestElbow = min(lowestElbow, elbow)
        highestElbow = max(highestElbow, elbow)
        lastDepth = lowestElbow

        if (elbow >= 148f) {
            topDepth = min(topDepth ?: depth, depth)
            seenTop = true
        }
        val drop = depth - (topDepth ?: depth)
        maxDrop = max(maxDrop, drop)
        val minDrop = max(0.042f, min(0.16f, feat.torso * 0.24f))

        val atBottom = if (feat.front) {
            (drop >= minDrop && elbow <= 150f) || elbow <= 122f
        } else {
            elbow <= 98f || (drop >= minDrop && elbow <= 118f)
        }
        val atTop = if (feat.front) {
            elbow >= 140f && drop <= minDrop * 0.42f
        } else {
            elbow >= 152f
        }

        var counted = false
        val now = frame.timestampMs
        if (atTop) {
            val sinceLast = if (lastRepAt == 0L) Long.MAX_VALUE else now - lastRepAt
            val cycle = if (downAt == 0L) 0L else now - downAt
            val rom = highestElbow - lowestElbow
            val deepEnough = maxDrop >= minDrop || rom >= if (feat.front) 24f else 38f
            val timeOk = sinceLast >= 400L && (downAt == 0L || (cycle in 220L..4200L))
            if (reachedBottom && seenTop && deepEnough && timeOk) {
                reps += 1
                lastRepAt = now
                counted = true
            }
            reachedBottom = false
            lowestElbow = elbow
            highestElbow = elbow
            maxDrop = drop
            downAt = 0L
            phase = MovementPhase.TOP
        } else if (atBottom) {
            if (!reachedBottom) downAt = now
            reachedBottom = true
            phase = MovementPhase.BOTTOM
        } else if (phase == MovementPhase.TOP || phase == MovementPhase.IDLE) {
            phase = MovementPhase.DESCENDING
        } else if (phase == MovementPhase.BOTTOM) {
            phase = MovementPhase.ASCENDING
        }
        return counted
    }

    fun reset() {
        reps = 0
        phase = MovementPhase.IDLE
        lastDepth = 180f
        lastFeat = null
        elbowEma = null
        depthEma = null
        topDepth = null
        reachedBottom = false
        lastRepAt = 0L
        lowestElbow = 180f
        highestElbow = 0f
        maxDrop = 0f
        downAt = 0L
        seenTop = false
        warm = 0
    }

    private fun measure(frame: PoseFrame): Feat? {
        val ls = frame.getOrNull(LandmarkIndex.LEFT_SHOULDER) ?: return null
        val rs = frame.getOrNull(LandmarkIndex.RIGHT_SHOULDER) ?: return null
        val le = frame.getOrNull(LandmarkIndex.LEFT_ELBOW)
        val re = frame.getOrNull(LandmarkIndex.RIGHT_ELBOW)
        val lw = frame.getOrNull(LandmarkIndex.LEFT_WRIST)
        val rw = frame.getOrNull(LandmarkIndex.RIGHT_WRIST)
        val lh = frame.getOrNull(LandmarkIndex.LEFT_HIP) ?: return null
        val rh = frame.getOrNull(LandmarkIndex.RIGHT_HIP) ?: return null
        val la = frame.getOrNull(LandmarkIndex.LEFT_ANKLE)
        val ra = frame.getOrNull(LandmarkIndex.RIGHT_ANKLE)

        val arms = mutableListOf<Pair<Float, Float>>()
        if (le != null && lw != null) {
            val vis = (ls.visibility + le.visibility + lw.visibility) / 3f
            if (vis >= 0.28f) arms += Geometry.angle(ls, le, lw) to vis
        }
        if (re != null && rw != null) {
            val vis = (rs.visibility + re.visibility + rw.visibility) / 3f
            if (vis >= 0.28f) arms += Geometry.angle(rs, re, rw) to vis
        }
        if (arms.isEmpty()) return null

        val elbow = if (arms.size == 2 && abs(arms[0].first - arms[1].first) < 55f) {
            val w = arms[0].second + arms[1].second
            (arms[0].first * arms[0].second + arms[1].first * arms[1].second) / w
        } else {
            arms.maxBy { it.second }.first
        }

        val sh = mid(ls, rs)
        val wr = when {
            lw != null && rw != null -> mid(lw, rw)
            lw != null -> lw
            rw != null -> rw
            else -> return null
        }
        val hp = mid(lh, rh)
        val an = if (la != null && ra != null) mid(la, ra) else la ?: ra
        val bodyVis = (ls.visibility + rs.visibility + lh.visibility + rh.visibility) / 4f
        if (bodyVis < 0.35f) return null

        val torso = max(0.08f, hypot(sh.x - hp.x, sh.y - hp.y))
        val front = abs(ls.x - rs.x) > 0.14f
        val align = if (an != null) Geometry.deviationFromStraight(sh, hp, an) else 0f
        return Feat(
            elbow = elbow,
            depth = sh.y,
            torso = torso,
            front = front,
            wristBelow = wr.y + 0.02f >= sh.y,
            align = align,
        )
    }

    private fun mid(a: Landmark, b: Landmark) = Landmark(
        x = (a.x + b.x) / 2f,
        y = (a.y + b.y) / 2f,
        z = (a.z + b.z) / 2f,
        visibility = min(a.visibility, b.visibility),
    )

    private fun ema(prev: Float?, next: Float, a: Float): Float =
        if (prev == null) next else prev + a * (next - prev)
}
