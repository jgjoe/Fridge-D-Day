package app.fridgedday.ui.scan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.fridgedday.util.DateUtils
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * OCR이 찾은 날짜 후보를 확정하는 task 화면.
 *
 * 카메라 화면과 달리 이 화면 자체는 아무것도 저장하지 않는다. 확정은 오직
 * "이 날짜 확인"으로만 일어나고, 그마저도 등록 화면에 이미 확정된 날짜를 넘길 뿐이다.
 */

/** 확정 전 수정한 날짜가 화면 재생성 후에도 그대로 복원되도록 epochDay로 저장한다. */
private val LocalDateSaver: Saver<LocalDate, Long> = Saver(
    save = { it.toEpochDay() },
    restore = { LocalDate.ofEpochDay(it) }
)

/**
 * 스크롤 본문의 최소 높이.
 *
 * 남는 높이가 있으면 인식 결과와 버튼 묶음을 위아래로 벌려 두고, 글자 확대가 커져 내용이
 * 이 높이를 넘으면 본문 전체가 스크롤되어 확정 버튼이 화면 밖으로 나가지 않는다.
 */
private val ConfirmContentMinHeight = 420.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrConfirmScreen(
    detectedDate: LocalDate,
    onConfirm: (LocalDate) -> Unit,
    onBack: () -> Unit
) {
    var currentDate by rememberSaveable(detectedDate, stateSaver = LocalDateSaver) {
        mutableStateOf(detectedDate)
    }
    // 사용자가 날짜를 직접 고른 뒤에는 라벨이 `인식된 날짜`에서 `선택한 날짜`로 바뀐다(D-016).
    // 다이얼로그를 열었다 취소한 경우는 수정이 아니므로 인식 결과 표기를 유지한다.
    var dateEdited by rememberSaveable(detectedDate) { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OCR 확인") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                    }
                }
            )
        }
    ) { paddingValues ->
        // 글자 확대가 커지면 확정 버튼이 화면 밖으로 밀릴 수 있어 스크롤을 둔다.
        // 높이가 남는 화면에서는 SpaceBetween이 인식 결과와 확정·수정 묶음을 위아래로 벌려 둔다.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = ConfirmContentMinHeight)
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (dateEdited) "선택한 날짜" else "인식된 날짜",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = DateUtils.formatKorean(currentDate),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "인식 결과는 저장 전 확인",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { onConfirm(currentDate) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Text("이 날짜 확인")
                }
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Text("수정")
                }
            }
        }
    }

    if (showDatePicker) {
        OcrDatePickerDialog(
            initialDate = currentDate,
            onDateSelected = { selectedDate ->
                currentDate = selectedDate
                dateEdited = true
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OcrDatePickerDialog(
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selectedDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                        onDateSelected(selectedDate)
                    }
                }
            ) {
                Text("확인")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}
