package app.axis.coach.pose

object LandmarkIndex {
    const val NOSE = 0
    const val LEFT_SHOULDER = 11
    const val RIGHT_SHOULDER = 12
    const val LEFT_ELBOW = 13
    const val RIGHT_ELBOW = 14
    const val LEFT_WRIST = 15
    const val RIGHT_WRIST = 16
    const val LEFT_HIP = 23
    const val RIGHT_HIP = 24
    const val LEFT_KNEE = 25
    const val RIGHT_KNEE = 26
    const val LEFT_ANKLE = 27
    const val RIGHT_ANKLE = 28
    const val LEFT_HEEL = 29
    const val RIGHT_HEEL = 30
    const val LEFT_FOOT = 31
    const val RIGHT_FOOT = 32

    val CONNECTIONS: List<Pair<Int, Int>> = listOf(
        11 to 12,
        11 to 13,
        13 to 15,
        12 to 14,
        14 to 16,
        11 to 23,
        12 to 24,
        23 to 24,
        23 to 25,
        25 to 27,
        27 to 29,
        27 to 31,
        29 to 31,
        24 to 26,
        26 to 28,
        28 to 30,
        28 to 32,
        30 to 32,
        0 to 11,
        0 to 12,
    )
}
