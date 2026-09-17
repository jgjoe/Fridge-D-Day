package app.fridgedday.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.fridgedday.FridgeDDayTheme
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * 날짜 필드는 달력 아이콘뿐 아니라 본문을 눌러도 날짜 선택을 연다.
 *
 * 실제 사용자는 날짜 칸 본문을 먼저 눌렀고, 아무 반응이 없어서야 달력 아이콘을 발견했다.
 * 등록 화면과 같은 빈 상태에서 자리 표시자를 눌러 같은 동작이 열리는지 확인한다.
 */
@RunWith(AndroidJUnit4::class)
class DatePickerFieldTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setFieldContent(onDateSelected: (LocalDate) -> Unit = {}) {
        composeTestRule.setContent {
            FridgeDDayTheme {
                DatePickerField(
                    label = "",
                    selectedDate = null,
                    onDateSelected = onDateSelected
                )
            }
        }
    }

    @Test
    fun tappingTheEmptyFieldBodyOpensTheDatePicker() {
        var selected: LocalDate? = null
        setFieldContent { selected = it }

        composeTestRule.onNodeWithText("날짜를 선택하세요").assertIsDisplayed()
        composeTestRule.onNodeWithText("날짜를 선택하세요").performClick()

        composeTestRule.onNodeWithText("확인").assertIsDisplayed()
        composeTestRule.onNodeWithText("취소").assertIsDisplayed()

        // 여는 것만으로는 날짜가 확정되지 않는다.
        assertNull(selected)
    }
}
