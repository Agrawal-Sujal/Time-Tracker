package com.sunflower.timetracker.presentation.screens.analysis

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sunflower.timetracker.domain.model.Tag
import com.sunflower.timetracker.domain.model.TagStats
import com.sunflower.timetracker.domain.model.TimeSession
import com.sunflower.timetracker.presentation.components.SectionCard
import com.sunflower.timetracker.presentation.components.StatBarRow
import com.sunflower.timetracker.presentation.theme.AccentGreen
import com.sunflower.timetracker.presentation.theme.AccentRed
import com.sunflower.timetracker.presentation.theme.Background
import com.sunflower.timetracker.presentation.theme.CardBg
import com.sunflower.timetracker.presentation.theme.Outline
import com.sunflower.timetracker.presentation.theme.Primary
import com.sunflower.timetracker.presentation.theme.SurfaceVar
import com.sunflower.timetracker.presentation.theme.TextPrimary
import com.sunflower.timetracker.presentation.theme.TextSecondary
import com.sunflower.timetracker.presentation.theme.TextTertiary
import com.sunflower.timetracker.presentation.viewmodel.AnalysisPeriod
import com.sunflower.timetracker.presentation.viewmodel.AnalysisViewModel
import com.sunflower.timetracker.presentation.viewmodel.SortMode
import com.sunflower.timetracker.util.formatDate
import com.sunflower.timetracker.util.formatDurationShort
import com.sunflower.timetracker.util.formatHours
import com.sunflower.timetracker.util.formatTime
import com.sunflower.timetracker.util.parseColor
import java.util.Calendar

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AnalysisScreen(vm: AnalysisViewModel) {
    val period by vm.period.collectAsState()
    val sortMode by vm.sortMode.collectAsState()
    val stats by vm.stats.collectAsState()
    val selectedTag by vm.selectedTag.collectAsState()
    val sessions by vm.tagSessions.collectAsState()

    // Back-stack: if tag selected, show detail; else show list
    if (selectedTag != null) {
        TagDetailScreen(
            tag = selectedTag!!,
            sessions = sessions,
            onBack = { vm.selectTag(null) },
            onDelete = vm::deleteSession,
            onUpdate = vm::updateSession,
            onAdd = { start, end -> vm.addManualSession(selectedTag!!.id, start, end) }
        )
    } else {
        AnalysisListScreen(
            period = period,
            sortMode = sortMode,
            stats = stats,
            onPeriod = vm::setPeriod,
            onSort = vm::setSortMode,
            onSelectTag = { vm.selectTag(it) }
        )
    }
}

// ── Analysis list ─────────────────────────────────────────────────────────────

@Composable
private fun AnalysisListScreen(
    period: AnalysisPeriod,
    sortMode: SortMode,
    stats: List<TagStats>,
    onPeriod: (AnalysisPeriod) -> Unit,
    onSort: (SortMode) -> Unit,
    onSelectTag: (Tag) -> Unit
) {
    val totalMs = stats.sumOf { it.totalDurationMs }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Analysis", style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold, color = TextPrimary
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Sort:", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                SmallFilterChip(
                    "Time",
                    sortMode == SortMode.BY_DURATION
                ) { onSort(SortMode.BY_DURATION) }
                SmallFilterChip("Name", sortMode == SortMode.BY_NAME) { onSort(SortMode.BY_NAME) }
            }
        }

        PeriodToggle(
            current = period,
            onChange = onPeriod,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (stats.isEmpty()) {
                EmptyState(period)
            } else {
                // Summary
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryTile("Total Time", formatHours(totalMs), Primary, Modifier.weight(1f))
                    SummaryTile("Tags Active", "${stats.size}", AccentGreen, Modifier.weight(1f))
                }
                // Donut
                SectionCard {
                    Text(
                        "Time Distribution",
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(16.dp))
                    DonutChart(
                        stats = stats, modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }
                // Breakdown — tappable rows
                SectionCard {
                    Text(
                        "Breakdown",
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Tap a tag to see full details", color = TextTertiary,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(Modifier.height(12.dp))
                    stats.forEach { stat ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSelectTag(stat.tag) }
                                .padding(vertical = 4.dp)
                        ) {
                            StatBarRow(
                                label = stat.tag.name,
                                value = formatHours(stat.totalDurationMs),
                                percentage = stat.percentage,
                                color = parseColor(stat.tag.colorHex)
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
            Spacer(Modifier.height(80.dp))
        }
    }
}

// ── Tag detail ────────────────────────────────────────────────────────────────

@Composable
private fun TagDetailScreen(
    tag: Tag,
    sessions: List<TimeSession>,
    onBack: () -> Unit,
    onDelete: (Long) -> Unit,
    onUpdate: (TimeSession) -> Unit,
    onAdd: (Long, Long) -> Unit
) {
    val color = parseColor(tag.colorHex)
    val finished = sessions.filter { it.endTime != null }
    val totalMs = finished.sumOf { it.durationMs }
    var showAddDialog by remember { mutableStateOf(false) }
    var editSession by remember { mutableStateOf<TimeSession?>(null) }
    var deleteTarget by remember { mutableStateOf<Long?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // ── Top bar ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Text(
                    tag.name, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = TextPrimary
                )
            }
            IconButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add session", tint = Primary)
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Stats summary ──
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MiniStatCard("Sessions", "${finished.size}", color, Modifier.weight(1f))
                    MiniStatCard("Total Time", formatHours(totalMs), color, Modifier.weight(1f))
                    MiniStatCard(
                        "Avg Session",
                        if (finished.isEmpty()) "—"
                        else formatDurationShort(totalMs / finished.size),
                        color,
                        Modifier.weight(1f)
                    )
                }
            }

            // ── First / Last used ──
            if (finished.isNotEmpty()) {
                item {
                    SectionCard {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            LabelValue("First used", formatDate(finished.minOf { it.startTime }))
                            LabelValue(
                                "Last used",
                                formatDate(finished.maxOf { it.startTime }),
                                alignEnd = true
                            )
                        }
                    }
                }
            }

            // ── Session list header ──
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "All Sessions",
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "${finished.size} total", color = TextTertiary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            if (finished.isEmpty()) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No completed sessions yet", color = TextTertiary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                items(finished.sortedByDescending { it.startTime }, key = { it.id }) { session ->
                    SessionRow(
                        session = session,
                        color = color,
                        onEdit = { editSession = session },
                        onDelete = { deleteTarget = session.id }
                    )
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    // ── Dialogs ──
    if (showAddDialog) {
        AddSessionDialog(
            tagName = tag.name,
            tagColor = color,
            onDismiss = { showAddDialog = false },
            onAdd = { start, end -> onAdd(start, end); showAddDialog = false }
        )
    }

    editSession?.let { session ->
        EditSessionDialog(
            session = session,
            tagColor = color,
            onDismiss = { editSession = null },
            onSave = { updated -> onUpdate(updated); editSession = null }
        )
    }

    deleteTarget?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor = CardBg,
            title = { Text("Delete session?", color = TextPrimary) },
            text = { Text("This session will be permanently removed.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { onDelete(id); deleteTarget = null }) {
                    Text("Delete", color = AccentRed, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(
                        "Cancel",
                        color = TextSecondary
                    )
                }
            }
        )
    }
}

@Composable
private fun SessionRow(
    session: TimeSession,
    color: Color,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    formatDate(session.startTime),
                    color = TextSecondary, style = MaterialTheme.typography.labelSmall
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Schedule, contentDescription = null,
                        tint = color, modifier = Modifier.size(14.dp)
                    )
                    Text(
                        "${formatTime(session.startTime)}  →  ${session.endTime?.let { formatTime(it) } ?: "—"}",
                        color = TextPrimary, style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    formatDurationShort(session.durationMs),
                    color = color,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Row {
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Filled.Edit, contentDescription = "Edit",
                        tint = TextSecondary, modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Filled.Delete, contentDescription = "Delete",
                        tint = AccentRed.copy(alpha = 0.7f), modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ── Add Session dialog ────────────────────────────────────────────────────────

@Composable
private fun AddSessionDialog(
    tagName: String,
    tagColor: Color,
    onDismiss: () -> Unit,
    onAdd: (Long, Long) -> Unit
) {
    // Simple duration-based manual entry: date + start hour/min + duration minutes
    var durationMinutes by remember { mutableStateOf("30") }
    var startHour by remember { mutableStateOf("09") }
    var startMin by remember { mutableStateOf("00") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBg,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(tagColor)
                )
                Text(
                    "Add Session – $tagName", color = TextPrimary, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    "Today's date will be used.",
                    color = TextTertiary,
                    style = MaterialTheme.typography.labelSmall
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = startHour, onValueChange = { if (it.length <= 2) startHour = it },
                        label = {
                            Text(
                                "Hour (HH)",
                                color = TextSecondary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        singleLine = true, modifier = Modifier.weight(1f),
                        colors = ttFieldColors()
                    )
                    OutlinedTextField(
                        value = startMin, onValueChange = { if (it.length <= 2) startMin = it },
                        label = {
                            Text(
                                "Min (MM)",
                                color = TextSecondary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        singleLine = true, modifier = Modifier.weight(1f),
                        colors = ttFieldColors()
                    )
                }
                OutlinedTextField(
                    value = durationMinutes,
                    onValueChange = { durationMinutes = it },
                    label = { Text("Duration (minutes)", color = TextSecondary) },
                    singleLine = true,
                    colors = ttFieldColors()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val h = startHour.toIntOrNull()?.coerceIn(0, 23) ?: 0
                val m = startMin.toIntOrNull()?.coerceIn(0, 59) ?: 0
                val dur = (durationMinutes.toLongOrNull() ?: 30L) * 60_000L
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, h); set(
                    Calendar.MINUTE,
                    m
                ); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                val start = cal.timeInMillis
                onAdd(start, start + dur)
            }) {
                Text("Add", color = Primary, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "Cancel",
                    color = TextSecondary
                )
            }
        }
    )
}

// ── Edit Session dialog ───────────────────────────────────────────────────────

@Composable
private fun EditSessionDialog(
    session: TimeSession,
    tagColor: Color,
    onDismiss: () -> Unit,
    onSave: (TimeSession) -> Unit
) {
    var durationMinutes by remember {
        mutableStateOf((session.durationMs / 60_000L).toString())
    }
    var startHour by remember {
        mutableStateOf(formatTime(session.startTime).substringBefore(":"))
    }
    var startMin by remember {
        val cal = Calendar.getInstance().apply { timeInMillis = session.startTime }
        mutableStateOf("%02d".format(cal.get(Calendar.MINUTE)))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBg,
        title = { Text("Edit Session", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    "Date: ${formatDate(session.startTime)}", color = TextTertiary,
                    style = MaterialTheme.typography.labelSmall
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = startHour, onValueChange = { if (it.length <= 2) startHour = it },
                        label = {
                            Text(
                                "Hour (HH)",
                                color = TextSecondary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        singleLine = true, modifier = Modifier.weight(1f),
                        colors = ttFieldColors()
                    )
                    OutlinedTextField(
                        value = startMin, onValueChange = { if (it.length <= 2) startMin = it },
                        label = {
                            Text(
                                "Min (MM)",
                                color = TextSecondary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        singleLine = true, modifier = Modifier.weight(1f),
                        colors = ttFieldColors()
                    )
                }
                OutlinedTextField(
                    value = durationMinutes,
                    onValueChange = { durationMinutes = it },
                    label = { Text("Duration (minutes)", color = TextSecondary) },
                    singleLine = true,
                    colors = ttFieldColors()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val h = startHour.toIntOrNull()?.coerceIn(0, 23) ?: 0
                val m = startMin.toIntOrNull()?.coerceIn(0, 59) ?: 0
                val dur = (durationMinutes.toLongOrNull() ?: 30L) * 60_000L
                val cal = Calendar.getInstance().apply {
                    timeInMillis = session.startTime
                    set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                val newStart = cal.timeInMillis
                onSave(
                    session.copy(
                        startTime = newStart,
                        endTime = newStart + dur,
                        durationMs = dur
                    )
                )
            }) {
                Text("Save", color = Primary, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "Cancel",
                    color = TextSecondary
                )
            }
        }
    )
}

// ── Shared sub-composables ────────────────────────────────────────────────────

@Composable
private fun MiniStatCard(label: String, value: String, color: Color, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(
            value,
            color = color,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(label, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun LabelValue(label: String, value: String, alignEnd: Boolean = false) {
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Text(label, color = TextTertiary, style = MaterialTheme.typography.labelSmall)
        Text(
            value,
            color = TextPrimary,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun PeriodToggle(
    current: AnalysisPeriod,
    onChange: (AnalysisPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceVar)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        AnalysisPeriod.values().forEach { p ->
            val sel = p == current
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (sel) Primary else Color.Transparent)
                    .clickable { onChange(p) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (p == AnalysisPeriod.DAY) "Today" else "This Week",
                    color = if (sel) Color.White else TextSecondary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun SmallFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Primary.copy(alpha = 0.2f) else Color.Transparent)
            .border(1.dp, if (selected) Primary else Outline, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            label,
            color = if (selected) Primary else TextSecondary,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun SummaryTile(label: String, value: String, color: Color, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Text(
            value,
            color = color,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(label, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun DonutChart(stats: List<TagStats>, modifier: Modifier = Modifier) {
    val totalMs = stats.sumOf { it.totalDurationMs }.coerceAtLeast(1L)
    val colors = stats.map { parseColor(it.tag.colorHex) }
    val sweeps = stats.map { it.totalDurationMs.toFloat() / totalMs * 360f }
    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.15f
        val radius = (size.minDimension / 2f) - strokeWidth / 2f
        val topLeft = Offset((size.width - radius * 2) / 2f, (size.height - radius * 2) / 2f)
        val arcSize = Size(radius * 2, radius * 2)
        var startAngle = -90f
        sweeps.forEachIndexed { i, sweep ->
            drawArc(
                color = colors[i], startAngle = startAngle, sweepAngle = sweep - 2f,
                useCenter = false, topLeft = topLeft, size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            startAngle += sweep
        }
    }
}

@Composable
private fun EmptyState(period: AnalysisPeriod) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(200.dp), contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Filled.BarChart, null, tint = TextTertiary, modifier = Modifier.size(48.dp))
            Text(
                "No data for ${if (period == AnalysisPeriod.DAY) "today" else "this week"}",
                color = TextSecondary, style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "Start tracking time on the home screen",
                color = TextTertiary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ttFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Primary,
    unfocusedBorderColor = Outline,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = Primary
)
