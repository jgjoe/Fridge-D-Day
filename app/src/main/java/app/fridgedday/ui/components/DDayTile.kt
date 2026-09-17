package app.fridgedday.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.fridgedday.util.DDayState
import app.fridgedday.util.DateUtils
import app.fridgedday.util.getDDayState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

internal const val DDayTileTag = "d-day-tile"

/** Compact relative-date-only status badge used by the demo list. */
@Composable
fun DDayTile(
    expiryDate: LocalDate,
    modifier: Modifier = Modifier
) {
    val state = getDDayState(DateUtils.daysUntil(expiryDate))
    val containerColor = when (state) {
        DDayState.SAFE -> MaterialTheme.colorScheme.primaryContainer
        DDayState.WARNING -> MaterialTheme.colorScheme.tertiaryContainer
        DDayState.EXPIRED -> MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = when (state) {
        DDayState.SAFE -> MaterialTheme.colorScheme.primary
        DDayState.WARNING -> MaterialTheme.colorScheme.tertiary
        DDayState.EXPIRED -> MaterialTheme.colorScheme.error
    }
    Surface(
        modifier = modifier.testTag(DDayTileTag),
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier
                .widthIn(min = 48.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = DateUtils.formatDDay(expiryDate),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}

internal fun absoluteMonthDay(date: LocalDate): String = "${date.monthValue}월 ${date.dayOfMonth}일"

internal fun todayHeroDateLabel(today: LocalDate): String = today.format(heroDateFormatter)

private val heroDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN)
