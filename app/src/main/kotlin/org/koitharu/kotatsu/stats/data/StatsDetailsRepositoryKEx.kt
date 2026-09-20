package org.koitharu.kotatsu.stats.data

import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.db.entity.MangaEntity
import org.koitharu.kotatsu.core.db.entity.MangaWithTags
import org.koitharu.kotatsu.core.db.entity.toManga
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.scrobbling.common.data.ScrobblingEntity
import org.koitharu.kotatsu.stats.domain.ReadingStats
import org.koitharu.kotatsu.stats.domain.StatsBucket
import org.koitharu.kotatsu.stats.domain.StatsBucketUnit
import org.koitharu.kotatsu.stats.domain.StatsPeriod
import org.koitharu.kotatsu.stats.ui.details.StatsDetailsTypeKEx
import org.koitharu.kotatsu.history.data.HistoryEntity
import org.koitharu.kotatsu.stats.ui.details.StatsHelperKEx
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.TreeMap
import javax.inject.Inject

class StatsDetailsRepositoryKEx @Inject constructor(
    private val db: MangaDatabase,
) {
    suspend fun getDetailedStats(
        type: StatsDetailsTypeKEx,
        fromDate: Long,
        categories: Set<Long>
    ): List<StatsDetailsDataKEx> {
        val sessions = db.getStatsDao().getSessions(fromDate, categories)
        val mangaIds = sessions.map { it.mangaId }.distinct()
        val mangaMap = db.getMangaDao().findLibraryMangaByIds(mangaIds).associateBy { it.manga.id }
        val historyMap = db.getHistoryDao().findAllForSync().filter { it.mangaId in mangaIds }.associateBy { it.mangaId }
        
        // Group sessions by manga to get aggregate stats per manga
        val perManga = sessions.groupBy { it.mangaId }
        
        return when (type) {
            StatsDetailsTypeKEx.SERIES_TYPE -> groupBySeriesType(perManga, mangaMap, historyMap)
            StatsDetailsTypeKEx.STATUS -> groupByStatus(perManga, mangaMap, historyMap)
            StatsDetailsTypeKEx.READ_DURATION -> groupByManga(perManga, mangaMap, historyMap)
            StatsDetailsTypeKEx.SOURCE -> groupBySource(perManga, mangaMap, historyMap)
            StatsDetailsTypeKEx.CATEGORY -> groupByCategory(perManga, mangaMap, categories)
            StatsDetailsTypeKEx.TAG -> groupByTag(perManga, mangaMap, historyMap)
            StatsDetailsTypeKEx.START_YEAR -> groupByStartYear(perManga, mangaMap, historyMap)
            // Other types (Score, Length, Language, Tracker) can be added here
            else -> emptyList()
        }
    }

    private fun groupBySeriesType(
        perManga: Map<Long, List<StatsEntity>>,
        mangaMap: Map<Long, MangaWithTags>,
        historyMap: Map<Long, HistoryEntity>
    ): List<StatsDetailsDataKEx> {
        val seriesTypeEntries = perManga.entries.groupBy { mangaMap[it.key]?.manga?.seriesType ?: 0 }
        return seriesTypeEntries.entries.mapIndexed { index, (seriesType, entries) ->
            StatsDetailsDataKEx(
                label = seriesType.toString(),
                count = entries.size,
                chaptersRead = entries.sumOf { it.value.sumOf { s -> s.chapters } },
                totalChapters = entries.sumOf { historyMap[it.key]?.chaptersCount ?: 0 },
                readDuration = entries.sumOf { it.value.sumOf { s -> s.duration } },
                id = seriesType.toLong(),
                color = StatsHelperKEx.PIE_CHART_COLOR_LIST[index % StatsHelperKEx.PIE_CHART_COLOR_LIST.size]
            )
        }
    }

    private fun groupByStatus(
        perManga: Map<Long, List<StatsEntity>>,
        mangaMap: Map<Long, MangaWithTags>,
        historyMap: Map<Long, HistoryEntity>
    ): List<StatsDetailsDataKEx> {
        return perManga.entries.groupBy { mangaMap[it.key]?.manga?.state ?: "UNKNOWN" }
            .map { (state, entries) ->
                StatsDetailsDataKEx(
                    label = state,
                    count = entries.size,
                    chaptersRead = entries.sumOf { it.value.sumOf { s -> s.chapters } },
                    totalChapters = entries.sumOf { historyMap[it.key]?.chaptersCount ?: 0 },
                    readDuration = entries.sumOf { it.value.sumOf { s -> s.duration } },
                    color = StatsHelperKEx.STATUS_COLOR_MAP[state]
                )
            }
    }

    private fun groupByManga(
        perManga: Map<Long, List<StatsEntity>>,
        mangaMap: Map<Long, MangaWithTags>,
        historyMap: Map<Long, HistoryEntity>
    ): List<StatsDetailsDataKEx> {
        return perManga.map { (mangaId, sessions) ->
            val manga = mangaMap[mangaId]
            StatsDetailsDataKEx(
                label = manga?.manga?.title ?: "Unknown",
                subLabel = manga?.manga?.sourceTitle,
                count = 1,
                chaptersRead = sessions.sumOf { it.chapters },
                totalChapters = historyMap[mangaId]?.chaptersCount ?: 0,
                readDuration = sessions.sumOf { it.duration },
                id = mangaId
            )
        }.sortedByDescending { it.readDuration }
    }

    private fun groupBySource(
        perManga: Map<Long, List<StatsEntity>>,
        mangaMap: Map<Long, MangaWithTags>,
        historyMap: Map<Long, HistoryEntity>
    ): List<StatsDetailsDataKEx> {
        return perManga.entries.groupBy { mangaMap[it.key]?.manga?.source ?: "UNKNOWN" }
            .map { (source, entries) ->
                val sourceTitle = entries.firstNotNullOfOrNull { mangaMap[it.key]?.manga?.sourceTitle }
                StatsDetailsDataKEx(
                    label = sourceTitle ?: source,
                    count = entries.size,
                    chaptersRead = entries.sumOf { it.value.sumOf { s -> s.chapters } },
                    totalChapters = entries.sumOf { historyMap[it.key]?.chaptersCount ?: 0 },
                    readDuration = entries.sumOf { it.value.sumOf { s -> s.duration } },
                    id = source.hashCode().toLong()
                )
            }
    }

    private fun groupByCategory(
        perManga: Map<Long, List<StatsEntity>>,
        mangaMap: Map<Long, MangaWithTags>,
        filterCategories: Set<Long>
    ): List<StatsDetailsDataKEx> {
        // Implementation for grouping by category would go here
        return emptyList()
    }

    private fun groupByTag(
        perManga: Map<Long, List<StatsEntity>>,
        mangaMap: Map<Long, MangaWithTags>,
        historyMap: Map<Long, HistoryEntity>
    ): List<StatsDetailsDataKEx> {
        val tagStats = mutableMapOf<String, MutableList<StatsEntity>>()
        perManga.forEach { (mangaId, sessions) ->
            mangaMap[mangaId]?.tags?.forEach { tag ->
                tagStats.getOrPut(tag.title) { mutableListOf() }.addAll(sessions)
            }
        }
        return tagStats.map { (tag, sessions) ->
            val groupMangaIds = sessions.map { it.mangaId }.distinct()
            StatsDetailsDataKEx(
                label = tag,
                count = groupMangaIds.size,
                chaptersRead = sessions.sumOf { it.chapters },
                totalChapters = groupMangaIds.sumOf { historyMap[it]?.chaptersCount ?: 0 },
                readDuration = sessions.sumOf { it.duration }
            )
        }.sortedByDescending { it.count }.take(100)
    }

    private fun groupByStartYear(
        perManga: Map<Long, List<StatsEntity>>,
        mangaMap: Map<Long, MangaWithTags>,
        historyMap: Map<Long, HistoryEntity>
    ): List<StatsDetailsDataKEx> {
        val zone = ZoneId.systemDefault()
        return perManga.entries.groupBy { entry ->
            val firstSession = entry.value.minByOrNull { it.startedAt }
            firstSession?.let { Instant.ofEpochMilli(it.startedAt).atZone(zone).year }
        }.map { (year, entries) ->
            StatsDetailsDataKEx(
                label = year?.toString() ?: "Not started",
                count = entries.size,
                chaptersRead = entries.sumOf { it.value.sumOf { s -> s.chapters } },
                totalChapters = entries.sumOf { historyMap[it.key]?.chaptersCount ?: 0 },
                readDuration = entries.sumOf { it.value.sumOf { s -> s.duration } },
                id = year?.toLong()
            )
        }.sortedBy { it.label }
    }

    suspend fun getDailyStats(fromDate: Long, categories: Set<Long>): Map<LocalDate, List<StatsEntity>> {
        val sessions = db.getStatsDao().getSessions(fromDate, categories)
        val zone = ZoneId.systemDefault()
        return sessions.groupBy { Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate() }
    }
}

data class StatsDetailsDataKEx(
    val label: String,
    val subLabel: String? = null,
    val count: Int = 0,
    val chaptersRead: Int = 0,
    val totalChapters: Int = 0,
    val readDuration: Long = 0,
    val meanScore: Double? = null,
    val id: Long? = null,
    val color: Int? = null
)
