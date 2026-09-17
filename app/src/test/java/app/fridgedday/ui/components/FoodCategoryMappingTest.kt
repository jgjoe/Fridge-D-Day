package app.fridgedday.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test
import app.fridgedday.data.db.entity.StorageLocation

/**
 * 음식군 매핑(D-022)의 순수 규칙을 고정한다.
 *
 * 표식은 장식이지만 어떤 도형이 나오는지는 결정적이어야 한다. 기존 `category`가 먼저고, 그다음
 * 이름의 낱말, 마지막이 generic이다. 이름 안에서 낱말이 겹치면 마지막 낱말이 이긴다(딸기우유 ->
 * 우유). 짧은 낱말이 다른 낱말 안에서 잘못 잡히지 않는 것도 함께 확인한다.
 */
class FoodCategoryMappingTest {

    @Test
    fun `recognizable category decides before the name`() {
        assertEquals(FoodGroup.VEGETABLES, resolveFoodGroup("채소", "우유"))
        assertEquals(FoodGroup.SEAFOOD, resolveFoodGroup("수산물", "두부"))
        assertEquals(FoodGroup.DAIRY, resolveFoodGroup("유제품", "사과"))
    }

    @Test
    fun `category matching ignores case and spacing`() {
        assertEquals(FoodGroup.VEGETABLES, resolveFoodGroup("  Vegetables ", "이름없음"))
        assertEquals(FoodGroup.FRUIT, resolveFoodGroup("과일", ""))
        assertEquals(FoodGroup.PROCESSED, resolveFoodGroup("가공식품", "이름없음"))
    }

    @Test
    fun `unrecognized category falls through to the name`() {
        assertEquals(FoodGroup.DAIRY, resolveFoodGroup("기타", "우유"))
        assertEquals(FoodGroup.EGG, resolveFoodGroup(null, "계란"))
        assertEquals(FoodGroup.GRAIN, resolveFoodGroup("", "식빵"))
    }

    @Test
    fun `every broad group is reachable from a clear name`() {
        val samples = mapOf(
            FoodGroup.VEGETABLES to "상추",
            FoodGroup.FRUIT to "사과",
            FoodGroup.MEAT to "삼겹살",
            FoodGroup.DAIRY to "우유",
            FoodGroup.EGG to "계란",
            FoodGroup.DRINK to "콜라",
            FoodGroup.SEAFOOD to "고등어",
            FoodGroup.GRAIN to "식빵",
            FoodGroup.PREPARED to "김치",
            FoodGroup.PROCESSED to "라면"
        )

        samples.forEach { (group, name) ->
            assertEquals("$name 이(가) $group 으로 읽히지 않는다", group, resolveFoodGroup(null, name))
        }
    }

    @Test
    fun `the last recognizable word wins inside one name`() {
        assertEquals(FoodGroup.DAIRY, resolveFoodGroup(null, "딸기우유"))
        assertEquals(FoodGroup.DRINK, resolveFoodGroup(null, "사과주스"))
        assertEquals(FoodGroup.DRINK, resolveFoodGroup(null, "오렌지주스"))
        assertEquals(FoodGroup.GRAIN, resolveFoodGroup(null, "우유식빵"))
        assertEquals(FoodGroup.PROCESSED, resolveFoodGroup(null, "참치캔"))
    }

    @Test
    fun `short ambiguous words only match the whole name`() {
        assertEquals(FoodGroup.FRUIT, resolveFoodGroup(null, "배"))
        assertEquals(FoodGroup.VEGETABLES, resolveFoodGroup(null, "배추"))
        assertEquals(FoodGroup.VEGETABLES, resolveFoodGroup(null, "파"))
        assertEquals(FoodGroup.FRUIT, resolveFoodGroup(null, "파인애플"))
        assertEquals(FoodGroup.VEGETABLES, resolveFoodGroup(null, "무"))
        assertEquals(FoodGroup.PREPARED, resolveFoodGroup(null, "오이무침"))
    }

    /**
     * A32 검증에서 일반 이름 `안전식품`이 한 글자 조리식품 낱말 `전`에 걸려 조리식품 표식이 붙었다.
     * 한 글자 낱말은 이름 전체일 때만 인정하므로 이런 이름은 generic으로 남고, `사탕`은 전에
     * 걸리던 `탕` 대신 긴 낱말 `사탕`으로 읽힌다.
     */
    @Test
    fun `one-character prepared words do not match inside ordinary names`() {
        assertEquals(FoodGroup.GENERIC, resolveFoodGroup(null, "안전식품"))
        assertEquals(FoodGroup.GENERIC, resolveFoodGroup(null, "한국산"))
        assertEquals(FoodGroup.GENERIC, resolveFoodGroup(null, "죽순"))
        assertEquals(FoodGroup.GENERIC, resolveFoodGroup(null, "전어"))
        assertEquals(FoodGroup.PROCESSED, resolveFoodGroup(null, "사탕"))
    }

    /**
     * 이름 전체가 `밥`·`죽`·`국`·`탕`·`전`이면 그대로 조리식품이고, `김치전`·`볶음밥`은 재료·형태
     * 낱말로, `전복`은 긴 낱말로 계속 읽힌다.
     */
    @Test
    fun `prepared words still match the whole name or a longer keyword`() {
        assertEquals(FoodGroup.PREPARED, resolveFoodGroup(null, "밥"))
        assertEquals(FoodGroup.PREPARED, resolveFoodGroup(null, "죽"))
        assertEquals(FoodGroup.PREPARED, resolveFoodGroup(null, "국"))
        assertEquals(FoodGroup.PREPARED, resolveFoodGroup(null, "탕"))
        assertEquals(FoodGroup.PREPARED, resolveFoodGroup(null, "전"))
        assertEquals(FoodGroup.PREPARED, resolveFoodGroup(null, "김치전"))
        assertEquals(FoodGroup.PREPARED, resolveFoodGroup(null, "볶음밥"))
        assertEquals(FoodGroup.SEAFOOD, resolveFoodGroup(null, "전복"))
    }

    /**
     * 낯선 영단어 안에 다른 그룹의 낱말이 들어 있어도 그 낱말로 읽지 않는다. `chocolate` 안의
     * `cola`, `steak` 안의 `tea`가 대표적인 함정이다.
     */
    @Test
    fun `english names do not trip on inner substrings`() {
        assertEquals(FoodGroup.PROCESSED, resolveFoodGroup(null, "Chocolate"))
        assertEquals(FoodGroup.MEAT, resolveFoodGroup(null, "Steak"))
        assertEquals(FoodGroup.DRINK, resolveFoodGroup(null, "Tea"))
        assertEquals(FoodGroup.DRINK, resolveFoodGroup(null, "Cola"))
    }

    @Test
    fun `uncertain input stays generic`() {
        assertEquals(FoodGroup.GENERIC, resolveFoodGroup(null, "코코넛오일"))
        assertEquals(FoodGroup.GENERIC, resolveFoodGroup(null, "이름없음"))
        assertEquals(FoodGroup.GENERIC, resolveFoodGroup("기타", "미분류"))
        assertEquals(FoodGroup.GENERIC, resolveFoodGroup(null, "   "))
        assertEquals(FoodGroup.GENERIC, resolveFoodGroup("", ""))
    }

    @Test
    fun `row metadata falls back to a conservative broad group only when category is blank`() {
        assertEquals(
            "냉장 · 유제품",
            foodMetadata(StorageLocation.FRIDGE, category = null, name = "우유")
        )
        assertEquals(
            "냉동",
            foodMetadata(StorageLocation.FREEZER, category = null, name = "이름없음")
        )
        assertEquals(
            "실온 · 사용자 분류",
            foodMetadata(StorageLocation.PANTRY, category = " 사용자 분류 ", name = "우유")
        )
    }
}
