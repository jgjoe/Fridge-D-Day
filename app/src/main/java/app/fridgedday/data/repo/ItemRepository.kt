package app.fridgedday.data.repo

import app.fridgedday.data.db.dao.ItemDao
import app.fridgedday.data.db.entity.ItemEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

interface AddEditItemRepository {
    suspend fun getById(id: Long): ItemEntity?
    suspend fun insert(item: ItemEntity): Long
    suspend fun update(item: ItemEntity)
}

/** 상세 화면이 쓰는 최소 표면. 소비 완료와 실행 취소가 스키마 변경 없이 끝난다. */
interface ItemDetailRepository {
    /**
     * 활성 항목 하나를 구독한다. 소비 완료로 아카이브된 항목은 null로 방출된다.
     *
     * 전용 단일 행 쿼리를 추가하는 대신 기존 [ItemDao.observeAll]을 재사용한다.
     */
    fun observeActiveById(id: Long): Flow<ItemEntity?>

    suspend fun getById(id: Long): ItemEntity?

    suspend fun markConsumed(id: Long)

    /** 소비 완료를 되돌린다. 아카이브를 풀고 소비 날짜만 지운다. */
    suspend fun restoreConsumed(id: Long)

    suspend fun delete(item: ItemEntity)
}

class ItemRepository(
    private val itemDao: ItemDao
) : AddEditItemRepository, ItemDetailRepository {

    fun observeAll(): Flow<List<ItemEntity>> = itemDao.observeAll()

    fun search(keyword: String): Flow<List<ItemEntity>> = itemDao.search(keyword)

    override fun observeActiveById(id: Long): Flow<ItemEntity?> =
        itemDao.observeAll().map { items -> items.firstOrNull { it.id == id } }

    override suspend fun getById(id: Long): ItemEntity? = itemDao.getById(id)

    override suspend fun insert(item: ItemEntity): Long = itemDao.insert(item)

    override suspend fun update(item: ItemEntity) = itemDao.update(item)

    suspend fun archive(id: Long) = itemDao.archive(id)

    override suspend fun markConsumed(id: Long) = itemDao.markConsumed(id, LocalDate.now())

    override suspend fun restoreConsumed(id: Long) {
        val item = itemDao.getById(id) ?: return
        itemDao.update(item.copy(isArchived = false, consumedDate = null))
    }

    override suspend fun delete(item: ItemEntity) = itemDao.delete(item)

    suspend fun getAllItems(): List<ItemEntity> = itemDao.getAllItems()

    suspend fun getDueItems(daysBefore: Int): List<ItemEntity> {
        val targetDate = LocalDate.now().plusDays(daysBefore.toLong())
        return itemDao.dueBefore(targetDate)
    }

    suspend fun getExpiredItems(): List<ItemEntity> {
        return itemDao.dueBefore(LocalDate.now())
    }
}
