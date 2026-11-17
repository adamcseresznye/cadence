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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal val GOAL_CHOICES = listOf(0, 5, 10, 15, 20)

/** Goal reached: the goal is on and today's minutes meet it. */
internal fun goalReached(todayMin: Int, goalMin: Int) = goalMin > 0 && todayMin >= goalMin

/** Grows from 0 to [target] once when the screen opens (unless [animateOpen] is false); instant when system animations are off. */
@Composable
internal fun rememberGrow(target: Float, animateOpen: Boolean = true): Float {
    var opened by remember { mutableStateOf(!animateOpen) }
    LaunchedEffect(Unit) { opened = true }
    val grown by animateFloatAsState(if (opened) target else 0f, motionTween(420), label = "grow")
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
            DayChart(state.last30Days, goal, stats.monthMin, stats.monthDays)
            Text(
                stringResource(
                    R.string.progress_week,
                    stats.weekMin,
                    pluralStringResource(R.plurals.days_count, stats.weekDays, stats.weekDays)
                )
            )
            Text(stringResource(R.string.progress_month, stats.monthMin))
            Text(stringResource(R.string.progress_all_time, stats.allTimeMin / 60, stats.allTimeMin % 60))
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

/** Calendar heatmap of the last 30 days. Tap a day for its date and minutes. */
@Composable
private fun DayChart(days: List<DayTotal>, goal: Int, monthMin: Int, monthDays: Int) {
    val colors = MaterialTheme.colorScheme
    val description = stringResource(
        R.string.chart_description,
        pluralStringResource(R.plurals.minutes_count, monthMin, monthMin),
        pluralStringResource(R.plurals.days_count, monthDays, monthDays)
    )
    val grow = rememberGrow(1f)
    val rows = remember(days) { heatmapRows(days) }
    var selected by remember { mutableStateOf<LocalDate?>(null) }
    val picked = days.firstOrNull { it.date == selected }
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
                drawRoundRect(fill, topLeft, box, corner, alpha = grow)
                if (day.minutes > 0 && !atGoal) drawRoundRect(colors.primary, topLeft, box, corner, Stroke(1.dp.toPx()), alpha = grow)
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
