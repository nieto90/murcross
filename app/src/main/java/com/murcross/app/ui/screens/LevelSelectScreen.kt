package com.murcross.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.murcross.domain.model.Level

@Composable
fun LevelSelectScreen(levels: List<Level>, onSelect: (Level) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Murcross", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Coloca peones anónimos y objetos. Exactamente uno comparte sala con V.",
            style = MaterialTheme.typography.bodyMedium,
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(levels) { level ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(level) },
                    colors = CardDefaults.cardColors(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(level.title.ifBlank { level.id }, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${level.play.size}×${level.play.size} · ${level.play.pawnCount}P · ${level.play.objects.size}O",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}
