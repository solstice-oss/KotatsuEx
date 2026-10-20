package org.koitharu.kotatsu.aniyomi

import eu.kanade.tachiyomi.animesource.model.AnimeFilter
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import org.koitharu.kotatsu.parsers.model.MangaListFilter
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.model.MangaTag

object AniyomiFilterMapperKEx {

	private const val TYPE_CHECKBOX = "cb"
	private const val TYPE_TRISTATE = "tri"
	private const val TYPE_SELECT = "sel"
	private const val TYPE_SORT = "srt"
	private const val TYPE_TEXT = "txt"

	const val SORT_KEY_PREFIX = "$TYPE_SORT@"

	private const val ARROW_ASC = "↑"
	private const val ARROW_DESC = "↓"

	fun encode(working: AnimeFilterList, defaults: AnimeFilterList, source: MangaSource): Set<MangaTag> {
		val tags = LinkedHashSet<MangaTag>()
		val sortPath = findSortFilter(defaults)?.path
		encodeInto(working.toList(), defaults.toList(), prefix = "", sortPath = sortPath, source = source, out = tags)
		return tags
	}

	private fun encodeInto(
		working: List<AnimeFilter<*>>,
		defaults: List<AnimeFilter<*>>,
		prefix: String,
		sortPath: String?,
		source: MangaSource,
		out: MutableSet<MangaTag>,
	) {
		working.forEachIndexed { index, filter ->
			val path = if (prefix.isEmpty()) index.toString() else "$prefix.$index"
			val default = defaults.getOrNull(index)
			when (filter) {
				is AnimeFilter.Header, is AnimeFilter.Separator -> Unit

				is AnimeFilter.CheckBox -> {
					val def = (default as? AnimeFilter.CheckBox)?.state ?: false
					if (filter.state != def) {
						out += tag(
							source = source,
							key = "$TYPE_CHECKBOX@$path=${if (filter.state) "1" else "0"}",
							title = if (filter.state) filter.name else "${filter.name} (off)",
						)
					}
				}

				is AnimeFilter.TriState -> {
					val def = (default as? AnimeFilter.TriState)?.state ?: AnimeFilter.TriState.STATE_IGNORE
					if (filter.state != def) {
						out += tag(
							source = source,
							key = "$TYPE_TRISTATE@$path=${filter.state}",
							title = when (filter.state) {
								AnimeFilter.TriState.STATE_EXCLUDE -> "−${filter.name}"
								AnimeFilter.TriState.STATE_IGNORE -> "${filter.name} (ignored)"
								else -> filter.name
							},
						)
					}
				}

				is AnimeFilter.Select<*> -> {
					val def = (default as? AnimeFilter.Select<*>)?.state ?: 0
					if (filter.state != def) {
						val type = if (path == sortPath) TYPE_SORT else TYPE_SELECT
						out += tag(
							source = source,
							key = "$type@$path=${filter.state}",
							title = "${filter.name}: ${filter.values.getOrNull(filter.state)}",
						)
					}
				}

				is AnimeFilter.Sort -> {
					val selection = filter.state
					val def = (default as? AnimeFilter.Sort)?.state
					if (selection != def) {
						val encodedSelection = selection?.let {
							"${it.index}:${if (it.ascending) "a" else "d"}"
						} ?: "none"
						val title = selection?.let {
							val arrow = if (it.ascending) ARROW_ASC else ARROW_DESC
							"${filter.name}: ${filter.values.getOrNull(it.index)} $arrow"
						} ?: "${filter.name} (none)"
						out += tag(
							source = source,
							key = "$TYPE_SORT@$path=$encodedSelection",
							title = title,
						)
					}
				}

				is AnimeFilter.Text -> {
					val def = (default as? AnimeFilter.Text)?.state.orEmpty()
					if (filter.state != def && filter.state.isNotBlank()) {
						out += tag(
							source = source,
							key = "$TYPE_TEXT@$path=${filter.state}",
							title = "${filter.name}: ${filter.state}",
						)
					}
				}

				is AnimeFilter.Group<*> -> {
					val groupWorking = filter.state as? List<AnimeFilter<*>> ?: emptyList()
					val groupDefault = (default as? AnimeFilter.Group<*>)?.state as? List<AnimeFilter<*>> ?: emptyList()
					encodeInto(groupWorking, groupDefault, prefix = path, sortPath = sortPath, source = source, out = out)
				}
			}
		}
	}

	fun decode(target: AnimeFilterList, filter: MangaListFilter) {
		val map = buildMapFromFilter(filter)
		decodeInto(target.toList(), prefix = "", map = map)
	}

	private fun decodeInto(filters: List<AnimeFilter<*>>, prefix: String, map: Map<String, String>) {
		filters.forEachIndexed { index, filter ->
			val path = if (prefix.isEmpty()) index.toString() else "$prefix.$index"
			when (filter) {
				is AnimeFilter.CheckBox -> {
					map["$TYPE_CHECKBOX@$path"]?.let { filter.state = it == "1" }
				}

				is AnimeFilter.TriState -> {
					map["$TYPE_TRISTATE@$path"]?.toIntOrNull()?.let { filter.state = it }
				}

				is AnimeFilter.Select<*> -> {
					val raw = map["$TYPE_SELECT@$path"] ?: map["$TYPE_SORT@$path"]
					raw?.toIntOrNull()?.let {
						if (it in filter.values.indices) filter.state = it
					}
				}

				is AnimeFilter.Sort -> {
					map["$TYPE_SORT@$path"]?.let { encoded ->
						if (encoded == "none") {
							filter.state = null
						} else {
							val parts = encoded.split(':')
							val idx = parts.getOrNull(0)?.toIntOrNull()
							val isAsc = parts.getOrNull(1) == "a"
							if (idx != null && idx in filter.values.indices) {
								filter.state = AnimeFilter.Sort.Selection(idx, isAsc)
							}
						}
					}
				}

				is AnimeFilter.Text -> {
					map["$TYPE_TEXT@$path"]?.let { filter.state = it }
				}

				is AnimeFilter.Group<*> -> {
					val groupList = filter.state as? List<AnimeFilter<*>> ?: emptyList()
					decodeInto(groupList, prefix = path, map = map)
				}

				else -> Unit
			}
		}
	}

	private fun buildMapFromFilter(filter: MangaListFilter): Map<String, String> {
		val result = HashMap<String, String>()
		filter.tags.forEach { tag ->
			val keyAndVal = tag.key
			val eq = keyAndVal.indexOf('=')
			if (eq > 0) {
				val pathKey = keyAndVal.substring(0, eq)
				val value = keyAndVal.substring(eq + 1)
				result[pathKey] = value
			}
		}
		return result
	}

	private fun tag(source: MangaSource, key: String, title: String): MangaTag = MangaTag(
		key = key,
		title = title,
		source = source,
	)

	private class LocatedFilter(val path: String, val filter: AnimeFilter<*>)

	private fun findSortFilter(filters: AnimeFilterList): LocatedFilter? =
		locateFilters(filters.toList(), prefix = "").firstOrNull {
			it.filter is AnimeFilter.Sort || (it.filter is AnimeFilter.Select<*> && isSortNamed(it.filter.name))
		}

	private fun locateFilters(filters: List<AnimeFilter<*>>, prefix: String): List<LocatedFilter> {
		val result = ArrayList<LocatedFilter>()
		filters.forEachIndexed { index, filter ->
			val path = if (prefix.isEmpty()) index.toString() else "$prefix.$index"
			result += LocatedFilter(path, filter)
			if (filter is AnimeFilter.Group<*>) {
				val children = filter.state as? List<AnimeFilter<*>> ?: emptyList()
				result += locateFilters(children, prefix = path)
			}
		}
		return result
	}

	private fun isSortNamed(name: String): Boolean {
		val lower = name.lowercase()
		return lower.contains("sort") || lower.contains("order")
	}
}
