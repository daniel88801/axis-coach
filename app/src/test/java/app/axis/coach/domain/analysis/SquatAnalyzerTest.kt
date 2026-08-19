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
        val stand0 = analyzer.analyze(stand(0))
        analyzer.analyze(stand(40))
        analyzer.analyze(bottom(200))
        analyzer.analyze(bottom(280))
        analyzer.analyze(bottom(360))
        val top = analyzer.analyze(stand(900))
        assertTrue(stand0.personDetected)
        assertTrue(
            "knee=${top.kneeAngle} phase=${top.phase} reps=${top.reps} new=${top.newRep}",
            top.reps == 1 && top.newRep,
        )
        assertEquals(1, top.reps)
    }

    private fun stand(t: Long) = frame(
        hipX = 0.50f, hipY = 0.32f,
        kneeX = 0.50f, kneeY = 0.56f,
        ankleX = 0.50f, ankleY = 0.88f,
        t = t,
    )

    private fun bottom(t: Long) = frame(
        hipX = 0.50f, hipY = 0.48f,
        kneeX = 0.50f, kneeY = 0.70f,
        ankleX = 0.74f, ankleY = 0.70f,
        t = t,
    )

    private fun frame(
        hipX: Float,
        hipY: Float,
        kneeX: Float,
        kneeY: Float,
        ankleX: Float,
        ankleY: Float,
        t: Long,
    ): PoseFrame {
        val points = MutableList(33) { Landmark(0.5f, 0.5f, visibility = 0.1f) }
        fun set(i: Int, x: Float, y: Float) {
            points[i] = Landmark(x, y, visibility = 0.95f)
        }
        set(11, hipX, hipY - 0.16f)
        set(12, hipX + 0.02f, hipY - 0.16f)
        set(13, hipX + 0.05f, hipY - 0.06f)
        set(14, hipX + 0.07f, hipY - 0.06f)
        set(15, hipX + 0.07f, hipY + 0.02f)
        set(16, hipX + 0.09f, hipY + 0.02f)
        set(23, hipX, hipY)
        set(24, hipX + 0.02f, hipY)
        set(25, kneeX, kneeY)
        set(26, kneeX + 0.02f, kneeY)
        set(27, ankleX, ankleY)
        set(28, ankleX + 0.02f, ankleY)
        return PoseFrame(points, 480, 640, t)
    }
}
