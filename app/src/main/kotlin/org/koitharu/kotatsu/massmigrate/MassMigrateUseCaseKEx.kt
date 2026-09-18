package org.koitharu.kotatsu.massmigrate

import androidx.room.withTransaction
import org.koitharu.kotatsu.alternatives.domain.MigrateUseCase
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.parsers.model.Manga
import javax.inject.Inject

class MassMigrateUseCaseKEx @Inject constructor(
	private val migrateUseCase: MigrateUseCase,
	private val database: MangaDatabase,
) {

	suspend operator fun invoke(
		oldManga: Manga,
		newManga: Manga,
	) {
		// First, read stats for old manga before MigrateUseCase removes/replaces them
		val statsDao = database.getStatsDao()
		val oldStats = statsDao.findAll(oldManga.id)

		// Run the standard migration (this carries favorites, preferences, bookmarks, history, tracker, scrobbler)
		migrateUseCase(oldManga, newManga, migrateProgress = true)

		// Now carry over reading statistics to the new manga ID (FK -> history(manga_id), which is now created)
		if (oldStats.isNotEmpty()) {
			database.withTransaction {
				for (s in oldStats) {
					statsDao.upsert(s.copy(mangaId = newManga.id))
				}
			}
		}
	}
}
