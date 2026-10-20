package org.koitharu.kotatsu.aniyomi

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.AnimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koitharu.kotatsu.aniyomi.model.AniyomiAnimeSourceKEx
import org.koitharu.kotatsu.aniyomi.model.AniyomiLoadResultKEx
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.observeAsFlow
import org.koitharu.kotatsu.extensions.runtime.ExternalExtensionManagerFacade
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AniyomiExtensionManagerKEx @Inject constructor(
	@ApplicationContext private val context: Context,
	private val loader: AniyomiExtensionLoaderKEx,
	private val settings: AppSettings,
) {

	companion object {
		@Volatile
		private var activeInstance: AniyomiExtensionManagerKEx? = null

		fun getById(sourceId: Long): AniyomiAnimeSourceKEx? = activeInstance?.getAniyomiAnimeSourceById(sourceId)

		fun getByName(name: String): AniyomiAnimeSourceKEx? = activeInstance?.getAniyomiAnimeSourceByName(name)
	}

	private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
	private val refreshMutex = Mutex()

	private val facade = ExternalExtensionManagerFacade<
		AniyomiLoadResultKEx,
		AniyomiLoadResultKEx.Success,
		AniyomiLoadResultKEx.Error,
		AnimeSource,
		AnimeCatalogueSource,
		AniyomiAnimeSourceKEx,
	>(
		context = context,
		scope = scope,
		loadResults = { ctx -> loader.loadExtensions(ctx, settings.isPrivateInstallEnabled) },
		isExtensionPackage = { ctx, pkgName -> AniyomiExtensionLoaderKEx.isExtensionPackage(ctx, pkgName) },
		successOf = { it as? AniyomiLoadResultKEx.Success },
		errorOf = { it as? AniyomiLoadResultKEx.Error },
		untrustedPackageNameOf = { (it as? AniyomiLoadResultKEx.Untrusted)?.pkgName },
		successSources = { it.sources },
		successPackageName = { it.pkgName },
		successContentWarning = { it.contentWarning },
		successCatalogueSources = { it.catalogueSources },
		sourceId = { it.id },
		asCatalogueSource = { it as? AnimeCatalogueSource },
		catalogueSourceName = { it.name },
		catalogueSourceLang = { it.lang },
		buildWrappedSource = { catalogueSource, pkgName, contentWarning, hasLanguageSuffix ->
			AniyomiAnimeSourceKEx(catalogueSource, pkgName, contentWarning, hasLanguageSuffix)
		},
		sourceNamePrefix = "ANIYOMI_",
		errorPackageName = { it.pkgName },
		errorMessage = { it.message },
		errorThrowable = { it.exception },
	)

	val installedExtensions: StateFlow<List<AniyomiLoadResultKEx.Success>> = facade.installedExtensions
	val failedExtensions: StateFlow<List<AniyomiLoadResultKEx.Error>> = facade.failedExtensions
	val isLoading: StateFlow<Boolean> = facade.isLoading
	val isReady: StateFlow<Boolean> = facade.isReady

	init {
		activeInstance = this
		scope.launch {
			settings.observeAsFlow(AppSettings.KEY_PRIVATE_INSTALLER) { isPrivateInstallEnabled }
				.drop(1)
				.collect {
					loadExtensions()
				}
		}
	}

	fun initialize() {
		facade.initialize()
	}

	suspend fun loadExtensions() {
		refreshMutex.withLock {
			facade.loadExtensions()
		}
	}

	suspend fun ensureReady(forceRefresh: Boolean = false) {
		initialize()
		if (forceRefresh) {
			loadExtensions()
			return
		}
		if (!isReady.value && !isLoading.value) {
			loadExtensions()
		}
		if (isLoading.value) {
			isLoading.filter { !it }.first()
		}
		if (!isReady.value) {
			loadExtensions()
		}
	}

	fun getCatalogueSources(): List<AnimeCatalogueSource> = facade.getCatalogueSources()

	fun getAniyomiAnimeSources(): List<AniyomiAnimeSourceKEx> = facade.getWrappedSources()

	fun getSourceById(sourceId: Long): AnimeSource? = facade.getSourceById(sourceId)

	fun getCatalogueSourceById(sourceId: Long): AnimeCatalogueSource? = facade.getCatalogueSourceById(sourceId)

	fun getAniyomiAnimeSourceById(sourceId: Long): AniyomiAnimeSourceKEx? = facade.getWrappedSourceById(sourceId)

	fun getAniyomiAnimeSourceByName(name: String): AniyomiAnimeSourceKEx? {
		val exact = facade.getWrappedSourceByName(name)
		if (exact != null) return exact
		val sourceId = name.removePrefix("ANIYOMI_").substringBefore(':').toLongOrNull() ?: return null
		return facade.getWrappedSourceById(sourceId)
	}

	fun getSourcesByLanguage(): Map<String, List<AnimeCatalogueSource>> = facade.getSourcesByLanguage()

	fun getSourceCount(): Int = facade.getSourceCount()

	fun hasExtensions(): Boolean = facade.hasExtensions()
}
