package app.fridgedday.data.pref

import java.time.LocalTime

data class AppSettings(
    val dailyNotify: Boolean = true,
    val notifyTime: LocalTime = LocalTime.of(9, 0),
    val defaultDaysBefore: Int = 3,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isFirstLaunch: Boolean = true,
    // 실제 거부 이력(다이얼로그 취소와 구분). 시스템이 다시 물어볼 수 없을 때만 앱 설정으로 올린다(D-015).
    val notificationPermissionDenied: Boolean = false
)

enum class ThemeMode {
    SYSTEM,  // 시스템 설정 따르기
    LIGHT,   // 라이트 모드
    DARK     // 다크 모드
}
