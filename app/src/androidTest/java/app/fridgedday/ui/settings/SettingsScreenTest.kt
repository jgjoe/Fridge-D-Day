package app.fridgedday.ui.settings

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.fridgedday.BuildConfig
import app.fridgedday.FridgeDDayTheme
import app.fridgedday.data.db.AppDatabase
import app.fridgedday.data.pref.SettingsDataStore
import app.fridgedday.data.pref.ThemeMode
import app.fridgedday.ui.navigation.AppNavHost
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 설정 화면의 계약 문구, 런타임 버전 표기, 알림 꺼짐 상태의 비활성 의미를 확인한다.
 */
@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /** 화면에 그대로 노출되어야 하는 개인정보 안내 문구. */
    private val privacyNotice =
        "식품 정보는 이 앱이 외부 서버로 전송하지 않아요. 백업 파일은 사용자가 직접 내보낼 때만 생성됩니다."

    /** 알림이 꺼져 있을 때만 알림 시간 행 아래에 붙는 안내. */
    private val notificationsOffHint = "매일 알림을 켜면 설정할 수 있어요"

    @Before
    fun prepareSettings() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        AppDatabase.getDatabase(context).clearAllTables()
        val dataStore = SettingsDataStore(context)
        dataStore.setFirstLaunchCompleted()
        dataStore.updateNotifyTime(9, 0)
        dataStore.updateDailyNotify(true)
    }

    private fun openSettings() {
        composeTestRule.setContent {
            FridgeDDayTheme {
                AppNavHost(navController = rememberNavController())
            }
        }
        composeTestRule.onNode(isSelectable() and hasText("설정")).performClick()
    }

    private fun awaitText(text: String, timeoutMillis: Long = 5_000) {
        composeTestRule.waitUntil(timeoutMillis) {
            composeTestRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun awaitTextGone(text: String, timeoutMillis: Long = 5_000) {
        composeTestRule.waitUntil(timeoutMillis) {
            composeTestRule.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun darkPreferenceStillUsesTheApprovedLightTheme() {
        var background = Color.Unspecified

        composeTestRule.setContent {
            FridgeDDayTheme(themeMode = ThemeMode.DARK) {
                background = MaterialTheme.colorScheme.background
            }
        }

        composeTestRule.runOnIdle {
            assertEquals(Color(0xFFFAF9F7), background)
        }
    }

    @Test
    fun themeChoiceIsNotOffered() {
        openSettings()
        awaitText("매일 알림 받기")

        composeTestRule.onNodeWithText("표시").assertDoesNotExist()
        composeTestRule.onNodeWithText("테마").assertDoesNotExist()
        composeTestRule.onNodeWithText("다크").assertDoesNotExist()
    }

    @Test
    fun privacyNoticeIsShownVerbatim() {
        openSettings()
        awaitText("매일 알림 받기")

        composeTestRule.onNodeWithText(privacyNotice)
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun appVersionIsShownFromRuntimeBuildConfig() {
        openSettings()
        awaitText("앱 버전")

        composeTestRule.onNodeWithText("v${BuildConfig.VERSION_NAME}")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun notificationTimeIsEditableOnlyWhileDailyNotificationsAreOn() {
        runBlocking {
            SettingsDataStore(ApplicationProvider.getApplicationContext()).updateDailyNotify(false)
        }
        openSettings()

        // 저장된 false가 화면에 반영될 때까지 기다린 뒤 비활성 의미와 동작을 확인한다.
        awaitText(notificationsOffHint)
        composeTestRule.onNodeWithText("알림 시간").assertIsNotEnabled()
        composeTestRule.onNodeWithText("알림 시간").performClick()
        composeTestRule.onNodeWithText("알림 시간 설정").assertDoesNotExist()

        composeTestRule.onNodeWithText("매일 알림 받기").performClick()
        awaitTextGone(notificationsOffHint)
        composeTestRule.onNodeWithText("알림 시간").assertIsEnabled()
        composeTestRule.onNodeWithText("알림 시간").performClick()
        composeTestRule.onNodeWithText("알림 시간 설정").assertIsDisplayed()
    }
}
