package com.chaos.finalfantasysettracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chaos.finalfantasysettracker.ui.components.PartProgressCard
import com.chaos.finalfantasysettracker.viewmodel.HomeUiState

@Composable
fun HomeScreen(uiState: HomeUiState, onPartClick: (Long) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Collection Overview", style = MaterialTheme.typography.headlineSmall)
                    Text("${uiState.overview.ownedCount} owned of ${uiState.overview.totalCount} needed")
                    Text("${(uiState.overview.completionPercentage * 100).toInt()}% complete", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        item { Text("By collection part", style = MaterialTheme.typography.titleMedium) }
        items(uiState.parts, key = { it.id }) {
            PartProgressCard(part = it, onClick = { onPartClick(it.id) })
        }
    }
}
