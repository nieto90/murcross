package com.murcross.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val scheme = lightColorScheme(
    primary = MxColors.Ink,
    onPrimary = MxColors.Surface,
    secondary = MxColors.Select,
    background = MxColors.Bg,
    onBackground = MxColors.Ink,
    surface = MxColors.Surface,
    onSurface = MxColors.Ink,
    error = MxColors.Illegal,
)

@Composable
fun MurcrossTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}

fun roomTint(roomId: Int): Color = when (roomId % 4) {
    0 -> MxColors.RoomA
    1 -> MxColors.RoomB
    2 -> MxColors.RoomC
    else -> MxColors.RoomD
}
