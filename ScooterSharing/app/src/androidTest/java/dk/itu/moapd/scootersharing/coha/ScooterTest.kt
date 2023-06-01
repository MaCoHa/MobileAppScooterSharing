package dk.itu.moapd.scootersharing.coha


import androidx.test.ext.junit.runners.AndroidJUnit4
import dk.itu.moapd.scootersharing.coha.models.Scooter
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScooterTest {

    @Test
    fun testToString() {
        val scooter = Scooter("Scooter 1", "Location 1", 1620136265000, 37.7749, -122.4194)
        val expected = "[Scooter] Scooter 1 is placed at Location 1 at time 3:51:05 PM"
        assertEquals(expected, scooter.toString())
    }

    @Test
    fun testTimeToString() {
        val scooter = Scooter("Scooter 1", "Location 1", 1620136265000, 37.7749, -122.4194)
        val expected = "3:51:05 PM"
        assertEquals(expected, scooter.time_to_String())
    }

    @Test
    fun testDateToString() {
        val scooter = Scooter("Scooter 1", "Location 1", 1620136265000, 37.7749, -122.4194)
        val expected = "15:51 2021-05-04"
        assertEquals(expected, scooter.date_to_String())
    }

}
