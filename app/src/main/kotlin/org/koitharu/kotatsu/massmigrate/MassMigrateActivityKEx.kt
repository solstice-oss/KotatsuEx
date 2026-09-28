package org.koitharu.kotatsu.massmigrate

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePaddingRelative
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.getTitle
import org.koitharu.kotatsu.core.model.parcelable.ParcelableManga
import org.koitharu.kotatsu.core.nav.AppRouter
import org.koitharu.kotatsu.core.nav.router
import org.koitharu.kotatsu.core.ui.BaseActivity
import org.koitharu.kotatsu.core.util.ext.end
import org.koitharu.kotatsu.core.util.ext.mangaExtra
import org.koitharu.kotatsu.core.util.ext.start
import org.koitharu.kotatsu.databinding.ActivityMassMigrateKexBinding
import org.koitharu.kotatsu.settings.compose.DropSauceTheme
import javax.inject.Inject

@Composable
private fun titleTextColor(): Color {
	val color = MaterialTheme.colorScheme.onSurface
	return if (color.luminance() < 0.4f) Color.White else color
}

@Composable
private fun subtitleTextColor(): Color {
	val color = MaterialTheme.colorScheme.onSurfaceVariant
	return if (color.luminance() < 0.3f) Color(0xFFC4C6D0) else color
}

@AndroidEntryPoint
class MassMigrateActivityKEx : BaseActivity<ActivityMassMigrateKexBinding>() {

	private val viewModel by viewModels<MassMigrateViewModelKEx>()

	@Inject
	lateinit var imageLoader: ImageLoader

	private val searchLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
		if (result.resultCode == RESULT_OK) {
			val data = result.data ?: return@registerForActivityResult
			val originalId = data.getLongExtra(KEY_ORIGINAL_ID, 0L)
			val parcelableManga = data.getParcelableExtra<ParcelableManga>("selected_manga")
			val selectedManga = parcelableManga?.manga ?: return@registerForActivityResult
			viewModel.updateAlternative(originalId, selectedManga)
		}
	}

	override fun setTitle(title: CharSequence?) {
		super.setTitle(title)
		viewBinding.collapsingToolbarLayout.title = title
	}

	override fun setTitle(titleId: Int) {
		setTitle(getText(titleId))
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityMassMigrateKexBinding.inflate(layoutInflater))
		setDisplayHomeAsUp(isEnabled = true, showUpAsClose = false)

		viewBinding.composeView.setViewCompositionStrategy(
			ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed,
		)
		viewBinding.composeViewBottom.setViewCompositionStrategy(
			ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed,
		)

		viewBinding.composeView.setContent {
			DropSauceTheme {
				val state by viewModel.state.collectAsState()

				LaunchedEffect(state.step) {
					val titleRes = when (state.step) {
						MassMigrateStep.SELECT_SOURCE -> R.string.select_source
						MassMigrateStep.SELECT_MANGA -> R.string.select_manga
						MassMigrateStep.SELECT_TARGETS -> R.string.select_target_sources
						MassMigrateStep.COMPARISON,
						MassMigrateStep.MIGRATING,
						MassMigrateStep.COMPLETED -> R.string.mass_migration
					}
					setTitle(titleRes)
				}

				MassMigrateContentScreen(state = state)
			}
		}

		viewBinding.composeViewBottom.setContent {
			DropSauceTheme {
				val state by viewModel.state.collectAsState()
				MassMigrateBottomBar(state = state)
			}
		}

		onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
			override fun handleOnBackPressed() {
				if (!viewModel.navigateBack()) {
					finish()
				}
			}
		})
	}

	override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
		val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
		viewBinding.appbar.updatePaddingRelative(
			start = bars.start(v),
			top = bars.top,
			end = bars.end(v),
		)
		return insets
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		if (item.itemId == android.R.id.home) {
			if (!viewModel.navigateBack()) {
				finish()
			}
			return true
		}
		return super.onOptionsItemSelected(item)
	}

	@Composable
	private fun MassMigrateContentScreen(state: MassMigrateState) {
		Box(
			modifier = Modifier
				.fillMaxSize()
				.nestedScroll(rememberNestedScrollInteropConnection()),
		) {
			when (state.step) {
				MassMigrateStep.SELECT_SOURCE -> SelectSourceStep(state)
				MassMigrateStep.SELECT_MANGA -> SelectMangaStep(state)
				MassMigrateStep.SELECT_TARGETS -> SelectTargetsStep(state)
				MassMigrateStep.COMPARISON -> ComparisonStep(state)
				MassMigrateStep.MIGRATING -> MigratingStep(state)
				MassMigrateStep.COMPLETED -> CompletedStep(state)
			}
		}
	}

	@Composable
	private fun MassMigrateBottomBar(state: MassMigrateState) {
		when (state.step) {
			MassMigrateStep.SELECT_MANGA -> {
				Surface(
					color = MaterialTheme.colorScheme.surface,
					tonalElevation = 3.dp,
					modifier = Modifier
						.fillMaxWidth()
						.navigationBarsPadding(),
				) {
					Box(
						modifier = Modifier
							.fillMaxWidth()
							.padding(horizontal = 16.dp, vertical = 12.dp),
					) {
						Button(
							onClick = { viewModel.onMangaSelectionConfirmed() },
							modifier = Modifier.fillMaxWidth(),
							enabled = state.selectedMangaIds.isNotEmpty(),
						) {
							Text(
								text = stringResource(R.string.action_continue),
								style = MaterialTheme.typography.labelLarge,
								fontWeight = FontWeight.SemiBold,
							)
						}
					}
				}
			}

			MassMigrateStep.SELECT_TARGETS -> {
				Surface(
					color = MaterialTheme.colorScheme.surface,
					tonalElevation = 3.dp,
					modifier = Modifier
						.fillMaxWidth()
						.navigationBarsPadding(),
				) {
					Box(
						modifier = Modifier
							.fillMaxWidth()
							.padding(horizontal = 16.dp, vertical = 12.dp),
					) {
						Button(
							onClick = { viewModel.prepareComparison() },
							modifier = Modifier.fillMaxWidth(),
							enabled = state.selectedTargetSourceKeys.isNotEmpty(),
						) {
							Text(
								text = stringResource(R.string.start_migration),
								style = MaterialTheme.typography.labelLarge,
								fontWeight = FontWeight.SemiBold,
							)
						}
					}
				}
			}

			MassMigrateStep.COMPARISON -> {
				Surface(
					color = MaterialTheme.colorScheme.surface,
					tonalElevation = 3.dp,
					modifier = Modifier
						.fillMaxWidth()
						.navigationBarsPadding(),
				) {
					Row(
						modifier = Modifier
							.fillMaxWidth()
							.padding(horizontal = 16.dp, vertical = 12.dp),
						horizontalArrangement = Arrangement.spacedBy(16.dp),
					) {
						OutlinedButton(
							onClick = { finish() },
							modifier = Modifier.weight(1f),
						) {
							Text(
								text = stringResource(R.string.label_cancel_mass_migration),
								style = MaterialTheme.typography.labelLarge,
							)
						}
						Button(
							onClick = { viewModel.executeFinalMigration() },
							modifier = Modifier.weight(1f),
							enabled = state.comparisonItems.isNotEmpty(),
						) {
							Text(
								text = stringResource(R.string.label_finalise_mass_migration),
								style = MaterialTheme.typography.labelLarge,
								fontWeight = FontWeight.SemiBold,
							)
						}
					}
				}
			}

			MassMigrateStep.COMPLETED -> {
				Surface(
					color = MaterialTheme.colorScheme.surface,
					tonalElevation = 3.dp,
					modifier = Modifier
						.fillMaxWidth()
						.navigationBarsPadding(),
				) {
					Box(
						modifier = Modifier
							.fillMaxWidth()
							.padding(horizontal = 16.dp, vertical = 12.dp),
					) {
						Button(
							onClick = { finish() },
							modifier = Modifier.fillMaxWidth(),
						) {
							Text(
								text = stringResource(R.string.action_finish),
								style = MaterialTheme.typography.labelLarge,
								fontWeight = FontWeight.SemiBold,
							)
						}
					}
				}
			}

			else -> Unit
		}
	}

	@Composable
	private fun SelectSourceStep(state: MassMigrateState) {
		if (state.sources.isEmpty()) {
			Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
				Text(
					text = stringResource(R.string.no_sources_to_migrate),
					style = MaterialTheme.typography.bodyLarge,
					color = titleTextColor(),
				)
			}
		} else {
			LazyColumn(
				modifier = Modifier.fillMaxSize(),
				contentPadding = PaddingValues(bottom = 16.dp),
			) {
				items(state.sources) { source ->
					Row(
						modifier = Modifier
							.fillMaxWidth()
							.clickable { viewModel.onSourceSelected(source.key) }
							.padding(16.dp),
						verticalAlignment = Alignment.CenterVertically,
					) {
						Column(modifier = Modifier.weight(1f)) {
							Text(
								text = source.title,
								style = MaterialTheme.typography.titleMedium,
								color = titleTextColor(),
							)
							Spacer(modifier = Modifier.height(2.dp))
							Text(
								text = "${source.mangaCount} manga entries",
								style = MaterialTheme.typography.bodySmall,
								color = subtitleTextColor(),
							)
						}
					}
					HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
				}
			}
		}
	}

	@Composable
	private fun SelectMangaStep(state: MassMigrateState) {
		LazyColumn(
			modifier = Modifier.fillMaxSize(),
			contentPadding = PaddingValues(bottom = 88.dp),
		) {
			items(state.mangaList) { manga ->
				val isChecked = manga.id in state.selectedMangaIds
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.clickable { viewModel.toggleMangaSelection(manga.id) }
						.padding(16.dp),
					verticalAlignment = Alignment.CenterVertically,
				) {
					Checkbox(
						checked = isChecked,
						onCheckedChange = { viewModel.toggleMangaSelection(manga.id) },
					)
					Spacer(modifier = Modifier.width(12.dp))
					Text(
						text = manga.title,
						style = MaterialTheme.typography.bodyLarge,
						color = titleTextColor(),
					)
				}
				HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
			}
		}
	}

	@Composable
	private fun SelectTargetsStep(state: MassMigrateState) {
		LazyColumn(
			modifier = Modifier.fillMaxSize(),
			contentPadding = PaddingValues(bottom = 88.dp),
		) {
			items(state.availableTargetSources) { source ->
				val isChecked = source.name in state.selectedTargetSourceKeys
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.clickable { viewModel.toggleTargetSource(source) }
						.padding(16.dp),
					verticalAlignment = Alignment.CenterVertically,
				) {
					Checkbox(
						checked = isChecked,
						onCheckedChange = { viewModel.toggleTargetSource(source) },
					)
					Spacer(modifier = Modifier.width(12.dp))
					Text(
						text = source.getTitle(this@MassMigrateActivityKEx),
						style = MaterialTheme.typography.bodyLarge,
						color = titleTextColor(),
					)
				}
				HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
			}
		}
	}

	@Composable
	private fun ComparisonStep(state: MassMigrateState) {
		if (state.isLoading) {
			Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
				CircularProgressIndicator()
			}
			return
		}

		val context = LocalContext.current

		LazyColumn(
			modifier = Modifier.fillMaxSize(),
			contentPadding = PaddingValues(bottom = 88.dp),
		) {
			items(state.comparisonItems) { item ->
				var menuExpanded by remember { mutableStateOf(false) }

				Row(
					modifier = Modifier
						.fillMaxWidth()
						.padding(12.dp),
					verticalAlignment = Alignment.CenterVertically,
				) {
					// Left: Original Cover & Title
					Row(
						modifier = Modifier
							.weight(1f)
							.clickable { router.openDetails(item.original) },
						verticalAlignment = Alignment.CenterVertically,
						horizontalArrangement = Arrangement.spacedBy(8.dp),
					) {
						AsyncImage(
							model = ImageRequest.Builder(context)
								.data(item.original.coverUrl)
								.mangaExtra(item.original)
								.build(),
							imageLoader = imageLoader,
							contentDescription = null,
							contentScale = ContentScale.Crop,
							modifier = Modifier
								.size(48.dp, 64.dp)
								.clip(RoundedCornerShape(4.dp)),
						)
						Text(
							text = item.original.title,
							style = MaterialTheme.typography.bodyMedium,
							color = titleTextColor(),
							maxLines = 2,
							overflow = TextOverflow.Ellipsis,
							modifier = Modifier.weight(1f),
						)
					}

					Spacer(modifier = Modifier.width(8.dp))

					// Right: Best Alternative Cover & Title
					val alt = item.alternative
					if (alt != null) {
						Row(
							modifier = Modifier
								.weight(1f)
								.clickable { router.openDetails(alt) },
							verticalAlignment = Alignment.CenterVertically,
							horizontalArrangement = Arrangement.spacedBy(8.dp),
						) {
							AsyncImage(
								model = ImageRequest.Builder(context)
									.data(alt.coverUrl)
									.mangaExtra(alt)
									.build(),
								imageLoader = imageLoader,
								contentDescription = null,
								contentScale = ContentScale.Crop,
								modifier = Modifier
									.size(48.dp, 64.dp)
									.clip(RoundedCornerShape(4.dp)),
							)
							Text(
								text = alt.title,
								style = MaterialTheme.typography.bodyMedium,
								color = titleTextColor(),
								maxLines = 2,
								overflow = TextOverflow.Ellipsis,
								modifier = Modifier.weight(1f),
							)
						}
					} else {
						Text(
							text = stringResource(R.string.label_no_match),
							style = MaterialTheme.typography.bodyMedium,
							color = MaterialTheme.colorScheme.error,
							modifier = Modifier.weight(1f),
						)
					}

					Box {
						IconButton(onClick = { menuExpanded = true }) {
							Icon(
								Icons.Filled.MoreVert,
								contentDescription = null,
								tint = subtitleTextColor(),
							)
						}
						DropdownMenu(
							expanded = menuExpanded,
							onDismissRequest = { menuExpanded = false },
						) {
							DropdownMenuItem(
								text = { Text(stringResource(R.string.label_search_manually)) },
								onClick = {
									menuExpanded = false
									val intent = Intent(this@MassMigrateActivityKEx, AlternativesSearchActivityKEx::class.java)
										.putExtra(AppRouter.KEY_QUERY, item.original.title)
										.putExtra(KEY_ORIGINAL_ID, item.original.id)
									searchLauncher.launch(intent)
								},
							)
							DropdownMenuItem(
								text = { Text(stringResource(R.string.label_cancel_mass_migration)) },
								onClick = {
									menuExpanded = false
									viewModel.removeItemFromComparison(item.original.id)
								},
							)
						}
					}
				}
				HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
			}
		}
	}

	@Composable
	private fun MigratingStep(state: MassMigrateState) {
		Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
			Column(horizontalAlignment = Alignment.CenterHorizontally) {
				Text(
					text = stringResource(R.string.migration_progress, state.migratedCount, state.totalToMigrate),
					style = MaterialTheme.typography.titleMedium,
					color = titleTextColor(),
				)
				Spacer(modifier = Modifier.height(16.dp))
				LinearProgressIndicator(
					progress = { if (state.totalToMigrate > 0) state.migratedCount.toFloat() / state.totalToMigrate else 0f },
					modifier = Modifier.fillMaxWidth(0.8f),
				)
			}
		}
	}

	@Composable
	private fun CompletedStep(state: MassMigrateState) {
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
			horizontalAlignment = Alignment.CenterHorizontally,
		) {
			Text(
				text = stringResource(R.string.migration_completed_successfully),
				style = MaterialTheme.typography.titleLarge,
				color = titleTextColor(),
			)
			Spacer(modifier = Modifier.height(12.dp))
			Text(
				text = stringResource(R.string.migration_results_summary, state.successCount, state.failedCount),
				style = MaterialTheme.typography.bodyLarge,
				color = subtitleTextColor(),
			)
			Spacer(modifier = Modifier.height(16.dp))
			if (state.failedMangas.isNotEmpty()) {
				Text(
					text = stringResource(R.string.migration_failed_list_header),
					style = MaterialTheme.typography.titleMedium,
					color = titleTextColor(),
					modifier = Modifier.align(Alignment.Start),
				)
				Spacer(modifier = Modifier.height(8.dp))
				LazyColumn(
					modifier = Modifier
						.weight(1f)
						.fillMaxWidth(),
				) {
					items(state.failedMangas) { manga ->
						Text(
							text = manga.title,
							style = MaterialTheme.typography.bodyMedium,
							color = titleTextColor(),
							modifier = Modifier.padding(vertical = 4.dp),
						)
						HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
					}
				}
			} else {
				Spacer(modifier = Modifier.weight(1f))
			}
		}
	}
}
