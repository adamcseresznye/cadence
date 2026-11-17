package com.cadence.daf

import android.content.Intent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

internal val GOAL_CHOICES = listOf(0, 5, 10, 15, 20)

/** Goal reached: the goal is on and today's minutes meet it. */
internal fun goalReached(todayMin: Int, goalMin: Int) = goalMin > 0 && todayMin >= goalMin

/** Grows from 0 to [target] once when the screen opens (unless [animateOpen] is false); instant when system animations are off. */
@Composable
internal fun rememberGrow(target: Float, animateOpen: Boolean = true): Float {
    var opened by remember { mutableStateOf(!animateOpen) }
    LaunchedEffect(Unit) { opened = true }
    val grown by animateFloatAsState(if (opened) target else 0f, motionTween(300), label = "grow")
    return grown
}

@Composable
fun ProgressScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val stats = state.practice
    val goal = state.settings.dailyGoalMin
    var confirmDelete by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    ScreenScaffold {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Text(stringResource(R.string.progress_title), style = MaterialTheme.typography.headlineMedium)
        }
        SectionCard {
            DayChart(state.dayMinutes, goal)
        }
        SectionCard {
            Row(Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile(Modifier.weight(1f), stringResource(R.string.stat_week), stats.weekMin, stringResource(R.string.stat_days_of, stats.weekDays, 7))
                StatTile(Modifier.weight(1f), stringResource(R.string.stat_month), stats.monthMin, stringResource(R.string.stat_days_of, stats.monthDays, 30))
                StatTile(Modifier.weight(1f), stringResource(R.string.stat_all_time), stats.allTimeMin, stringResource(R.string.stat_in_total))
            }
        }
        SectionCard {
            Text(stringResource(R.string.daily_goal), style = MaterialTheme.typography.titleMedium)
            Caption(R.string.goal_caption)
            MinuteChips(GOAL_CHOICES, goal, viewModel::setDailyGoalMin)
            TextButton(onClick = {
                scope.launch {
                    // shortcut: shared as plain text, not a file; switch to FileProvider if the log outgrows ~500 kB.
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, viewModel.practiceCsv())
                    context.startActivity(Intent.createChooser(send, null))
                }
            }) {
                Text(stringResource(R.string.export_history), color = MaterialTheme.colorScheme.primary)
            }
            TextButton(onClick = { confirmDelete = true }) {
                Text(stringResource(R.string.delete_history), color = MaterialTheme.colorScheme.primary)
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            text = { Text(stringResource(R.string.delete_history_text)) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearPracticeHistory(); confirmDelete = false }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

/** Reads top to bottom as a sentence: "Last 7 days / 60 / minutes / on 4 of 7 days". */
@Composable
private fun StatTile(modifier: Modifier, label: String, minutes: Int, detail: String) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(MaterialTheme.shapes.medium)
            .background(colors.surfaceContainerHigh)
            .padding(horizontal = 6.dp, vertical = 12.dp)
            .semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
        Text(minutes.toString(), style = MaterialTheme.typography.headlineSmall, color = colors.primary, maxLines = 1, modifier = Modifier.padding(top = 6.dp))
        Text(pluralStringResource(R.plurals.stat_unit_minutes, minutes), style = MaterialTheme.typography.bodySmall)
        Text(detail, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MinuteChips(choices: List<Int>, goal: Int, onSelect: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        choices.forEach { choice ->
            val selected = choice == goal
            FilterChip(
                selected = selected,
                onClick = { onSelect(choice) },
                label = {
                    Text(stringResource(if (choice == 0) R.string.goal_off else R.string.goal_minutes, choice))
                },
                leadingIcon = if (selected) {
                    { Icon(Icons.Filled.Check, contentDescription = null) }
                } else {
                    null
                },
                border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) colors.primary else colors.outline)
            )
        }
    }
}

/** Weeks as rows, Monday first; null pads before the first day and after today. */
internal fun heatmapRows(days: List<DayTotal>): List<List<DayTotal?>> {
    if (days.isEmpty()) return emptyList()
    val padded = List(days.first().date.dayOfWeek.value - 1) { null } + days
    return padded.chunked(7).map { it + List(7 - it.size) { null } }
}

private val dayFormat = DateTimeFormatter.ofPattern("EEE d MMM")

private val monthFormat = DateTimeFormatter.ofPattern("LLLL yyyy")

/** One month at a time, with arrows back to the first logged month. Tap a day for its date and minutes. */
@Composable
private fun DayChart(dayMinutes: Map<LocalDate, Int>, goal: Int) {
    val colors = MaterialTheme.colorScheme
    val today = LocalDate.now()
    val thisMonth = YearMonth.from(today)
    val firstMonth = remember(dayMinutes) { dayMinutes.keys.minOrNull()?.let { YearMonth.from(it) } ?: thisMonth }
    var monthText by rememberSaveable { mutableStateOf(thisMonth.toString()) }
    val month = YearMonth.parse(monthText)
    val days = remember(dayMinutes, month) { monthDays(dayMinutes, month, today) }
    val monthMin = days.sumOf { it.minutes }
    val monthTitle = month.format(monthFormat)
    val description = stringResource(
        R.string.chart_description,
        monthTitle,
        pluralStringResource(R.plurals.minutes_count, monthMin, monthMin),
        pluralStringResource(R.plurals.days_count, days.count { it.minutes > 0 }, days.count { it.minutes > 0 })
    )
    val rows = remember(days) { heatmapRows(days) }
    var selected by remember(month) { mutableStateOf<LocalDate?>(null) }
    val picked = days.firstOrNull { it.date == selected }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { monthText = month.minusMonths(1).toString() }, enabled = month > firstMonth) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.previous_month))
        }
        Text(monthTitle, style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = { monthText = month.plusMonths(1).toString() }, enabled = month < thisMonth) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.next_month))
        }
    }
    // Monday first, like the grid; letters follow the phone's language.
    Row(Modifier.fillMaxWidth().clearAndSetSemantics {}) {
        DayOfWeek.values().forEach { d ->
            Text(
                d.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant
            )
        }
    }
    Canvas(
        Modifier
            .fillMaxWidth()
            .aspectRatio(7f / rows.size.coerceAtLeast(1))
            .semantics { contentDescription = description }
            .pointerInput(rows) {
                detectTapGestures { tap ->
                    val cell = size.width / 7f
                    selected = rows.getOrNull((tap.y / cell).toInt())?.getOrNull((tap.x / cell).toInt())?.date
                }
            }
    ) {
        val cell = size.width / 7f
        val gap = 3.dp.toPx()
        val corner = CornerRadius(4.dp.toPx())
        rows.forEachIndexed { r, row ->
            row.forEachIndexed { c, day ->
                if (day == null) return@forEachIndexed
                val topLeft = Offset(c * cell + gap, r * cell + gap)
                val box = Size(cell - 2 * gap, cell - 2 * gap)
                val atGoal = goal > 0 && day.minutes >= goal
                val fill = when {
                    day.minutes == 0 -> colors.surfaceVariant
                    atGoal -> colors.primary
                    else -> colors.primaryContainer
                }
                drawRoundRect(fill, topLeft, box, corner)
                if (day.minutes > 0 && !atGoal) drawRoundRect(colors.primary, topLeft, box, corner, Stroke(1.dp.toPx()))
                if (day.date == selected) drawRoundRect(colors.onSurface, topLeft, box, corner, Stroke(2.dp.toPx()))
            }
        }
    }
    Text(
        if (picked == null) {
            stringResource(R.string.heatmap_hint)
        } else {
            stringResource(
                R.string.heatmap_day,
                picked.date.format(dayFormat),
                pluralStringResource(R.plurals.minutes_count, picked.minutes, picked.minutes)
            )
        },
        style = MaterialTheme.typography.bodySmall,
        color = colors.onSurfaceVariant
    )
}
