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
 * 라이트에서는 깊은 forest가 화면의 유일한 강한 색면이 되고, 다크에서는 같은 forest의 lifted
 * 컨테이너가 대비를 만든다. 다크에 `primary`를 그대로 쓰면 옅은 연두가 화면에서 가장 밝은 면이
 * 되어 "짙은 forest" 언어가 뒤집힌다. 두 값 모두 기존 역할에서만 나오며 새 색을 만들지 않는다.
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
 * 라이트 배경은 0.88, 다크 배경은 0.01이라 어느 테마 모드로 그려지든 값이 흔들리지 않는다.
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
