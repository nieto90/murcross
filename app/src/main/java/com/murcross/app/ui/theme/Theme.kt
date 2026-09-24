package com.murcross.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RoomA = Color(0xFFE8F0FE)
private val RoomB = Color(0xFFFFF3E0)
private val RoomC = Color(0xFFE8F5E9)
private val RoomD = Color(0xFFF3E5F5)

val RoomColors = listOf(RoomA, RoomB, RoomC, RoomD)

private val LightColors = lightColorScheme(
    primary = Color(0xFF1A237E),
    secondary = Color(0xFFC62828),
    tertiary = Color(0xFF2E7D32),
    background = Color(0xFFF5F5F7),
    surface = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9FA8DA),
    secondary = Color(0xFFEF9A9A),
    tertiary = Color(0xFFA5D6A7),
)

@Composable
fun MurcrossTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        content = content,
    )
}
