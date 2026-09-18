package app.fridgedday.ui.addedit

import android.text.format.DateFormat
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.fridgedday.FridgeDDayTheme
import app.fridgedday.ui.scan.OcrConfirmScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DecimalStyle
import java.util.Locale

/**
 * OCR 확인 화면은 인식 결과를 보여주기만 하고, "이 날짜 확인" 전에는 아무것도 확정하지 않는다.
 */
class OcrConfirmScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val detectedDate = LocalDate.of(2026, 8, 20)

    private fun setConfirmContent(
        onConfirm: (LocalDate) -> Unit = {},
        onBack: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            FridgeDDayTheme {
                OcrConfirmScreen(
                    detectedDate = detectedDate,
                    onConfirm = onConfirm,
                    onBack = onBack
                )
            }
        }
    }

    /**
     * material3 DatePicker의 날짜 셀은 화면에 보이는 숫자가 아니라 `yMMMMEEEEd` 스켈레톤으로
     * 포맷한 전체 날짜를 semantics 텍스트로 노출한다(`DatePicker` 안 `Day` 컴포저블).
     * material3와 같은 스켈레톤·포맷터로 그 설명을 계산해, 기기 로케일과 무관하게 원하는
     * 날짜 셀 하나만 정확히 집는다.
     */
    private fun datePickerDayDescription(date: LocalDate): String {
        val locale = Locale.getDefault()
        val pattern = DateFormat.getBestDateTimePattern(locale, "yMMMMEEEEd")
        return DateTimeFormatter.ofPattern(pattern, locale)
            .withDecimalStyle(DecimalStyle.of(locale))
            .format(date)
    }

    @Test
    fun detectedDateIsShownWithoutConfirmation() {
        var confirmed: LocalDate? = null

        setConfirmContent(onConfirm = { confirmed = it })

        composeTestRule.onNodeWithText("OCR 확인").assertIsDisplayed()
        // 아직 손대지 않은 값은 인식 결과다.
        composeTestRule.onNodeWithText("인식된 날짜").assertIsDisplayed()
        composeTestRule.onNodeWithText("2026년 8월 20일").assertIsDisplayed()
        composeTestRule.onNodeWithText("인식 결과는 저장 전 확인").assertIsDisplayed()
        assertNull(confirmed)
    }

    @Test
    fun confirmingReportsTheDetectedDate() {
        var confirmed: LocalDate? = null

        setConfirmContent(onConfirm = { confirmed = it })

        composeTestRule.onNodeWithText("이 날짜 확인").performClick()

        assertEquals(detectedDate, confirmed)
    }

    @Test
    fun editingThenCancellingKeepsTheDetectedDate() {
        var confirmed: LocalDate? = null

        setConfirmContent(onConfirm = { confirmed = it })

        composeTestRule.onNodeWithText("수정").performClick()
        composeTestRule.onNodeWithText("확인").assertIsDisplayed()

        composeTestRule.onNodeWithText("취소").performClick()

        composeTestRule.onNodeWithText("2026년 8월 20일").assertIsDisplayed()
        // 다이얼로그를 열었다 취소한 것은 수정이 아니므로 인식 결과 표기를 유지한다.
        composeTestRule.onNodeWithText("인식된 날짜").assertIsDisplayed()
        assertNull(confirmed)
    }

    @Test
    fun editedDateStillRequiresTheExplicitConfirmation() {
        var confirmed: LocalDate? = null

        setConfirmContent(onConfirm = { confirmed = it })

        composeTestRule.onNodeWithText("수정").performClick()
        // DatePicker는 처음 선택된 날짜로 열리므로 확인만 누르면 같은 날짜가 된다.
        composeTestRule.onNodeWithText("확인").performClick()

        composeTestRule.onNodeWithText("2026년 8월 20일").assertIsDisplayed()
        assertNull(confirmed)

        composeTestRule.onNodeWithText("이 날짜 확인").performClick()

        assertEquals(detectedDate, confirmed)
    }

    @Test
    fun editedDateSurvivesStateRestoration() {
        val restorationTester = StateRestorationTester(composeTestRule)
        var confirmed: LocalDate? = null
        val correctedDate = LocalDate.of(2026, 8, 15)

        restorationTester.setContent {
            FridgeDDayTheme {
                OcrConfirmScreen(
                    detectedDate = detectedDate,
                    onConfirm = { confirmed = it },
                    onBack = {}
                )
            }
        }

        composeTestRule.onNodeWithText("수정").performClick()
        // 날짜 셀은 화면에 보이는 "15"가 아니라 전체 날짜 설명을 텍스트로 노출하므로,
        // 그 설명으로 2026-08-15 셀 하나만 집는다.
        composeTestRule
            .onNodeWithText(datePickerDayDescription(correctedDate), substring = true)
            .performClick()
        composeTestRule.onNodeWithText("확인").performClick()
        composeTestRule.onNodeWithText("2026년 8월 15일").assertIsDisplayed()

        // 사용자가 직접 고른 값이므로 라벨이 인식 결과에서 선택한 값으로 바뀐다(D-016).
        composeTestRule.onNodeWithText("선택한 날짜").assertIsDisplayed()
        composeTestRule.onNodeWithText("인식된 날짜").assertDoesNotExist()

        restorationTester.emulateSavedInstanceStateRestore()

        // 재생성 후에도 수정한 날짜와 그 표기가 복원되고 인식된 원래 후보로 되돌아가지 않는다.
        composeTestRule.onNodeWithText("2026년 8월 15일").assertIsDisplayed()
        composeTestRule.onNodeWithText("선택한 날짜").assertIsDisplayed()
        composeTestRule.onNodeWithText("2026년 8월 20일").assertDoesNotExist()

        composeTestRule.onNodeWithText("이 날짜 확인").performClick()

        assertEquals(correctedDate, confirmed)
    }

    @Test
    fun backDoesNotConfirmOrSave() {
        var confirmed: LocalDate? = null
        var wentBack = false

        setConfirmContent(onConfirm = { confirmed = it }, onBack = { wentBack = true })

        composeTestRule.onNodeWithContentDescription("뒤로가기").performClick()

        assertTrue(wentBack)
        assertNull(confirmed)
    }
}
