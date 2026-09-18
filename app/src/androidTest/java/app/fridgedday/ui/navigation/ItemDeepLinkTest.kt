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
import app.fridgedday.data.db.entity.ItemEntity
import app.fridgedday.data.db.entity.StorageLocation
import app.fridgedday.data.pref.SettingsDataStore
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * 기존 항목 딥링크(fridgedday://item/{id})는 수정 화면이 아니라 상세 화면으로 연결된다.
 */
@RunWith(AndroidJUnit4::class)
class ItemDeepLinkTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Test
    fun itemDeepLinkOpensTheDetailInsteadOfTheEditForm() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val id = runBlocking {
            val database = AppDatabase.getDatabase(context)
            database.clearAllTables()
            SettingsDataStore(context).setFirstLaunchCompleted()
            database.itemDao().insert(
                ItemEntity(
                    name = "딥링크우유",
                    location = StorageLocation.FRIDGE,
                    expiryDate = LocalDate.now().plusDays(5)
                )
            )
        }

        ActivityScenario.launch<MainActivity>(
            Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse("fridgedday://item/$id")
            }
        ).use {
            composeTestRule.waitUntil(timeoutMillis = 10_000) {
                composeTestRule.onAllNodesWithText("식품 상세").fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText("딥링크우유").assertIsDisplayed()
            composeTestRule.onNodeWithText("소비 완료").assertIsDisplayed()
            composeTestRule.onNodeWithText("항목 수정").assertDoesNotExist()
        }
    }
}
