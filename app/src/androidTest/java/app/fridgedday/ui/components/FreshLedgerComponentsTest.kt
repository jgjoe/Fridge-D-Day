package app.fridgedday.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.fridgedday.FridgeDDayTheme
import app.fridgedday.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Fresh Ledger의 두 표현 primitive를 고정한다.
 *
 * - [DDayTile]은 상대 신호를 한 줄로 온전히 보여주고, 큰 글자 크기에서도 잘리지 않는다.
 * - [IconDisc]는 라벨을 넘겼을 때만 그 라벨을 노출한다. 옆 텍스트가 의미를 전달하는 자리에서는
 *   장식으로 남아 TalkBack 문구를 중복시키지 않는다.
 */
@RunWith(AndroidJUnit4::class)
class FreshLedgerComponentsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun dDayTileShowsSafeWarningAndExpiredSignals() {
        val today = LocalDate.now()

        composeTestRule.setContent {
            FridgeDDayTheme {
                Row {
                    DDayTile(expiryDate = today.plusDays(20))
                    DDayTile(expiryDate = today.plusDays(3))
                    DDayTile(expiryDate = today.minusDays(1))
                }
            }
        }

        composeTestRule.onNodeWithText("D-20").assertIsDisplayed()
        composeTestRule.onNodeWithText("D-3").assertIsDisplayed()
        composeTestRule.onNodeWithText("D+1").assertIsDisplayed()
    }

    @Test
    fun dDayTileKeepsTheFullRelativeSignalAtLargeFontScale() {
        // A32 회귀: font_scale=1.3에서 D-295가 타일 폭 상한 72dp에 잘려 D-29로 읽혔다.
        val expiry = LocalDate.now().plusDays(295)
        val dDayText = DateUtils.formatDDay(expiry)
        var requiredTextWidth = 0.dp

        composeTestRule.setContent {
            FridgeDDayTheme {
                CompositionLocalProvider(
                    LocalDensity provides Density(
                        density = LocalDensity.current.density,
                        fontScale = 1.3f
                    )
                ) {
                    // 같은 테마·글자 스타일로 잰 한 줄 폭. 이보다 좁으면 Text가 잘린 것이다.
                    val textMeasurer = rememberTextMeasurer()
                    requiredTextWidth = with(LocalDensity.current) {
                        textMeasurer.measure(
                            text = dDayText,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        ).size.width.toDp()
                    }
                    DDayTile(expiryDate = expiry)
                }
            }
        }

        composeTestRule.onNodeWithText(dDayText).assertWidthIsAtLeast(requiredTextWidth)
        val tileBounds = composeTestRule.onNodeWithTag(DDayTileTag)
            .getUnclippedBoundsInRoot()
        val textBounds = composeTestRule.onNodeWithText(dDayText)
            .getUnclippedBoundsInRoot()
        val tileCenter = (tileBounds.left.value + tileBounds.right.value) / 2f
        val textCenter = (textBounds.left.value + textBounds.right.value) / 2f
        assertEquals(tileCenter, textCenter, 0.5f)
    }

    @Test
    fun dDayTileCarriesTheDayWithoutAnExpiryDate() {
        composeTestRule.setContent {
            FridgeDDayTheme {
                DDayTile(expiryDate = LocalDate.now())
            }
        }

        composeTestRule.onNodeWithText("D-Day").assertIsDisplayed()
    }

    @Test
    fun iconDiscSpeaksTheLabelItIsGiven() {
        composeTestRule.setContent {
            FridgeDDayTheme {
                IconDisc(
                    icon = Icons.Filled.Inventory2,
                    contentDescription = "비밀번호 변경",
                    action = true
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("비밀번호 변경").assertIsDisplayed()
    }

    @Test
    fun iconDiscStaysDecorativeWhenTheAdjacentTextAlreadyLabelsIt() {
        composeTestRule.setContent {
            FridgeDDayTheme {
                IconDisc(icon = Icons.Filled.Inventory2)
            }
        }

        // 옆 텍스트가 의미를 전달하는 자리에서는 아이콘이 별도 라벨을 만들지 않는다.
        composeTestRule.onNodeWithContentDescription("").assertDoesNotExist()
    }
}
