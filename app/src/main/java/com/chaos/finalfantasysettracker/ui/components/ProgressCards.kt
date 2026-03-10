package com.chaos.finalfantasysettracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chaos.finalfantasysettracker.model.CollectionPartProgress

@Composable
fun PartProgressCard(
    part: CollectionPartProgress,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = part.name, style = MaterialTheme.typography.titleMedium)
            Text(text = part.description, style = MaterialTheme.typography.bodySmall)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${part.ownedCount} / ${part.totalCount}")
                Text("${(part.completionPercentage * 100).toInt()}%")
            }
            LinearProgressIndicator(progress = { part.completionPercentage }, modifier = Modifier.fillMaxWidth())
        }
    }
}
