package app.axis.coach.domain.analysis

import app.axis.coach.domain.model.Landmark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeometryTest {
    @Test
    fun rightAngleIsNinety() {
        val angle = Geometry.angle(
            Landmark(0f, 1f),
            Landmark(0f, 0f),
            Landmark(1f, 0f),
        )
        assertEquals(90f, angle, 0.01f)
    }

    @Test
    fun straightLineIsOneEighty() {
        val angle = Geometry.angle(
            Landmark(0f, 0f),
            Landmark(0.5f, 0f),
            Landmark(1f, 0f),
        )
        assertEquals(180f, angle, 0.01f)
    }

    @Test
    fun deviationFromStraightIsZeroOnLine() {
        val error = Geometry.deviationFromStraight(
            Landmark(0f, 0f),
            Landmark(0.5f, 0f),
            Landmark(1f, 0f),
        )
        assertTrue(error < 0.5f)
    }
}
