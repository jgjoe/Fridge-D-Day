package app.fridgedday.ui.addedit

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.fridgedday.FridgeDDayTheme
import app.fridgedday.data.db.AppDatabase
import app.fridgedday.data.pref.SettingsDataStore
import app.fridgedday.ui.navigation.AppNavHost
import app.fridgedday.ui.navigation.Destinations
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 등록 화면(D-015/D-016)의 저장 버튼 배치와 필수 필드 표현을 앱 내비게이션 그대로 확인한다.
 *
 * 저장은 폼과 함께 스크롤되는 상단 액션이 아니라 하단에 고정된 버튼 하나뿐이어야 하고, "추가 정보"를
 * 펼쳐도 그 자리에 남아야 한다. 처음 들어온 화면은 조용하고, 첫 저장 시도에만 이름·유통기한 오류가
 * 각각의 필드에 붙으며, 한 필드를 고치면 그 필드 오류만 사라진다.
 */
@RunWith(AndroidJUnit4::class)
class AddEditScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var navController: NavHostController

    @Before
    fun resetAppState() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        AppDatabase.getDatabase(context).clearAllTables()
        SettingsDataStore(context).setFirstLaunchCompleted()
    }

    private fun openAddScreen() {
        composeTestRule.setContent {
            FridgeDDayTheme {
                navController = rememberNavController()
                AppNavHost(navController = navController)
            }
        }
        composeTestRule.runOnUiThread { navController.navigate(Destinations.ADD) }
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("식품 등록").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun theOnlySaveIsThePinnedBottomButtonAndSubmitShowsFieldErrors() {
        openAddScreen()

        // 상단 액션은 없고 하단 고정 버튼 하나뿐이다.
        composeTestRule.onAllNodesWithText("저장").assertCountEquals(1)

        // 처음 들어온 화면에는 필수 필드 오류가 미리 붙어 있지 않다.
        composeTestRule.onNodeWithText("이름을 입력해주세요").assertDoesNotExist()
        composeTestRule.onNodeWithText("유통기한을 선택하고 확인해주세요").assertDoesNotExist()

        composeTestRule.onNodeWithText("저장").performClick()

        // 한 번의 저장 시도가 이름·유통기한 오류를 각각의 필드에 붙이고 화면은 그대로 남는다.
        composeTestRule.onNodeWithText("식품 등록").assertIsDisplayed()
        composeTestRule.onNodeWithText("이름을 입력해주세요").assertIsDisplayed()
        composeTestRule.onNodeWithText("유통기한을 선택하고 확인해주세요").assertIsDisplayed()

        // 이름을 고치면 이름 오류만 사라지고 유통기한 오류는 남는다.
        val nameField = hasSetTextAction() and hasText("식품명 *")
        composeTestRule.onNode(nameField).performTextInput("우유")
        composeTestRule.onNodeWithText("이름을 입력해주세요").assertDoesNotExist()
        composeTestRule.onNodeWithText("유통기한을 선택하고 확인해주세요").assertIsDisplayed()

        // 다시 비우면 저장을 누르지 않아도 이름 필수 오류가 같은 필드에 돌아온다.
        composeTestRule.onNode(nameField).performTextClearance()
        composeTestRule.onNodeWithText("이름을 입력해주세요").assertIsDisplayed()
        composeTestRule.onNodeWithText("유통기한을 선택하고 확인해주세요").assertIsDisplayed()
    }

    @Test
    fun expiryFieldKeepsItsLabelAndOpensTheCalendarFromTheFieldBody() {
        openAddScreen()

        // 화면이 따로 붙이던 라벨은 없어지고 필드 라벨 하나만 남는다.
        composeTestRule.onAllNodesWithText("유통기한 *").assertCountEquals(1)

        // 달력 아이콘이 아니라 필드 본문을 눌러도 날짜 선택이 열린다.
        composeTestRule.onNodeWithText("유통기한 *").performClick()
        composeTestRule.onNodeWithText("확인").assertIsDisplayed()
        composeTestRule.onNodeWithText("취소").assertIsDisplayed()

        // 여는 것만으로는 날짜가 확정되지 않는다.
        composeTestRule.onNodeWithText("확인된 날짜", substring = true).assertDoesNotExist()
    }

    @Test
    fun bottomSaveStaysPinnedWhenAdditionalInfoExpands() {
        openAddScreen()

        composeTestRule.onNodeWithText("추가 정보").performClick()
        composeTestRule.onNodeWithText("메모").assertExists()

        composeTestRule.onAllNodesWithText("저장").assertCountEquals(1)
        composeTestRule.onNodeWithText("저장").assertIsDisplayed()
    }
}
