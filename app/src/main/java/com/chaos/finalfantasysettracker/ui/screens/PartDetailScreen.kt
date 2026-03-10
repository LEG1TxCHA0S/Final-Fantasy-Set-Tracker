package com.chaos.finalfantasysettracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.chaos.finalfantasysettracker.model.CollectibleItemStatus
import com.chaos.finalfantasysettracker.model.ItemSortOption
import com.chaos.finalfantasysettracker.model.OwnershipFilter
import com.chaos.finalfantasysettracker.viewmodel.PartDetailUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartDetailScreen(
    uiState: PartDetailUiState,
    onQueryChanged: (String) -> Unit,
    onSortChanged: (ItemSortOption) -> Unit,
    onOwnershipFilterChanged: (OwnershipFilter) -> Unit,
    onOwnedToggle: (CollectibleItemStatus, Boolean) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = uiState.query,
            onValueChange = onQueryChanged,
            label = { Text("Search") },
            singleLine = true
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OwnershipFilter.entries.forEach { filter ->
                val label = when (filter) {
                    OwnershipFilter.ALL -> "All (${uiState.allCount})"
                    OwnershipFilter.OWNED -> "Owned (${uiState.ownedCount})"
                    OwnershipFilter.MISSING -> "Missing (${uiState.missingCount})"
                }
                FilterChip(
                    selected = uiState.ownershipFilter == filter,
                    onClick = { onOwnershipFilterChanged(filter) },
                    label = { Text(label) }
                )
            }
        }

        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                value = uiState.sortOption.label,
                onValueChange = {},
                readOnly = true,
                label = { Text("Sort") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                ItemSortOption.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            onSortChanged(option)
                            expanded = false
                        }
                    )
                }
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(uiState.items, key = { it.id }) { item ->
                ItemRow(item = item, onOwnedToggle = { onOwnedToggle(item, it) })
            }
        }
    }
}

@Composable
private fun ItemRow(item: CollectibleItemStatus, onOwnedToggle: (Boolean) -> Unit) {
    Card {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ItemThumbnail(item = item)

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (item.details.isNotBlank()) {
                    Text(item.details, style = MaterialTheme.typography.bodySmall)
                }
                Text(item.ruleLabel, style = MaterialTheme.typography.bodySmall)
            }

            Checkbox(checked = item.isOwned, onCheckedChange = onOwnedToggle)
        }
    }
}

@Composable
private fun ItemThumbnail(item: CollectibleItemStatus) {
    val shape = RoundedCornerShape(6.dp)
    val imageUrl = when {
        item.itemType == com.chaos.finalfantasysettracker.model.ItemType.PRECON -> item.imageUrlNormal ?: item.imageUrlSmall ?: item.imageUrlLarge
        else -> item.imageUrlSmall ?: item.imageUrlNormal ?: item.imageUrlLarge
    }
    val placeholderPainter = ColorPainter(MaterialTheme.colorScheme.surfaceVariant)

    if (imageUrl != null) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "${item.name} thumbnail",
            placeholder = placeholderPainter,
            error = placeholderPainter,
            fallback = placeholderPainter,
            modifier = Modifier
                .size(width = 52.dp, height = 72.dp)
                .clip(shape),
            contentScale = ContentScale.Crop
        )
        return
    }

    val preconIcon = when (item.checklistId) {
        "precon-revival-trance" -> Icons.Default.AutoAwesome
        "precon-limit-break" -> Icons.Default.Bolt
        "precon-counter-blitz" -> Icons.Default.Shield
        "precon-scions-spellcraft" -> Icons.Default.MenuBook
        else -> Icons.Default.Style
    }

    Box(
        modifier = Modifier
            .size(width = 52.dp, height = 72.dp)
            .clip(shape),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Icon(
            imageVector = if (item.itemType == com.chaos.finalfantasysettracker.model.ItemType.PRECON) preconIcon else Icons.Default.Image,
            contentDescription = if (item.itemType == com.chaos.finalfantasysettracker.model.ItemType.PRECON) "Precon image placeholder" else "No image",
            tint = MaterialTheme.colorScheme.outline
        )
    }
}
