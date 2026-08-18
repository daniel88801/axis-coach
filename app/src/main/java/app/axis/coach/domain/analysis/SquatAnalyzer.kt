package app.axis.coach.domain.analysis

import app.axis.coach.domain.model.CueSeverity
import app.axis.coach.domain.model.FrameVerdict
import app.axis.coach.domain.model.MovementPhase
import app.axis.coach.domain.model.PoseFrame
import app.axis.coach.pose.LandmarkIndex

class SquatAnalyzer : ExerciseAnalyzer {
    private val machine = RepMachine(topAngle = 158f, bottomAngle = 100f)
    private var rollingScore = 80f
    private var shallowAttempts = 0

    override fun analyze(frame: PoseFrame): FrameVerdict {
        val side = frame.pickSide()
        if (side == null || side.visibility < 0.45f) {
            return notDetected().copy(reps = machine.reps, formScore = rollingScore.toInt())
        }

        val kneeAngle = Geometry.angle(side.hip, side.knee, side.ankle)
        val hipAngle = Geometry.angle(side.shoulder, side.hip, side.knee)
        val counted = machine.onAngle(kneeAngle, frame.timestampMs)

        val highlights = mutableSetOf<Int>()
        var cue: String? = null
        var severity = CueSeverity.NONE
        var penalty = 0

        val kneeTravel = side.knee.x - side.ankle.x
        val shinLen = Geometry.distance(side.knee, side.ankle).coerceAtLeast(0.05f)
        val forwardRatio = kotlin.math.abs(kneeTravel) / shinLen

        if (machine.phase == MovementPhase.BOTTOM || machine.phase == MovementPhase.DESCENDING) {
            if (hipAngle < 55f) {
                cue = Cues.CHEST_UP
                severity = CueSeverity.COACH
                penalty += 18
                highlights += LandmarkIndex.LEFT_SHOULDER
                highlights += LandmarkIndex.RIGHT_SHOULDER
                highlights += LandmarkIndex.LEFT_HIP
                highlights += LandmarkIndex.RIGHT_HIP
            } else if (forwardRatio > 0.85f && kneeAngle < 150f) {
                cue = Cues.HIPS_BACK
                severity = CueSeverity.COACH
                penalty += 14
                highlights += LandmarkIndex.LEFT_KNEE
                highlights += LandmarkIndex.RIGHT_KNEE
            }
        }

        if (machine.phase == MovementPhase.ASCENDING && machine.lastDepth > 118f) {
            shallowAttempts += 1
            cue = Cues.DEEPER
            severity = CueSeverity.COACH
            penalty += 16
            highlights += LandmarkIndex.LEFT_HIP
            highlights += LandmarkIndex.RIGHT_HIP
        }

        val leftKnee = frame.getOrNull(LandmarkIndex.LEFT_KNEE)
        val rightKnee = frame.getOrNull(LandmarkIndex.RIGHT_KNEE)
        val leftAnkle = frame.getOrNull(LandmarkIndex.LEFT_ANKLE)
        val rightAnkle = frame.getOrNull(LandmarkIndex.RIGHT_ANKLE)
        val leftHip = frame.getOrNull(LandmarkIndex.LEFT_HIP)
        val rightHip = frame.getOrNull(LandmarkIndex.RIGHT_HIP)
        if (leftKnee != null && rightKnee != null && leftAnkle != null && rightAnkle != null &&
            leftHip != null && rightHip != null &&
            listOf(leftKnee, rightKnee, leftAnkle, rightAnkle).all { it.visibility > 0.55f }
        ) {
            val kneeWidth = kotlin.math.abs(leftKnee.x - rightKnee.x)
            val hipWidth = kotlin.math.abs(leftHip.x - rightHip.x).coerceAtLeast(0.04f)
            if (kneeWidth < hipWidth * 0.72f && kneeAngle < 150f) {
                cue = Cues.KNEES_OUT
                severity = CueSeverity.WARN
                penalty += 20
                highlights += LandmarkIndex.LEFT_KNEE
                highlights += LandmarkIndex.RIGHT_KNEE
            }
        }

        if (counted && cue == null) {
            cue = Cues.GOOD_REP
            severity = CueSeverity.GOOD
        }

        val instant = Geometry.clampScore(100 - penalty)
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
            kneeAngle = kneeAngle,
        )
    }

    override fun reset() {
        machine.reset()
        rollingScore = 80f
        shallowAttempts = 0
    }
}
