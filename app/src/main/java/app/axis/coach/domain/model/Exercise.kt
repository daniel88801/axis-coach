package app.axis.coach.domain.model

enum class Exercise(
    val id: String,
    val title: String,
    val indexLabel: String,
    val blurb: String,
    val setupHint: String,
    val metricLabel: String,
) {
    SQUAT(
        id = "squat",
        title = "Squat",
        indexLabel = "01",
        blurb = "Depth, knees, torso",
        setupHint = "Stand side-on. Full body in frame. Phone ~2.5 m away.",
        metricLabel = "reps",
    ),
    PUSH_UP(
        id = "push_up",
        title = "Push-up",
        indexLabel = "02",
        blurb = "Elbows and a straight line",
        setupHint = "Side-on on the floor. Head to heels visible.",
        metricLabel = "reps",
    ),
    PLANK(
        id = "plank",
        title = "Plank",
        indexLabel = "03",
        blurb = "Hold a single line",
        setupHint = "Side-on. Shoulders, hips and ankles in one line.",
        metricLabel = "sec",
    );

    companion object {
        fun fromId(id: String): Exercise = entries.firstOrNull { it.id == id } ?: SQUAT
    }
}
