package com.murcross.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.murcross.app.ui.theme.RoomColors
import com.murcross.domain.model.Cell
import com.murcross.domain.model.Level
import com.murcross.domain.model.PieceKind
import com.murcross.domain.model.RevealOutcome
import com.murcross.domain.model.absoluteObjectCells
import com.murcross.domain.model.objectCells
import com.murcross.audio.MurcrossSfx
import com.murcross.audio.Sfx
import com.murcross.engine.GameController
import androidx.compose.ui.platform.LocalContext

@Composable
fun GameScreen(level: Level, onBack: () -> Unit) {
    val context = LocalContext.current
    val sfx = remember { MurcrossSfx(context) }
    val game = remember(level.id) { GameController(level) }
    var tick by remember { mutableIntStateOf(0) }
    fun refresh() { tick++ }
    // Force recomposition dependency
    @Suppress("UNUSED_EXPRESSION")
    tick

    val canResolve = game.canResolve()
    var showReveal by remember { mutableStateOf<RevealOutcome?>(null) }
    val illegal = game.lastIllegalReason

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("←") }
            Column(Modifier.weight(1f)) {
                Text(level.title.ifBlank { level.id }, style = MaterialTheme.typography.titleLarge)
                Text(
                    "Peones anónimos · V fija · Resolver al legal",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        BoardWithEdges(game = game, onCell = {
            val beforeX = game.modeX
            game.tapCell(it.r, it.c)
            when {
                game.lastIllegalReason != null -> sfx.play(Sfx.SfxIllegal)
                beforeX -> sfx.play(Sfx.SfxMarkX)
                else -> sfx.play(Sfx.SfxPlaceOk)
            }
            refresh()
        })

        if (illegal != null) {
            Text("⚠ $illegal", color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp)
        }

        TrayBar(
            game = game,
            onSelect = { id ->
                sfx.play(Sfx.SfxTapUi)
                game.selectPiece(id)
                refresh()
            },
            onRotate = { id ->
                game.rotateObject(id)
                sfx.play(Sfx.SfxRotateO)
                refresh()
            },
            onReturn = { id ->
                game.returnToTray(id)
                refresh()
            },
        )

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = game.modeX,
                onClick = {
                    if (game.modeX) game.clearSelection() else game.setModeX()
                    refresh()
                },
                label = { Text("X") },
            )
            OutlinedButton(onClick = { game.undo(); sfx.play(Sfx.SfxUndo); refresh() }) { Text("Undo") }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    val outcome = game.resolve()
                    refresh()
                    if (outcome != null) {
                        sfx.play(Sfx.SfxReveal)
                        showReveal = outcome
                    } else {
                        sfx.play(Sfx.SfxIllegal)
                    }
                },
                enabled = canResolve,
            ) { Text("Resolver") }
        }
    }

    showReveal?.let { outcome ->
        AlertDialog(
            onDismissRequest = { showReveal = null },
            title = { Text("¡Resuelto!") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Culpable: ${outcome.culpritName}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Text("Sospechosos:")
                    outcome.suspectNamesByCell.entries.sortedBy { it.key.r * 10 + it.key.c }.forEach {
                        Text(" · ${it.value} @ (${it.key.r + 1},${it.key.c + 1})")
                    }
                    Text("Objetos:")
                    outcome.objectNames.forEach { (id, name) ->
                        Text(" · $name ($id)")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showReveal = null; onBack() }) { Text("Listo") }
            },
            dismissButton = {
                TextButton(onClick = { showReveal = null }) { Text("Seguir") }
            },
        )
    }
}

@Composable
private fun BoardWithEdges(game: GameController, onCell: (Cell) -> Unit) {
    val play = game.play
    val n = play.size
    val cellSize = 44.dp

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Top edge (column clues)
        Row(verticalAlignment = Alignment.Bottom) {
            Spacer(Modifier.width(56.dp)) // left edge gutter
            for (c in 0 until n) {
                Column(
                    Modifier.width(cellSize),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val line = play.edge.cols[c]
                    PoBadge(line.people, line.objects)
                    Text(
                        run {
                        val visible = line.segments.map { it.count }.filter { it > 0 }.ifEmpty { listOf(0) }
                        visible.joinToString("|")
                    },
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        for (r in 0 until n) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Left edge
                Column(
                    Modifier.width(56.dp),
                    horizontalAlignment = Alignment.End,
                ) {
                    val line = play.edge.rows[r]
                    PoBadge(line.people, line.objects)
                    Text(
                        run {
                        val visible = line.segments.map { it.count }.filter { it > 0 }.ifEmpty { listOf(0) }
                        visible.joinToString("|")
                    },
                        fontSize = 11.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                }
                for (c in 0 until n) {
                    BoardCell(
                        game = game,
                        r = r,
                        c = c,
                        modifier = Modifier.size(cellSize),
                        onClick = { onCell(Cell(r, c)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PoBadge(people: Int, objects: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        if (people > 0) {
            Text(
                "P$people",
                fontSize = 9.sp,
                color = Color(0xFF1B5E20),
                modifier = Modifier
                    .background(Color(0xFFC8E6C9), RoundedCornerShape(3.dp))
                    .padding(horizontal = 3.dp),
            )
        }
        if (objects > 0) {
            Text(
                "O$objects",
                fontSize = 9.sp,
                color = Color(0xFF4A148C),
                modifier = Modifier
                    .background(Color(0xFFE1BEE7), RoundedCornerShape(3.dp))
                    .padding(horizontal = 3.dp),
            )
        }
    }
}

@Composable
private fun BoardCell(
    game: GameController,
    r: Int,
    c: Int,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val play = game.play
    val roomId = play.roomOf(r, c)
    val highlightRooms = game.selectedId != null // Lucas: highlight salas al seleccionar (no badge must_room)
    val bg = RoomColors[roomId % RoomColors.size].let { if (highlightRooms) it else it.copy(alpha = 0.85f) }
    val selected = game.selectedId != null && game.pieceAt(r, c) == game.selectedId
    val label = when {
        game.isVictim(r, c) -> "V"
        game.hasX(r, c) -> "X"
        else -> {
            val pid = game.pieceAt(r, c)
            when {
                pid == null -> ""
                pid.startsWith("pawn_") -> "P"
                else -> pid.take(1).uppercase()
            }
        }
    }
    val borderColor = when {
        selected -> MaterialTheme.colorScheme.primary
        game.isVictim(r, c) -> MaterialTheme.colorScheme.secondary
        else -> Color(0xFF90A4AE)
    }
    Box(
        modifier
            .padding(1.dp)
            .background(bg, RoundedCornerShape(4.dp))
            .border(if (selected) 2.dp else 1.dp, borderColor, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = when (label) {
                "V" -> MaterialTheme.colorScheme.secondary
                "X" -> Color.Gray
                "P" -> Color(0xFF0D47A1)
                else -> Color(0xFF4A148C)
            },
        )
    }
}

@Composable
private fun TrayBar(
    game: GameController,
    onSelect: (String) -> Unit,
    onRotate: (String) -> Unit,
    onReturn: (String) -> Unit,
) {
    val play = game.play
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Bandeja", style = MaterialTheme.typography.titleSmall)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (i in 0 until play.pawnCount) {
                val id = play.pawnId(i)
                val placed = game.state.placementOf(id) != null
                TrayChip(
                    label = "P",
                    selected = game.selectedId == id,
                    placed = placed,
                    onClick = {
                        if (placed) onReturn(id) else onSelect(id)
                    },
                )
            }
            for (obj in play.objects) {
                val placed = game.state.placementOf(obj.id) != null
                val rot = game.getObjectRot(obj.id)
                val shape = objectCells(obj, rot)
                TrayChip(
                    label = obj.id.take(3).replaceFirstChar { it.uppercase() },
                    selected = game.selectedId == obj.id,
                    placed = placed,
                    subtitle = "r$rot · ${shape.size}",
                    onClick = {
                        when {
                            game.selectedId == obj.id && !placed -> onRotate(obj.id)
                            placed -> onReturn(obj.id)
                            else -> onSelect(obj.id)
                        }
                    },
                )
            }
        }
        Text(
            "Tap pieza → celda. Tap de nuevo en O = rotar 90° CW. Tap X = marcar vacío. Peón/O colocados: tap bandeja para devolver.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
        )
    }
}

@Composable
private fun TrayChip(
    label: String,
    selected: Boolean,
    placed: Boolean,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    val bg = when {
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        placed -> Color(0xFFE0E0E0)
        else -> Color(0xFFFFF8E1)
    }
    Column(
        Modifier
            .width(64.dp)
            .height(56.dp)
            .background(bg, RoundedCornerShape(8.dp))
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) MaterialTheme.colorScheme.primary else Color(0xFFBDBDBD),
                RoundedCornerShape(8.dp),
            )
            .clickable {
                onClick()
            }
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(if (placed) "✓$label" else label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        if (subtitle != null) Text(subtitle, fontSize = 9.sp, color = Color.Gray)
    }
}
