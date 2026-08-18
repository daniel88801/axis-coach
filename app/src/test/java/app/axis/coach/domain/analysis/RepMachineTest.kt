package app.axis.coach.domain.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RepMachineTest {
    @Test
    fun countsOneRepThroughBottom() {
        val machine = RepMachine(topAngle = 160f, bottomAngle = 100f, minRepMs = 0)
        assertFalse(machine.onAngle(170f, 0))
        assertFalse(machine.onAngle(130f, 50))
        assertFalse(machine.onAngle(90f, 100))
        assertTrue(machine.onAngle(165f, 200))
        assertEquals(1, machine.reps)
    }

    @Test
    fun doesNotCountWithoutBottom() {
        val machine = RepMachine(topAngle = 160f, bottomAngle = 100f, minRepMs = 0)
        machine.onAngle(170f, 0)
        machine.onAngle(140f, 50)
        assertFalse(machine.onAngle(165f, 100))
        assertEquals(0, machine.reps)
    }
}
