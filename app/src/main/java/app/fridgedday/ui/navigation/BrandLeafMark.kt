package app.fridgedday.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.fridgedday.R
import app.fridgedday.ui.components.BundledArtwork

/** 제목 옆에 앉는 20dp 표식 상자. 글자 확대에서도 제목과 한 덩어리로 남는다. */
private val BrandMarkSize = 20.dp

/**
 * 교체한 번들 래스터는 잎을 더 촘촘하게 담은 고해상도 자산이다.
 *
 * 20dp 표식 상자는 그대로 두고 내부에서 1.2배만 살짝 확대한다.
 */
private const val BrandLeafZoom = 1.2f

/**
 * 오늘 화면 제목 옆 표식을 테스트에서 찾을 때 쓰는 tag.
 *
 * 표식은 장식이라 접근성 트리에 문구를 남기지 않는다. 헤더 통합은 이 tag로만 확인한다.
 */
internal const val BrandLeafMarkTag = "today-brand-mark"

/**
 * 오늘 화면 제목 옆의 잎 브랜드 표식.
 *
 * D-020의 브랜드 서명 하나다. 이모지도, 일러스트 시스템도, 화면마다 반복되는 장식도 아니다.
 * D-023에서 도형 대신 앱에 번들된 래스터 잎 자산을 쓰고, 표식 크기와 자리, 장식 계약은 그대로
 * 유지한다. 옆 제목이 이미 의미를 전달하므로 접근성 문구를 넘기지 않아 TalkBack 문구를 중복시키지
 * 않는다.
 */
@Composable
fun BrandLeafMark(modifier: Modifier = Modifier) {
    BundledArtwork(
        resId = R.drawable.brand_leaf,
        size = BrandMarkSize,
        zoom = BrandLeafZoom,
        modifier = modifier.testTag(BrandLeafMarkTag)
    )
}
