package com.sunflower.timetracker.presentation.screens.tags

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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sunflower.timetracker.domain.model.TAG_COLORS
import com.sunflower.timetracker.domain.model.Tag
import com.sunflower.timetracker.presentation.components.SectionCard
import com.sunflower.timetracker.presentation.screens.home.AddTagDialog
import com.sunflower.timetracker.presentation.theme.AccentRed
import com.sunflower.timetracker.presentation.theme.Background
import com.sunflower.timetracker.presentation.theme.CardBg
import com.sunflower.timetracker.presentation.theme.Outline
import com.sunflower.timetracker.presentation.theme.Primary
import com.sunflower.timetracker.presentation.theme.TextPrimary
import com.sunflower.timetracker.presentation.theme.TextSecondary
import com.sunflower.timetracker.presentation.theme.TextTertiary
import com.sunflower.timetracker.presentation.viewmodel.HomeViewModel
import com.sunflower.timetracker.util.parseColor

@Composable
fun TagsScreen(vm: HomeViewModel) {
    val tags       by vm.tags.collectAsState()
    var showAdd    by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<Tag?>(null) }

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
                "Tags",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            FloatingActionButton(
                onClick           = { showAdd = true },
                containerColor    = Primary,
                contentColor      = Color.White,
                modifier          = Modifier.size(40.dp),
                shape             = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Tag", modifier = Modifier.size(18.dp))
            }
        }

        if (tags.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.AutoMirrored.Filled.Label, null, tint = TextTertiary, modifier = Modifier.size(56.dp))
                    Text("No tags yet", color = TextSecondary, style = MaterialTheme.typography.bodyLarge)
                    Text("Tap + to create your first tag", color = TextTertiary, style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            LazyColumn(
                modifier            = Modifier.fillMaxSize(),
                contentPadding      = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(tags, key = { it.id }) { tag ->
                    TagRow(
                        tag      = tag,
                        onEdit   = { editTarget = tag },
                        onDelete = { vm.deleteTag(tag) }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (showAdd) {
        AddTagDialog(
            onDismiss = { showAdd = false },
            onAdd     = { name, color -> vm.addTag(name, color); showAdd = false }
        )
    }

    editTarget?.let { tag ->
        EditTagDialog(
            tag       = tag,
            onDismiss = { editTarget = null },
            onSave    = { updated -> vm.updateTag(updated); editTarget = null }
        )
    }
}

@Composable
private fun TagRow(tag: Tag, onEdit: () -> Unit, onDelete: () -> Unit) {
    var confirmDelete by remember { mutableStateOf(false) }
    val color = parseColor(tag.colorHex)

    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(color.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(Modifier.size(14.dp).clip(CircleShape).background(color))
                }
                Text(tag.name, color = TextPrimary, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = AccentRed.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor   = CardBg,
            title = { Text("Delete \"${tag.name}\"?", color = TextPrimary) },
            text  = { Text("All sessions for this tag will be deleted.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { onDelete(); confirmDelete = false }) {
                    Text("Delete", color = AccentRed, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun EditTagDialog(tag: Tag, onDismiss: () -> Unit, onSave: (Tag) -> Unit) {
    var name  by remember { mutableStateOf(tag.name) }
    var color by remember { mutableStateOf(tag.colorHex) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = CardBg,
        title = { Text("Edit Tag", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = { Text("Tag name", color = TextSecondary) },
                    singleLine    = true,
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = Primary,
                        unfocusedBorderColor = Outline,
                        focusedTextColor     = TextPrimary,
                        unfocusedTextColor   = TextPrimary,
                        cursorColor          = Primary
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
                                .border(2.dp, if (hex == color) Color.White else Color.Transparent, CircleShape)
                                .clickable { color = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onSave(tag.copy(name = name.trim(), colorHex = color)) }, enabled = name.isNotBlank()) {
                Text("Save", color = Primary, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

// Need this import for LazyRow inside dialog
@Composable
private fun LazyRow(
    horizontalArrangement: Arrangement.Horizontal,
    content: LazyListScope.() -> Unit
) = androidx.compose.foundation.lazy.LazyRow(
    horizontalArrangement = horizontalArrangement,
    content = content
)