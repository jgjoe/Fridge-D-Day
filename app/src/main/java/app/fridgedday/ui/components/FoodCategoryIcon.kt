package app.fridgedday.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.fridgedday.R

/**
 * 행 앞머리에 붙는 음식군 표식의 기본 크기. 행 이름과 D-Day 신호 사이에서 시선을 뺏지 않는 크기다.
 */
private val DefaultFoodMarkSize = 40.dp

/**
 * D-024 assets are cropped and Lanczos-resampled ahead of time, so runtime rendering no longer
 * magnifies a tiny picture through a mostly transparent canvas.
 */
private const val FoodArtworkZoom = 1f

/**
 * 화면에 노출되지 않는 장식용 음식군.
 *
 * 새 필드가 아니고 `ItemEntity.category`에 들어올 수 있는 값도 아니다. 저장소·백업·카테고리
 * 표기는 그대로 두고, 이미 있는 category와 이름에서 이 표식이 쓸 도형만 고른다.
 */
enum class FoodGroup {
    VEGETABLES,
    FRUIT,
    MEAT,
    DAIRY,
    EGG,
    DRINK,
    SEAFOOD,
    GRAIN,
    PREPARED,
    PROCESSED,
    GENERIC
}

/**
 * 로컬 음식군 표식.
 *
 * 그림은 앱에 번들된 래스터 자산에서만 온다. 네트워크·사용자 사진·생성 모델을 쓰지 않으므로
 * 오프라인에서 항상 같은 그림이 나온다. 자산 자체가 색을 가진 그림이므로 테마 색으로 물들이지
 * 않고 원본 색을 그대로 쓴다. shipping v2는 D-028에 따라 라이트 전용이다.
 *
 * 장식이다. 음식 이름·보관 위치·날짜가 계속 의미를 전달하므로 접근성 문구를 만들지 않는다
 * ([BundledArtwork]가 `contentDescription = null`로 그린다).
 */
@Composable
internal fun FoodCategoryMark(
    group: FoodGroup,
    modifier: Modifier = Modifier,
    size: Dp = DefaultFoodMarkSize
) {
    BundledArtwork(
        resId = artworkResource(group),
        size = size,
        zoom = FoodArtworkZoom,
        modifier = modifier
    )
}

/**
 * 앱에 번들된 래스터 그림을 [size] 크기의 상자에 맞춰 그린다.
 *
 * 자산 캔버스는 그림 둘레에 투명한 여백을 크게 두므로, 캔버스를 [zoom]배로 그린 뒤 상자 밖 여백만
 * [clipToBounds]로 잘라낸다. 여백은 투명하므로 잘리는 것은 배경뿐이고, 상자의 크기가 곧 화면에
 * 보이는 그림의 크기가 된다. 자식에 `requiredSize`를 써야 부모 상자의 제약을 넘겨 그릴 수 있다.
 *
 * 늘 장식이다. 옆 텍스트가 이미 의미를 전달하는 자리에서 쓰이므로 접근성 문구를 넘기지 않는다.
 * 그림을 대신 설명할 정보가 필요하면 호출자가 텍스트로 만든다.
 */
@Composable
internal fun BundledArtwork(
    @DrawableRes resId: Int,
    size: Dp,
    zoom: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clipToBounds(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(resId),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.requiredSize(size * zoom)
        )
    }
}

/**
 * 기존 category와 품목 이름에서 음식군을 고른다.
 *
 * 우선순위는 (1) category에서 읽히는 낱말, (2) 이름에서 읽히는 낱말, (3) [FoodGroup.GENERIC]이다.
 * category가 비었거나 낯설면 이름으로 내려가고, 둘 다 확실하지 않으면 장식을 단정하지 않고
 * generic 음식 표식으로 남긴다.
 *
 * 이름 안에서 여러 낱말이 겹치면 오른쪽(마지막) 낱말을 고른다. 한국어 식품 이름에서 실제 품목은
 * 대개 마지막 낱말이기 때문이다 — `딸기우유`는 우유, `사과주스`는 주스다. 같은 위치에서 길이가
 * 다르면 긴 낱말이 이긴다(`라면`이 `면`보다 먼저다).
 */
internal fun resolveFoodGroup(category: String?, name: String): FoodGroup =
    matchFoodKeyword(category)
        ?: matchFoodKeyword(name)
        ?: FoodGroup.GENERIC

private fun matchFoodKeyword(text: String?): FoodGroup? {
    if (text.isNullOrBlank()) return null
    val normalized = text.lowercase().filterNot { it.isWhitespace() }
    if (normalized.isEmpty()) return null

    var bestGroup: FoodGroup? = null
    var bestIndex = -1
    var bestLength = 0
    FoodKeywords.forEach { keyword ->
        val index = normalized.lastIndexOf(keyword.text)
        if (index < 0) return@forEach
        if (keyword.wholeNameOnly && normalized != keyword.text) return@forEach
        if (index > bestIndex || (index == bestIndex && keyword.text.length > bestLength)) {
            bestIndex = index
            bestLength = keyword.text.length
            bestGroup = keyword.group
        }
    }
    return bestGroup
}

/**
 * 낱말 하나와 그 낱말이 가리키는 음식군.
 *
 * [wholeNameOnly]는 이름 전체가 그 낱말일 때만 인정한다. `배`·`무`·`파`처럼 짧은 낱말이 다른
 * 낱말(`배추`, `무침`, `파인애플`) 안에서 잘못 잡히는 것을 막는다.
 */
private class FoodKeyword(
    val text: String,
    val group: FoodGroup,
    val wholeNameOnly: Boolean = false
)

/** 확실한 낱말만 담는다. 목록 순서는 같은 위치·같은 길이의 경합에서만 쓰이는 마지막 기준이다. */
private val FoodKeywords: List<FoodKeyword> = listOf(
    // 채소
    FoodKeyword("채소", FoodGroup.VEGETABLES),
    FoodKeyword("야채", FoodGroup.VEGETABLES),
    FoodKeyword("vegetable", FoodGroup.VEGETABLES),
    FoodKeyword("상추", FoodGroup.VEGETABLES),
    FoodKeyword("lettuce", FoodGroup.VEGETABLES),
    FoodKeyword("시금치", FoodGroup.VEGETABLES),
    FoodKeyword("spinach", FoodGroup.VEGETABLES),
    FoodKeyword("배추", FoodGroup.VEGETABLES),
    FoodKeyword("양배추", FoodGroup.VEGETABLES),
    FoodKeyword("cabbage", FoodGroup.VEGETABLES),
    FoodKeyword("당근", FoodGroup.VEGETABLES),
    FoodKeyword("carrot", FoodGroup.VEGETABLES),
    FoodKeyword("오이", FoodGroup.VEGETABLES),
    FoodKeyword("cucumber", FoodGroup.VEGETABLES),
    FoodKeyword("양파", FoodGroup.VEGETABLES),
    FoodKeyword("대파", FoodGroup.VEGETABLES),
    FoodKeyword("onion", FoodGroup.VEGETABLES),
    FoodKeyword("파프리카", FoodGroup.VEGETABLES),
    FoodKeyword("피망", FoodGroup.VEGETABLES),
    FoodKeyword("마늘", FoodGroup.VEGETABLES),
    FoodKeyword("garlic", FoodGroup.VEGETABLES),
    FoodKeyword("감자", FoodGroup.VEGETABLES),
    FoodKeyword("고구마", FoodGroup.VEGETABLES),
    FoodKeyword("potato", FoodGroup.VEGETABLES),
    FoodKeyword("버섯", FoodGroup.VEGETABLES),
    FoodKeyword("mushroom", FoodGroup.VEGETABLES),
    FoodKeyword("브로콜리", FoodGroup.VEGETABLES),
    FoodKeyword("broccoli", FoodGroup.VEGETABLES),
    FoodKeyword("콩나물", FoodGroup.VEGETABLES),
    FoodKeyword("깻잎", FoodGroup.VEGETABLES),
    FoodKeyword("부추", FoodGroup.VEGETABLES),
    FoodKeyword("미역", FoodGroup.VEGETABLES),
    FoodKeyword("호박", FoodGroup.VEGETABLES),
    FoodKeyword("pumpkin", FoodGroup.VEGETABLES),
    FoodKeyword("옥수수", FoodGroup.VEGETABLES),
    FoodKeyword("corn", FoodGroup.VEGETABLES),
    FoodKeyword("토마토", FoodGroup.VEGETABLES),
    FoodKeyword("tomato", FoodGroup.VEGETABLES),
    FoodKeyword("eggplant", FoodGroup.VEGETABLES),
    FoodKeyword("가지", FoodGroup.VEGETABLES, wholeNameOnly = true),
    FoodKeyword("무우", FoodGroup.VEGETABLES),
    FoodKeyword("파", FoodGroup.VEGETABLES, wholeNameOnly = true),
    FoodKeyword("무", FoodGroup.VEGETABLES, wholeNameOnly = true),
    // 과일
    FoodKeyword("과일", FoodGroup.FRUIT),
    FoodKeyword("청과", FoodGroup.FRUIT),
    FoodKeyword("fruit", FoodGroup.FRUIT),
    FoodKeyword("사과", FoodGroup.FRUIT),
    FoodKeyword("apple", FoodGroup.FRUIT),
    FoodKeyword("바나나", FoodGroup.FRUIT),
    FoodKeyword("banana", FoodGroup.FRUIT),
    FoodKeyword("귤", FoodGroup.FRUIT),
    FoodKeyword("오렌지", FoodGroup.FRUIT),
    FoodKeyword("orange", FoodGroup.FRUIT),
    FoodKeyword("포도", FoodGroup.FRUIT),
    FoodKeyword("grape", FoodGroup.FRUIT),
    FoodKeyword("딸기", FoodGroup.FRUIT),
    FoodKeyword("strawberry", FoodGroup.FRUIT),
    FoodKeyword("수박", FoodGroup.FRUIT),
    FoodKeyword("watermelon", FoodGroup.FRUIT),
    FoodKeyword("참외", FoodGroup.FRUIT),
    FoodKeyword("복숭아", FoodGroup.FRUIT),
    FoodKeyword("peach", FoodGroup.FRUIT),
    FoodKeyword("키위", FoodGroup.FRUIT),
    FoodKeyword("kiwi", FoodGroup.FRUIT),
    FoodKeyword("망고", FoodGroup.FRUIT),
    FoodKeyword("mango", FoodGroup.FRUIT),
    FoodKeyword("블루베리", FoodGroup.FRUIT),
    FoodKeyword("베리", FoodGroup.FRUIT),
    FoodKeyword("berry", FoodGroup.FRUIT),
    FoodKeyword("자두", FoodGroup.FRUIT),
    FoodKeyword("체리", FoodGroup.FRUIT),
    FoodKeyword("cherry", FoodGroup.FRUIT),
    FoodKeyword("레몬", FoodGroup.FRUIT),
    FoodKeyword("lemon", FoodGroup.FRUIT),
    FoodKeyword("라임", FoodGroup.FRUIT),
    FoodKeyword("멜론", FoodGroup.FRUIT),
    FoodKeyword("melon", FoodGroup.FRUIT),
    FoodKeyword("파인애플", FoodGroup.FRUIT),
    FoodKeyword("pineapple", FoodGroup.FRUIT),
    FoodKeyword("아보카도", FoodGroup.FRUIT),
    FoodKeyword("avocado", FoodGroup.FRUIT),
    FoodKeyword("석류", FoodGroup.FRUIT),
    FoodKeyword("무화과", FoodGroup.FRUIT),
    FoodKeyword("배", FoodGroup.FRUIT, wholeNameOnly = true),
    FoodKeyword("감", FoodGroup.FRUIT, wholeNameOnly = true),
    // 고기
    FoodKeyword("고기", FoodGroup.MEAT),
    FoodKeyword("육류", FoodGroup.MEAT),
    FoodKeyword("정육", FoodGroup.MEAT),
    FoodKeyword("meat", FoodGroup.MEAT),
    FoodKeyword("소고기", FoodGroup.MEAT),
    FoodKeyword("돼지고기", FoodGroup.MEAT),
    FoodKeyword("닭고기", FoodGroup.MEAT),
    FoodKeyword("닭가슴살", FoodGroup.MEAT),
    FoodKeyword("삼겹살", FoodGroup.MEAT),
    FoodKeyword("목살", FoodGroup.MEAT),
    FoodKeyword("갈비", FoodGroup.MEAT),
    FoodKeyword("안심", FoodGroup.MEAT),
    FoodKeyword("등심", FoodGroup.MEAT),
    FoodKeyword("불고기", FoodGroup.MEAT),
    FoodKeyword("닭", FoodGroup.MEAT),
    FoodKeyword("오리", FoodGroup.MEAT),
    FoodKeyword("스테이크", FoodGroup.MEAT),
    FoodKeyword("steak", FoodGroup.MEAT),
    FoodKeyword("beef", FoodGroup.MEAT),
    FoodKeyword("pork", FoodGroup.MEAT),
    FoodKeyword("chicken", FoodGroup.MEAT),
    FoodKeyword("duck", FoodGroup.MEAT),
    FoodKeyword("lamb", FoodGroup.MEAT),
    // 유제품
    FoodKeyword("우유", FoodGroup.DAIRY),
    FoodKeyword("유제품", FoodGroup.DAIRY),
    FoodKeyword("치즈", FoodGroup.DAIRY),
    FoodKeyword("cheese", FoodGroup.DAIRY),
    FoodKeyword("요거트", FoodGroup.DAIRY),
    FoodKeyword("요구르트", FoodGroup.DAIRY),
    FoodKeyword("yogurt", FoodGroup.DAIRY),
    FoodKeyword("버터", FoodGroup.DAIRY),
    FoodKeyword("butter", FoodGroup.DAIRY),
    FoodKeyword("크림", FoodGroup.DAIRY),
    FoodKeyword("cream", FoodGroup.DAIRY),
    FoodKeyword("아이스크림", FoodGroup.DAIRY),
    FoodKeyword("연유", FoodGroup.DAIRY),
    FoodKeyword("milk", FoodGroup.DAIRY),
    // 달걀
    FoodKeyword("계란", FoodGroup.EGG),
    FoodKeyword("달걀", FoodGroup.EGG),
    FoodKeyword("메추리알", FoodGroup.EGG),
    FoodKeyword("egg", FoodGroup.EGG),
    FoodKeyword("알", FoodGroup.EGG, wholeNameOnly = true),
    // 음료
    FoodKeyword("음료", FoodGroup.DRINK),
    FoodKeyword("주스", FoodGroup.DRINK),
    FoodKeyword("주류", FoodGroup.DRINK),
    FoodKeyword("생수", FoodGroup.DRINK),
    FoodKeyword("탄산수", FoodGroup.DRINK),
    FoodKeyword("콜라", FoodGroup.DRINK),
    FoodKeyword("사이다", FoodGroup.DRINK),
    FoodKeyword("맥주", FoodGroup.DRINK),
    FoodKeyword("소주", FoodGroup.DRINK),
    FoodKeyword("와인", FoodGroup.DRINK),
    FoodKeyword("커피", FoodGroup.DRINK),
    FoodKeyword("녹차", FoodGroup.DRINK),
    FoodKeyword("홍차", FoodGroup.DRINK),
    FoodKeyword("보리차", FoodGroup.DRINK),
    FoodKeyword("에이드", FoodGroup.DRINK),
    FoodKeyword("스무디", FoodGroup.DRINK),
    FoodKeyword("두유", FoodGroup.DRINK),
    FoodKeyword("drink", FoodGroup.DRINK),
    FoodKeyword("juice", FoodGroup.DRINK),
    FoodKeyword("water", FoodGroup.DRINK),
    FoodKeyword("soda", FoodGroup.DRINK),
    FoodKeyword("beer", FoodGroup.DRINK),
    FoodKeyword("wine", FoodGroup.DRINK),
    FoodKeyword("coffee", FoodGroup.DRINK),
    FoodKeyword("물", FoodGroup.DRINK, wholeNameOnly = true),
    FoodKeyword("차", FoodGroup.DRINK, wholeNameOnly = true),
    // `chocolate`·`steak` 안에서 잘못 잡히므로 이 둘은 이름 전체일 때만 인정한다.
    FoodKeyword("cola", FoodGroup.DRINK, wholeNameOnly = true),
    FoodKeyword("tea", FoodGroup.DRINK, wholeNameOnly = true),
    // 수산
    FoodKeyword("생선", FoodGroup.SEAFOOD),
    FoodKeyword("수산물", FoodGroup.SEAFOOD),
    FoodKeyword("해산물", FoodGroup.SEAFOOD),
    FoodKeyword("해물", FoodGroup.SEAFOOD),
    FoodKeyword("어패류", FoodGroup.SEAFOOD),
    FoodKeyword("고등어", FoodGroup.SEAFOOD),
    FoodKeyword("갈치", FoodGroup.SEAFOOD),
    FoodKeyword("조기", FoodGroup.SEAFOOD),
    FoodKeyword("연어", FoodGroup.SEAFOOD),
    FoodKeyword("참치", FoodGroup.SEAFOOD),
    FoodKeyword("대구", FoodGroup.SEAFOOD),
    FoodKeyword("명태", FoodGroup.SEAFOOD),
    FoodKeyword("오징어", FoodGroup.SEAFOOD),
    FoodKeyword("낙지", FoodGroup.SEAFOOD),
    FoodKeyword("새우", FoodGroup.SEAFOOD),
    FoodKeyword("조개", FoodGroup.SEAFOOD),
    FoodKeyword("굴", FoodGroup.SEAFOOD),
    FoodKeyword("홍합", FoodGroup.SEAFOOD),
    FoodKeyword("전복", FoodGroup.SEAFOOD),
    FoodKeyword("멸치", FoodGroup.SEAFOOD),
    FoodKeyword("seafood", FoodGroup.SEAFOOD),
    FoodKeyword("fish", FoodGroup.SEAFOOD),
    FoodKeyword("salmon", FoodGroup.SEAFOOD),
    FoodKeyword("tuna", FoodGroup.SEAFOOD),
    FoodKeyword("shrimp", FoodGroup.SEAFOOD),
    FoodKeyword("squid", FoodGroup.SEAFOOD),
    FoodKeyword("mackerel", FoodGroup.SEAFOOD),
    FoodKeyword("oyster", FoodGroup.SEAFOOD),
    FoodKeyword("clam", FoodGroup.SEAFOOD),
    FoodKeyword("게", FoodGroup.SEAFOOD, wholeNameOnly = true),
    // 곡물·빵
    FoodKeyword("곡물", FoodGroup.GRAIN),
    FoodKeyword("쌀", FoodGroup.GRAIN),
    FoodKeyword("현미", FoodGroup.GRAIN),
    FoodKeyword("잡곡", FoodGroup.GRAIN),
    FoodKeyword("보리", FoodGroup.GRAIN),
    FoodKeyword("귀리", FoodGroup.GRAIN),
    FoodKeyword("오트밀", FoodGroup.GRAIN),
    FoodKeyword("식빵", FoodGroup.GRAIN),
    FoodKeyword("빵", FoodGroup.GRAIN),
    FoodKeyword("케이크", FoodGroup.GRAIN),
    FoodKeyword("떡", FoodGroup.GRAIN),
    FoodKeyword("토스트", FoodGroup.GRAIN),
    FoodKeyword("베이글", FoodGroup.GRAIN),
    FoodKeyword("크루아상", FoodGroup.GRAIN),
    FoodKeyword("국수", FoodGroup.GRAIN),
    FoodKeyword("소면", FoodGroup.GRAIN),
    FoodKeyword("칼국수", FoodGroup.GRAIN),
    FoodKeyword("우동", FoodGroup.GRAIN),
    FoodKeyword("파스타", FoodGroup.GRAIN),
    FoodKeyword("스파게티", FoodGroup.GRAIN),
    FoodKeyword("당면", FoodGroup.GRAIN),
    FoodKeyword("냉면", FoodGroup.GRAIN),
    FoodKeyword("짜장면", FoodGroup.GRAIN),
    FoodKeyword("비빔면", FoodGroup.GRAIN),
    FoodKeyword("grain", FoodGroup.GRAIN),
    FoodKeyword("rice", FoodGroup.GRAIN),
    FoodKeyword("bread", FoodGroup.GRAIN),
    FoodKeyword("toast", FoodGroup.GRAIN),
    FoodKeyword("bagel", FoodGroup.GRAIN),
    FoodKeyword("noodle", FoodGroup.GRAIN),
    FoodKeyword("pasta", FoodGroup.GRAIN),
    FoodKeyword("spaghetti", FoodGroup.GRAIN),
    FoodKeyword("밀", FoodGroup.GRAIN, wholeNameOnly = true),
    // 반찬·조리식품
    FoodKeyword("반찬", FoodGroup.PREPARED),
    FoodKeyword("조리", FoodGroup.PREPARED),
    FoodKeyword("김치", FoodGroup.PREPARED),
    FoodKeyword("찌개", FoodGroup.PREPARED),
    FoodKeyword("볶음", FoodGroup.PREPARED),
    FoodKeyword("무침", FoodGroup.PREPARED),
    FoodKeyword("조림", FoodGroup.PREPARED),
    FoodKeyword("전골", FoodGroup.PREPARED),
    FoodKeyword("만두", FoodGroup.PREPARED),
    FoodKeyword("샐러드", FoodGroup.PREPARED),
    FoodKeyword("도시락", FoodGroup.PREPARED),
    FoodKeyword("카레", FoodGroup.PREPARED),
    // `안전식품`의 `전`, `한국산`의 `국`, `사탕`의 `탕`처럼 한 글자 낱말은 다른 낱말 안에서 잘못
    // 잡힌다. 이 다섯은 이름 전체일 때만 인정하고, `김치전`·`볶음밥`은 재료·형태 낱말이 계속 받는다.
    FoodKeyword("밥", FoodGroup.PREPARED, wholeNameOnly = true),
    FoodKeyword("죽", FoodGroup.PREPARED, wholeNameOnly = true),
    FoodKeyword("국", FoodGroup.PREPARED, wholeNameOnly = true),
    FoodKeyword("탕", FoodGroup.PREPARED, wholeNameOnly = true),
    FoodKeyword("전", FoodGroup.PREPARED, wholeNameOnly = true),
    // 가공식품
    FoodKeyword("가공", FoodGroup.PROCESSED),
    FoodKeyword("라면", FoodGroup.PROCESSED),
    FoodKeyword("통조림", FoodGroup.PROCESSED),
    FoodKeyword("캔", FoodGroup.PROCESSED),
    FoodKeyword("소시지", FoodGroup.PROCESSED),
    FoodKeyword("햄", FoodGroup.PROCESSED),
    FoodKeyword("베이컨", FoodGroup.PROCESSED),
    FoodKeyword("어묵", FoodGroup.PROCESSED),
    FoodKeyword("맛살", FoodGroup.PROCESSED),
    FoodKeyword("스팸", FoodGroup.PROCESSED),
    FoodKeyword("너겟", FoodGroup.PROCESSED),
    FoodKeyword("두부", FoodGroup.PROCESSED),
    FoodKeyword("과자", FoodGroup.PROCESSED),
    FoodKeyword("쿠키", FoodGroup.PROCESSED),
    FoodKeyword("크래커", FoodGroup.PROCESSED),
    FoodKeyword("초콜릿", FoodGroup.PROCESSED),
    FoodKeyword("사탕", FoodGroup.PROCESSED),
    FoodKeyword("젤리", FoodGroup.PROCESSED),
    FoodKeyword("시리얼", FoodGroup.PROCESSED),
    FoodKeyword("processed", FoodGroup.PROCESSED),
    FoodKeyword("snack", FoodGroup.PROCESSED),
    FoodKeyword("cookie", FoodGroup.PROCESSED),
    FoodKeyword("cracker", FoodGroup.PROCESSED),
    FoodKeyword("chocolate", FoodGroup.PROCESSED),
    FoodKeyword("candy", FoodGroup.PROCESSED),
    FoodKeyword("cereal", FoodGroup.PROCESSED),
    FoodKeyword("sausage", FoodGroup.PROCESSED),
    FoodKeyword("bacon", FoodGroup.PROCESSED),
    FoodKeyword("ham", FoodGroup.PROCESSED)
)

/**
 * 음식군별 번들 그림.
 *
 * 그림 자체는 리뷰를 거쳐 승인된 로컬 자산이고, 이 함수는 그중 어떤 파일을 쓸지만 정한다.
 * [FoodGroup]은 저장소·백업에 없는 장식용 분류라 자산 이름과 1:1로만 맞춘다.
 *
 * `food_other`(버섯)는 짝이 되는 음식군이 없어 번들만 하고 쓰지 않는다. 그것을 쓰려면 낯선 이름을
 * 버섯으로 단정하거나 새 분류를 만들어야 하는데, 둘 다 이 표식의 계약 밖이다.
 */
@DrawableRes
private fun artworkResource(group: FoodGroup): Int = when (group) {
    FoodGroup.VEGETABLES -> R.drawable.food_vegetables
    FoodGroup.FRUIT -> R.drawable.food_fruit
    FoodGroup.MEAT -> R.drawable.food_meat
    FoodGroup.DAIRY -> R.drawable.food_dairy
    FoodGroup.EGG -> R.drawable.food_egg
    FoodGroup.DRINK -> R.drawable.food_drink
    FoodGroup.SEAFOOD -> R.drawable.food_seafood
    FoodGroup.GRAIN -> R.drawable.food_grain_bread
    FoodGroup.PREPARED -> R.drawable.food_prepared
    FoodGroup.PROCESSED -> R.drawable.food_processed
    FoodGroup.GENERIC -> R.drawable.food_unknown
}
