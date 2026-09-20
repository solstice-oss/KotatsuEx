package org.koitharu.kotatsu.core.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.koitharu.kotatsu.stats.ui.details.StatsDetailsTypeKEx
import java.util.Locale

class Migration37To38KEx : Migration(37, 38) {

    override fun migrate(db: SupportSQLiteDatabase) {
        // Add series_type column
        db.execSQL("ALTER TABLE `manga` ADD COLUMN `series_type` INTEGER NOT NULL DEFAULT 0")

        // Populate series_type based on inference logic
        // We use a simplified version of tachiyomiJ2K logic that can be executed in SQL or in a single pass
        
        // 1. Mark as TYPE_MANGA by default (handled by DEFAULT 0, but we'll use 1 for clarity if needed)
        // TYPE_MANGA = 1, TYPE_MANHWA = 2, TYPE_MANHUA = 3, TYPE_COMIC = 4, TYPE_WEBTOON = 5
        db.execSQL("UPDATE manga SET series_type = ${StatsDetailsTypeKEx.TYPE_MANGA}")

        // 2. Comics
        val comicSources = listOf("gunnerkrigg", "dilbert", "cyanide", "xkcd", "tapas", "ComicExtra", "Read Comics Online", "ReadComicOnline")
        comicSources.forEach {
            db.execSQL("UPDATE manga SET series_type = ${StatsDetailsTypeKEx.TYPE_COMIC} WHERE source_title LIKE '%$it%'")
        }
        
        // 3. Webtoons / Manhwa sources
        val webtoonSources = listOf("webtoon", "manhwa", "toonily")
        webtoonSources.forEach {
            db.execSQL("UPDATE manga SET series_type = ${StatsDetailsTypeKEx.TYPE_MANHWA} WHERE source_title LIKE '%$it%'")
        }
        
        // 4. Manhua sources
        db.execSQL("UPDATE manga SET series_type = ${StatsDetailsTypeKEx.TYPE_MANHUA} WHERE source_title LIKE '%manhua%'")

        // 5. Refine based on tags
        // Since tags are in a separate table, we use subqueries
        val mangaTags = listOf("manga", "манга", "jp", "japanese")
        val manhwaTags = listOf("long strip", "manhwa", "манхва", "kr", "korean")
        val manhuaTags = listOf("manhua", "маньхуа", "cn", "hk", "zh-Hans", "zh-Hant", "chinese")
        val comicTags = listOf("comic", "комикс", "en", "gb", "english")
        val webtoonTags = listOf("webtoon")

        fun updateByTags(tags: List<String>, type: Int) {
            val tagList = tags.joinToString { "'$it'" }
            db.execSQL("""
                UPDATE manga SET series_type = $type 
                WHERE manga_id IN (
                    SELECT mt.manga_id FROM manga_tags mt 
                    JOIN tags t ON mt.tag_id = t.tag_id 
                    WHERE LOWER(t.title) IN ($tagList)
                )
            """.trimIndent())
        }

        updateByTags(mangaTags, StatsDetailsTypeKEx.TYPE_MANGA)
        updateByTags(manhwaTags, StatsDetailsTypeKEx.TYPE_MANHWA)
        updateByTags(manhuaTags, StatsDetailsTypeKEx.TYPE_MANHUA)
        updateByTags(comicTags, StatsDetailsTypeKEx.TYPE_COMIC)
        updateByTags(webtoonTags, StatsDetailsTypeKEx.TYPE_WEBTOON)
        
        // Special case for Webtoon where it could be Manhwa or Manhua but Webtoon tag is present
        db.execSQL("""
            UPDATE manga SET series_type = ${StatsDetailsTypeKEx.TYPE_WEBTOON}
            WHERE series_type = ${StatsDetailsTypeKEx.TYPE_MANGA} AND manga_id IN (
                SELECT mt.manga_id FROM manga_tags mt 
                JOIN tags t ON mt.tag_id = t.tag_id 
                WHERE LOWER(t.title) LIKE 'webtoon%'
            )
        """.trimIndent())
    }
}
