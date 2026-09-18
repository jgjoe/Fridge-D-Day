package app.fridgedday.widget

import app.fridgedday.data.db.entity.ItemEntity
import app.fridgedday.data.db.entity.StorageLocation
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * 위젯이 고르는 행과 문구가 Today와 같은 임박 판정을 따르는지 확인한다.
 *
 * 주입하는 [today]는 실제 기기 시각과 멀리 떨어진 날짜라, 구현이 벽시계에 기대면 이 테스트에서
 * 바로 드러난다.
 */
class WidgetPresentationTest {

    private val today = LocalDate.of(2026, 1, 5)

    private fun item(
        id: Long,
        name: String,
        daysFromToday: Long,
        archived: Boolean = false,
        consumedDate: LocalDate? = null
    ) = ItemEntity(
        id = id,
        name = name,
        location = StorageLocation.FRIDGE,
        expiryDate = today.plusDays(daysFromToday),
        isArchived = archived,
        consumedDate = consumedDate
    )

    @Test
    fun `near-expiry items are listed in expiry order`() {
        val rows = buildWidgetRows(
            listOf(item(3, "요거트", 6), item(1, "우유", 0), item(2, "계란", 2)),
            today
        )

        assertEquals(listOf("우유", "계란", "요거트"), rows.map { it.name })
        assertEquals(listOf(1L, 2L, 3L), rows.map { it.id })
    }

    @Test
    fun `items with more than a week left never reach the widget`() {
        val rows = buildWidgetRows(listOf(item(1, "여유", 8), item(2, "임박", 7)), today)

        assertEquals(listOf("임박"), rows.map { it.name })
    }

    @Test
    fun `archived items never reach the widget`() {
        val rows = buildWidgetRows(
            listOf(
                item(1, "소비한우유", 1, archived = true, consumedDate = today),
                item(2, "지운계란", 1, archived = true),
                item(3, "남은요거트", 1)
            ),
            today
        )

        assertEquals(listOf("남은요거트"), rows.map { it.name })
    }

    @Test
    fun `urgency is readable from the text alone`() {
        val rows = buildWidgetRows(
            listOf(item(1, "지난두부", -2), item(2, "오늘우유", 0), item(3, "내일계란", 1)),
            today
        )

        assertEquals(listOf("기한 지남", "D-Day", "D-1"), rows.map { it.dDayLabel })
    }

    @Test
    fun `only the most urgent few rows are shown`() {
        val rows = buildWidgetRows((1L..5L).map { item(it, "식품$it", it) }, today)

        assertEquals(WIDGET_ROW_LIMIT, rows.size)
        assertEquals(listOf("식품1", "식품2", "식품3"), rows.map { it.name })
    }

    @Test
    fun `items sharing an expiry date keep a stable order`() {
        val rows = buildWidgetRows(listOf(item(9, "나중", 3), item(4, "먼저", 3)), today)

        assertEquals(listOf("먼저", "나중"), rows.map { it.name })
    }

    @Test
    fun `names are shortened only beyond the display width`() {
        val exact = buildWidgetRows(listOf(item(1, "정확히열두글자이름입니다", 1)), today)
        val korean = buildWidgetRows(listOf(item(2, "아주 긴 이름의 냉장 보관 식품 항목", 1)), today)
        // 경계가 이모지 한가운데에 걸려도 코드 포인트 기준으로 잘라 반쪽 문자를 만들지 않는다.
        val emoji = buildWidgetRows(listOf(item(3, "가나다라마바사아자차카🍎타", 1)), today)

        assertEquals("정확히열두글자이름입니다", exact.single().name)
        assertEquals("아주 긴 이름의 냉장…", korean.single().name)
        assertEquals("가나다라마바사아자차카🍎…", emoji.single().name)
    }
}
