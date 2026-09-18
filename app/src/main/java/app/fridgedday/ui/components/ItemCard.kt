package app.fridgedday.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.fridgedday.data.db.entity.ItemEntity
import app.fridgedday.data.db.entity.StorageLocation

private val FoodRowMinHeight = 72.dp
private val FoodRowMarkSize = 52.dp

/** Food art, identity, safe metadata, compact D-Day badge, and detail chevron. */
@Composable
fun FoodRow(
    item: ItemEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    markSize: Dp = FoodRowMarkSize
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .heightIn(min = FoodRowMinHeight)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FoodCategoryMark(
            group = resolveFoodGroup(item.category, item.name),
            size = markSize
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = foodMetadata(item.location, item.category, item.name),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        DDayTile(expiryDate = item.expiryDate)
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Storage plus the persisted category, or a conservative broad group inferred from the name. */
internal fun foodMetadata(
    location: StorageLocation,
    category: String?,
    name: String
): String {
    val categoryLabel = category?.trim()?.takeIf { it.isNotEmpty() }
        ?: foodGroupLabel(resolveFoodGroup(category = null, name = name))
    return listOfNotNull(locationLabel(location), categoryLabel).joinToString(" · ")
}

private fun foodGroupLabel(group: FoodGroup): String? = when (group) {
    FoodGroup.VEGETABLES -> "채소"
    FoodGroup.FRUIT -> "과일"
    FoodGroup.MEAT -> "육류"
    FoodGroup.DAIRY -> "유제품"
    FoodGroup.EGG -> "달걀"
    FoodGroup.DRINK -> "음료"
    FoodGroup.SEAFOOD -> "수산물"
    FoodGroup.GRAIN -> "곡류·빵"
    FoodGroup.PREPARED -> "조리식품"
    FoodGroup.PROCESSED -> "가공식품"
    FoodGroup.GENERIC -> null
}

internal fun quantityLabel(item: ItemEntity): String? {
    val quantity = item.quantity ?: return null
    return if (quantity % 1f == 0f) {
        "${quantity.toInt()}${item.unit ?: ""}"
    } else {
        "$quantity${item.unit ?: ""}"
    }
}

private fun locationLabel(location: StorageLocation): String = when (location) {
    StorageLocation.FRIDGE -> "냉장"
    StorageLocation.FREEZER -> "냉동"
    StorageLocation.PANTRY -> "실온"
}
