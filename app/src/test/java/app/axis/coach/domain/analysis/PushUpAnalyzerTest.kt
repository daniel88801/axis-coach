package app.axis.coach.domain.analysis

import app.axis.coach.domain.model.Landmark
import app.axis.coach.domain.model.PoseFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PushUpAnalyzerTest {
    @Test
    fun fullPushUpCountsOneRep() {
        val analyzer = PushUpAnalyzer()
        var last = analyzer.analyze(pushUp(top = true, t = 0))
        repeat(10) { i ->
            last = analyzer.analyze(pushUp(top = true, t = 40L * (i + 1)))
        }
        assertFalse(last.newRep)
        assertEquals(0, last.reps)

        repeat(8) { i ->
            last = analyzer.analyze(pushUp(top = false, t = 500L + 40L * i))
        }
        assertEquals(0, last.reps)

        repeat(10) { i ->
            last = analyzer.analyze(pushUp(top = true, t = 1100L + 40L * i))
        }
        assertEquals(1, last.reps)
        assertTrue(last.personDetected)
    }

    @Test
    fun shallowPulseDoesNotCount() {
        val analyzer = PushUpAnalyzer()
        repeat(10) { i ->
            analyzer.analyze(pushUp(top = true, t = 40L * i))
        }
        repeat(6) { i ->
            analyzer.analyze(pushUp(top = true, shoulderY = 0.44f, t = 500L + 40L * i))
        }
        val back = analyzer.analyze(pushUp(top = true, t = 900))
        assertEquals(0, back.reps)
        assertFalse(back.newRep)
    }

    private fun pushUp(
        top: Boolean,
        t: Long,
        shoulderY: Float? = null,
    ): PoseFrame {
        val points = MutableList(33) { Landmark(0.5f, 0.5f, visibility = 0.1f) }
        fun set(i: Int, x: Float, y: Float) {
            points[i] = Landmark(x, y, visibility = 0.95f)
        }
        val sy = shoulderY ?: if (top) 0.40f else 0.58f
        val wristY = 0.72f
        val wristX = 0.36f
        val elbowX = if (top) 0.36f else 0.22f
        val elbowY = if (top) 0.56f else 0.64f
        val hipY = if (top) 0.42f else 0.58f
        set(11, 0.36f, sy)
        set(12, 0.38f, sy)
        set(13, elbowX, elbowY)
        set(14, elbowX + 0.02f, elbowY)
        set(15, wristX, wristY)
        set(16, wristX + 0.02f, wristY)
        set(23, 0.58f, hipY)
        set(24, 0.60f, hipY)
        set(25, 0.70f, hipY + 0.02f)
        set(26, 0.72f, hipY + 0.02f)
        set(27, 0.82f, 0.44f)
        set(28, 0.84f, 0.44f)
        return PoseFrame(points, 480, 640, t)
    }
}
