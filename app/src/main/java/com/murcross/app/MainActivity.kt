package com.murcross.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.murcross.app.ui.screens.GameScreen
import com.murcross.app.ui.screens.LevelSelectScreen
import com.murcross.app.ui.theme.MurcrossTheme
import com.murcross.data.LevelLoader
import com.murcross.domain.model.Level

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val levels = LevelLoader.listBundledIds().map { id ->
            assets.open("levels/$id.json").use { LevelLoader.fromStream(it) }
        }
        setContent {
            MurcrossTheme {
                var selected: Level? by remember { mutableStateOf(null) }
                val level = selected
                if (level == null) {
                    LevelSelectScreen(levels = levels, onSelect = { selected = it })
                } else {
                    GameScreen(level = level, onBack = { selected = null })
                }
            }
        }
    }
}
