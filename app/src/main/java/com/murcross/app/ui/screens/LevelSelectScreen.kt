package com.murcross.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.murcross.app.ui.theme.MxColors
import com.murcross.audio.Bgm
import com.murcross.audio.MurcrossBgm
import com.murcross.audio.MurcrossSfx
import com.murcross.domain.model.Level

@Composable
fun LevelSelectScreen(levels: List<Level>, onSelect: (Level) -> Unit) {
    val context = LocalContext.current
    val bgm = remember { MurcrossBgm(context) }
    val sfx = remember { MurcrossSfx(context) }
    var muted by remember { mutableStateOf(sfx.muted) }

    DisposableEffect(Unit) {
        bgm.start(Bgm.BgmMenu)
        onDispose {
            bgm.release()
            sfx.release()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MxColors.Bg)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Murcross", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = {
                muted = !muted
                sfx.muted = muted
                bgm.muted = muted
            }) { Text(if (muted) "🔇" else "🔊") }
        }
        Text(
            "Coloca peones anónimos y objetos. Exactamente uno comparte sala con V.",
            style = MaterialTheme.typography.bodyMedium,
            color = MxColors.InkMuted,
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(levels) { level ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(level) },
                    colors = CardDefaults.cardColors(containerColor = MxColors.Surface),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(level.title.ifBlank { level.id }, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${level.play.size}×${level.play.size} · ${level.play.pawnCount}P · ${level.play.objects.size}O",
                            style = MaterialTheme.typography.bodySmall,
                            color = MxColors.InkMuted,
                        )
                    }
                }
            }
        }
    }
}
