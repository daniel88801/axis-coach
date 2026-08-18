package app.axis.coach.domain.analysis

import app.axis.coach.domain.model.CueSeverity
import app.axis.coach.domain.model.FrameVerdict
import app.axis.coach.domain.model.MovementPhase
import app.axis.coach.domain.model.PoseFrame
import app.axis.coach.pose.LandmarkIndex

class PushUpAnalyzer : ExerciseAnalyzer {
    private val machine = RepMachine(topAngle = 155f, bottomAngle = 95f)
    private var rollingScore = 80f

    override fun analyze(frame: PoseFrame): FrameVerdict {
        val side = frame.pickSide()
        if (side == null || side.visibility < 0.45f) {
            return notDetected().copy(reps = machine.reps, formScore = rollingScore.toInt())
        }

        val elbowAngle = Geometry.angle(side.shoulder, side.elbow, side.wrist)
        val alignment = Geometry.deviationFromStraight(side.shoulder, side.hip, side.ankle)
        val counted = machine.onAngle(elbowAngle, frame.timestampMs)

        val highlights = mutableSetOf<Int>()
        var cue: String? = null
        var severity = CueSeverity.NONE
        var penalty = 0

        if (alignment > 22f) {
            val hipsHigh = side.hip.y < (side.shoulder.y + side.ankle.y) / 2f
            if (hipsHigh) {
                cue = Cues.HIPS_DOWN
            } else {
                cue = Cues.HIPS_UP
            }
            severity = CueSeverity.COACH
            penalty += 18
            highlights += LandmarkIndex.LEFT_HIP
            highlights += LandmarkIndex.RIGHT_HIP
        }

        if (machine.phase == MovementPhase.BOTTOM && elbowAngle > 105f) {
            cue = Cues.LOWER
            severity = CueSeverity.COACH
            penalty += 12
            highlights += LandmarkIndex.LEFT_ELBOW
            highlights += LandmarkIndex.RIGHT_ELBOW
        }

        if (machine.phase == MovementPhase.ASCENDING && machine.lastDepth > 115f) {
            cue = Cues.LOWER
            severity = CueSeverity.COACH
            penalty += 14
        }

        if (counted && cue == null) {
            cue = Cues.GOOD_REP
            severity = CueSeverity.GOOD
        }

        val instant = Geometry.clampScore(100 - penalty - (alignment / 2f).toInt())
        rollingScore = Geometry.lerp(rollingScore, instant.toFloat(), 0.18f)

        return FrameVerdict(
            phase = machine.phase,
            reps = machine.reps,
            holdMillis = 0,
            formScore = rollingScore.toInt(),
            cue = cue,
            severity = severity,
            highlightedJoints = highlights,
            newRep = counted,
            personDetected = true,
            elbowAngle = elbowAngle,
            alignmentError = alignment,
        )
    }

    override fun reset() {
        machine.reset()
        rollingScore = 80f
    }
}
