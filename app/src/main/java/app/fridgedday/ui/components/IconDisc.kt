package app.fridgedday.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 기본 원형 컨테이너 크기. 일반 도구 아이콘이 들어가는 ordinary 크기다. */
private val DefaultDiscSize = 36.dp

/** 원 안의 아이콘. 컨테이너 지름의 약 절반이라 여백이 남고 작은 원에서도 뭉개지지 않는다. */
private val DiscIconSize = 18.dp

/**
 * 원형 아이콘 컨테이너.
 *
 * v2 후반의 Fresh Ledger 언어에서 아이콘은 맨 Material 아이콘으로 떠 있지 않고, 의도된 원 안에
 * 들어가 계층을 만든다. 기본은 중립 표면이고, 지금 위치/현재 동작을 가리킬 때만 [action]으로
 * 같은 forest 톤을 쓴다.
 *
 * 접근성: [contentDescription]을 넘기지 않으면 장식 아이콘으로 남는다. 옆 텍스트가 이미 의미를
 * 전달하는 자리에서는 넘기지 않는다 — TalkBack 문구를 중복해서 읽지 않기 위해서다.
 */
@Composable
fun IconDisc(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    size: Dp = DefaultDiscSize,
    action: Boolean = false
) {
    val containerColor = if (action) actionCurrentContainer() else neutralDiscContainer()
    val contentColor = if (action) actionCurrentContent() else neutralDiscContent()

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(DiscIconSize)
        )
    }
}

/**
 * 지금 위치/현재 동작을 가리키는 forest 컨테이너.
 *
 * shipping v2는 D-028에 따라 라이트 전용이므로 실제 제품에서는 `primary` 역할을 사용한다.
 * 비-light ColorScheme이 테스트/프리뷰에서 주입될 때의 방어 분기는 남아 있지만 제품의 다크 모드
 * 지원을 의미하지 않는다.
 */
@Composable
internal fun actionCurrentContainer(): Color =
    if (isLightScheme()) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }

/** [actionCurrentContainer] 위에서 읽히는 content 역할. */
@Composable
internal fun actionCurrentContent(): Color =
    if (isLightScheme()) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }

/**
 * 지금 색 구성표가 밝은 쪽인지.
 *
 * `ColorScheme.isLight`는 Material3 1.2.0에서 공개 API가 아니므로 배경의 실제 휘도로 판정한다.
 * 제품은 light-only지만 비-light ColorScheme을 주입하는 테스트/프리뷰 방어를 위해 이 판정은 유지한다.
 */
@Composable
private fun isLightScheme(): Boolean = MaterialTheme.colorScheme.background.luminance() > 0.5f

/**
 * 중립 원형 컨테이너.
 *
 * grouped 표면이 `surfaceContainerLow`이므로 일반 도구 아이콘은 그보다 한 단계 들어 올린
 * `surfaceContainerHigh`에 앉힌다.
 */
@Composable
private fun neutralDiscContainer(): Color = MaterialTheme.colorScheme.surfaceContainerHigh

/** 중립 원 위에서 읽히는 아이콘 색. */
@Composable
private fun neutralDiscContent(): Color = MaterialTheme.colorScheme.onSurfaceVariant
