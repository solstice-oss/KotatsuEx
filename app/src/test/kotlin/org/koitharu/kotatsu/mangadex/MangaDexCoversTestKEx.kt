package org.koitharu.kotatsu.mangadex

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.koitharu.kotatsu.core.model.LocalMangaSource
import org.koitharu.kotatsu.core.model.MissingMangaSource
import org.koitharu.kotatsu.core.model.UnknownMangaSource
import org.koitharu.kotatsu.mangadex.data.MangaDexCoverKEx
import org.koitharu.kotatsu.mangadex.data.MangaDexUrlUtilKEx
import org.koitharu.kotatsu.mangadex.ui.MangaDexCoversUiStateKEx
import org.koitharu.kotatsu.parsers.model.MangaSource

class MangaDexCoversTestKEx {

	@Test
	fun `extracts title UUID from valid MangaDex URLs`() {
		val uuid = "62040a44-0935-46b7-a691-5ae5833af0ae"
		assertEquals(
			uuid,
			MangaDexUrlUtilKEx.extractTitleId("https://mangadex.org/title/$uuid/demon-slayer"),
		)
		assertEquals(
			uuid,
			MangaDexUrlUtilKEx.extractTitleId("https://mangadex.org/title/$uuid"),
		)
		assertEquals(
			uuid,
			MangaDexUrlUtilKEx.extractTitleId("https://mangadex.org/title/$uuid/"),
		)
		assertEquals(
			uuid,
			MangaDexUrlUtilKEx.extractTitleId("https://mangadex.org/title/$uuid?tab=art"),
		)
	}

	@Test
	fun `returns null title UUID for invalid or non-MangaDex URLs`() {
		assertNull(MangaDexUrlUtilKEx.extractTitleId("https://example.com/title/62040a44-0935-46b7-a691-5ae5833af0ae"))
		assertNull(MangaDexUrlUtilKEx.extractTitleId("https://mangadex.org/title/invalid-uuid"))
		assertNull(MangaDexUrlUtilKEx.extractTitleId(null))
		assertNull(MangaDexUrlUtilKEx.extractTitleId(""))
	}

	@Test
	fun `constructs art tab URL correctly preserving existing query params and trailing slashes`() {
		val uuid = "62040a44-0935-46b7-a691-5ae5833af0ae"

		val simple = "https://mangadex.org/title/$uuid/demon-slayer"
		assertEquals(
			"https://mangadex.org/title/$uuid/demon-slayer?tab=art",
			MangaDexUrlUtilKEx.buildArtTabUrl(simple),
		)

		val trailingSlash = "https://mangadex.org/title/$uuid/"
		assertEquals(
			"https://mangadex.org/title/$uuid/?tab=art",
			MangaDexUrlUtilKEx.buildArtTabUrl(trailingSlash),
		)

		val existingQueryParams = "https://mangadex.org/title/$uuid?lang=en"
		val built = MangaDexUrlUtilKEx.buildArtTabUrl(existingQueryParams)
		assertNotNull(built)
		assertTrue(built!!.contains("tab=art"))
		assertTrue(built.contains("lang=en"))
	}

	@Test
	fun `identifies MangaDex source correctly`() {
		val mangaDexSource = MissingMangaSource("MANGADEX")
		assertTrue(MangaDexUrlUtilKEx.isMangaDexSource(mangaDexSource))

		val otherSource = MissingMangaSource("MangaFire")
		assertFalse(MangaDexUrlUtilKEx.isMangaDexSource(otherSource))

		assertFalse(MangaDexUrlUtilKEx.isMangaDexSource(LocalMangaSource))
		assertFalse(MangaDexUrlUtilKEx.isMangaDexSource(UnknownMangaSource))
	}

	@Test
	fun `evaluates visibility rules for MangaDex covers action`() {
		val mangaDexSource = MissingMangaSource("MANGADEX")
		val otherSource = MissingMangaSource("OTHER")
		val enabledSources = listOf(mangaDexSource)

		val validUrl = "https://mangadex.org/title/62040a44-0935-46b7-a691-5ae5833af0ae/title-slug"
		val invalidUrl = "https://other.org/title/12345"

		// 1. MangaDex source + installed/enabled => visible
		assertTrue(checkVisibility(mangaDexSource, validUrl, enabledSources))

		// 2. MangaDex source + not installed / disabled => hidden
		assertFalse(checkVisibility(mangaDexSource, validUrl, emptyList()))

		// 3. Non-MangaDex source => hidden
		assertFalse(checkVisibility(otherSource, validUrl, enabledSources))

		// 4. Null / invalid remote URL => hidden
		assertFalse(checkVisibility(mangaDexSource, invalidUrl, enabledSources))
		assertFalse(checkVisibility(mangaDexSource, null, enabledSources))

		// 5. Local source WITH MangaDex remote URL + MangaDex installed => visible
		assertTrue(checkVisibility(LocalMangaSource, validUrl, enabledSources))

		// 6. Local source WITHOUT MangaDex remote URL => hidden
		assertFalse(checkVisibility(LocalMangaSource, invalidUrl, enabledSources))
	}

	@Test
	fun `maps cover metadata to CDN URLs and handles UI states`() {
		val cover = MangaDexCoverKEx(
			id = "cover1",
			url = "https://uploads.mangadex.org/covers/manga1/cover1.jpg",
			volume = "1",
			description = "Vol 1 Cover",
			locale = "ja",
		)

		assertEquals("cover1", cover.id)
		assertEquals("https://uploads.mangadex.org/covers/manga1/cover1.jpg", cover.url)
		assertEquals("1", cover.volume)

		val successState = MangaDexCoversUiStateKEx.Success(listOf(cover), "Demo Title")
		assertEquals(1, successState.covers.size)

		val emptyState = MangaDexCoversUiStateKEx.Empty("Demo Title")
		assertEquals("Demo Title", emptyState.title)

		val errorState = MangaDexCoversUiStateKEx.Error("Network Error", "Demo Title")
		assertEquals("Network Error", errorState.message)
	}

	private fun checkVisibility(
		source: MangaSource,
		remoteUrl: String?,
		enabledSources: List<MangaSource>,
	): Boolean {
		if (MangaDexUrlUtilKEx.extractTitleId(remoteUrl) == null) return false
		val isMangaDexEntry = MangaDexUrlUtilKEx.isMangaDexSource(source)
		if (!isMangaDexEntry && source != LocalMangaSource) return false
		return enabledSources.any { MangaDexUrlUtilKEx.isMangaDexSource(it) }
	}
}
