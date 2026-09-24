package com.murcross.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Murcross visual v3 / art bible V3-3 — Iris.
 * Mood: misterio amigable (Mira estudio Murdoku) · papel cálido + acento púrpura.
 * Source: murcross-mvp/assets/compose/MxColors.kt · gdd/MURCROSS-visual-v3.md
 */
object MxColors {
    // Surfaces
    val Bg = Color(0xFFF4EFE6)
    val BgTint = Color(0xFFF0EBF5) // soft purple-tint app chrome (no gris web)
    val Surface = Color(0xFFFFFDF8)
    val TraySurface = Color(0xFFFFFDF8)
    val Ink = Color(0xFF1C2430)
    val InkMuted = Color(0xFF5A6573)
    val Line = Color(0xFFC9BFAE)
    val LineStrong = Color(0xFF8B7F6C)

    // Brand / feedback
    val Brand = Color(0xFF5B4A8A) // púrpura amigable (wordmark / chrome)
    val Select = Color(0xFF2F6FED) // --mx-select outline 2 dp
    val Illegal = Color(0xFFC44B3A)
    val Ok = Color(0xFF2F7A4E)
    val AccentRed = Color(0xFFC44B3A) // mínimo: V / reveal — no sangre ambient

    // Pieces
    val PersonFill = Color(0xFF3A4A5C) // P mono anónima
    val VictimFill = Color(0xFF5C4A5A)
    val ObjectFill = Color(0xFF6B5A45)
    val ObjectHatch = Color(0xFF1C2430) // @0.22 over ObjectFill

    // Edge chips
    val EdgeP = Color(0xFFD6E4F7)
    val EdgeO = Color(0xFFF3DCC4)

    // Rooms (pastel + pattern in draw)
    val RoomA = Color(0xFFF3D9B0)
    val RoomB = Color(0xFFC8D9C4)
    val RoomC = Color(0xFFC5D4E8)
    val RoomD = Color(0xFFE4CFD8)

    // Pill label
    val PillFill = Color(0xFFFFFFFF)
    val PillStroke = Color(0xFF8B7F6C)
}

val RoomColors = listOf(MxColors.RoomA, MxColors.RoomB, MxColors.RoomC, MxColors.RoomD)

/** AC-T5 / shape-radius: chips 12–16 dp. */
val MxChipRadius = 14.dp
val MxCardRadius = 12.dp

private val LightColors = lightColorScheme(
    primary = MxColors.Brand,
    secondary = MxColors.Select,
    tertiary = MxColors.Ok,
    background = MxColors.Bg,
    surface = MxColors.Surface,
    onPrimary = MxColors.Surface,
    onSecondary = MxColors.Surface,
    onBackground = MxColors.Ink,
    onSurface = MxColors.Ink,
    error = MxColors.Illegal,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9FA8DA),
    secondary = Color(0xFF90CAF9),
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
