package app.fridgedday.ui.statistics

import app.fridgedday.data.db.entity.ItemEntity
import java.time.LocalDate
import java.time.YearMonth

/**
 * 기록 화면 상단 요약. 표시 순서도 이 순서다.
 */
data class RecordSummary(
    /** 이번 달 소비 완료. consumedDate가 이번 달인 항목 수. */
    val consumedThisMonth: Int,
    /**
     * 만료. 소비되지 않은 채 기한이 지난 항목 수.
     *
     * 기한 지남 사건과 같은 [hasPassedExpiry] 판정을 쓰되 소비된 항목은 제외한다. 소비된 항목은
     * 더 이상 만료 상태가 아니기 때문이다.
     */
    val expired: Int,
    /** 현재 관리 중. 아카이브되지 않은 항목 수. */
    val active: Int
)

/** 기록 타임라인의 사건 종류. [label]이 화면에 그대로 나온다. */
enum class RecordEventType(val label: String) {
    REGISTERED("등록"),
    CONSUMED("소비 완료"),
    EXPIRED("기한 지남")
}

/** ItemEntity의 날짜·상태에서만 파생한 사건 하나. 별도 event log 테이블은 없다. */
data class RecordEvent(
    val date: LocalDate,
    val type: RecordEventType,
    val item: ItemEntity
)

/** 같은 날짜의 사건 묶음. */
data class RecordDay(
    val date: LocalDate,
    val events: List<RecordEvent>
)

/**
 * 항목이 기한을 실제로 지났는지 판정한다.
 *
 * - 소비된 항목: 기한이 소비일보다 앞선 경우(= 기한 후 소비)만 참이다. 기한 당일이나 그 전에
 *   소비한 항목은 기한을 지났다고 보지 않는다.
 * - 소비되지 않은 항목: 기한이 오늘보다 앞선 경우만 참이다. 기한 당일은 아직 지나지 않았다.
 */
internal fun hasPassedExpiry(item: ItemEntity, today: LocalDate): Boolean {
    val consumed = item.consumedDate
    return if (consumed == null) {
        item.expiryDate < today
    } else {
        item.expiryDate < consumed
    }
}

/** 상단 요약 세 수치를 정본 항목 전체에서 파생한다. */
internal fun buildRecordSummary(items: List<ItemEntity>, today: LocalDate): RecordSummary {
    val thisMonth = YearMonth.from(today)
    return RecordSummary(
        consumedThisMonth = items.count { item ->
            item.consumedDate?.let { YearMonth.from(it) == thisMonth } == true
        },
        expired = items.count { item ->
            item.consumedDate == null && hasPassedExpiry(item, today)
        },
        active = items.count { item -> !item.isArchived }
    )
}

/**
 * 타임라인을 날짜 내림차순(최신 먼저)으로 묶는다.
 *
 * 항목마다 등록 사건 하나를 만들고, 소비일이 있으면 소비 완료 사건을, [hasPassedExpiry]가 참이면
 * 기한 지남 사건을 만든다. 같은 날짜 안에서는 등록 → 소비 완료 → 기한 지남 순서로, 그다음 이름과
 * id 순서로 고정한다.
 */
internal fun buildRecordDays(items: List<ItemEntity>, today: LocalDate): List<RecordDay> =
    items
        .flatMap { item -> eventsOf(item, today) }
        .groupBy { event -> event.date }
        .map { (date, events) -> RecordDay(date, events.sortedWith(eventOrder)) }
        .sortedByDescending { day -> day.date }

private val eventOrder = compareBy<RecordEvent>(
    { event -> event.type.ordinal },
    { event -> event.item.name },
    { event -> event.item.id }
)

private fun eventsOf(item: ItemEntity, today: LocalDate): List<RecordEvent> = buildList {
    add(RecordEvent(item.createdDate, RecordEventType.REGISTERED, item))
    item.consumedDate?.let { consumedDate ->
        add(RecordEvent(consumedDate, RecordEventType.CONSUMED, item))
    }
    if (hasPassedExpiry(item, today)) {
        add(RecordEvent(item.expiryDate, RecordEventType.EXPIRED, item))
    }
}
