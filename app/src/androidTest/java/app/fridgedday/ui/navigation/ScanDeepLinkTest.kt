package app.fridgedday.ui.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.fridgedday.MainActivity
import app.fridgedday.data.db.AppDatabase
import app.fridgedday.data.pref.SettingsDataStore
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 홈 위젯의 바로 스캔 동작이 여는 fridgedday://scan 딥링크가 기존 스캔 화면으로 연결된다.
 *
 * 스캔·OCR 동작 자체는 건드리지 않고 진입 경로만 확인한다.
 */
@RunWith(AndroidJUnit4::class)
class ScanDeepLinkTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Test
    fun scanDeepLinkOpensTheExistingScanScreen() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        runBlocking {
            AppDatabase.getDatabase(context).clearAllTables()
            SettingsDataStore(context).setFirstLaunchCompleted()
        }

        ActivityScenario.launch<MainActivity>(
            Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse("fridgedday://scan")
            }
        ).use {
            composeTestRule.waitUntil(timeoutMillis = 10_000) {
                composeTestRule.onAllNodesWithText("촬영 이미지는 저장하지 않아요")
                    .fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule
                .onNodeWithText("유통기한 라벨이 화면에 크게 보이도록 촬영하세요.")
                .assertIsDisplayed()
        }
    }
}
