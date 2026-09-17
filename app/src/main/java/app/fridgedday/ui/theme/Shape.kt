package app.fridgedday

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * v2 "Modern Fresh Utility" shape system.
 *
 * - Action (scan pill, filter chip, segmented control, buttons): fully rounded at the call site
 * - extraSmall 16dp: the default shape of outlined fields, menus and snackbar, so the Add/Edit
 *   field stack lands in the 16-18dp band without every call site overriding it
 * - small 12dp: small status blocks and badges
 * - medium 18dp: inputs/search and the secondary controls that sit beside them
 * - large 22dp: grouped surfaces (settings groups, record summary)
 * - extraLarge 28dp: sheets and dialogs
 */
val FridgeDDayShapes = Shapes(
    extraSmall = RoundedCornerShape(16.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)
