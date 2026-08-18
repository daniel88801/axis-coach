package app.axis.coach.domain.analysis

import app.axis.coach.domain.model.FrameVerdict
import app.axis.coach.domain.model.Landmark
import app.axis.coach.domain.model.PoseFrame
import app.axis.coach.pose.LandmarkIndex

interface ExerciseAnalyzer {
    fun analyze(frame: PoseFrame): FrameVerdict
    fun reset()
}

internal fun PoseFrame.pickSide(): BodySide? {
    val left = side(
        shoulder = getOrNull(LandmarkIndex.LEFT_SHOULDER),
        elbow = getOrNull(LandmarkIndex.LEFT_ELBOW),
        wrist = getOrNull(LandmarkIndex.LEFT_WRIST),
        hip = getOrNull(LandmarkIndex.LEFT_HIP),
        knee = getOrNull(LandmarkIndex.LEFT_KNEE),
        ankle = getOrNull(LandmarkIndex.LEFT_ANKLE),
    )
    val right = side(
        shoulder = getOrNull(LandmarkIndex.RIGHT_SHOULDER),
        elbow = getOrNull(LandmarkIndex.RIGHT_ELBOW),
        wrist = getOrNull(LandmarkIndex.RIGHT_WRIST),
        hip = getOrNull(LandmarkIndex.RIGHT_HIP),
        knee = getOrNull(LandmarkIndex.RIGHT_KNEE),
        ankle = getOrNull(LandmarkIndex.RIGHT_ANKLE),
    )
    return listOfNotNull(left, right).maxByOrNull { it.visibility }
}

private fun side(
    shoulder: Landmark?,
    elbow: Landmark?,
    wrist: Landmark?,
    hip: Landmark?,
    knee: Landmark?,
    ankle: Landmark?,
): BodySide? {
    if (shoulder == null || elbow == null || wrist == null || hip == null || knee == null || ankle == null) {
        return null
    }
    val visibility = listOf(
        shoulder.visibility,
        elbow.visibility,
        wrist.visibility,
        hip.visibility,
        knee.visibility,
        ankle.visibility,
    ).average().toFloat()
    return BodySide(shoulder, elbow, wrist, hip, knee, ankle, visibility)
}

internal fun notDetected(): FrameVerdict = FrameVerdict(
    phase = app.axis.coach.domain.model.MovementPhase.IDLE,
    reps = 0,
    holdMillis = 0,
    formScore = 0,
    cue = Cues.FULL_BODY,
    severity = app.axis.coach.domain.model.CueSeverity.WARN,
    highlightedJoints = emptySet(),
    newRep = false,
    personDetected = false,
)
