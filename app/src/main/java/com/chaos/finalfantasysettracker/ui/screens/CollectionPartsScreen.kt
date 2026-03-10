package com.chaos.finalfantasysettracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chaos.finalfantasysettracker.model.CollectionPartProgress
import com.chaos.finalfantasysettracker.ui.components.PartProgressCard

@Composable
fun CollectionPartsScreen(parts: List<CollectionPartProgress>, onPartClick: (Long) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(parts, key = { it.id }) {
            PartProgressCard(part = it, onClick = { onPartClick(it.id) })
        }
    }
}
