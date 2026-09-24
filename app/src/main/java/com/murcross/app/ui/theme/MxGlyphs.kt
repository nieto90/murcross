package com.murcross.app.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Iris SVG tokens drawn with Compose Canvas (no PNG densities yet).
 * Refs: murcross-mvp/assets/svg/glyph-*.svg · tray-chip-*.svg · o-hatch-swatch.svg
 */

@Composable
fun GlyphP(
    modifier: Modifier = Modifier,
    fill: Color = MxColors.PersonFill,
) {
    Canvas(modifier) {
        val s = this.size.minDimension
        if (s <= 0f) return@Canvas
        val cx = this.size.width / 2f
        drawCircle(color = fill, radius = s * 0.18f, center = Offset(cx, s * 0.32f))
        val body = Path().apply {
            moveTo(cx - s * 0.32f, s * 0.88f)
            quadraticBezierTo(cx - s * 0.32f, s * 0.52f, cx, s * 0.52f)
            quadraticBezierTo(cx + s * 0.32f, s * 0.52f, cx + s * 0.32f, s * 0.88f)
            close()
        }
        drawPath(body, color = fill)
    }
}

@Composable
fun GlyphO(
    modifier: Modifier = Modifier,
    fill: Color = MxColors.ObjectFill,
    hatch: Color = MxColors.ObjectHatch.copy(alpha = 0.22f),
) {
    Canvas(modifier) {
        val s = this.size.minDimension
        if (s <= 0f) return@Canvas
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val r = s * 0.36f
        val diamond = Path().apply {
            moveTo(cx, cy - r)
            lineTo(cx + r, cy)
            lineTo(cx, cy + r)
            lineTo(cx - r, cy)
            close()
        }
        drawPath(diamond, color = fill)
        val step = 5.dp.toPx()
        val stroke = 1.dp.toPx()
        var x = cx - r - s
        while (x < cx + r + s) {
            drawLine(
                color = hatch,
                start = Offset(x, cy + r),
                end = Offset(x + 2 * r, cy - r),
                strokeWidth = stroke,
            )
            x += step
        }
    }
}

@Composable
fun GlyphV(
    modifier: Modifier = Modifier,
    badge: Color = MxColors.VictimFill,
    ink: Color = MxColors.Surface,
) {
    Canvas(modifier) {
        val s = this.size.minDimension
        if (s <= 0f) return@Canvas
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        drawCircle(color = badge, radius = s * 0.45f, center = Offset(cx, cy))
        val w = s * 0.12f
        drawLine(ink, Offset(cx - s * 0.18f, cy - s * 0.08f), Offset(cx, cy + s * 0.18f), strokeWidth = w, cap = StrokeCap.Round)
        drawLine(ink, Offset(cx + s * 0.18f, cy - s * 0.08f), Offset(cx, cy + s * 0.18f), strokeWidth = w, cap = StrokeCap.Round)
    }
}

@Composable
fun GlyphX(
    modifier: Modifier = Modifier,
    color: Color = MxColors.Ink.copy(alpha = 0.75f),
) {
    Canvas(modifier) {
        val s = this.size.minDimension
        if (s <= 0f) return@Canvas
        val pad = s * 0.22f
        val stroke = s * 0.12f
        drawLine(color, Offset(pad, pad), Offset(s - pad, s - pad), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(s - pad, pad), Offset(pad, s - pad), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

/** Diagonal hatch overlay (Iris o-hatch @0.22). */
fun DrawScope.drawObjectHatch(
    hatch: Color = MxColors.ObjectHatch.copy(alpha = 0.22f),
    stepDp: Float = 6f,
) {
    val step = stepDp * density
    val stroke = density
    var x = -size.height
    while (x < size.width + size.height) {
        drawLine(
            color = hatch,
            start = Offset(x, size.height),
            end = Offset(x + size.height, 0f),
            strokeWidth = stroke,
        )
        x += step
    }
}

/** Cheap a11y room patterns (dots / dashes / grid / stripes). */
fun DrawScope.drawRoomPattern(
    roomId: Int,
    ink: Color = MxColors.Ink.copy(alpha = 0.12f),
) {
    val step = 12.dp.toPx()
    when (roomId % 4) {
        0 -> { // dots (RoomA)
            var y = step / 2
            while (y < size.height) {
                var x = step / 2
                while (x < size.width) {
                    drawCircle(ink, radius = 1.2.dp.toPx(), center = Offset(x, y))
                    x += step
                }
                y += step
            }
        }
        1 -> { // horizontal dashes (RoomB)
            var y = step / 2
            while (y < size.height) {
                var x = 4.dp.toPx()
                while (x < size.width) {
                    drawLine(ink, Offset(x, y), Offset(x + 6.dp.toPx(), y), strokeWidth = 1.dp.toPx())
                    x += step
                }
                y += step
            }
        }
        2 -> { // light cross grid (RoomC)
            var x = step
            while (x < size.width) {
                drawLine(ink.copy(alpha = 0.08f), Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.dp.toPx())
                x += step
            }
            var y = step
            while (y < size.height) {
                drawLine(ink.copy(alpha = 0.08f), Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                y += step
            }
        }
        else -> { // diagonal stripes (RoomD)
            val s = 10.dp.toPx()
            var x = -size.height
            while (x < size.width + size.height) {
                drawLine(ink.copy(alpha = 0.1f), Offset(x, size.height), Offset(x + size.height, 0f), strokeWidth = 1.dp.toPx())
                x += s
            }
        }
    }
}

/** Room label pill (Iris room-pill.svg) — ALL CAPS capsule. */
@Composable
fun RoomPill(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .background(MxColors.PillFill, RoundedCornerShape(50))
            .border(1.5.dp, MxColors.PillStroke, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MxColors.Ink,
            letterSpacing = 0.8.sp,
        )
    }
}
