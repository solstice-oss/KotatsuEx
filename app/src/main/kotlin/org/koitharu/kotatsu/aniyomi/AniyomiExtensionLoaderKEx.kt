package org.koitharu.kotatsu.aniyomi

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.content.pm.PackageInfoCompat
import dagger.Lazy
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.AnimeSourceFactory
import eu.kanade.tachiyomi.util.lang.Hash
import eu.kanade.tachiyomi.util.system.ChildFirstPathClassLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.aniyomi.model.AniyomiExtensionInfoKEx
import org.koitharu.kotatsu.aniyomi.model.AniyomiLoadResultKEx
import org.koitharu.kotatsu.core.logs.AppLogger
import org.koitharu.kotatsu.core.model.ContentWarning
import org.koitharu.kotatsu.mihon.compat.KotoInjektBridge
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AniyomiExtensionLoaderKEx @Inject constructor(
	private val injektBridge: Lazy<KotoInjektBridge>,
) {

	private val loadedExtensions = ConcurrentHashMap<String, LoadedExtension>()

	private class LoadedExtension(val apkPath: String?, val result: AniyomiLoadResultKEx.Success)

	companion object {
		private const val TAG = "AniyomiExtensionLoader"
		private const val EXTENSION_FEATURE = "tachiyomi.animeextension"

		private const val METADATA_SOURCE_CLASS = "tachiyomi.animeextension.class"
		private const val METADATA_SOURCE_FACTORY = "tachiyomi.animeextension.factory"
		private const val METADATA_NSFW = "tachiyomi.animeextension.nsfw"
		private const val METADATA_CONTENT_WARNING = "aniyomix.contentWarning"

		@Suppress("DEPRECATION")
		fun getSignatures(pkgInfo: PackageInfo): List<String> {
			val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
				val signingInfo = pkgInfo.signingInfo ?: return emptyList()
				if (signingInfo.hasMultipleSigners()) {
					signingInfo.apkContentsSigners
				} else {
					signingInfo.signingCertificateHistory
				}
			} else {
				pkgInfo.signatures
			}
			return signatures.orEmpty().map { Hash.sha256(it.toByteArray()) }
		}

		private const val PRIVATE_EXTENSION_DIR = "exts"
		private const val PRIVATE_EXTENSION_EXT = "ext"

		fun getPrivateExtensionDir(context: Context): File =
			File(context.filesDir, PRIVATE_EXTENSION_DIR)

		fun isPrivateExtensionInstalled(context: Context, pkgName: String): Boolean =
			File(getPrivateExtensionDir(context), "$pkgName.$PRIVATE_EXTENSION_EXT").isFile

		@Suppress("DEPRECATION")
		private val packageFlags: Int
			get() = PackageManager.GET_META_DATA or
				PackageManager.GET_CONFIGURATIONS or
				PackageManager.GET_SIGNATURES or
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES else 0

		fun isExtensionPackage(context: Context, pkgName: String): Boolean = try {
			@Suppress("DEPRECATION")
			val pkgInfo = context.packageManager.getPackageInfo(
				pkgName,
				PackageManager.GET_META_DATA or PackageManager.GET_CONFIGURATIONS,
			)
			isPackageAnExtensionStatic(pkgInfo)
		} catch (_: PackageManager.NameNotFoundException) {
			false
		}

		internal fun isPackageAnExtensionStatic(pkgInfo: PackageInfo): Boolean {
			val pkgName = pkgInfo.packageName ?: ""
			if (pkgName.startsWith("eu.kanade.tachiyomi.animeextension")) {
				return true
			}
			val appInfo = pkgInfo.applicationInfo ?: return false
			val hasFeature = pkgInfo.reqFeatures?.any { it.name == EXTENSION_FEATURE } == true
			return hasFeature || readSourceClassNames(appInfo.metaData) != null
		}

		internal fun readSourceClassNames(metaData: Bundle?): String? {
			metaData ?: return null
			return metaData.getString(METADATA_SOURCE_CLASS)
				?: metaData.getString(METADATA_SOURCE_FACTORY)
		}
	}

	suspend fun loadExtensions(context: Context, includePrivate: Boolean = true): List<AniyomiLoadResultKEx> =
		withContext(Dispatchers.IO) {
			injektBridge.get().initialize()

			val systemPackages = getInstalledExtensionPackages(context)
			val privatePackages = if (includePrivate) getInstalledPrivateExtensionPackages(context) else emptyList()

			val allPackages = (systemPackages + privatePackages).associateBy { it.packageName }.values

			coroutineScope {
				allPackages.map { pkg ->
					async {
						loadExtension(context, pkg)
					}
				}.awaitAll()
			}
		}

	private fun loadExtension(context: Context, pkgInfo: PackageInfo): AniyomiLoadResultKEx {
		val pkgName = pkgInfo.packageName
		val appInfo = pkgInfo.applicationInfo ?: return AniyomiLoadResultKEx.Error(pkgName, "Missing applicationInfo")
		val apkPath = appInfo.sourceDir

		val cached = loadedExtensions[pkgName]
		if (cached != null && cached.apkPath == apkPath && File(apkPath).exists()) {
			return cached.result
		}

		return try {
			val classLoader = ChildFirstPathClassLoader(apkPath, appInfo.nativeLibraryDir, context.classLoader)

			val metaData = appInfo.metaData
			val sourceClassNames = Companion.readSourceClassNames(metaData)
				?: return AniyomiLoadResultKEx.Error(pkgName, "Missing source class metadata")

			val sources = mutableListOf<AnimeSource>()
			val catalogueSources = mutableListOf<AnimeCatalogueSource>()

			sourceClassNames.split(';', ':', ',').map { it.trim() }.filter { it.isNotEmpty() }.forEach { rawClassName ->
				val className = if (rawClassName.startsWith('.')) pkgName + rawClassName else rawClassName
				try {
					val clazz = Class.forName(className, false, classLoader)
					val instance = clazz.getDeclaredConstructor().newInstance()
					if (instance is AnimeSource) {
						sources.add(instance)
						if (instance is AnimeCatalogueSource) {
							catalogueSources.add(instance)
						}
					} else if (instance is AnimeSourceFactory) {
						val factorySources = instance.createSources()
						sources.addAll(factorySources)
						catalogueSources.addAll(factorySources.filterIsInstance<AnimeCatalogueSource>())
					}
				} catch (e: Throwable) {
					Log.e(TAG, "Failed to instantiate source $className in $pkgName", e)
				}
			}

			if (sources.isEmpty()) {
				return AniyomiLoadResultKEx.Error(pkgName, "No valid sources created from $pkgName")
			}

			val versionName = pkgInfo.versionName ?: "1.0"
			val versionCode = PackageInfoCompat.getLongVersionCode(pkgInfo)
			val lang = metaData?.getString("tachiyomi.animeextension.lang") ?: "all"
			val isNsfw = metaData?.getInt(METADATA_NSFW, 0) == 1

			val info = AniyomiExtensionInfoKEx(
				name = pkgInfo.applicationInfo?.loadLabel(context.packageManager)?.toString() ?: pkgName,
				pkgName = pkgName,
				versionName = versionName,
				versionCode = versionCode,
				lang = lang,
				isNsfw = isNsfw,
				signatures = Companion.getSignatures(pkgInfo),
			)

			val result = AniyomiLoadResultKEx.Success(
				info = info,
				pkgName = pkgName,
				sources = sources,
				catalogueSources = catalogueSources,
				contentWarning = if (isNsfw) ContentWarning.NSFW else ContentWarning.SAFE,
			)

			loadedExtensions[pkgName] = LoadedExtension(apkPath, result)
			result
		} catch (e: Throwable) {
			Log.e(TAG, "Failed to load extension package $pkgName", e)
			AniyomiLoadResultKEx.Error(pkgName, "Failed to load extension: ${e.message}", e)
		}
	}

	@Suppress("DEPRECATION")
	private fun getInstalledExtensionPackages(context: Context): List<PackageInfo> {
		val pkgManager = context.packageManager
		return try {
			val packages = pkgManager.getInstalledPackages(Companion.packageFlags)
			packages.filter { Companion.isPackageAnExtensionStatic(it) }
		} catch (e: Exception) {
			Log.e(TAG, "Failed to get installed packages", e)
			emptyList()
		}
	}

	@Suppress("DEPRECATION")
	private fun getInstalledPrivateExtensionPackages(context: Context): List<PackageInfo> {
		val dir = Companion.getPrivateExtensionDir(context)
		val files = dir.listFiles() ?: return emptyList()
		val pkgManager = context.packageManager
		return files.filter { it.isFile && it.extension == "ext" }.mapNotNull { file ->
			try {
				val pkgInfo = pkgManager.getPackageArchiveInfo(file.absolutePath, Companion.packageFlags)
				if (pkgInfo != null) {
					pkgInfo.applicationInfo?.let { app ->
						app.sourceDir = file.absolutePath
						app.publicSourceDir = file.absolutePath
					}
					pkgInfo
				} else null
			} catch (e: Exception) {
				Log.e(TAG, "Failed to parse private extension ${file.name}", e)
				null
			}
		}
	}
}
