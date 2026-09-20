package org.koitharu.kotatsu.stats.ui.details

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.favourites.domain.FavouritesRepository
import org.koitharu.kotatsu.stats.data.StatsDetailsDataKEx
import org.koitharu.kotatsu.stats.data.StatsDetailsRepositoryKEx
import org.koitharu.kotatsu.stats.data.StatsRepository
import org.koitharu.kotatsu.stats.domain.StatsPeriod
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

@HiltViewModel
class StatsDetailsViewModelKEx @Inject constructor(
    private val repository: StatsDetailsRepositoryKEx,
    private val statsRepository: StatsRepository,
    favouritesRepository: FavouritesRepository,
) : BaseViewModel() {

    val selectedType = MutableStateFlow(StatsDetailsTypeKEx.SERIES_TYPE)
    val period = MutableStateFlow(StatsPeriod.ALL)
    val selectedCategories = MutableStateFlow<Set<Long>>(emptySet())
    val selectedSort = MutableStateFlow(StatsDetailsSortKEx.COUNT_DESC)
    val favoriteCategories = favouritesRepository.observeCategories()
    
    val statsData = MutableStateFlow<List<StatsDetailsDataKEx>>(emptyList())
    val dailyStats = MutableStateFlow<Map<LocalDate, Long>>(emptyMap())
    val selectedDate = MutableStateFlow<LocalDate?>(null)
    
    // For Read Duration period navigation (start of current week)
    val startDate = MutableStateFlow<LocalDate>(LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))
    val endDate = MutableStateFlow<LocalDate>(LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)))

    init {
        launchJob(Dispatchers.Default) {
            combine(
                selectedType,
                period,
                selectedCategories,
                selectedSort,
                startDate,
                endDate
            ) { args ->
                @Suppress("UNCHECKED_CAST")
                DataState(
                    type = args[0] as StatsDetailsTypeKEx,
                    period = args[1] as StatsPeriod,
                    categories = args[2] as Set<Long>,
                    sort = args[3] as StatsDetailsSortKEx,
                    start = args[4] as LocalDate,
                    end = args[5] as LocalDate
                )
            }.collectLatest { state ->
                val fromDate = if (state.type == StatsDetailsTypeKEx.READ_DURATION && state.period != StatsPeriod.ALL) {
                    state.start.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                } else {
                    getFromDate(state.period)
                }
                
                // We'll just fetch all sessions for now as getDetailedStats currently only takes fromDate
                // and Kotatsu sessions are normally ordered by started_at
                val data = withLoading {
                    repository.getDetailedStats(state.type, fromDate, state.categories)
                        .filter { it.readDuration > 0 || it.count > 0 }
                }
                statsData.value = sortData(data, state.sort)
                
                if (state.type == StatsDetailsTypeKEx.READ_DURATION) {
                    val daily = repository.getDailyStats(fromDate, state.categories)
                    dailyStats.value = daily.mapValues { it.value.sumOf { s -> s.duration } }
                }
            }
        }
    }

    fun movePeriod(weeks: Long) {
        startDate.value = startDate.value.plusWeeks(weeks)
        endDate.value = endDate.value.plusWeeks(weeks)
    }

    private fun getFromDate(period: StatsPeriod): Long {
        if (period == StatsPeriod.ALL) return 0L
        val zone = ZoneId.systemDefault()
        val now = LocalDate.now(zone)
        val from = when (period) {
            StatsPeriod.DAY -> now
            StatsPeriod.WEEK -> now.minusWeeks(1)
            StatsPeriod.MONTH -> now.minusMonths(1)
            StatsPeriod.MONTHS_3 -> now.minusMonths(3)
            StatsPeriod.YEAR -> now.minusYears(1)
            else -> now
        }
        return from.atStartOfDay(zone).toInstant().toEpochMilli()
    }

    private fun sortData(data: List<StatsDetailsDataKEx>, sort: StatsDetailsSortKEx): List<StatsDetailsDataKEx> {
        return when (sort) {
            StatsDetailsSortKEx.COUNT_DESC -> data.sortedByDescending { it.count }
            StatsDetailsSortKEx.PROGRESS_DESC -> data.sortedByDescending { it.chaptersRead }
            StatsDetailsSortKEx.MEAN_SCORE_DESC -> data.sortedByDescending { it.meanScore ?: 0.0 }
        }
    }

    fun toggleCategory(category: FavouriteCategory) {
        val snapshot = selectedCategories.value
        selectedCategories.value = if (category.id in snapshot) {
            snapshot - category.id
        } else {
            snapshot + category.id
        }
    }

    fun clearCategories() {
        selectedCategories.value = emptySet()
    }

    private data class DataState(
        val type: StatsDetailsTypeKEx,
        val period: StatsPeriod,
        val categories: Set<Long>,
        val sort: StatsDetailsSortKEx,
        val start: LocalDate,
        val end: LocalDate
    )
}
