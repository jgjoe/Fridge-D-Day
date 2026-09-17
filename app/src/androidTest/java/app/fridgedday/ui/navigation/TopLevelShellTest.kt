package app.fridgedday.ui.navigation

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.fridgedday.FridgeDDayTheme
import app.fridgedday.data.db.AppDatabase
import app.fridgedday.data.pref.SettingsDataStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TopLevelShellTest {
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

    private fun awaitText(text: String) {
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun tapTab(label: String) {
        composeTestRule.onNode(isSelectable() and hasText(label)).performClick()
    }

    private fun routes() = navController.currentBackStack.value.mapNotNull { it.destination.route }

    @Test
    fun tabsSwapRoutesImmediatelyWithoutAccumulatingCopies() {
        setAppContent()
        awaitText("어떤 식품을 기록할까요?")

        tapTab("기록")
        awaitText("식품 기록")
        assertEquals(listOf(Destinations.HOME, Destinations.RECORD), routes())

        tapTab("설정")
        awaitText("매일 알림 받기")
        assertEquals(listOf(Destinations.HOME, Destinations.SETTINGS), routes())

        tapTab("오늘")
        awaitText("어떤 식품을 기록할까요?")
        assertEquals(listOf(Destinations.HOME), routes())
    }

    @Test
    fun scanAndAddActionsOpenTheExistingScanRoute() {
        setAppContent()
        awaitText("바로 스캔하기")

        composeTestRule.onNodeWithText("바로 스캔하기").performClick()
        awaitText("촬영 이미지는 저장하지 않아요")
        assertEquals(listOf(Destinations.HOME, Destinations.SCAN), routes())
    }

    @Test
    fun sharedBrandAndSearchActionsMatchEachDestination() {
        setAppContent()
        awaitText("어떤 식품을 기록할까요?")
        composeTestRule.onNodeWithTag(BrandLeafMarkTag).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("식품 검색").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("스캔").assertDoesNotExist()
        val todayHeaderBounds = sharedHeaderBounds()

        tapTab("기록")
        awaitText("식품 기록")
        composeTestRule.onNodeWithTag(BrandLeafMarkTag).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("식품 검색").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("스캔").assertIsDisplayed()
        val recordHeaderBounds = sharedHeaderBounds()

        tapTab("설정")
        awaitText("매일 알림 받기")
        composeTestRule.onNodeWithTag(BrandLeafMarkTag).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("식품 검색").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("스캔").assertDoesNotExist()
        val settingsHeaderBounds = sharedHeaderBounds()

        assertHeaderBoundsEqual(todayHeaderBounds, recordHeaderBounds)
        assertHeaderBoundsEqual(todayHeaderBounds, settingsHeaderBounds)
    }

    @Test
    fun floatingActionInsetExistsOnlyWhenTheActionIsShown() {
        val showAddAction = mutableStateOf(false)
        composeTestRule.setContent {
            FridgeDDayTheme {
                TopLevelScaffold(
                    selected = TopLevelDestination.SETTINGS,
                    navController = rememberNavController(),
                    showAddAction = showAddAction.value,
                    modifier = Modifier.fillMaxSize()
                ) { paddingValues ->
                    Column(Modifier.fillMaxSize().padding(paddingValues)) {
                        Text(
                            LocalTopLevelBottomContentInset.current.value.toString(),
                            modifier = Modifier.testTag(InsetProbeTag)
                        )
                    }
                }
            }
        }

        assertEquals("0.0", composeTestRule.onNodeWithTag(InsetProbeTag).fetchSemanticsNode()
            .config[androidx.compose.ui.semantics.SemanticsProperties.Text].first().text)

        composeTestRule.runOnIdle { showAddAction.value = true }
        composeTestRule.onNodeWithContentDescription("스캔").assertIsDisplayed()
        val insetText = composeTestRule.onNodeWithTag(InsetProbeTag).fetchSemanticsNode()
            .config[androidx.compose.ui.semantics.SemanticsProperties.Text].first().text
        assertTrue(insetText.toFloat() >= AddFoodActionLane.value)
    }

    private fun sharedHeaderBounds(): HeaderBounds {
        val brand = composeTestRule.onNodeWithTag(BrandLeafMarkTag).getUnclippedBoundsInRoot()
        val title = composeTestRule.onNodeWithText("오늘도 신선").getUnclippedBoundsInRoot()
        return HeaderBounds(
            brand.top.value,
            brand.bottom.value,
            title.top.value,
            title.bottom.value
        )
    }

    private fun assertHeaderBoundsEqual(expected: HeaderBounds, actual: HeaderBounds) {
        assertEquals(expected.brandTop, actual.brandTop, 0.5f)
        assertEquals(expected.brandBottom, actual.brandBottom, 0.5f)
        assertEquals(expected.titleTop, actual.titleTop, 0.5f)
        assertEquals(expected.titleBottom, actual.titleBottom, 0.5f)
    }

    private data class HeaderBounds(
        val brandTop: Float,
        val brandBottom: Float,
        val titleTop: Float,
        val titleBottom: Float
    )

    private companion object {
        const val InsetProbeTag = "shell-bottom-inset-probe"
    }
}
