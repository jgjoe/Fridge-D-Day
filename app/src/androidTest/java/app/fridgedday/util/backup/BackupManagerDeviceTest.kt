package app.fridgedday.util.backup

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.fridgedday.data.db.AppDatabase
import app.fridgedday.data.db.entity.ItemEntity
import app.fridgedday.data.db.entity.StorageLocation
import app.fridgedday.data.repo.ItemRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate

/**
 * 백업/복원 데이터 경로를 실제 기기의 디버그 앱 프로세스에서 확인한다.
 *
 * 설정 화면과 같은 순서를 밟는다: DB의 모든 항목을 읽어 [BackupManager.exportToJson]으로
 * ContentResolver URI에 쓰고, DB만 비운 뒤 [BackupManager.importFromJson]으로 되읽어
 * 반환된 항목을 그대로 insert 한다. 확인 대상은 백업 v2 이력 필드(quantity 0, 아카이브/소비
 * 상태, createdDate, note/category/storage)가 기기 DB 왕복 뒤에도 그대로 남는지다.
 *
 * 앱이 실제로 쓰는 `fridgedday_database`는 열지 않는다. 이 테스트 전용 DB 파일과
 * 캐시 백업 파일만 만들고 끝나면 지운다.
 */
@RunWith(AndroidJUnit4::class)
class BackupManagerDeviceTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var database: AppDatabase
    private lateinit var repository: ItemRepository

    private val backupFile = File(context.cacheDir, "backup-device-test.json")
    private val backupUri: Uri = Uri.fromFile(backupFile)

    @Before
    fun setUp() {
        context.deleteDatabase(TEST_DATABASE)
        database = Room.databaseBuilder(context, AppDatabase::class.java, TEST_DATABASE).build()
        repository = ItemRepository(database.itemDao())
        backupFile.delete()
    }

    @After
    fun tearDown() {
        database.close()
        context.deleteDatabase(TEST_DATABASE)
        backupFile.delete()
    }

    @Test
    fun exportedDatabaseItemsSurviveImportBackIntoDeviceDatabase() = runBlocking {
        assertTrue(
            "디버그 앱에서만 실행한다: ${context.packageName}",
            context.packageName.endsWith(".debug")
        )

        val consumed = ItemEntity(
            name = "기기 검증 소비 완료 식품",
            category = "유제품",
            location = StorageLocation.FREEZER,
            quantity = 0f,
            unit = "개",
            expiryDate = LocalDate.of(2026, 9, 20),
            daysBeforeNotify = 5,
            note = "기기 검증 메모",
            isArchived = true,
            consumedDate = LocalDate.of(2026, 9, 10),
            createdDate = LocalDate.of(2026, 8, 31)
        )
        val active = ItemEntity(
            name = "기기 검증 활성 식품",
            location = StorageLocation.PANTRY,
            quantity = null,
            expiryDate = LocalDate.of(2026, 10, 1),
            createdDate = LocalDate.of(2026, 9, 1)
        )
        repository.insert(consumed)
        repository.insert(active)

        val exportResult = BackupManager.exportToJson(context, repository.getAllItems(), backupUri)
        assertTrue("export 실패: ${exportResult.exceptionOrNull()}", exportResult.isSuccess)
        assertTrue("백업 파일이 비어 있음", backupFile.length() > 0)

        // 백업 파일은 그대로 두고 DB만 비운다. 복원은 파일만으로 끝나야 한다.
        repository.getAllItems().forEach { item -> repository.delete(item) }
        assertEquals(0, repository.getAllItems().size)

        val importResult = BackupManager.importFromJson(context, backupUri)
        assertTrue("import 실패: ${importResult.exceptionOrNull()}", importResult.isSuccess)
        val restored = importResult.getOrThrow()
        assertEquals(2, restored.size)

        // 설정 화면과 동일하게 반환된 항목을 그대로 insert 한다.
        restored.forEach { item -> repository.insert(item) }

        val persisted = repository.getAllItems().associateBy { it.name }
        assertEquals(setOf(consumed.name, active.name), persisted.keys)

        val persistedConsumed = persisted.getValue(consumed.name)
        assertEquals(consumed.category, persistedConsumed.category)
        assertEquals(consumed.location, persistedConsumed.location)
        assertEquals("quantity 0 유지", 0f, persistedConsumed.quantity ?: Float.NaN, 0f)
        assertEquals(consumed.unit, persistedConsumed.unit)
        assertEquals(consumed.expiryDate, persistedConsumed.expiryDate)
        assertEquals(consumed.daysBeforeNotify, persistedConsumed.daysBeforeNotify)
        assertEquals(consumed.note, persistedConsumed.note)
        assertTrue(persistedConsumed.isArchived)
        assertEquals(consumed.consumedDate, persistedConsumed.consumedDate)
        assertEquals(consumed.createdDate, persistedConsumed.createdDate)

        val persistedActive = persisted.getValue(active.name)
        assertEquals(StorageLocation.PANTRY, persistedActive.location)
        assertNull(persistedActive.quantity)
        assertNull(persistedActive.category)
        assertNull(persistedActive.unit)
        assertNull(persistedActive.note)
        assertNull(persistedActive.consumedDate)
        assertFalse(persistedActive.isArchived)
        assertEquals(active.expiryDate, persistedActive.expiryDate)
        assertEquals(active.createdDate, persistedActive.createdDate)
    }

    private companion object {
        const val TEST_DATABASE = "backup-device-test"
    }
}
