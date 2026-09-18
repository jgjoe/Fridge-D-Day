package app.fridgedday

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import app.fridgedday.data.pref.SettingsDataStore
import app.fridgedday.data.repo.SettingsRepository
import app.fridgedday.ui.navigation.AppNavHost
import app.fridgedday.util.NotificationUtils
import app.fridgedday.worker.WorkScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create notification channels
        NotificationUtils.createNotificationChannels(this)

        // Schedule initial work
        lifecycleScope.launch {
            val dataStore = SettingsDataStore(applicationContext)
            val repository = SettingsRepository(dataStore)
            val settings = repository.settings.first()
            WorkScheduler.scheduleDailyCheck(applicationContext, settings)
        }

        setContent {
            val context = LocalContext.current
            val dataStore = remember { SettingsDataStore(context) }
            val repository = remember { SettingsRepository(dataStore) }
            val settings by repository.settings.collectAsState(initial = app.fridgedday.data.pref.AppSettings())

            // Re-schedule work when settings change
            LaunchedEffect(settings.dailyNotify, settings.notifyTime) {
                if (settings.dailyNotify) {
                    WorkScheduler.scheduleDailyCheck(context, settings)
                } else {
                    WorkScheduler.cancelDailyCheck(context)
                }
            }

            FridgeDDayTheme(themeMode = settings.themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    AppNavHost(navController = navController)
                }
            }
        }
    }
}

