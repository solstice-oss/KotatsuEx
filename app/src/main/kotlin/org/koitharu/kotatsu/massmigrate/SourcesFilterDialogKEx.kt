package org.koitharu.kotatsu.massmigrate

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.getTitle
import org.koitharu.kotatsu.parsers.model.MangaSource

@Composable
fun SourcesFilterDialogKEx(
	installedSources: List<MangaSource>,
	initialSelectedKeys: Set<String>,
	onDismiss: () -> Unit,
	onApply: (Set<String>) -> Unit,
) {
	val context = LocalContext.current
	var selectedKeys by remember(initialSelectedKeys) { mutableStateOf(initialSelectedKeys) }

	Dialog(onDismissRequest = onDismiss) {
		Surface(
			shape = RoundedCornerShape(24.dp),
			color = MaterialTheme.colorScheme.surface,
			tonalElevation = 6.dp,
			modifier = Modifier
				.fillMaxWidth()
				.padding(vertical = 16.dp),
		) {
			Column(
				modifier = Modifier
					.fillMaxWidth()
					.padding(vertical = 16.dp),
			) {
				Text(
					text = stringResource(R.string.source),
					style = MaterialTheme.typography.titleLarge,
					fontWeight = FontWeight.Bold,
					color = MaterialTheme.colorScheme.onSurface,
					modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
				)

				HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

				LazyColumn(
					modifier = Modifier
						.weight(1f, fill = false)
						.heightIn(max = 360.dp)
						.fillMaxWidth(),
				) {
					items(installedSources) { source ->
						val isChecked = source.name in selectedKeys
						Row(
							modifier = Modifier
								.fillMaxWidth()
								.clickable {
									selectedKeys = if (isChecked) {
										selectedKeys - source.name
									} else {
										selectedKeys + source.name
									}
								}
								.padding(horizontal = 20.dp, vertical = 10.dp),
							verticalAlignment = Alignment.CenterVertically,
						) {
							Checkbox(
								checked = isChecked,
								onCheckedChange = { checked ->
									selectedKeys = if (checked) {
										selectedKeys + source.name
									} else {
										selectedKeys - source.name
									}
								},
							)
							Spacer(Modifier.width(12.dp))
							Text(
								text = source.getTitle(context),
								style = MaterialTheme.typography.bodyLarge,
								color = MaterialTheme.colorScheme.onSurface,
							)
						}
					}
				}

				HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

				Row(
					modifier = Modifier
						.fillMaxWidth()
						.padding(start = 16.dp, end = 16.dp, top = 12.dp),
					horizontalArrangement = Arrangement.End,
					verticalAlignment = Alignment.CenterVertically,
				) {
					TextButton(onClick = onDismiss) {
						Text(stringResource(android.R.string.cancel))
					}
					Spacer(Modifier.width(8.dp))
					TextButton(
						onClick = { selectedKeys = emptySet() },
					) {
						Text(stringResource(R.string.reset))
					}
					Spacer(Modifier.width(8.dp))
					Button(
						onClick = {
							onApply(selectedKeys)
							onDismiss()
						},
					) {
						Text(stringResource(R.string.action_finish))
					}
				}
			}
		}
	}
}
