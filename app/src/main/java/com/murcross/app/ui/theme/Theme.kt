package com.murcross.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Iris art-bible tokens (v1 + V3-3). */
object MxColors {
    val Bg = Color(0xFFF4EFE6)
    val Surface = Color(0xFFFFFDF8)
    val Ink = Color(0xFF1C2430)
    val InkMuted = Color(0xFF5A6573)
    val Line = Color(0xFFC9BFAE)
    val LineStrong = Color(0xFF8B7F6C)
    val Select = Color(0xFF2F6FED)
    val Illegal = Color(0xFFC44B3A)
    val Ok = Color(0xFF2F7A4E)

    val PersonFill = Color(0xFF3A4A5C)
    val VictimFill = Color(0xFF5C4A5A)
    val ObjectFill = Color(0xFF6B5A45)
    val ObjectHatch = Color(0xFF1C2430) // use @ ~0.22 alpha

    val EdgeP = Color(0xFFD6E4F7)
    val EdgeO = Color(0xFFF3DCC4)

    val RoomA = Color(0xFFF3D9B0)
    val RoomB = Color(0xFFC8D9C4)
    val RoomC = Color(0xFFC5D4E8)
    val RoomD = Color(0xFFE4CFD8)
}

val RoomColors = listOf(MxColors.RoomA, MxColors.RoomB, MxColors.RoomC, MxColors.RoomD)

private val LightColors = lightColorScheme(
    primary = MxColors.Select,
    secondary = MxColors.Illegal,
    tertiary = MxColors.Ok,
    background = MxColors.Bg,
    surface = MxColors.Surface,
    onBackground = MxColors.Ink,
    onSurface = MxColors.Ink,
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
