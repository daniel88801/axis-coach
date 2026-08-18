package app.axis.coach.domain.analysis

import app.axis.coach.domain.model.CueSeverity
import app.axis.coach.domain.model.FrameVerdict
import app.axis.coach.domain.model.MovementPhase
import app.axis.coach.domain.model.PoseFrame
import app.axis.coach.pose.LandmarkIndex

class PlankAnalyzer : ExerciseAnalyzer {
    private var holdMillis = 0L
    private var lastTimestamp: Long? = null
    private var rollingScore = 80f

    override fun analyze(frame: PoseFrame): FrameVerdict {
        val side = frame.pickSide()
        if (side == null || side.visibility < 0.45f) {
            lastTimestamp = frame.timestampMs
            return notDetected().copy(
                holdMillis = holdMillis,
                formScore = rollingScore.toInt(),
            )
        }

        val alignment = Geometry.deviationFromStraight(side.shoulder, side.hip, side.ankle)
        val previous = lastTimestamp
        lastTimestamp = frame.timestampMs
        val dt = if (previous == null) 0L else (frame.timestampMs - previous).coerceIn(0L, 80L)

        val aligned = alignment <= 18f
        if (aligned) {
            holdMillis += dt
        }

        val highlights = mutableSetOf<Int>()
        var cue: String? = null
        var severity = CueSeverity.NONE
        var penalty = 0

        if (!aligned) {
            val midY = (side.shoulder.y + side.ankle.y) / 2f
            if (side.hip.y > midY) {
                cue = Cues.HIPS_UP
            } else {
                cue = Cues.HIPS_DOWN
            }
            severity = CueSeverity.COACH
            penalty += 22
            highlights += LandmarkIndex.LEFT_HIP
            highlights += LandmarkIndex.RIGHT_HIP
        } else if (alignment > 10f) {
            cue = Cues.LOCK_CORE
            severity = CueSeverity.COACH
            penalty += 8
        }

        val instant = Geometry.clampScore(100 - penalty - (alignment * 1.4f).toInt())
        rollingScore = Geometry.lerp(rollingScore, instant.toFloat(), 0.12f)

        return FrameVerdict(
            phase = if (aligned) MovementPhase.HOLDING else MovementPhase.IDLE,
            reps = 0,
            holdMillis = holdMillis,
            formScore = rollingScore.toInt(),
            cue = cue,
            severity = severity,
            highlightedJoints = highlights,
            newRep = false,
            personDetected = true,
            alignmentError = alignment,
        )
    }

    override fun reset() {
        holdMillis = 0L
        lastTimestamp = null
        rollingScore = 80f
    }
}
