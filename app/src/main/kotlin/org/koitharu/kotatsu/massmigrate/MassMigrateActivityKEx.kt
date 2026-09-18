package org.koitharu.kotatsu.massmigrate

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.getTitle
import org.koitharu.kotatsu.core.model.parcelable.ParcelableManga
import org.koitharu.kotatsu.core.nav.AppRouter
import org.koitharu.kotatsu.core.nav.router
import org.koitharu.kotatsu.core.ui.BaseActivity
import org.koitharu.kotatsu.databinding.ActivityMassMigrateKexBinding
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.settings.compose.DropSauceTheme

@AndroidEntryPoint
class MassMigrateActivityKEx : BaseActivity<ActivityMassMigrateKexBinding>() {

	private val viewModel by viewModels<MassMigrateViewModelKEx>()

	private val searchLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
		if (result.resultCode == RESULT_OK) {
			val data = result.data ?: return@registerForActivityResult
			val originalId = data.getLongExtra(KEY_ORIGINAL_ID, 0L)
			val parcelableManga = data.getParcelableExtra<ParcelableManga>("selected_manga")
			val selectedManga = parcelableManga?.manga ?: return@registerForActivityResult
			viewModel.updateAlternative(originalId, selectedManga)
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityMassMigrateKexBinding.inflate(layoutInflater))
		setDisplayHomeAsUp(isEnabled = true, showUpAsClose = false)

		viewBinding.composeView.setContent {
			DropSauceTheme {
				val state by viewModel.state.collectAsState()
				MassMigrateScreen(state)
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

	@OptIn(ExperimentalMaterial3Api::class)
	@Composable
	private fun MassMigrateScreen(state: MassMigrateState) {
		Scaffold(
			topBar = {
				TopAppBar(
					title = {
						Text(
							text = when (state.step) {
								MassMigrateStep.SELECT_SOURCE -> stringResource(R.string.select_source)
								MassMigrateStep.SELECT_MANGA -> stringResource(R.string.select_manga)
								MassMigrateStep.SELECT_TARGETS -> stringResource(R.string.select_target_sources)
								MassMigrateStep.COMPARISON -> stringResource(R.string.mass_migration)
								MassMigrateStep.MIGRATING -> stringResource(R.string.mass_migration)
								MassMigrateStep.COMPLETED -> stringResource(R.string.mass_migration)
							}
						)
					}
				)
			},
			modifier = Modifier.systemBarsPadding()
		) { paddingValues ->
			Box(
				modifier = Modifier
					.fillMaxSize()
					.padding(paddingValues)
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
	}

	@Composable
	private fun SelectSourceStep(state: MassMigrateState) {
		if (state.sources.isEmpty()) {
			Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
				Text(text = stringResource(R.string.no_sources_to_migrate), style = MaterialTheme.typography.bodyLarge)
			}
		} else {
			LazyColumn(modifier = Modifier.fillMaxSize()) {
				items(state.sources) { source ->
					Row(
						modifier = Modifier
							.fillMaxWidth()
							.clickable { viewModel.onSourceSelected(source.key) }
							.padding(16.dp),
						verticalAlignment = Alignment.CenterVertically
					) {
						Column(modifier = Modifier.weight(1f)) {
							Text(text = source.title, style = MaterialTheme.typography.titleMedium)
							Text(
								text = "${source.mangaCount} manga entries",
								style = MaterialTheme.typography.bodySmall,
								color = MaterialTheme.colorScheme.onSurfaceVariant
							)
						}
					}
					HorizontalDivider()
				}
			}
		}
	}

	@Composable
	private fun SelectMangaStep(state: MassMigrateState) {
		Column(modifier = Modifier.fillMaxSize()) {
			LazyColumn(modifier = Modifier.weight(1f)) {
				items(state.mangaList) { manga ->
					val isChecked = manga.id in state.selectedMangaIds
					Row(
						modifier = Modifier
							.fillMaxWidth()
							.clickable { viewModel.toggleMangaSelection(manga.id) }
							.padding(16.dp),
						verticalAlignment = Alignment.CenterVertically
					) {
						Checkbox(checked = isChecked, onCheckedChange = { viewModel.toggleMangaSelection(manga.id) })
						Spacer(modifier = Modifier.width(12.dp))
						Text(text = manga.title, style = MaterialTheme.typography.bodyLarge)
					}
					HorizontalDivider()
				}
			}
			Button(
				onClick = { viewModel.onMangaSelectionConfirmed() },
				modifier = Modifier
					.fillMaxWidth()
					.padding(16.dp),
				enabled = state.selectedMangaIds.isNotEmpty()
			) {
				Text(text = "Continue")
			}
		}
	}

	@Composable
	private fun SelectTargetsStep(state: MassMigrateState) {
		Column(modifier = Modifier.fillMaxSize()) {
			LazyColumn(modifier = Modifier.weight(1f)) {
				items(state.availableTargetSources) { source ->
					val isChecked = source.name in state.selectedTargetSourceKeys
					Row(
						modifier = Modifier
							.fillMaxWidth()
							.clickable { viewModel.toggleTargetSource(source) }
							.padding(16.dp),
						verticalAlignment = Alignment.CenterVertically
					) {
						Checkbox(checked = isChecked, onCheckedChange = { viewModel.toggleTargetSource(source) })
						Spacer(modifier = Modifier.width(12.dp))
						Text(text = source.getTitle(this@MassMigrateActivityKEx), style = MaterialTheme.typography.bodyLarge)
					}
					HorizontalDivider()
				}
			}
			Button(
				onClick = { viewModel.prepareComparison() },
				modifier = Modifier
					.fillMaxWidth()
					.padding(16.dp),
				enabled = state.selectedTargetSourceKeys.isNotEmpty()
			) {
				Text(text = stringResource(R.string.start_migration))
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

		Column(modifier = Modifier.fillMaxSize()) {
			LazyColumn(
				modifier = Modifier
					.weight(1f)
					.fillMaxWidth()
			) {
				items(state.comparisonItems) { item ->
					var menuExpanded by remember { mutableStateOf(false) }

					Row(
						modifier = Modifier
							.fillMaxWidth()
							.padding(12.dp),
						verticalAlignment = Alignment.CenterVertically
					) {
						// Left: Original
						Text(
							text = item.original.title,
							style = MaterialTheme.typography.bodyMedium,
							maxLines = 2,
							overflow = TextOverflow.Ellipsis,
							modifier = Modifier
								.weight(1f)
								.clickable { router.openDetails(item.original) }
								.padding(4.dp)
						)

						Spacer(modifier = Modifier.width(8.dp))

						// Right: Best Alternative
						val alt = item.alternative
						if (alt != null) {
							Text(
								text = alt.title,
								style = MaterialTheme.typography.bodyMedium,
								maxLines = 2,
								overflow = TextOverflow.Ellipsis,
								modifier = Modifier
									.weight(1f)
									.clickable { router.openDetails(alt) }
									.padding(4.dp)
							)
						} else {
							Text(
								text = stringResource(R.string.label_no_match),
								style = MaterialTheme.typography.bodyMedium,
								color = MaterialTheme.colorScheme.error,
								modifier = Modifier.weight(1f)
							)
						}

						Box {
							IconButton(onClick = { menuExpanded = true }) {
								Icon(Icons.Filled.MoreVert, contentDescription = null)
							}
							DropdownMenu(
								expanded = menuExpanded,
								onDismissRequest = { menuExpanded = false }
							) {
								DropdownMenuItem(
									text = { Text(stringResource(R.string.label_search_manually)) },
									onClick = {
										menuExpanded = false
										val intent = Intent(this@MassMigrateActivityKEx, AlternativesSearchActivityKEx::class.java)
											.putExtra(AppRouter.KEY_QUERY, item.original.title)
											.putExtra(KEY_ORIGINAL_ID, item.original.id)
										searchLauncher.launch(intent)
									}
								)
							}
						}

						// Cross button to cancel/remove this entry
						IconButton(onClick = { viewModel.removeItemFromComparison(item.original.id) }) {
							Icon(Icons.Filled.Close, contentDescription = "Cancel")
						}
					}
					HorizontalDivider()
				}
			}

			Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(16.dp),
				horizontalArrangement = Arrangement.spacedBy(16.dp)
			) {
				OutlinedButton(
					onClick = { finish() },
					modifier = Modifier.weight(1f)
				) {
					Text(text = stringResource(R.string.label_cancel_mass_migration))
				}
				Button(
					onClick = { viewModel.executeFinalMigration() },
					modifier = Modifier.weight(1f),
					enabled = state.comparisonItems.isNotEmpty()
				) {
					Text(text = stringResource(R.string.label_finalise_mass_migration))
				}
			}
		}
	}

	@Composable
	private fun MigratingStep(state: MassMigrateState) {
		Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
			Column(horizontalAlignment = Alignment.CenterHorizontally) {
				Text(
					text = stringResource(R.string.migration_progress, state.migratedCount, state.totalToMigrate),
					style = MaterialTheme.typography.titleMedium
				)
				Spacer(modifier = Modifier.height(16.dp))
				LinearProgressIndicator(
					progress = { if (state.totalToMigrate > 0) state.migratedCount.toFloat() / state.totalToMigrate else 0f },
					modifier = Modifier.fillMaxWidth(0.8f)
				)
			}
		}
	}

	@Composable
	private fun CompletedStep(state: MassMigrateState) {
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(16.dp),
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			Text(text = stringResource(R.string.migration_completed_successfully), style = MaterialTheme.typography.titleLarge)
			Spacer(modifier = Modifier.height(12.dp))
			Text(
				text = stringResource(R.string.migration_results_summary, state.successCount, state.failedCount),
				style = MaterialTheme.typography.bodyLarge
			)
			Spacer(modifier = Modifier.height(16.dp))
			if (state.failedMangas.isNotEmpty()) {
				Text(
					text = stringResource(R.string.migration_failed_list_header),
					style = MaterialTheme.typography.titleMedium,
					modifier = Modifier.align(Alignment.Start)
				)
				Spacer(modifier = Modifier.height(8.dp))
				LazyColumn(
					modifier = Modifier
						.weight(1f)
						.fillMaxWidth()
				) {
					items(state.failedMangas) { manga ->
						Text(
							text = manga.title,
							style = MaterialTheme.typography.bodyMedium,
							modifier = Modifier.padding(vertical = 4.dp)
						)
						HorizontalDivider()
					}
				}
			} else {
				Spacer(modifier = Modifier.weight(1f))
			}
			Spacer(modifier = Modifier.height(16.dp))
			Button(
				onClick = { finish() },
				modifier = Modifier.fillMaxWidth()
			) {
				Text(text = "Finish")
			}
		}
	}
}
