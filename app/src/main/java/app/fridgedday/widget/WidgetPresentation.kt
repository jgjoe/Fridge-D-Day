package app.fridgedday.widget

import app.fridgedday.data.db.entity.ItemEntity
import app.fridgedday.util.DDayState
import app.fridgedday.util.getDDayState
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** 위젯이 한 번에 보여주는 행 수. 4x2 최소 크기에서 잘리지 않는 상한이다. */
internal const val WIDGET_ROW_LIMIT = 3

/** 긴 이름이 D-Day 문구를 밀어내지 않도록 줄이는 표시 길이(코드 포인트 기준). */
internal const val WIDGET_NAME_MAX_CODE_POINTS = 12

/** 기한이 지난 항목의 문구. 기록 화면과 같은 표현을 쓴다. */
private const val OVERDUE_LABEL = "기한 지남"

/** 위젯 한 행이 실제로 그리는 값. Compose 계층은 이 값만 그린다. */
internal data class WidgetRow(
    val id: Long,
    val name: String,
    val dDayLabel: String
)

/**
 * 위젯에 넣을 활성·임박 항목만 골라 유통기한 순으로 자른다.
 *
 * Today 화면과 같은 [getDDayState] 판정을 쓰므로 여유 있는 항목(8일 이상)은 위젯에 나오지 않는다.
 * 아카이브·소비 완료 항목은 DAO 쿼리에서 이미 빠지지만 위젯 계약으로 여기서 한 번 더 막는다.
 * 같은 유통기한은 id 순으로 고정해 표시 순서가 흔들리지 않는다.
 */
internal fun buildWidgetRows(
    items: List<ItemEntity>,
    today: LocalDate,
    limit: Int = WIDGET_ROW_LIMIT
): List<WidgetRow> = items
    .asSequence()
    .filterNot { it.isArchived }
    .map { item -> item to ChronoUnit.DAYS.between(today, item.expiryDate) }
    .filterNot { (_, daysUntil) -> getDDayState(daysUntil) == DDayState.SAFE }
    .sortedWith(compareBy({ (item, _) -> item.expiryDate }, { (item, _) -> item.id }))
    .take(limit)
    .map { (item, daysUntil) -> WidgetRow(item.id, shortenName(item.name), dDayLabel(daysUntil)) }
    .toList()

/** D-Day 문구. 기한이 지난 항목만 숫자 대신 명시적인 문구로 바꾼다. */
private fun dDayLabel(daysUntil: Long): String = when {
    daysUntil < 0 -> OVERDUE_LABEL
    daysUntil == 0L -> "D-Day"
    else -> "D-$daysUntil"
}

/** 코드 포인트 기준으로 잘라 이모지가 반쪽으로 잘리지 않게 한다. */
private fun shortenName(name: String): String {
    if (name.codePointCount(0, name.length) <= WIDGET_NAME_MAX_CODE_POINTS) return name
    val end = name.offsetByCodePoints(0, WIDGET_NAME_MAX_CODE_POINTS)
    return name.substring(0, end).trimEnd() + "…"
}
