package app.fridgedday.data.pref

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalTime

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val DAILY_NOTIFY = booleanPreferencesKey("daily_notify")
        val NOTIFY_HOUR = intPreferencesKey("notify_hour")
        val NOTIFY_MINUTE = intPreferencesKey("notify_minute")
        val DEFAULT_DAYS_BEFORE = intPreferencesKey("default_days_before")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
        val NOTIFICATION_PERMISSION_DENIED = booleanPreferencesKey("notification_permission_denied")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            dailyNotify = prefs[Keys.DAILY_NOTIFY] ?: true,
            notifyTime = LocalTime.of(
                prefs[Keys.NOTIFY_HOUR] ?: 9,
                prefs[Keys.NOTIFY_MINUTE] ?: 0
            ),
            defaultDaysBefore = prefs[Keys.DEFAULT_DAYS_BEFORE] ?: 3,
            themeMode = try {
                ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: "SYSTEM")
            } catch (e: Exception) {
                ThemeMode.SYSTEM
            },
            isFirstLaunch = prefs[Keys.IS_FIRST_LAUNCH] ?: true,
            notificationPermissionDenied = prefs[Keys.NOTIFICATION_PERMISSION_DENIED] ?: false
        )
    }

    suspend fun updateDailyNotify(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DAILY_NOTIFY] = enabled
        }
    }

    suspend fun updateNotifyTime(hour: Int, minute: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.NOTIFY_HOUR] = hour
            prefs[Keys.NOTIFY_MINUTE] = minute
        }
    }

    suspend fun updateDefaultDaysBefore(days: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DEFAULT_DAYS_BEFORE] = days
        }
    }

    suspend fun updateThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
        }
    }

    suspend fun setFirstLaunchCompleted() {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_FIRST_LAUNCH] = false
        }
    }

    /**
     * 시스템 알림 권한 요청 결과를 실제 거부 이력에 반영한다.
     *
     * 허용이면 이력을 지우고, 거부여도 rationale이 남아 있으면 실제 거부로 기록하며,
     * 다이얼로그 취소처럼 둘 다 아니면 기존 이력을 그대로 둔다.
     */
    suspend fun recordNotificationPermissionResult(isGranted: Boolean, shouldShowRationale: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.NOTIFICATION_PERMISSION_DENIED] = notificationPermissionDenialAfterResult(
                priorRealDenial = prefs[Keys.NOTIFICATION_PERMISSION_DENIED] ?: false,
                isGranted = isGranted,
                shouldShowRationale = shouldShowRationale
            )
        }
    }
}

/**
 * 시스템 요청 결과 뒤에 남길 실제 거부 이력.
 *
 * - 허용: 이력을 지운다.
 * - 거부 + rationale=true: 사용자가 실제로 거부했으므로 이력을 남긴다.
 * - 거부 + rationale=false: 다이얼로그 취소일 수 있으므로 기존 이력을 그대로 둔다.
 */
internal fun notificationPermissionDenialAfterResult(
    priorRealDenial: Boolean,
    isGranted: Boolean,
    shouldShowRationale: Boolean
): Boolean = when {
    isGranted -> false
    shouldShowRationale -> true
    else -> priorRealDenial
}
