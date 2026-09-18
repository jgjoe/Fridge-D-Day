package app.fridgedday.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class DestinationsTest {

    @Test
    fun `task routes carry the ISO date as the only argument`() {
        val date = LocalDate.of(2026, 8, 20)

        assertEquals(date, parseIsoDate(Destinations.ocrConfirmRoute(date).substringAfterLast('/')))
        assertEquals(date, parseIsoDate(Destinations.registerRoute(date).substringAfterLast('/')))
    }

    @Test
    fun `malformed date arguments are rejected instead of navigating with garbage`() {
        assertNull(parseIsoDate(null))
        assertNull(parseIsoDate("2026-8-20"))
        assertNull(parseIsoDate("not-a-date"))
    }
}
