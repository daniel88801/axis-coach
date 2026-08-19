package app.axis.coach.domain.analysis

import app.axis.coach.domain.model.CueSeverity
import app.axis.coach.domain.model.FrameVerdict
import app.axis.coach.domain.model.MovementPhase
import app.axis.coach.domain.model.PoseFrame
import app.axis.coach.pose.LandmarkIndex

class PushUpAnalyzer : ExerciseAnalyzer {
    private val tracker = PushUpTracker()
    private var rollingScore = 80f

    override fun analyze(frame: PoseFrame): FrameVerdict {
        val counted = tracker.onFrame(frame)
        val feat = tracker.lastFeat
        val side = frame.pickSide()

        if (feat == null || side == null || side.visibility < 0.35f) {
            return notDetected().copy(reps = tracker.reps, formScore = rollingScore.toInt())
        }

        val highlights = mutableSetOf<Int>()
        var cue: String? = null
        var severity = CueSeverity.NONE
        var penalty = 0

        if (!feat.wristBelow) {
            cue = "Упор лёжа — ладони ниже плеч"
            severity = CueSeverity.WARN
            penalty += 10
        }

        if (feat.align > 22f) {
            val hipsHigh = side.hip.y < (side.shoulder.y + side.ankle.y) / 2f
            cue = if (hipsHigh) Cues.HIPS_DOWN else Cues.HIPS_UP
            severity = CueSeverity.COACH
            penalty += 18
            highlights += LandmarkIndex.LEFT_HIP
            highlights += LandmarkIndex.RIGHT_HIP
        }

        if (tracker.phase == MovementPhase.BOTTOM && feat.elbow > 105f) {
            cue = Cues.LOWER
            severity = CueSeverity.COACH
            penalty += 12
            highlights += LandmarkIndex.LEFT_ELBOW
            highlights += LandmarkIndex.RIGHT_ELBOW
        }

        if (tracker.phase == MovementPhase.ASCENDING && tracker.lastDepth > 115f) {
            cue = Cues.LOWER
            severity = CueSeverity.COACH
            penalty += 14
        }

        if (counted && cue == null) {
            cue = Cues.GOOD_REP
            severity = CueSeverity.GOOD
        }

        val instant = Geometry.clampScore(100 - penalty - (feat.align / 2f).toInt())
        rollingScore = Geometry.lerp(rollingScore, instant.toFloat(), 0.18f)

        return FrameVerdict(
            phase = tracker.phase,
            reps = tracker.reps,
            holdMillis = 0,
            formScore = rollingScore.toInt(),
            cue = cue,
            severity = severity,
            highlightedJoints = highlights,
            newRep = counted,
            personDetected = true,
            elbowAngle = feat.elbow,
            alignmentError = feat.align,
        )
    }

    override fun reset() {
        tracker.reset()
        rollingScore = 80f
    }
}
