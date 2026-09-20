package org.koitharu.kotatsu.stats.ui.details

import androidx.annotation.StringRes
import org.koitharu.kotatsu.R

enum class StatsDetailsTypeKEx(@StringRes val resourceId: Int) {
    SERIES_TYPE(R.string.series_type),
    STATUS(R.string.status),
    READ_DURATION(R.string.read_duration),
    SCORE(R.string.score),
    LENGTH(R.string.length),
    LANGUAGE(R.string.language),
    SOURCE(R.string.source),
    TRACKER(R.string.tracker),
    CATEGORY(R.string.category),
    TAG(R.string.tag),
    START_YEAR(R.string.start_year);

    companion object {
        const val TYPE_MANGA = 1
        const val TYPE_MANHWA = 2
        const val TYPE_MANHUA = 3
        const val TYPE_COMIC = 4
        const val TYPE_WEBTOON = 5
    }
}

enum class StatsDetailsSortKEx(@StringRes val resourceId: Int) {
    COUNT_DESC(R.string.most_entries),
    PROGRESS_DESC(R.string.chapters_read),
    MEAN_SCORE_DESC(R.string.mean_tracking_score);
}
