package app.fridgedday.ui.scan

import app.fridgedday.util.ocr.TextRecognitionHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * 스캔 결과 분기(D-015).
 *
 * 날짜를 찾으면 확인 화면으로 넘길 날짜 하나만 남고, 실패는 원인별 문구를 가진 한 갈래로 모인다.
 * 어느 실패든 대안은 `직접 입력` 하나이므로 화면이 실패 종류마다 다른 길을 만들지 않는다.
 *
 * 순수 분기라 기기 상태에 의존하지 않지만, 단위 테스트 소스 루트에 `ui/scan` 경로가 없어 이
 * 모듈의 기존 관례를 따라 instrumented 소스에 둔다. 에뮬레이터나 실기기에서 실행한다.
 */
class ScanOutcomeTest {

    private fun evaluation(
        selectedDate: LocalDate? = null,
        processedVariantCount: Int = 1,
        failedVariantCount: Int = 0
    ) = TextRecognitionHelper.OcrEvaluation(
        selectedDate = selectedDate,
        candidateCount = 0,
        candidateDates = emptySet(),
        hasRecognizedText = selectedDate != null,
        processedVariantCount = processedVariantCount,
        failedVariantCount = failedVariantCount
    )

    @Test
    fun recognizedDateBecomesTheConfirmationRouteArgument() {
        val date = LocalDate.of(2026, 9, 30)

        assertEquals(ScanOutcome.Recognized(date), scanOutcome(evaluation(selectedDate = date)))
    }

    @Test
    fun noProcessedVariantWithFailuresReportsTheModelAsUnavailable() {
        val outcome = scanOutcome(evaluation(processedVariantCount = 0, failedVariantCount = 2))

        assertEquals(ScanOutcome.Failed.RECOGNITION_UNAVAILABLE, outcome)
    }

    @Test
    fun processedImageWithoutADateStaysAPlainNoDateFailure() {
        val outcome = scanOutcome(evaluation(processedVariantCount = 2))

        assertEquals(ScanOutcome.Failed.NO_DATE_FOUND, outcome)
    }

    @Test
    fun everyFailureCarriesTheManualEntryCopy() {
        for (failure in ScanOutcome.Failed.entries) {
            assertTrue(failure.message.isNotBlank())
        }
    }

    @Test
    fun failureCopyNeverBlamesAMissingInternetConnection() {
        for (failure in ScanOutcome.Failed.entries) {
            assertFalse(
                "한국어 모델은 앱에 포함되어 오프라인에서도 동작한다: ${failure.message}",
                failure.message.contains("인터넷")
            )
        }
    }
}
