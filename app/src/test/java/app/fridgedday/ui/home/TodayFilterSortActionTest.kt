package app.fridgedday.ui.home

import app.fridgedday.data.db.entity.StorageLocation
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Filter sheet activity follows the four D-024 freshness buckets, location, and sort. */
class TodayFilterSortActionTest {

    @Test
    fun `stays inactive at the default filter, location and sort`() {
        assertFalse(isFilterSortActive(HomeUiState()))
    }

    @Test
    fun `activates for every non-default status filter`() {
        assertTrue(isFilterSortActive(HomeUiState(filterType = FilterType.NEEDS_ATTENTION)))
        assertTrue(isFilterSortActive(HomeUiState(filterType = FilterType.SOON)))
        assertTrue(isFilterSortActive(HomeUiState(filterType = FilterType.REST)))
    }

    @Test
    fun `activates for a storage location filter`() {
        assertTrue(isFilterSortActive(HomeUiState(locationFilter = StorageLocation.FREEZER)))
    }

    @Test
    fun `activates for every non-default sort`() {
        assertTrue(isFilterSortActive(HomeUiState(sortType = SortType.NAME)))
        assertTrue(isFilterSortActive(HomeUiState(sortType = SortType.CREATED_DATE)))
    }

    @Test
    fun `search text alone does not light up the filter indicator`() {
        assertFalse(isFilterSortActive(HomeUiState(searchKeyword = "우유")))
    }
}
