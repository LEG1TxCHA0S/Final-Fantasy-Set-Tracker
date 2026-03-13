package com.chaos.finalfantasysettracker.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import java.text.NumberFormat
import com.chaos.finalfantasysettracker.repository.CardPricePoint
import com.chaos.finalfantasysettracker.ui.theme.ArcaneTeal
import com.chaos.finalfantasysettracker.ui.theme.ElevatedCardColors
import com.chaos.finalfantasysettracker.ui.theme.SoftGold
import com.chaos.finalfantasysettracker.viewmodel.CardDetailUiState

@Composable
fun CardDetailScreen(
    uiState: CardDetailUiState,
    onOwnedToggled: (Boolean) -> Unit
) {
    val detail = uiState.detail
    if (uiState.loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading card details…")
        }
        return
    }

    if (detail == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Card not found")
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(colors = ElevatedCardColors) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    LargeImage(detail.item.resolvedImageUrl(version = com.chaos.finalfantasysettracker.model.ScryfallImageVersion.NORMAL), detail.item.name)
                    Text(detail.item.name, style = MaterialTheme.typography.headlineSmall)
                    detail.printedName?.takeIf { it.isNotBlank() && !it.equals(detail.item.name, true) }?.let {
                        Text(it, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        "${detail.item.setCode.orEmpty()} • ${detail.setName.orEmpty()} • #${detail.item.collectorNumber.orEmpty()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        item { SectionCard(title = "Info") {
            StatRow("Rarity", detail.item.rarity)
            StatRow("Type", detail.item.typeLine)
            StatRow("Layout", detail.layout)
            StatRow("Finishes", detail.finishes.joinToString().ifBlank { null })
            StatRow("Promo Types", detail.promoTypes.joinToString().ifBlank { null })
            StatRow("Source Kind", detail.sourceKind)
            StatRow("Group", detail.group)
            StatRow("Scryfall ID", detail.item.scryfallId)
        } }

        item { SectionCard(title = "Collection") {
            StatRow("Category", detail.categoryName)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Checkbox(checked = detail.item.isOwned, onCheckedChange = onOwnedToggled)
                Text(if (detail.item.isOwned) "Owned" else "Missing", fontWeight = FontWeight.SemiBold)
            }
        } }

        item { SectionCard(title = "Pricing") {
            val p = detail.currentPrice
            Text(
                text = if (p != null) "Current Price: ${formatCurrency(p)}" else "Price unavailable",
                style = MaterialTheme.typography.titleMedium,
                color = SoftGold
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text("Price History", style = MaterialTheme.typography.titleSmall)
            if (detail.priceHistory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Price history is not available yet.")
                }
            } else {
                PriceHistoryChart(points = detail.priceHistory)
            }
        } }
    }
}

@Composable
private fun LargeImage(url: String?, name: String) {
    val context = LocalContext.current
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context).data(url).crossfade(true).build(),
        contentDescription = "$name image",
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentScale = ContentScale.Fit,
        loading = { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Loading image") } },
        error = { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No image") } },
        success = { SubcomposeAsyncImageContent() }
    )
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(colors = ElevatedCardColors) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}

@Composable
private fun StatRow(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, modifier = Modifier.padding(start = 12.dp))
    }
}

private fun formatCurrency(value: Double): String = NumberFormat.getCurrencyInstance().format(value)

@Composable
private fun PriceHistoryChart(points: List<CardPricePoint>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(10.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (points.isEmpty()) return@Canvas
            if (points.size == 1) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                drawCircle(color = ArcaneTeal, radius = 6f, center = Offset(cx, cy))
                return@Canvas
            }
            val minY = points.minOf { it.value }
            val maxY = points.maxOf { it.value }
            val range = (maxY - minY).takeIf { it > 0 } ?: 1.0
            val stepX = size.width / (points.size - 1)
            val path = Path()
            points.forEachIndexed { index, point ->
                val x = stepX * index
                val norm = ((point.value - minY) / range).toFloat()
                val y = size.height - (norm * size.height)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                drawCircle(color = ArcaneTeal, radius = 4f, center = Offset(x, y))
            }
            drawPath(path = path, color = ArcaneTeal, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f))
        }
    }
}
