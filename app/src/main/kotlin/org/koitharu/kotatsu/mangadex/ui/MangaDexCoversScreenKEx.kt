package org.koitharu.kotatsu.mangadex.ui

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.util.ext.mangaSourceExtra
import org.koitharu.kotatsu.main.ui.nav.composeColorSchemeFromTheme
import org.koitharu.kotatsu.mangadex.data.MangaDexCoverKEx
import org.koitharu.kotatsu.parsers.model.Manga

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MangaDexCoversScreenKEx(
	viewModel: MangaDexCoversViewModelKEx,
	imageLoader: ImageLoader,
	onBackClick: () -> Unit,
	onCoverClick: (MangaDexCoverKEx) -> Unit,
) {
	val context = LocalContext.current
	val isDark = (LocalConfiguration.current.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
	val monetScheme = remember(context, isDark) {
		composeColorSchemeFromTheme(context, isDark)
	}

	MaterialExpressiveTheme(
		colorScheme = monetScheme,
	) {
		val state by viewModel.uiState.collectAsState()
		val manga = viewModel.manga

		Scaffold(
			topBar = {
				TopAppBar(
					title = {
						Column {
							Text(
								text = stringResource(R.string.mangadex_covers),
								style = MaterialTheme.typography.titleLarge,
								fontWeight = FontWeight.SemiBold,
								color = MaterialTheme.colorScheme.onSurface,
								maxLines = 1,
								overflow = TextOverflow.Ellipsis,
							)
							if (!manga?.title.isNullOrBlank()) {
								Text(
									text = manga.title,
									style = MaterialTheme.typography.bodyMedium,
									color = MaterialTheme.colorScheme.onSurfaceVariant,
									maxLines = 1,
									overflow = TextOverflow.Ellipsis,
								)
							}
						}
					},
					navigationIcon = {
						IconButton(onClick = onBackClick) {
							Icon(
								painter = painterResource(R.drawable.ic_arrow_back),
								contentDescription = stringResource(R.string.back),
								tint = MaterialTheme.colorScheme.onSurface,
							)
						}
					},
					colors = TopAppBarDefaults.topAppBarColors(
						containerColor = MaterialTheme.colorScheme.surface,
						scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
						navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
						titleContentColor = MaterialTheme.colorScheme.onSurface,
					),
				)
			},
			containerColor = MaterialTheme.colorScheme.surface,
		) { paddingValues ->
			Box(
				modifier = Modifier
					.fillMaxSize()
					.padding(paddingValues),
			) {
				when (val s = state) {
					is MangaDexCoversUiStateKEx.Loading -> {
						Box(
							modifier = Modifier.fillMaxSize(),
							contentAlignment = Alignment.Center,
						) {
							CircularProgressIndicator()
						}
					}

					is MangaDexCoversUiStateKEx.Empty -> {
						Box(
							modifier = Modifier
								.fillMaxSize()
								.padding(24.dp),
							contentAlignment = Alignment.Center,
						) {
							Column(horizontalAlignment = Alignment.CenterHorizontally) {
								Icon(
									painter = painterResource(R.drawable.ic_images),
									contentDescription = null,
									tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
									modifier = Modifier.size(64.dp),
								)
								Spacer(Modifier.height(16.dp))
								Text(
									text = stringResource(R.string.no_covers_found),
									style = MaterialTheme.typography.titleMedium,
									color = MaterialTheme.colorScheme.onSurfaceVariant,
									textAlign = TextAlign.Center,
								)
							}
						}
					}

					is MangaDexCoversUiStateKEx.Error -> {
						Box(
							modifier = Modifier
								.fillMaxSize()
								.padding(24.dp),
							contentAlignment = Alignment.Center,
						) {
							Column(horizontalAlignment = Alignment.CenterHorizontally) {
								Icon(
									painter = painterResource(R.drawable.ic_error_large),
									contentDescription = null,
									tint = MaterialTheme.colorScheme.error,
									modifier = Modifier.size(64.dp),
								)
								Spacer(Modifier.height(16.dp))
								Text(
									text = s.message ?: stringResource(R.string.failed_to_load_covers),
									style = MaterialTheme.typography.bodyLarge,
									color = MaterialTheme.colorScheme.onSurface,
									textAlign = TextAlign.Center,
								)
								Spacer(Modifier.height(16.dp))
								Button(onClick = viewModel::retry) {
									Text(stringResource(R.string.action_retry))
								}
							}
						}
					}

					is MangaDexCoversUiStateKEx.Success -> {
						CoversGrid(
							covers = s.covers,
							manga = manga,
							imageLoader = imageLoader,
							onCoverClick = onCoverClick,
						)
					}
				}
			}
		}
	}
}

@Composable
private fun CoversGrid(
	covers: List<MangaDexCoverKEx>,
	manga: Manga?,
	imageLoader: ImageLoader,
	onCoverClick: (MangaDexCoverKEx) -> Unit,
) {
	LazyVerticalGrid(
		columns = GridCells.Adaptive(minSize = 120.dp),
		contentPadding = PaddingValues(16.dp),
		verticalArrangement = Arrangement.spacedBy(12.dp),
		horizontalArrangement = Arrangement.spacedBy(12.dp),
		modifier = Modifier.fillMaxSize(),
	) {
		itemsIndexed(
			items = covers,
			key = { _, item -> item.id },
		) { index, cover ->
			CoverGridItem(
				cover = cover,
				index = index,
				manga = manga,
				imageLoader = imageLoader,
				onClick = { onCoverClick(cover) },
			)
		}
	}
}

@Composable
private fun CoverGridItem(
	cover: MangaDexCoverKEx,
	index: Int,
	manga: Manga?,
	imageLoader: ImageLoader,
	onClick: () -> Unit,
) {
	val ctx = LocalContext.current
	val request = remember(cover.url, manga?.source) {
		ImageRequest.Builder(ctx)
			.data(cover.url)
			.crossfade(true)
			.mangaSourceExtra(manga?.source)
			.build()
	}

	val contentDesc = if (!cover.volume.isNullOrBlank()) {
		stringResource(R.string.cover_item_description, cover.volume)
	} else {
		stringResource(R.string.cover_item_default_description, index + 1)
	}

	Card(
		shape = RoundedCornerShape(16.dp),
		elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
		colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
		modifier = Modifier
			.fillMaxWidth()
			.aspectRatio(0.67f)
			.clickable(onClick = onClick),
	) {
		Box(modifier = Modifier.fillMaxSize()) {
			AsyncImage(
				model = request,
				imageLoader = imageLoader,
				contentDescription = contentDesc,
				contentScale = ContentScale.Crop,
				error = painterResource(R.drawable.ic_images),
				modifier = Modifier.fillMaxSize(),
			)

			if (!cover.volume.isNullOrBlank()) {
				Surface(
					shape = RoundedCornerShape(bottomEnd = 12.dp, topStart = 16.dp),
					color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f),
					modifier = Modifier.align(Alignment.TopStart),
				) {
					Text(
						text = "Vol. ${cover.volume}",
						style = MaterialTheme.typography.labelSmall,
						fontWeight = FontWeight.Bold,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
						modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
					)
				}
			}
		}
	}
}
