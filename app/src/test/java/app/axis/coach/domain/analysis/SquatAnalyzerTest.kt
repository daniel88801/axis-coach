package app.axis.coach.domain.analysis

import app.axis.coach.domain.model.Landmark
import app.axis.coach.domain.model.PoseFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SquatAnalyzerTest {
    @Test
    fun standingToBottomToStandCountsRep() {
        val analyzer = SquatAnalyzer()
        analyzer.analyze(pose(kneeY = 0.45f, hipY = 0.30f, t = 0))
        analyzer.analyze(pose(kneeY = 0.58f, hipY = 0.52f, t = 80))
        val bottom = analyzer.analyze(pose(kneeY = 0.70f, hipY = 0.68f, t = 160))
        val top = analyzer.analyze(pose(kneeY = 0.45f, hipY = 0.30f, t = 800))
        assertTrue(bottom.personDetected)
        assertEquals(1, top.reps)
        assertTrue(top.newRep)
    }

    private fun pose(kneeY: Float, hipY: Float, t: Long): PoseFrame {
        val points = MutableList(33) { Landmark(0.5f, 0.5f, visibility = 0.1f) }
        fun set(i: Int, x: Float, y: Float) {
            points[i] = Landmark(x, y, visibility = 0.95f)
        }
        set(11, 0.50f, 0.18f)
        set(12, 0.50f, 0.18f)
        set(13, 0.58f, 0.28f)
        set(14, 0.58f, 0.28f)
        set(15, 0.62f, 0.36f)
        set(16, 0.62f, 0.36f)
        set(23, 0.50f, hipY)
        set(24, 0.50f, hipY)
        set(25, 0.52f, kneeY)
        set(26, 0.52f, kneeY)
        set(27, 0.50f, 0.88f)
        set(28, 0.50f, 0.88f)
        return PoseFrame(points, 480, 640, t)
    }
}
