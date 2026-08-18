package app.axis.coach.domain.analysis

import app.axis.coach.domain.model.MovementPhase

/**
 * Hysteresis machine for up/down reps (squat, push-up).
 * Angles: larger = more extended (standing / lockout).
 */
class RepMachine(
    private val topAngle: Float,
    private val bottomAngle: Float,
    private val minRepMs: Long = 450L,
) {
    var phase: MovementPhase = MovementPhase.IDLE
        private set
    var reps: Int = 0
        private set
    private var lastRepAt: Long = 0L
    private var reachedBottom: Boolean = false
    private var lowestAngle: Float = 180f

    fun onAngle(angle: Float, nowMs: Long): Boolean {
        var counted = false
        lowestAngle = minOf(lowestAngle, angle)
        when {
            angle >= topAngle -> {
                if (reachedBottom && nowMs - lastRepAt >= minRepMs) {
                    reps += 1
                    lastRepAt = nowMs
                    counted = true
                }
                reachedBottom = false
                lowestAngle = 180f
                phase = MovementPhase.TOP
            }
            angle <= bottomAngle -> {
                reachedBottom = true
                phase = MovementPhase.BOTTOM
            }
            phase == MovementPhase.TOP || phase == MovementPhase.IDLE -> {
                phase = MovementPhase.DESCENDING
            }
            phase == MovementPhase.BOTTOM -> {
                phase = MovementPhase.ASCENDING
            }
        }
        return counted
    }

    fun bouncedShallow(threshold: Float): Boolean {
        return phase == MovementPhase.ASCENDING && lowestAngle > threshold && reachedBottom.not()
    }

    val lastDepth: Float get() = lowestAngle

    fun reset() {
        phase = MovementPhase.IDLE
        reps = 0
        lastRepAt = 0L
        reachedBottom = false
        lowestAngle = 180f
    }
}
