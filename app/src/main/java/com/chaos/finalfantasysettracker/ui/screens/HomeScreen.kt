package com.chaos.finalfantasysettracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.chaos.finalfantasysettracker.ui.components.PartProgressCard
import com.chaos.finalfantasysettracker.ui.theme.AbyssBlue
import com.chaos.finalfantasysettracker.ui.theme.CrystalBlue
import com.chaos.finalfantasysettracker.ui.theme.MidnightBlue
import com.chaos.finalfantasysettracker.ui.theme.SoftGold
import com.chaos.finalfantasysettracker.viewmodel.HomeUiState

@Composable
fun HomeScreen(uiState: HomeUiState, onPartClick: (Long) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            val completionPercent = (uiState.overview.completionPercentage * 100).toInt()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(CrystalBlue.copy(alpha = 0.25f), AbyssBlue.copy(alpha = 0.75f), MidnightBlue)
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Collection Overview", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "${uiState.overview.ownedCount} owned of ${uiState.overview.totalCount} needed",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    "$completionPercent% complete",
                    style = MaterialTheme.typography.titleLarge,
                    color = SoftGold
                )
            }
        }
        item {
            Text(
                "By Collection Part",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }
        items(uiState.parts, key = { it.id }) {
            PartProgressCard(part = it, onClick = { onPartClick(it.id) })
        }
    }
}
