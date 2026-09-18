package app.fridgedday.ui.navigation

/**
 * Top-level destinations of the v2 shell, in bottom-bar order.
 *
 * The v2 shell has exactly these three destinations and they share the bottom bar with equal
 * widths. 스캔 is deliberately absent: it is a global shell action represented by [ShellAction] and
 * rendered by [ScanPill] above the navigation rather than a fourth tab.
 */
enum class TopLevelDestination(val label: String, val route: String) {
    TODAY("오늘", Destinations.HOME),
    LOG("기록", Destinations.RECORD),
    SETTINGS("설정", Destinations.SETTINGS);

    companion object {
        /** Top-level destinations in bottom-bar order. */
        val topLevel: List<TopLevelDestination> get() = entries
    }
}

/** Shell actions that are not top-level destinations. */
enum class ShellAction(val label: String) {
    SCAN("스캔")
}
