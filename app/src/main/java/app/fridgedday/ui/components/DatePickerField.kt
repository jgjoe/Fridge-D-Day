package app.fridgedday.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    label: String,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: String? = null
) {
    var showDialog by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    // readOnly 필드는 본문을 눌러도 포커스만 받고 아무 일도 일어나지 않아, 사용자가 날짜 칸을
    // 눌러도 반응이 없다고 느꼈다. 필드가 내보내는 누름 이벤트를 받아 본문 어디를 눌러도
    // 날짜 선택이 열리게 한다. 스크롤·드래그로 중간에 취소된 제스처는 Release가 아니라
    // Cancel로 오므로 열리지 않는다.
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release) {
                showDialog = true
            }
        }
    }

    OutlinedTextField(
        value = selectedDate?.toString().orEmpty(),
        onValueChange = {},
        // 빈 라벨을 그대로 넘기면 Material3가 자리 표시자 대신 빈 라벨을 그려, 날짜를 고르기 전
        // 필드가 비어 보인다. 라벨 없이 쓰는 호출자는 자리 표시자인 "날짜를 선택하세요"가 보이도록
        // null로 낮추고, 등록 화면처럼 라벨을 주는 호출자는 그 라벨이 필드에 그대로 붙는다.
        label = if (label.isBlank()) null else { { Text(label) } },
        placeholder = { Text("날짜를 선택하세요") },
        readOnly = true,
        isError = isError,
        supportingText = supportingText?.let { message -> { Text(message) } },
        interactionSource = interactionSource,
        trailingIcon = {
            IconButton(onClick = { showDialog = true }) {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = "날짜 선택"
                )
            }
        },
        modifier = modifier.fillMaxWidth()
    )

    if (showDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = (selectedDate ?: LocalDate.now())
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val instant = Instant.ofEpochMilli(millis)
                            val date = instant.atZone(ZoneOffset.UTC).toLocalDate()
                            onDateSelected(date)
                        }
                        showDialog = false
                    }
                ) {
                    Text("확인")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("취소")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
