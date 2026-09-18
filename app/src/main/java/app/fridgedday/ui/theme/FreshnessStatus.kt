package app.fridgedday.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import app.fridgedday.data.db.entity.StorageLocation
import app.fridgedday.util.DDayState

/**
 * Single source of truth for freshness status colors.
 * Uses MaterialTheme tonal containers so semantic contrast comes from the active theme roles,
 * never from hardcoded ARGB in cards or badges. Shipping v2 is light-only (D-028).
 */
@Composable
fun DDayState.containerColor(): Color = when (this) {
    DDayState.SAFE -> MaterialTheme.colorScheme.surfaceVariant
    DDayState.WARNING -> MaterialTheme.colorScheme.tertiaryContainer
    DDayState.EXPIRED -> MaterialTheme.colorScheme.errorContainer
}

@Composable
fun DDayState.contentColor(): Color = when (this) {
    DDayState.SAFE -> MaterialTheme.colorScheme.onSurfaceVariant
    DDayState.WARNING -> MaterialTheme.colorScheme.onTertiaryContainer
    DDayState.EXPIRED -> MaterialTheme.colorScheme.onErrorContainer
}

/**
 * Accessible location colors drawn from theme roles, not hardcoded blue/orange.
 */
@Composable
fun StorageLocation.containerColor(): Color = when (this) {
    StorageLocation.FRIDGE -> MaterialTheme.colorScheme.primaryContainer
    StorageLocation.FREEZER -> MaterialTheme.colorScheme.secondaryContainer
    StorageLocation.PANTRY -> MaterialTheme.colorScheme.tertiaryContainer
}

@Composable
fun StorageLocation.contentColor(): Color = when (this) {
    StorageLocation.FRIDGE -> MaterialTheme.colorScheme.onPrimaryContainer
    StorageLocation.FREEZER -> MaterialTheme.colorScheme.onSecondaryContainer
    StorageLocation.PANTRY -> MaterialTheme.colorScheme.onTertiaryContainer
}

@Composable
fun StorageLocation.solidColor(): Color = when (this) {
    StorageLocation.FRIDGE -> MaterialTheme.colorScheme.primary
    StorageLocation.FREEZER -> MaterialTheme.colorScheme.secondary
    StorageLocation.PANTRY -> MaterialTheme.colorScheme.tertiary
}
