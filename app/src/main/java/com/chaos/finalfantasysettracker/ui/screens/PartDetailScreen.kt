package com.chaos.finalfantasysettracker.ui.screens

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AssistChip
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.chaos.finalfantasysettracker.model.CollectibleItemStatus
import com.chaos.finalfantasysettracker.model.ItemSortOption
import com.chaos.finalfantasysettracker.model.OwnershipFilter
import com.chaos.finalfantasysettracker.ui.theme.ArcaneTeal
import com.chaos.finalfantasysettracker.ui.theme.ElevatedCardColors
import com.chaos.finalfantasysettracker.ui.theme.EmberRose
import com.chaos.finalfantasysettracker.ui.theme.SoftGold
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = uiState.query,
            onValueChange = onQueryChanged,
            label = { Text("Search cards") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp)
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
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                value = uiState.sortOption.label,
                onValueChange = {},
                readOnly = true,
                label = { Text("Sort") },
                shape = RoundedCornerShape(14.dp),
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

        if (uiState.items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No cards match your filters.", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(uiState.items, key = { it.id }) { item ->
                    ItemRow(item = item, onOwnedToggle = { onOwnedToggle(item, it) })
                }
            }
        }
    }
}

@Composable
private fun ItemRow(item: CollectibleItemStatus, onOwnedToggle: (Boolean) -> Unit) {
    Log.d(TAG, "[UI_ROW] name=${item.name}, uiScryfallId=${item.scryfallId}, generatedImageUrl=${item.resolvedImageUrl()}")
    val statusColor = if (item.isOwned) ArcaneTeal else EmberRose
    Card(
        colors = ElevatedCardColors,
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ItemThumbnail(item = item)

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (item.details.isNotBlank()) {
                    Text(item.details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.ruleLabel,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = statusColor
                    )
                    item.rarity?.takeIf { it.isNotBlank() }?.let {
                        AssistChip(
                            onClick = {},
                            enabled = false,
                            label = { Text(it.uppercase(), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Checkbox(checked = item.isOwned, onCheckedChange = onOwnedToggle)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    androidx.compose.material3.Icon(
                        imageVector = if (item.isOwned) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        if (item.isOwned) "Owned" else "Missing",
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor
                    )
                }
            }
        }
    }
}

@Composable
private fun ItemThumbnail(item: CollectibleItemStatus) {
    val shape = RoundedCornerShape(8.dp)
    val context = LocalContext.current
    val imageUrl = item.resolvedImageUrl()

    if (!imageUrl.isNullOrBlank()) {
        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .crossfade(true)
            .listener(
                onStart = { Log.d(TAG, "[COIL_START] name=${item.name}, url=$imageUrl") },
                onSuccess = { _, _ -> Log.d(TAG, "[COIL_SUCCESS] name=${item.name}, url=$imageUrl") },
                onError = { _, result ->
                    Log.w(
                        TAG,
                        "[COIL_ERROR] name=${item.name}, url=$imageUrl, throwable=${result.throwable.message}"
                    )
                }
            )
            .build()

        SubcomposeAsyncImage(
            model = request,
            contentDescription = "${item.name} thumbnail",
            modifier = Modifier
                .size(width = 58.dp, height = 80.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop,
            loading = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Loading", style = MaterialTheme.typography.labelSmall, color = SoftGold)
                }
            },
            error = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.errorContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Image failed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer)
                }
            },
            success = { SubcomposeAsyncImageContent() }
        )
        return
    }

    Log.d(TAG, "[UI_NO_URL] name=${item.name}, scryfallId=${item.scryfallId}, generatedImageUrl=${item.resolvedImageUrl()}")

    Box(
        modifier = Modifier
            .size(width = 58.dp, height = 80.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Text("No image", style = MaterialTheme.typography.labelSmall)
    }
}

private const val TAG = "PartDetailScreen"
