package app.fridgedday.ui.scan

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.fridgedday.MainActivity
import app.fridgedday.data.db.AppDatabase
import app.fridgedday.data.pref.SettingsDataStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 스캔 화면 첫 진입에 뷰파인더 안내와 주 동작이 실제로 화면에 있는지 고정한다.
 *
 * SM-A325N 실기기 회귀에서 뷰파인더가 0 높이로 접혀 "유통기한 라벨이 화면에 크게 보이도록
 * 촬영하세요."와 "카메라로 촬영"이 통째로 사라지고 보조 동작만 남았다. 카메라가 주 동작이므로
 * 첫 진입에서 안내와 56dp 이상 촬영 버튼이 함께 보여야 하며, 글자 확대에서 밀린 갤러리·직접
 * 입력은 본문 스크롤로 계속 닿아야 한다.
 *
 * 스캔 화면은 top-level route가 아니라 `fridgedday://scan` 딥링크로만 열리므로 진입 경로는
 * [ScanDeepLinkTest]와 동일한 딥링크를 쓴다.
 */
@RunWith(AndroidJUnit4::class)
class ScanPrimaryActionTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Test
    fun scanEntersWithTheViewfinderInstructionAndThePrimaryCameraAction() {
        withScanScreen {
            waitForText(CameraAction)

            composeTestRule.onNodeWithText(Instruction).assertIsDisplayed()

            composeTestRule.onNodeWithText(CameraAction).assertIsDisplayed()
            val cameraBounds = composeTestRule.onNodeWithText(CameraAction)
                .getUnclippedBoundsInRoot()
            assertTrue(
                "주 동작 촬영 버튼이 56dp 미만이거나 접혔다: $cameraBounds",
                cameraBounds.height >= MinPrimaryHeight
            )
        }
    }

    @Test
    fun scanKeepsSecondaryActionsReachableByScrolling() {
        withScanScreen {
            waitForText(PrivacyNotice)

            composeTestRule.onNodeWithText(GalleryAction)
                .performScrollTo()
                .assertIsDisplayed()
            composeTestRule.onNodeWithText(ManualAction)
                .performScrollTo()
                .assertIsDisplayed()
        }
    }

    @Test
    fun scanManualEntryOpensTheSameRegistrationScreenAsOtherEntryPoints() {
        withScanScreen {
            waitForText(PrivacyNotice)

            // OCR 실패 스낵바의 `직접 입력`이 쓰는 경로와 같다. 대안이 실제 등록 화면을 여는지 본다.
            composeTestRule.onNodeWithText(ManualAction)
                .performScrollTo()
                .performClick()

            waitForText(RegistrationTitle)
            composeTestRule.onNodeWithText(RegistrationTitle).assertIsDisplayed()
        }
    }

    private fun withScanScreen(block: () -> Unit) {
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
            block()
        }
    }

    private fun waitForText(text: String) {
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private companion object {
        const val Instruction = "유통기한 라벨이 화면에 크게 보이도록 촬영하세요."
        const val CameraAction = "카메라로 촬영"
        const val PrivacyNotice = "촬영 이미지는 저장하지 않아요"
        const val GalleryAction = "갤러리에서 선택"
        const val ManualAction = "직접 입력"
        const val RegistrationTitle = "식품 등록"
        val MinPrimaryHeight = 56.dp
    }
}
