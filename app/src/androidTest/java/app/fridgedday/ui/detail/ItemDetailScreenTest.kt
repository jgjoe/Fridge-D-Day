package app.fridgedday.ui.detail

import android.content.Context
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnySibling
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.fridgedday.FridgeDDayTheme
import app.fridgedday.data.db.AppDatabase
import app.fridgedday.data.db.dao.ItemDao
import app.fridgedday.data.db.entity.ItemEntity
import app.fridgedday.data.db.entity.StorageLocation
import app.fridgedday.util.DateUtils
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * 상세 화면은 저장된 정보만 보여주고, 소비 완료는 확인 모달 없이 즉시 처리하며 삭제만 확인을 받는다.
 */
@RunWith(AndroidJUnit4::class)
class ItemDetailScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun clearDatabase() = runBlocking {
        database().clearAllTables()
    }

    private fun database() = AppDatabase
        .getDatabase(ApplicationProvider.getApplicationContext<Context>())

    private fun dao(): ItemDao = database().itemDao()

    private fun insertItem(
        name: String,
        daysUntilExpiry: Long,
        quantity: Float? = null,
        unit: String? = null,
        note: String? = null
    ): Long = runBlocking {
        dao().insert(
            ItemEntity(
                name = name,
                location = StorageLocation.FREEZER,
                quantity = quantity,
                unit = unit,
                expiryDate = LocalDate.now().plusDays(daysUntilExpiry),
                note = note
            )
        )
    }

    private fun loadItem(id: Long): ItemEntity? = runBlocking { dao().getById(id) }

    private fun setDetailContent(itemId: Long) {
        composeTestRule.setContent {
            FridgeDDayTheme {
                ItemDetailScreen(navController = rememberNavController(), itemId = itemId)
            }
        }
    }

    private fun awaitText(text: String, timeoutMillis: Long = 5_000) {
        composeTestRule.waitUntil(timeoutMillis) {
            composeTestRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun detailShowsOnlyPersistedFieldsWithLargeDDay() {
        val id = insertItem("그릭요거트", daysUntilExpiry = 3, quantity = 2f, unit = "개", note = "아침에 먹기")

        setDetailContent(id)

        awaitText("그릭요거트")
        composeTestRule.onNodeWithText("D-3").assertIsDisplayed()
        composeTestRule
            .onNodeWithText(DateUtils.formatKorean(LocalDate.now().plusDays(3)))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("냉동").assertIsDisplayed()
        composeTestRule.onNodeWithText("2개").assertIsDisplayed()
        composeTestRule.onNodeWithText("아침에 먹기").assertIsDisplayed()
        composeTestRule.onNodeWithText("유통기한 3일 전").assertIsDisplayed()
        composeTestRule.onNodeWithText("소비 완료").assertIsDisplayed()
        composeTestRule.onNodeWithText("수정").assertIsDisplayed()
        composeTestRule.onNodeWithText("삭제").assertIsDisplayed()
    }

    @Test
    fun consumeArchivesImmediatelyWithoutADetailHostedUndo() {
        val id = insertItem("우유", daysUntilExpiry = 4)

        setDetailContent(id)
        awaitText("우유")
        composeTestRule.onNodeWithText("소비 완료").performClick()

        // 확인 모달 없이 즉시 저장되고, 아카이브된 행이 화면에서 사라지며 빈 상세로 보이지 않는다.
        composeTestRule.waitUntil(5_000) { loadItem(id)!!.isArchived }
        composeTestRule.onNodeWithText("우유").assertIsDisplayed()

        // 되돌리기는 Today가 맡는다(D-015). 이 화면은 스스로 스낵바를 띄우거나 항목을 되살리지 않는다.
        composeTestRule.onNodeWithText("실행 취소").assertDoesNotExist()
        val consumed = loadItem(id)!!
        assertTrue(consumed.isArchived)
        assertNotNull(consumed.consumedDate)
    }

    @Test
    fun deleteNeedsTheDestructiveConfirmationAndCancellingKeepsTheItem() {
        val id = insertItem("버터", daysUntilExpiry = 10)

        setDetailContent(id)
        awaitText("버터")

        composeTestRule.onNodeWithText("삭제").performClick()
        composeTestRule.onNodeWithText("삭제 확인").assertIsDisplayed()
        composeTestRule.onNodeWithText("취소").performClick()
        assertNotNull(loadItem(id))

        composeTestRule.onNodeWithText("삭제").performClick()
        composeTestRule
            .onNode(hasText("삭제") and hasAnySibling(hasText("취소")))
            .performClick()

        composeTestRule.waitUntil(5_000) { loadItem(id) == null }
        assertNull(loadItem(id))
    }

    @Test
    fun unknownItemIdShowsTheMissingStateInsteadOfAnEmptyDetail() {
        setDetailContent(itemId = 4242)

        awaitText("식품을 찾을 수 없어요")
        composeTestRule.onNodeWithText("오늘로 돌아가기").assertHasClickAction()
        composeTestRule.onNodeWithText("소비 완료").assertDoesNotExist()
    }
}
