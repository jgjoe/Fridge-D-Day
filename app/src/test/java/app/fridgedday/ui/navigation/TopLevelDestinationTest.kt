package app.fridgedday.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class TopLevelDestinationTest {

    @Test
    fun `shell exposes exactly today log and settings as top-level destinations`() {
        assertEquals(
            listOf("오늘", "기록", "설정"),
            TopLevelDestination.topLevel.map { it.label }
        )
    }

    @Test
    fun `scan is not a top-level destination`() {
        assertFalse(TopLevelDestination.topLevel.any { it.label == ShellAction.SCAN.label })
    }

    @Test
    fun `every top-level destination owns a distinct route`() {
        val routes = TopLevelDestination.topLevel.map { it.route }

        assertEquals(routes.size, routes.toSet().size)
        assertEquals(
            listOf(Destinations.HOME, Destinations.RECORD, Destinations.SETTINGS),
            routes
        )
    }
}
