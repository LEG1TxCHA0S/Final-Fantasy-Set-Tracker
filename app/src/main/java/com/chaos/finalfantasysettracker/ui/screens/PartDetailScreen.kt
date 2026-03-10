package com.chaos.finalfantasysettracker.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
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
    Log.d(TAG, "[UI_ROW] name=${item.name}, uiScryfallId=${item.scryfallId}, generatedImageUrl=${item.resolvedImageUrl()}")
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
                .size(width = 52.dp, height = 72.dp)
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
                    Text("Loading", style = MaterialTheme.typography.labelSmall)
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
            .size(width = 52.dp, height = 72.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Text("No image", style = MaterialTheme.typography.labelSmall)
    }
}


private const val TAG = "PartDetailScreen"
