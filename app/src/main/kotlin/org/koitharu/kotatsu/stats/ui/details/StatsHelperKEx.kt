package org.koitharu.kotatsu.stats.ui.details

import android.graphics.Color
import org.koitharu.kotatsu.parsers.model.MangaState
import java.util.concurrent.TimeUnit

object StatsHelperKEx {
    private val STATUS_COLOR_LIST = arrayListOf(
        Color.parseColor("#3BAEEA"), // Ongoing
        Color.parseColor("#7BD555"), // Finished
        Color.parseColor("#F79A63"), // Licensed (unused here)
        Color.parseColor("#d29d2f"), // Upcoming
        Color.parseColor("#E85D75"), // Abandoned
        Color.parseColor("#F17575"), // Paused
        Color.parseColor("#5b5b5b"), // Restricted
    )

    val STATUS_COLOR_MAP = hashMapOf(
        MangaState.ONGOING.name to STATUS_COLOR_LIST[0],
        MangaState.FINISHED.name to STATUS_COLOR_LIST[1],
        MangaState.ABANDONED.name to STATUS_COLOR_LIST[4],
        MangaState.PAUSED.name to STATUS_COLOR_LIST[5],
        MangaState.UPCOMING.name to STATUS_COLOR_LIST[3],
        MangaState.RESTRICTED.name to STATUS_COLOR_LIST[6],
    )

    val SCORE_COLOR_LIST = arrayListOf(
        Color.parseColor("#d2492d"),
        Color.parseColor("#d2642c"),
        Color.parseColor("#d2802e"),
        Color.parseColor("#d29d2f"),
        Color.parseColor("#d2b72e"),
        Color.parseColor("#d3d22e"),
        Color.parseColor("#b8d22c"),
        Color.parseColor("#9cd42e"),
        Color.parseColor("#81d12d"),
        Color.parseColor("#63d42e"),
    )

    val PIE_CHART_COLOR_LIST = arrayListOf(
        Color.parseColor("#55e2cf"),
        Color.parseColor("#57aee2"),
        Color.parseColor("#5668e2"),
        Color.parseColor("#8a56e2"),
        Color.parseColor("#ce56e2"),
        Color.parseColor("#e256ae"),
        Color.parseColor("#e25768"),
        Color.parseColor("#e28956"),
        Color.parseColor("#e3cf56"),
        Color.parseColor("#aee256"),
        Color.parseColor("#68e257"),
        Color.parseColor("#56e28a"),
    )

    fun formatDuration(millis: Long): String {
        val days = TimeUnit.MILLISECONDS.toDays(millis)
        val hours = TimeUnit.MILLISECONDS.toHours(millis) % 24
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60

        return buildString {
            if (days > 0) append("${days}d ")
            if (hours > 0) append("${hours}h ")
            if (minutes > 0 || (days == 0L && hours == 0L)) append("${minutes}min")
        }.trim()
    }
}
