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
        title = "Присед",
        indexLabel = "01",
        blurb = "Глубина, колени, корпус",
        setupHint = "Встань боком. Всё тело в кадре. Телефон примерно в 2,5 м.",
        metricLabel = "повт",
    ),
    PUSH_UP(
        id = "push_up",
        title = "Отжимания",
        indexLabel = "02",
        blurb = "Локти и прямая линия",
        setupHint = "Боком на полу. От головы до пяток в кадре.",
        metricLabel = "повт",
    ),
    PLANK(
        id = "plank",
        title = "Планка",
        indexLabel = "03",
        blurb = "Держи одну линию",
        setupHint = "Боком. Плечи, таз и лодыжки на одной линии.",
        metricLabel = "сек",
    );

    companion object {
        fun fromId(id: String): Exercise = entries.firstOrNull { it.id == id } ?: SQUAT
    }
}
