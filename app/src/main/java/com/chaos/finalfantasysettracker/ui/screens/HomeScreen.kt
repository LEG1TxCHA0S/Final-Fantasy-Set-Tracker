package com.chaos.finalfantasysettracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chaos.finalfantasysettracker.repository.PriceDropItem
import com.chaos.finalfantasysettracker.ui.theme.AbyssBlue
import com.chaos.finalfantasysettracker.ui.theme.CrystalBlue
import com.chaos.finalfantasysettracker.ui.theme.ElevatedCardColors
import com.chaos.finalfantasysettracker.ui.theme.MidnightBlue
import com.chaos.finalfantasysettracker.ui.theme.SoftGold
import com.chaos.finalfantasysettracker.viewmodel.HomeUiState
import java.text.NumberFormat

@Composable
@Suppress("UNUSED_PARAMETER")
fun HomeScreen(uiState: HomeUiState, onPartClick: (Long) -> Unit) {
    val dashboard = uiState.dashboard

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
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
                Text("Collection Dashboard", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "${dashboard.totalOwned} owned of ${dashboard.totalCards}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    "${(dashboard.completionPercent * 100).toInt()}% complete",
                    style = MaterialTheme.typography.titleLarge,
                    color = SoftGold
                )
            }
        }

        item {
            DashboardOverviewCard(
                totalMissing = dashboard.totalMissing,
                totalValue = dashboard.totalValue,
                totalPriceToComplete = dashboard.totalPriceToComplete
            )
        }

        item {
            SectionTitle("Rarity Breakdown")
        }
        items(dashboard.rarityStats, key = { it.label }) { stat ->
            Card(colors = ElevatedCardColors) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stat.label, fontWeight = FontWeight.SemiBold)
                    Text("${stat.owned} / ${stat.total}")
                }
            }
        }

        item { SectionTitle("Collection Metrics") }
        items(dashboard.extraStats, key = { it.label }) { stat ->
            Card(colors = ElevatedCardColors) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stat.label, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stat.value, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }

        item { SectionTitle("Biggest Price Drops") }
        if (dashboard.biggestPriceDrops.isEmpty()) {
            item {
                Card(colors = ElevatedCardColors) {
                    Text(
                        dashboard.priceDropMessage,
                        modifier = Modifier.padding(14.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(dashboard.biggestPriceDrops, key = { it.cardName }) { drop ->
                PriceDropRow(drop)
            }
        }
    }
}

@Composable
private fun DashboardOverviewCard(totalMissing: Int, totalValue: Double?, totalPriceToComplete: Double?) {
    Card(colors = ElevatedCardColors) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Overview", style = MaterialTheme.typography.titleMedium)
            Text("Missing cards: $totalMissing")
            Text(
                "Collection value: ${totalValue?.let { NumberFormat.getCurrencyInstance().format(it) } ?: "Unavailable"}",
                color = SoftGold
            )
            Text(
                "Price to complete: ${totalPriceToComplete?.let { NumberFormat.getCurrencyInstance().format(it) } ?: "Unavailable"}",
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp, start = 2.dp)
    )
}

@Composable
private fun PriceDropRow(drop: PriceDropItem) {
    Card(colors = ElevatedCardColors) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(drop.cardName, fontWeight = FontWeight.SemiBold)
            Text(
                "${NumberFormat.getCurrencyInstance().format(drop.previousPrice)} → ${NumberFormat.getCurrencyInstance().format(drop.currentPrice)}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "Drop: ${NumberFormat.getCurrencyInstance().format(drop.dropAmount)} (${String.format("%.1f", drop.dropPercent)}%)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
