package app.axis.coach.domain.model

data class Landmark(
    val x: Float,
    val y: Float,
    val z: Float = 0f,
    val visibility: Float = 1f,
)

data class PoseFrame(
    val landmarks: List<Landmark>,
    val imageWidth: Int,
    val imageHeight: Int,
    val timestampMs: Long,
) {
    fun getOrNull(index: Int): Landmark? = landmarks.getOrNull(index)

    fun visible(index: Int, threshold: Float = 0.55f): Boolean {
        val point = getOrNull(index) ?: return false
        return point.visibility >= threshold
    }
}

enum class MovementPhase { IDLE, TOP, DESCENDING, BOTTOM, ASCENDING, HOLDING }

enum class CueSeverity { NONE, GOOD, COACH, WARN }

data class FrameVerdict(
    val phase: MovementPhase,
    val reps: Int,
    val holdMillis: Long,
    val formScore: Int,
    val cue: String?,
    val severity: CueSeverity,
    val highlightedJoints: Set<Int>,
    val newRep: Boolean,
    val personDetected: Boolean,
    val kneeAngle: Float? = null,
    val elbowAngle: Float? = null,
    val alignmentError: Float? = null,
)

data class FinishedSession(
    val id: Long,
    val exercise: Exercise,
    val startedAt: Long,
    val endedAt: Long,
    val durationMs: Long,
    val reps: Int,
    val holdSeconds: Int,
    val formScore: Int,
    val topCue: String?,
)
