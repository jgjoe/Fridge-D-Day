package app.fridgedday.ui.navigation

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.fridgedday.FridgeDDayTheme
import app.fridgedday.data.db.AppDatabase
import app.fridgedday.data.db.entity.ItemEntity
import app.fridgedday.data.db.entity.StorageLocation
import app.fridgedday.data.pref.SettingsDataStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * 오늘 목록 → 상세 → 소비 완료/수정 이동 경로를 실제 NavHost로 확인한다.
 */
@RunWith(AndroidJUnit4::class)
class ItemDetailFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun resetAppState() = runBlocking {
        database().clearAllTables()
        SettingsDataStore(ApplicationProvider.getApplicationContext()).setFirstLaunchCompleted()
    }

    private fun context(): Context = ApplicationProvider.getApplicationContext()

    private fun database() = AppDatabase.getDatabase(context())

    private fun insertItem(name: String, daysUntilExpiry: Long): Long = runBlocking {
        database().itemDao().insert(
            ItemEntity(
                name = name,
                location = StorageLocation.FRIDGE,
                expiryDate = LocalDate.now().plusDays(daysUntilExpiry)
            )
        )
    }

    private fun loadItem(id: Long): ItemEntity? = runBlocking { database().itemDao().getById(id) }

    private fun setAppContent() {
        composeTestRule.setContent {
            FridgeDDayTheme {
                AppNavHost(navController = rememberNavController())
            }
        }
    }

    private fun awaitText(text: String, timeoutMillis: Long = 5_000) {
        composeTestRule.waitUntil(timeoutMillis) {
            composeTestRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun todayRowTapOpensTheItemDetail() {
        insertItem("요구르트", daysUntilExpiry = 4)

        setAppContent()
        awaitText("요구르트")

        composeTestRule.onNodeWithText("요구르트").performClick()

        awaitText("식품 상세")
        composeTestRule.onNodeWithText("요구르트").assertIsDisplayed()
        composeTestRule.onNodeWithText("소비 완료").assertIsDisplayed()
        composeTestRule.onNodeWithText("수정").assertIsDisplayed()
        composeTestRule.onNodeWithText("삭제").assertIsDisplayed()
    }

    @Test
    fun consumeReturnsToTodayAndHostsTheUndoThere() {
        val id = insertItem("치즈", daysUntilExpiry = 6)

        setAppContent()
        awaitText("치즈")
        composeTestRule.onNodeWithText("치즈").performClick()
        awaitText("식품 상세")

        composeTestRule.onNodeWithText("소비 완료").performClick()

        // 상세는 바로 닫히고, 되돌리기는 Today가 띄운다(D-015).
        awaitText("오늘도 신선")
        composeTestRule.onNodeWithText("식품 상세").assertDoesNotExist()
        awaitText("소비 완료로 표시했어요")
        composeTestRule.onNodeWithText("실행 취소").assertIsDisplayed()
        composeTestRule.onNodeWithText("치즈").assertDoesNotExist()
        assertTrue(loadItem(id)!!.isArchived)
    }

    @Test
    fun consumeUndoOnTodayRestoresTheRowToTheList() {
        val id = insertItem("치즈", daysUntilExpiry = 6)

        setAppContent()
        awaitText("치즈")
        composeTestRule.onNodeWithText("치즈").performClick()
        awaitText("식품 상세")
        composeTestRule.onNodeWithText("소비 완료").performClick()
        awaitText("실행 취소")

        composeTestRule.onNodeWithText("실행 취소").performClick()

        // 되돌리면 같은 행이 다시 활성 상태로 돌아와 Today 목록에 보인다.
        awaitText("치즈")
        val restored = loadItem(id)!!
        assertFalse(restored.isArchived)
        assertNull(restored.consumedDate)
        composeTestRule.onNodeWithText("소비 완료로 표시했어요").assertDoesNotExist()
    }

    @Test
    fun editActionOpensTheEditRouteAndReturningShowsThePersistedEdit() {
        val id = insertItem("두부", daysUntilExpiry = 8)

        setAppContent()
        awaitText("두부")
        composeTestRule.onNodeWithText("두부").performClick()
        awaitText("식품 상세")

        composeTestRule.onNodeWithText("수정").performClick()

        awaitText("항목 수정")
        // 수정 화면에는 읽기 전용 날짜·보관 위치 필드도 SetText를 노출하므로 라벨로 이름 필드를 고른다.
        val nameField = hasSetTextAction() and hasText("식품명 *")
        composeTestRule.onNode(nameField).performTextClearance()
        composeTestRule.onNode(nameField).performTextInput("순두부")
        composeTestRule.onNodeWithText("저장").performClick()

        // 저장하면 상세로 돌아오고, 화면은 방금 저장된 값을 다시 읽는다.
        awaitText("순두부")
        composeTestRule.onNodeWithText("식품 상세").assertIsDisplayed()
        composeTestRule.onNodeWithText("두부").assertDoesNotExist()
        assertEquals("순두부", loadItem(id)!!.name)
    }

    @Test
    fun detailBackReturnsToToday() {
        insertItem("계란", daysUntilExpiry = 12)

        setAppContent()
        awaitText("계란")
        composeTestRule.onNodeWithText("계란").performClick()
        awaitText("식품 상세")

        composeTestRule.onNodeWithContentDescription("뒤로가기").performClick()

        awaitText("오늘도 신선")
        composeTestRule.onNodeWithText("계란").assertIsDisplayed()
    }
}
