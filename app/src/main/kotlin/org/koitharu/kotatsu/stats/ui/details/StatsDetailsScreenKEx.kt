package org.koitharu.kotatsu.stats.ui.details

import android.graphics.Color
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.core.model.titleResId
import org.koitharu.kotatsu.parsers.model.MangaState
import org.koitharu.kotatsu.stats.data.StatsDetailsDataKEx
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.unit.Dp
import org.koitharu.kotatsu.stats.domain.StatsPeriod
import org.koitharu.kotatsu.stats.ui.STATS_PADDING
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.roundToInt

@Composable
fun StatsDetailsScreenKEx(
    viewModel: StatsDetailsViewModelKEx,
    bottomInset: Dp = 0.dp,
) {
    val statsData by viewModel.statsData.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()
    val selectedSort by viewModel.selectedSort.collectAsState()
    val categories by viewModel.favoriteCategories.collectAsState(emptyList())
    val selectedCategories by viewModel.selectedCategories.collectAsState()
    val dailyStats by viewModel.dailyStats.collectAsState()
    val startDate by viewModel.startDate.collectAsState()
    val endDate by viewModel.endDate.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(rememberNestedScrollInteropConnection())
    ) {
        FilterRowKEx(
            selectedType = selectedType,
            categories = categories,
            selectedCategories = selectedCategories,
            onTypeChange = { viewModel.selectedType.value = it },
            onCategoryToggle = viewModel::toggleCategory,
            onCategoriesClear = viewModel::clearCategories
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottomInset + 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
                item {
                    SortActionRow(selectedSort) { viewModel.selectedSort.value = it }
                }

                item {
                    ChartSectionKEx(
                        type = selectedType,
                        data = statsData,
                        dailyStats = dailyStats,
                        startDate = startDate,
                        endDate = endDate,
                        selectedDate = selectedDate,
                        onDateSelect = { viewModel.selectedDate.value = it },
                        onMovePeriod = { viewModel.movePeriod(it) }
                    )
                }

                if (selectedType == StatsDetailsTypeKEx.READ_DURATION) {
                    itemsIndexed(statsData) { index, data ->
                        RankedStatsRow(index + 1, data)
                    }
                } else {
                    val totalCount = statsData.sumOf { it.count }.coerceAtLeast(1)
                    val totalChaptersRead = statsData.sumOf { it.chaptersRead }.coerceAtLeast(1)
                    items(statsData) { data ->
                        DetailedStatsRow(data, selectedType, totalCount, totalChaptersRead)
                    }
                }
        }
    }
}

@Composable
private fun FilterRowKEx(
    selectedType: StatsDetailsTypeKEx,
    categories: List<FavouriteCategory>,
    selectedCategories: Set<Long>,
    onTypeChange: (StatsDetailsTypeKEx) -> Unit,
    onCategoryToggle: (FavouriteCategory) -> Unit,
    onCategoriesClear: () -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = STATS_PADDING, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item {
            DropdownFilterChip(
                label = stringResource(selectedType.resourceId),
                icon = painterResource(R.drawable.ic_usage),
                onTypeChange = onTypeChange
            )
        }
        item {
            CategoryFilterChip(categories, selectedCategories, onCategoryToggle, onCategoriesClear)
        }
        item {
            SourceFilterChip()
        }
    }
}

@Composable
private fun DropdownFilterChip(
    label: String,
    icon: Painter,
    onTypeChange: (StatsDetailsTypeKEx) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = true,
            onClick = { expanded = true },
            label = { Text(label) },
            leadingIcon = { Icon(icon, null, Modifier.size(18.dp)) },
            trailingIcon = { Icon(painterResource(R.drawable.ic_expand_more), null, Modifier.size(18.dp)) }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            StatsDetailsTypeKEx.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(stringResource(type.resourceId)) },
                    onClick = {
                        onTypeChange(type)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun CategoryFilterChip(
    categories: List<FavouriteCategory>,
    selected: Set<Long>,
    onToggle: (FavouriteCategory) -> Unit,
    onClear: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val label = when (selected.size) {
        0 -> stringResource(R.string.category)
        1 -> categories.find { it.id in selected }?.title ?: stringResource(R.string.category)
        else -> pluralStringResource(R.plurals.category_plural, selected.size, selected.size)
    }
    Box {
        FilterChip(
            selected = selected.isNotEmpty(),
            onClick = { expanded = true },
            label = { Text(label) },
            trailingIcon = { Icon(painterResource(R.drawable.ic_expand_more), null, Modifier.size(18.dp)) }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.stats_categories_all)) },
                onClick = { onClear(); expanded = false }
            )
            categories.forEach { cat ->
                DropdownMenuItem(
                    text = { Text(cat.title) },
                    onClick = { onToggle(cat) }
                )
            }
        }
    }
}

@Composable
private fun SourceFilterChip() {
    FilterChip(
        selected = false,
        onClick = { },
        label = { Text(stringResource(R.string.source)) },
        trailingIcon = { Icon(painterResource(R.drawable.ic_expand_more), null, Modifier.size(18.dp)) }
    )
}

@Composable
private fun SortActionRow(selected: StatsDetailsSortKEx, onSortChange: (StatsDetailsSortKEx) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = STATS_PADDING),
        horizontalArrangement = Arrangement.End
    ) {
        Box {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { expanded = true }
            ) {
                Text(
                    stringResource(selected.resourceId),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    painterResource(R.drawable.ic_sort),
                    null,
                    Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                StatsDetailsSortKEx.entries.forEach { sort ->
                    DropdownMenuItem(
                        text = { Text(stringResource(sort.resourceId)) },
                        onClick = {
                            onSortChange(sort)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChartSectionKEx(
    type: StatsDetailsTypeKEx,
    data: List<StatsDetailsDataKEx>,
    dailyStats: Map<LocalDate, Long>,
    startDate: LocalDate,
    endDate: LocalDate,
    selectedDate: LocalDate?,
    onDateSelect: (LocalDate?) -> Unit,
    onMovePeriod: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = STATS_PADDING)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(16.dp)
    ) {
        if (type == StatsDetailsTypeKEx.READ_DURATION) {
            ReadDurationHeader(startDate, endDate, dailyStats, selectedDate, onMovePeriod)
        }

        Box(modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)) {
            when (type) {
                StatsDetailsTypeKEx.SERIES_TYPE, StatsDetailsTypeKEx.STATUS, StatsDetailsTypeKEx.LANGUAGE -> {
                    J2KPieChart(data)
                }
                StatsDetailsTypeKEx.READ_DURATION -> {
                    J2KBarChart(dailyStats, startDate, endDate, selectedDate, onDateSelect)
                }
                else -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Chart implementation pending", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadDurationHeader(
    start: LocalDate,
    end: LocalDate,
    dailyStats: Map<LocalDate, Long>,
    selectedDate: LocalDate?,
    onMovePeriod: (Long) -> Unit
) {
    val configuration = LocalConfiguration.current
    val locale = configuration.locales[0]
    val formatter = remember(locale) { DateTimeFormatter.ofPattern("d MMMM", locale) }
    val rangeText = "${start.format(formatter)} - ${end.format(formatter)}"
    val totalTime = if (selectedDate != null) {
        dailyStats[selectedDate] ?: 0L
    } else {
        dailyStats.values.sum()
    }
    
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = { onMovePeriod(-1) }) {
                Icon(painterResource(R.drawable.ic_prev), null)
            }
            Text(
                rangeText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            IconButton(onClick = { onMovePeriod(1) }) {
                Icon(painterResource(R.drawable.ic_chevron_right), null)
            }
        }
        Text(
            StatsHelperKEx.formatDuration(totalTime),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun J2KPieChart(data: List<StatsDetailsDataKEx>) {
    AndroidView(
        factory = { PieChart(it) },
        modifier = Modifier.fillMaxSize(),
        update = { chart ->
            val entries = data.map { PieEntry(it.count.toFloat(), it.label) }
            val dataSet = PieDataSet(entries, "").apply {
                colors = data.map { it.color ?: Color.BLUE }
                setDrawValues(false)
                sliceSpace = 2f
            }
            chart.data = PieData(dataSet)
            chart.isDrawHoleEnabled = true
            chart.holeRadius = 60f
            chart.transparentCircleRadius = 65f
            chart.setHoleColor(Color.TRANSPARENT)
            chart.description.isEnabled = false
            chart.legend.isEnabled = false
            chart.setTouchEnabled(false)
            chart.invalidate()
        }
    )
}

@Composable
private fun J2KBarChart(
    dailyStats: Map<LocalDate, Long>,
    start: LocalDate,
    end: LocalDate,
    selectedDate: LocalDate?,
    onDateSelect: (LocalDate?) -> Unit
) {
    val days = remember(start, end) {
        (0..6).map { start.plusDays(it.toLong()) }
    }

    AndroidView(
        factory = { BarChart(it) },
        modifier = Modifier.fillMaxSize(),
        update = { chart ->
            val entries = days.mapIndexed { index, date ->
                BarEntry(index.toFloat(), dailyStats[date]?.toFloat() ?: 0f)
            }
            val dataSet = BarDataSet(entries, "").apply {
                color = Color.parseColor("#57aee2")
                setDrawValues(false)
                highLightColor = Color.parseColor("#3BAEEA")
            }
            chart.data = BarData(dataSet)
            chart.description.isEnabled = false
            chart.legend.isEnabled = false
            chart.axisRight.isEnabled = false
            chart.axisLeft.apply {
                textColor = Color.WHITE
                gridColor = Color.GRAY
                setDrawAxisLine(false)
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return StatsHelperKEx.formatDuration(value.toLong()).replace(" ", "")
                    }
                }
            }
            chart.xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = Color.WHITE
                setDrawGridLines(false)
                setDrawAxisLine(false)
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        val index = value.toInt()
                        return if (index in days.indices) {
                            days[index].dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
                        } else ""
                    }
                }
            }
            chart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    e?.let { onDateSelect(days[it.x.toInt()]) }
                }
                override fun onNothingSelected() {
                    onDateSelect(null)
                }
            })
            chart.invalidate()
        }
    )
}

@Composable
private fun DetailedStatsRow(
    data: StatsDetailsDataKEx,
    type: StatsDetailsTypeKEx,
    totalCount: Int,
    totalChaptersRead: Int
) {
    val resources = LocalContext.current.resources
    val label = remember(data.label, type) {
        when (type) {
            StatsDetailsTypeKEx.SERIES_TYPE -> {
                when (data.label.toIntOrNull()) {
                    StatsDetailsTypeKEx.TYPE_MANGA -> resources.getString(R.string.manga)
                    StatsDetailsTypeKEx.TYPE_MANHWA -> resources.getString(R.string.manhwa)
                    StatsDetailsTypeKEx.TYPE_MANHUA -> resources.getString(R.string.manhua)
                    StatsDetailsTypeKEx.TYPE_COMIC -> resources.getString(R.string.comic)
                    StatsDetailsTypeKEx.TYPE_WEBTOON -> resources.getString(R.string.webtoon)
                    else -> resources.getString(R.string.unknown)
                }
            }
            StatsDetailsTypeKEx.STATUS -> {
                MangaState.entries.find { it.name == data.label }?.titleResId?.let { resources.getString(it) } ?: data.label
            }
            else -> data.label
        }
    }

    val countPercent = (data.count.toFloat() / totalCount * 100).roundToInt()
    val progressPercent = if (totalChaptersRead > 0) (data.chaptersRead.toFloat() / totalChaptersRead * 100).roundToInt() else 0

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = STATS_PADDING),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (data.color != null) androidx.compose.ui.graphics.Color(data.color) else MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                StatItem(stringResource(R.string.category), "${data.count} ($countPercent%)", Modifier.weight(1f))
                StatItem(stringResource(R.string.chapters_read), "${data.chaptersRead} / ${data.totalChapters} ($progressPercent%)", Modifier.weight(1.5f))
                StatItem(stringResource(R.string.read_duration), StatsHelperKEx.formatDuration(data.readDuration), Modifier.weight(1.5f), Alignment.End)
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, modifier: Modifier = Modifier, alignment: Alignment.Horizontal = Alignment.Start) {
    Column(modifier = modifier, horizontalAlignment = alignment) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RankedStatsRow(rank: Int, data: StatsDetailsDataKEx) {
    val configuration = LocalConfiguration.current
    val locale = configuration.locales[0]
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = STATS_PADDING),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    String.format(locale, "%02d", rank),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = data.label.uppercase(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.primary
                )
                if (!data.subLabel.isNullOrBlank()) {
                    Text(
                        data.subLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                StatsHelperKEx.formatDuration(data.readDuration),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
