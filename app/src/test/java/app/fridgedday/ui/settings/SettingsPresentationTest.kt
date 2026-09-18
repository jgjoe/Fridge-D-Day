package app.fridgedday.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

/**
 * 설정 화면이 표시하는 값 표기 규칙을 고정한다.
 */
class SettingsPresentationTest {

    @Test
    fun notifyTimeIsShownAsZeroPaddedTwentyFourHourClock() {
        assertEquals("00:00", formatNotifyTime(LocalTime.MIDNIGHT))
        assertEquals("09:05", formatNotifyTime(LocalTime.of(9, 5)))
    }
}
