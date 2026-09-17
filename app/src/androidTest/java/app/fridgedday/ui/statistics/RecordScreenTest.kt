package app.fridgedday.ui.statistics

import android.content.Context
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.fridgedday.FridgeDDayTheme
import app.fridgedday.data.db.AppDatabase
import app.fridgedday.data.db.dao.ItemDao
import app.fridgedday.data.db.entity.ItemEntity
import app.fridgedday.data.db.entity.StorageLocation
import app.fridgedday.data.pref.SettingsDataStore
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class RecordScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun resetAppState() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        AppDatabase.getDatabase(context).clearAllTables()
        SettingsDataStore(context).setFirstLaunchCompleted()
    }

    private fun dao(): ItemDao = AppDatabase
        .getDatabase(ApplicationProvider.getApplicationContext<Context>())
        .itemDao()

    private fun insertItem(
        name: String,
        expiryDate: LocalDate,
        consumedDate: LocalDate? = null,
        createdDate: LocalDate = LocalDate.now().minusDays(1)
    ) = runBlocking {
        dao().insert(
            ItemEntity(
                name = name,
                location = StorageLocation.FRIDGE,
                expiryDate = expiryDate,
                isArchived = consumedDate != null,
                consumedDate = consumedDate,
                createdDate = createdDate
            )
        )
    }

    private fun setRecordContent() {
        composeTestRule.setContent {
            FridgeDDayTheme {
                RecordScreen(navController = rememberNavController())
            }
        }
    }

    private fun awaitText(text: String) {
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun recordFilterNode(label: String) = composeTestRule.onNode(
        hasText(label) and
            hasClickAction() and
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)
    )

    @Test
    fun recordRestStateMatchesTheApprovedTopLevelContract() {
        setRecordContent()
        awaitText("아직 기록이 없습니다")

        composeTestRule.onNodeWithText("오늘도 신선").assertIsDisplayed()
        composeTestRule.onNodeWithText("식품 기록").assertIsDisplayed()
        composeTestRule.onNodeWithText("지금까지 이런 기록이 있어요.").assertIsDisplayed()
        recordFilterNode("전체").assertIsDisplayed()
        recordFilterNode("소비 완료").assertIsDisplayed()
        recordFilterNode("기한 지남").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("스캔").assertIsDisplayed().assertHasClickAction()
        composeTestRule.onNodeWithText("이름으로 검색").assertDoesNotExist()
    }

    @Test
    fun statusChipsFilterGroupedEvents() {
        val today = LocalDate.now()
        insertItem("소비한 우유", today.plusDays(2), consumedDate = today)
        insertItem("기한 지난 두부", today.minusDays(2))
        setRecordContent()
        awaitText("기한 지난 두부")

        recordFilterNode("소비 완료").performClick()
        composeTestRule.onNodeWithText("소비한 우유").assertIsDisplayed()
        composeTestRule.onNodeWithText("기한 지난 두부").assertDoesNotExist()

        recordFilterNode("기한 지남").performClick()
        composeTestRule.onNodeWithText("기한 지난 두부").assertIsDisplayed()
        composeTestRule.onNodeWithText("소비한 우유").assertDoesNotExist()
    }

    @Test
    fun searchIsHiddenAtRestAndFiltersByItemName() {
        val today = LocalDate.now()
        insertItem("우유", today.plusDays(3))
        insertItem("두부", today.plusDays(4))
        setRecordContent()
        awaitText("두부")

        composeTestRule.onNodeWithContentDescription("식품 검색").performClick()
        composeTestRule.onNodeWithText("이름으로 검색").performTextInput("우유")

        composeTestRule
            .onNode(hasText("우유") and !hasSetTextAction())
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("두부").assertDoesNotExist()
    }

    @Test
    fun ordinaryRecordRowsDoNotAdvertiseNavigation() {
        val today = LocalDate.now()
        insertItem("등록한 달걀", today.plusDays(5), createdDate = today)
        setRecordContent()
        awaitText("등록한 달걀")

        composeTestRule.onNodeWithText("등록한 달걀")
            .assertIsDisplayed()
            .assertHasNoClickAction()
        composeTestRule.onNodeWithText("냉장 · 달걀 · 등록").assertIsDisplayed()
        composeTestRule.onNodeWithText("등록").assertDoesNotExist()
    }

    @Test
    fun consumedEventsKeepTheExplicitRestoreAction() {
        val today = LocalDate.now()
        val id = insertItem("소비한 우유", today.plusDays(3), consumedDate = today)
        setRecordContent()
        awaitText("소비한 우유")

        composeTestRule.onNodeWithContentDescription("소비한 우유 복원").performClick()

        composeTestRule.waitUntil(5_000) { runBlocking { dao().getById(id)?.isArchived == false } }
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithContentDescription("소비한 우유 복원")
                .fetchSemanticsNodes().isEmpty()
        }
    }
}
