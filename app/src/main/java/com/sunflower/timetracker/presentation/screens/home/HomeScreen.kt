package com.sunflower.timetracker.presentation.screens.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sunflower.timetracker.domain.model.TAG_COLORS
import com.sunflower.timetracker.domain.model.Tag
import com.sunflower.timetracker.presentation.components.BigTimerDisplay
import com.sunflower.timetracker.presentation.components.PulsingCircle
import com.sunflower.timetracker.presentation.components.SectionCard
import com.sunflower.timetracker.presentation.components.TagChip
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
import com.sunflower.timetracker.presentation.viewmodel.HomeViewModel
import com.sunflower.timetracker.util.parseColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: HomeViewModel) {
    val tags by vm.tags.collectAsState()
    val active by vm.activeState.collectAsState()
    val durationMs by vm.durationMs.collectAsState()

    var selectedTagId by remember { mutableStateOf<Long?>(null) }
    var showAddTag by remember { mutableStateOf(false) }

    val notifLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    LaunchedEffect(active.session) {
        if (active.session != null) selectedTagId = active.session!!.tagId
    }

    val hasSession = active.session != null
    val isPaused = active.session?.isPaused == true

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
                "Time Tracker", style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold, color = TextPrimary
            )
            IconButton(onClick = { showAddTag = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Tag", tint = Primary)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Timer card ──
            TimerCard(
                isRunning = hasSession,
                isPaused = isPaused,
                elapsedMs = durationMs,
                activeTagName = active.tag?.name,
                activeTagColor = active.tag?.colorHex,
                onPause = { vm.pauseTimer() },
                onResume = { vm.resumeTimer() },
                onStop = { vm.stopTimer() }
            )

            // ── Tag picker (only when no session) ──
            if (!hasSession) {
                TagPickerSection(
                    tags = tags,
                    selectedId = selectedTagId,
                    onSelect = { selectedTagId = it }
                )

                Button(
                    onClick = { selectedTagId?.let { vm.startTimer(it) } },
                    enabled = selectedTagId != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Primary,
                        disabledContainerColor = SurfaceVar
                    )
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Start Timer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }

    if (showAddTag) {
        AddTagDialog(
            onDismiss = { showAddTag = false },
            onAdd = { name, color -> vm.addTag(name, color); showAddTag = false }
        )
    }
}

@Composable
private fun TimerCard(
    isRunning: Boolean,
    isPaused: Boolean,
    elapsedMs: Long,
    activeTagName: String?,
    activeTagColor: String?,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    SectionCard {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isRunning && activeTagName != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!isPaused) {
                        PulsingCircle(color = activeTagColor?.let { parseColor(it) } ?: AccentGreen)
                    } else {
                        Box(
                            Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background((activeTagColor?.let { parseColor(it) }
                                    ?: AccentGreen).copy(alpha = 0.5f))
                        )
                    }
                    Column {
                        Text(
                            activeTagName,
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (isPaused) {
                            Text(
                                "Paused",
                                color = TextTertiary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            } else {
                Text(
                    "No timer running",
                    color = TextTertiary,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Stop button (always visible when session exists)
            AnimatedVisibility(isRunning) {
                FilledTonalIconButton(
                    onClick = onStop,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = AccentRed.copy(alpha = 0.15f),
                        contentColor = AccentRed
                    )
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = "Stop")
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Big timer display
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            BigTimerDisplay(elapsedMs)
        }

        Spacer(Modifier.height(20.dp))

        // Pause / Resume button — only when session active
        AnimatedVisibility(
            visible = isRunning,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                if (isPaused) {
                    // Resume button
                    Button(
                        onClick = onResume,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                        modifier = Modifier
                            .height(48.dp)
                            .fillMaxWidth(0.6f)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Resume", fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    // Pause button
                    OutlinedButton(
                        onClick = onPause,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Primary.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .height(48.dp)
                            .fillMaxWidth(0.6f)
                    ) {
                        Icon(Icons.Filled.Pause, contentDescription = null, tint = Primary)
                        Spacer(Modifier.width(6.dp))
                        Text("Pause", color = Primary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        if (!isRunning) {
            Text(
                "Select a tag below to start",
                color = TextTertiary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TagPickerSection(tags: List<Tag>, selectedId: Long?, onSelect: (Long) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Select Tag", color = TextSecondary,
            style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold
        )
        if (tags.isEmpty()) {
            Text(
                "No tags yet. Tap + to create one.",
                color = TextTertiary,
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tags, key = { it.id }) { tag ->
                    TagChip(
                        tag = tag,
                        selected = tag.id == selectedId,
                        onClick = { onSelect(tag.id) })
                }
            }
        }
    }
}

@Composable
fun AddTagDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var pickedColor by remember { mutableStateOf(TAG_COLORS.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBg,
        title = { Text("New Tag", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Tag name", color = TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = Outline,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = Primary
                    )
                )
                Text("Color", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(TAG_COLORS) { hex ->
                        val c = parseColor(hex)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(
                                    if (hex == pickedColor) 2.dp else 0.dp,
                                    Color.White,
                                    CircleShape
                                )
                                .clickable { pickedColor = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onAdd(name, pickedColor) },
                enabled = name.isNotBlank()
            ) {
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
