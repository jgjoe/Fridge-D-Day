package app.fridgedday.ui

import android.content.Context
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.fridgedday.FridgeDDayTheme
import app.fridgedday.data.db.AppDatabase
import app.fridgedday.data.db.entity.ItemEntity
import app.fridgedday.data.db.entity.StorageLocation
import app.fridgedday.data.pref.SettingsDataStore
import app.fridgedday.ui.home.HomeScreen
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun resetAppState() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        AppDatabase.getDatabase(context).clearAllTables()
        SettingsDataStore(context).setFirstLaunchCompleted()
    }

    private fun setHomeContent() {
        composeTestRule.setContent {
            FridgeDDayTheme {
                HomeScreen(navController = rememberNavController())
            }
        }
    }

    private fun insertItem(name: String, daysUntilExpiry: Long, category: String? = null) = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        AppDatabase.getDatabase(context).itemDao().insert(
            ItemEntity(
                name = name,
                category = category,
                location = StorageLocation.FRIDGE,
                expiryDate = LocalDate.now().plusDays(daysUntilExpiry)
            )
        )
    }

    private fun awaitText(text: String) {
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun emptyTodayMatchesTheApprovedEntryContract() {
        setHomeContent()

        composeTestRule.onNodeWithText("오늘도 신선").assertIsDisplayed()
        composeTestRule.onNodeWithText("어떤 식품을 기록할까요?").assertIsDisplayed()
        composeTestRule.onNodeWithText("식품을 등록하고\n더 신선한 일상을 시작해보세요.").assertIsDisplayed()
        composeTestRule.onNodeWithText("바로 스캔하기").assertIsDisplayed().assertHasClickAction()
        composeTestRule.onNodeWithText("직접 입력하기").assertIsDisplayed().assertHasClickAction()
        composeTestRule.onNodeWithText("작은 기록이\n더 맛있는 일상을 만들어줘요.").assertIsDisplayed()
        composeTestRule.onNodeWithText("샘플로 둘러보기").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("스캔").assertDoesNotExist()
    }

    @Test
    fun populatedTodayShowsCountsAndSeparateFoodCards() {
        insertItem("지난 우유", -1, "유제품")
        insertItem("곧 먹을 두부", 3, "가공식품")
        insertItem("여유 있는 사과", 20, "과일")
        setHomeContent()
        awaitText("여유 있는 사과")

        composeTestRule.onNodeWithText("오늘 확인할 식품").assertIsDisplayed()
        composeTestRule.onNodeWithText("전체 3").assertIsDisplayed()
        composeTestRule.onNodeWithText("임박 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("보통 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("여유 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("보통 1").performClick()
        composeTestRule.onNodeWithContentDescription("필터 및 정렬").assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                "필터 또는 정렬 적용됨"
            )
        )
        composeTestRule.onNodeWithContentDescription("스캔").assertIsDisplayed().assertHasClickAction()
        composeTestRule.onNodeWithText("직접 입력하기").assertDoesNotExist()
    }

    @Test
    fun searchIsHiddenAtRestAndFiltersByNameWhenOpened() {
        insertItem("우유", 3)
        insertItem("사과", 20)
        setHomeContent()
        awaitText("사과")

        composeTestRule.onNodeWithText("이름으로 검색").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("식품 검색").performClick()
        composeTestRule.onNodeWithText("이름으로 검색").performTextInput("우유")

        composeTestRule
            .onNode(hasText("우유") and !hasSetTextAction())
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("사과").assertDoesNotExist()
    }

    @Test
    fun filterAndSortTriggerOpensTheExistingLocationAndSortSheet() {
        insertItem("우유", 3)
        setHomeContent()
        awaitText("우유")

        composeTestRule.onNodeWithContentDescription("필터 및 정렬")
            .assertHasClickAction()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "기본값"
                )
            )
            .performClick()
        val expandMatcher = SemanticsMatcher.keyIsDefined(SemanticsActions.Expand)
        val expandableNodes = composeTestRule.onAllNodes(expandMatcher)
        if (expandableNodes.fetchSemanticsNodes().isNotEmpty()) {
            expandableNodes[0].performSemanticsAction(SemanticsActions.Expand)
            composeTestRule.waitUntil(5_000) {
                composeTestRule.onAllNodes(expandMatcher).fetchSemanticsNodes().isEmpty()
            }
        }


        composeTestRule.onNodeWithText("보관 위치").assertExists()
        composeTestRule.onNodeWithText("정렬").assertExists()
        composeTestRule.onNodeWithText("유통기한 임박순")
            .performScrollTo()
            .assertIsDisplayed()
    }
}
