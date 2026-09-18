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
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.fridgedday.FridgeDDayTheme
import app.fridgedday.data.db.AppDatabase
import app.fridgedday.data.db.entity.ItemEntity
import app.fridgedday.data.db.entity.StorageLocation
import app.fridgedday.data.pref.SettingsDataStore
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * 등록 직후 Today가 방금 저장한 행을 실제로 보여주는지(D-016) 앱 내비게이션 그대로 확인한다.
 *
 * 등록은 사용자가 하는 그대로 화면에서 진행한다. 이름을 입력하고 날짜를 확정한 뒤 하단 "저장"을
 * 누르면 등록 화면이 저장소가 돌려준 id를 이전 화면에 남기고 Today로 돌아온다. Today는 사용자가
 * 고른 정렬을 유지한 채 새 행을 가리는 검색 조건만 풀고 그 행까지 스크롤해야 한다.
 *
 * 새 행의 유통기한(날짜 선택기의 기본값인 오늘)이 채운 목록(지난 날짜들)보다 뒤에 오도록 만들어,
 * 스크롤이 실제로 일어나야만 행이 화면에 보이게 한다.
 */
@RunWith(AndroidJUnit4::class)
class TodayRevealNavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var navController: NavHostController

    @Before
    fun resetAppState() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        AppDatabase.getDatabase(context).clearAllTables()
        SettingsDataStore(context).setFirstLaunchCompleted()
    }

    private fun setAppContent() {
        composeTestRule.setContent {
            FridgeDDayTheme {
                navController = rememberNavController()
                AppNavHost(navController = navController)
            }
        }
    }

    private fun insertItem(name: String, daysUntilExpiry: Long) = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        AppDatabase.getDatabase(context).itemDao().insert(
            ItemEntity(
                name = name,
                location = StorageLocation.FRIDGE,
                expiryDate = LocalDate.now().plusDays(daysUntilExpiry)
            )
        )
    }

    private fun awaitText(text: String, timeoutMillis: Long = 5_000) {
        composeTestRule.waitUntil(timeoutMillis) {
            composeTestRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun savedRowIsRevealedAfterRegistrationEvenWhenTheSearchHidesIt() {
        // 유통기한 오름차순에서 새 행(오늘)이 마지막에 오도록 지난 날짜만 채운다.
        repeat(12) { index -> insertItem("기존식품$index", daysUntilExpiry = index - 12L) }

        setAppContent()
        awaitText("오늘도 신선")
        awaitText("기존식품0")

        // 새로 등록할 행을 가리는 검색어를 남긴다. 검색 필드는 평상시 숨겨져 있고 상단 검색 동작을 눌러야 열린다.
        composeTestRule.onNodeWithContentDescription("식품 검색").performClick()
        composeTestRule.onNodeWithText("이름으로 검색").performTextInput("기존식품")

        // 실제 등록 화면에서 이름을 입력하고 날짜를 확정한 뒤 하단 저장을 누른다.
        // Today 하단의 스캔으로 스캔 화면을 연다. 스캔은 하단 탭이 아니라 셸 동작 하나이므로
        // 등록 진입은 이 화면의 보조 동작인 `직접 입력`으로 이어간다.
        composeTestRule.onNodeWithContentDescription("스캔").performClick()
        awaitText("카메라로 촬영")
        composeTestRule.onNodeWithText("직접 입력").performScrollTo().performClick()
        awaitText("식품 등록")

        val nameField = hasSetTextAction() and hasText("식품명 *")
        composeTestRule.onNode(nameField).performTextInput("두부")
        composeTestRule.onNodeWithText("유통기한 *").performClick()
        composeTestRule.onNodeWithText("확인").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("확인된 날짜", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithText("저장").performClick()

        // 스크롤이 실제로 일어나야만 목록 끝의 새 행이 semantics에 나타난다.
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("두부").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("두부").assertIsDisplayed()

        composeTestRule.onNodeWithText("이름으로 검색").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("식품 검색").performClick()
        composeTestRule.onNodeWithText("이름으로 검색").assertIsDisplayed()
    }
}
