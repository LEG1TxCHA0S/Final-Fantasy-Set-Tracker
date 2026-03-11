package com.chaos.finalfantasysettracker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chaos.finalfantasysettracker.model.CollectionPartProgress
import com.chaos.finalfantasysettracker.ui.theme.ArcaneTeal
import com.chaos.finalfantasysettracker.ui.theme.ElevatedCardColors
import com.chaos.finalfantasysettracker.ui.theme.SoftGold

@Composable
fun PartProgressCard(
    part: CollectionPartProgress,
    onClick: (() -> Unit)? = null
) {
    val completionPercent = (part.completionPercentage * 100).toInt()
    Card(
        colors = ElevatedCardColors,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = SoftGold
                )
                Text(text = part.name, style = MaterialTheme.typography.titleMedium)
            }
            if (part.description.isNotBlank()) {
                Text(text = part.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${part.ownedCount} owned / ${part.totalCount}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ArcaneTeal)
                    Text("$completionPercent%", style = MaterialTheme.typography.labelLarge)
                }
            }
            LinearProgressIndicator(
                progress = { part.completionPercentage },
                modifier = Modifier.fillMaxWidth(),
                color = ArcaneTeal,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
            )
        }
    }
}
