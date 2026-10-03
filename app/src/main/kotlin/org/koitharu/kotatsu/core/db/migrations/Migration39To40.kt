// 38To39 in upstream
package org.koitharu.kotatsu.core.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Partial "merge scanlators": the picked branch names. */
class Migration39To40 : Migration(39, 40) {

	override fun migrate(db: SupportSQLiteDatabase) {
		db.execSQL("ALTER TABLE preferences ADD COLUMN merged_scanlators TEXT")
		// The sync update trigger lists the columns it watches; drop it so onOpen re-creates it with the new one.
		db.execSQL("DROP TRIGGER IF EXISTS sync_preferences_upd")
	}
}
