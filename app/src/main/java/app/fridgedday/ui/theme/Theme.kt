package app.fridgedday

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import app.fridgedday.data.pref.ThemeMode

/**
 * v2 "Calm Premium Utility" theme entry point.
 *
 * The implementation lives under `ui/theme/` but intentionally keeps the `app.fridgedday`
 * package: the public symbol `app.fridgedday.FridgeDDayTheme` is consumed by instrumentation
 * tests outside this slice's write scope, so the fully qualified call shape must not change.
 *
 * D-028: the product ships light-only. The approved light scheme renders regardless of the
 * Android system night mode or the persisted `theme_mode` value. The parameter remains only to
 * preserve the existing activity call boundary; it never selects a dark scheme.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun FridgeDDayTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = true
                isAppearanceLightNavigationBars = true
            }
        }
    }
    MaterialTheme(
        colorScheme = FridgeDDayLightColorScheme,
        typography = FridgeDDayTypography,
        shapes = FridgeDDayShapes,
        content = content
    )
}
