package app.fridgedday.ui.navigation

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.fridgedday.FridgeDDayTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppBottomBarBoundsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var scrollState: ScrollState

    private fun setShellContent() {
        composeTestRule.setContent {
            FridgeDDayTheme {
                val state = rememberScrollState()
                scrollState = state
                TopLevelScaffold(
                    selected = TopLevelDestination.TODAY,
                    navController = rememberNavController(),
                    showAddAction = true,
                    modifier = Modifier.fillMaxSize()
                ) { paddingValues ->
                    Box(Modifier.fillMaxSize().padding(paddingValues)) {
                        Box(Modifier.fillMaxSize().testTag(ViewportTag)) {
                            Column(
                                Modifier
                                    .fillMaxSize()
                                    .verticalScroll(state)
                                    .padding(bottom = LocalTopLevelBottomContentInset.current)
                            ) {
                                Box(Modifier.fillMaxWidth().height(1200.dp))
                                Text("마지막 행", Modifier.testTag(LastRowTag))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun tab(label: String) = composeTestRule.onNode(isSelectable() and hasText(label))
    private fun barTop() = tab("오늘").onParent().getUnclippedBoundsInRoot().top

    @Test
    fun allNavigationTargetsStayAtLeast48DpAndNearTheBottom() {
        setShellContent()
        val rootHeight = composeTestRule.onRoot().getUnclippedBoundsInRoot().height
        val targets = listOf(
            tab("오늘"),
            tab("기록"),
            tab("설정"),
            composeTestRule.onNodeWithContentDescription("스캔")
        )

        targets.forEach { node ->
            val bounds = node.getUnclippedBoundsInRoot()
            assertTrue(bounds.height >= 48.dp)
            assertTrue(bounds.height <= rootHeight / 2)
            assertTrue(bounds.top >= rootHeight * 0.6f)
        }
    }

    @Test
    fun threeTabsHaveEqualWidthsAndEvenCenters() {
        setShellContent()
        val today = tab("오늘").getUnclippedBoundsInRoot()
        val record = tab("기록").getUnclippedBoundsInRoot()
        val settings = tab("설정").getUnclippedBoundsInRoot()

        assertEquals(today.width.value, record.width.value, 1f)
        assertEquals(record.width.value, settings.width.value, 1f)
        assertEquals(
            ((record.left + record.right) / 2 - (today.left + today.right) / 2).value,
            ((settings.left + settings.right) / 2 - (record.left + record.right) / 2).value,
            1f
        )
    }

    @Test
    fun addActionIsLowerRightAndScrollContentClearsIt() {
        setShellContent()
        composeTestRule.runOnIdle { runBlocking { scrollState.scrollTo(scrollState.maxValue) } }

        val root = composeTestRule.onRoot().getUnclippedBoundsInRoot()
        val action = composeTestRule.onNodeWithContentDescription("스캔").getUnclippedBoundsInRoot()
        val lastRow = composeTestRule.onNodeWithTag(LastRowTag).getUnclippedBoundsInRoot()

        assertTrue(action.right.value >= root.right.value - 32.dp.value)
        assertTrue((barTop() - action.bottom).value >= 15f)
        assertTrue(lastRow.bottom.value <= action.top.value + 1f)
    }

    @Test
    fun contentViewportStillReachesTheNavigationBar() {
        setShellContent()
        val viewport = composeTestRule.onNodeWithTag(ViewportTag).getUnclippedBoundsInRoot()
        assertTrue(viewport.bottom.value >= barTop().value - 2f)
    }

    private companion object {
        const val ViewportTag = "top-level-content-viewport"
        const val LastRowTag = "top-level-last-row"
    }
}
