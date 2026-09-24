package com.murcross.app.ui.screens

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.murcross.app.ui.theme.MxColors
import com.murcross.app.ui.theme.RoomColors
import com.murcross.audio.Bgm
import com.murcross.audio.MurcrossBgm
import com.murcross.audio.MurcrossSfx
import com.murcross.audio.Sfx
import com.murcross.domain.model.Cell
import com.murcross.domain.model.Level
import com.murcross.domain.model.RevealOutcome
import com.murcross.domain.model.objectCells
import com.murcross.engine.GameController

@Composable
fun GameScreen(level: Level, onBack: () -> Unit) {
    val context = LocalContext.current
    val sfx = remember { MurcrossSfx(context) }
    val bgm = remember { MurcrossBgm(context) }
    val game = remember(level.id) { GameController(level) }
    var tick by remember { mutableIntStateOf(0) }
    fun refresh() { tick++ }
    @Suppress("UNUSED_EXPRESSION")
    tick

    var muted by remember { mutableStateOf(sfx.muted) }
    var showReveal by remember { mutableStateOf<RevealOutcome?>(null) }
    val prefs = remember { context.getSharedPreferences("murcross", Context.MODE_PRIVATE) }
    var showRemoveCoach by remember {
        mutableStateOf(
            level.id.contains("n1", ignoreCase = true) &&
                !prefs.getBoolean("ftue_remove_longpress_done", false),
        )
    }
    val illegal = game.lastIllegalReason
    val canResolve = game.canResolve()
    val validAnchors = remember(tick, game.selectedId, game.modeX) { game.validAnchorCells() }

    DisposableEffect(Unit) {
        bgm.start(Bgm.BgmGameplayCalm)
        onDispose {
            sfx.release()
            bgm.release()
        }
    }

    fun toggleMute() {
        muted = !muted
        sfx.muted = muted
        bgm.muted = muted
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MxColors.Bg)
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
                    color = MxColors.InkMuted,
                )
            }
            TextButton(onClick = { toggleMute(); refresh() }) {
                Text(if (muted) "🔇" else "🔊")
            }
        }

        if (showRemoveCoach) {
            Surface(
                color = MxColors.Surface,
                shape = RoundedCornerShape(8.dp),
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Mantén pulsado para devolver a la bandeja",
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    TextButton(onClick = {
                        prefs.edit().putBoolean("ftue_remove_longpress_done", true).apply()
                        showRemoveCoach = false
                        sfx.play(Sfx.SfxCoachDismiss)
                    }) { Text("OK") }
                }
            }
        }

        BoardWithEdges(
            game = game,
            validAnchors = validAnchors,
            onCell = { cell ->
                val beforeX = game.modeX
                val beforeSel = game.selectedId
                game.tapCell(cell.r, cell.c)
                when {
                    game.lastIllegalReason != null -> sfx.play(Sfx.SfxIllegal)
                    beforeX -> sfx.play(Sfx.SfxMarkX)
                    beforeSel != null && game.state.placementOf(beforeSel) != null ->
                        sfx.play(Sfx.SfxPlaceOk)
                    else -> sfx.play(Sfx.SfxTapUi)
                }
                refresh()
            },
            onLongPress = { cell ->
                val ok = game.longPressCell(cell.r, cell.c)
                if (ok) {
                    sfx.play(Sfx.SfxUndo)
                    if (showRemoveCoach) {
                        prefs.edit().putBoolean("ftue_remove_longpress_done", true).apply()
                        showRemoveCoach = false
                    }
                }
                refresh()
            },
        )

        if (illegal != null) {
            Text("⚠ $illegal", color = MxColors.Illegal, fontSize = 12.sp)
        }

        // Tools above tray (Vera layout); Resolver below tray
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = game.modeX,
                onClick = {
                    if (game.modeX) game.clearSelection() else game.setModeX()
                    sfx.play(Sfx.SfxTapUi)
                    refresh()
                },
                label = { Text("X") },
            )
            OutlinedButton(onClick = {
                game.undo()
                sfx.play(Sfx.SfxUndo)
                refresh()
            }) { Text("Undo") }
            Spacer(Modifier.weight(1f))
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
                if (game.returnToTray(id)) sfx.play(Sfx.SfxUndo)
                refresh()
            },
        )

        Button(
            onClick = {
                val outcome = game.resolve()
                refresh()
                if (outcome != null) {
                    sfx.play(Sfx.SfxReveal) // same reveal regardless of culprit
                    bgm.start(Bgm.BgmVictory)
                    showReveal = outcome
                } else {
                    sfx.play(Sfx.SfxIllegal)
                }
            },
            enabled = canResolve,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Resolver") }
    }

    showReveal?.let { outcome ->
        AlertDialog(
            onDismissRequest = {
                showReveal = null
                bgm.start(Bgm.BgmGameplayCalm)
            },
            title = { Text("¡Resuelto!") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Culpable: ${outcome.culpritName}",
                        fontWeight = FontWeight.Bold,
                        color = MxColors.Illegal,
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
                TextButton(onClick = {
                    showReveal = null
                    bgm.start(Bgm.BgmGameplayCalm)
                }) { Text("Seguir") }
            },
        )
    }
}

@Composable
private fun BoardWithEdges(
    game: GameController,
    validAnchors: Set<Cell>,
    onCell: (Cell) -> Unit,
    onLongPress: (Cell) -> Unit,
) {
    val play = game.play
    val n = play.size
    val cellSize = when {
        n <= 5 -> 48.dp
        n == 6 -> 42.dp
        else -> 36.dp
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Bottom) {
            Spacer(Modifier.width(56.dp))
            for (c in 0 until n) {
                Column(
                    Modifier.width(cellSize),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val line = play.edge.cols[c]
                    PoBadge(line.people, line.objects)
                    Text(
                        visibleTramos(line.segments.map { it.count }),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        color = MxColors.Ink,
                    )
                }
            }
        }
        for (r in 0 until n) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    Modifier.width(56.dp),
                    horizontalAlignment = Alignment.End,
                ) {
                    val line = play.edge.rows[r]
                    PoBadge(line.people, line.objects)
                    Text(
                        visibleTramos(line.segments.map { it.count }),
                        fontSize = 11.sp,
                        textAlign = TextAlign.End,
                        color = MxColors.Ink,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                }
                for (c in 0 until n) {
                    BoardCell(
                        game = game,
                        r = r,
                        c = c,
                        validAnchor = Cell(r, c) in validAnchors,
                        hasSelection = game.selectedId != null && !game.modeX,
                        modifier = Modifier.size(cellSize),
                        onClick = { onCell(Cell(r, c)) },
                        onLongClick = { onLongPress(Cell(r, c)) },
                    )
                }
            }
        }
    }
}

/** V3-2: omit tramo 0; empty line → single 0. */
private fun visibleTramos(counts: List<Int>): String {
    val visible = counts.filter { it > 0 }
    return if (visible.isEmpty()) "0" else visible.joinToString("|")
}

@Composable
private fun PoBadge(people: Int, objects: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        if (people > 0) {
            Text(
                "P$people",
                fontSize = 9.sp,
                color = MxColors.Ink,
                modifier = Modifier
                    .background(MxColors.EdgeP, RoundedCornerShape(3.dp))
                    .padding(horizontal = 3.dp),
            )
        }
        if (objects > 0) {
            Text(
                "O$objects",
                fontSize = 9.sp,
                color = MxColors.Ink,
                modifier = Modifier
                    .background(MxColors.EdgeO, RoundedCornerShape(3.dp))
                    .padding(horizontal = 3.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BoardCell(
    game: GameController,
    r: Int,
    c: Int,
    validAnchor: Boolean,
    hasSelection: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val play = game.play
    val roomId = play.roomOf(r, c)
    val baseRoom = RoomColors[roomId % RoomColors.size]
    val selected = game.selectedId != null && game.pieceAt(r, c) == game.selectedId
    val pid = game.pieceAt(r, c)
    val isObj = pid != null && play.objectById(pid) != null
    val isPawn = pid != null && pid.startsWith("pawn_")
    val label = when {
        game.isVictim(r, c) -> "V"
        game.hasX(r, c) -> "X"
        isPawn -> "P"
        isObj -> "O"
        else -> ""
    }
    // Iris tokens: room pastel under empty; P mono / O masa+hatch. Never Murdoku-colored pawns.
    val pieceFill = when {
        label == "V" -> MxColors.VictimFill
        isPawn -> MxColors.PersonFill
        isObj -> MxColors.ObjectFill
        else -> null
    }
    val roomBg = when {
        hasSelection && validAnchor -> baseRoom
        hasSelection && !validAnchor -> baseRoom.copy(alpha = 0.4f)
        else -> baseRoom.copy(alpha = 0.9f)
    }
    val bg = pieceFill ?: roomBg
    val borderColor = when {
        selected -> MxColors.Select
        game.isVictim(r, c) -> MxColors.VictimFill
        hasSelection && validAnchor -> MxColors.Select.copy(alpha = 0.55f)
        else -> MxColors.Line
    }
    val pieceColor = when (label) {
        "V", "P", "O" -> MxColors.Surface
        "X" -> MxColors.InkMuted
        else -> MxColors.Ink
    }
    Box(
        modifier
            .padding(1.dp)
            .background(bg, RoundedCornerShape(4.dp))
            .then(
                if (isObj) {
                    Modifier.drawBehind {
                        // diagonal hatch @ 0.22 alpha (Iris)
                        val step = 6.dp.toPx()
                        val stroke = 1.dp.toPx()
                        val hatch = MxColors.ObjectHatch.copy(alpha = 0.22f)
                        var x = -size.height
                        while (x < size.width + size.height) {
                            drawLine(
                                color = hatch,
                                start = Offset(x, size.height),
                                end = Offset(x + size.height, 0f),
                                strokeWidth = stroke,
                                pathEffect = PathEffect.cornerPathEffect(0f),
                            )
                            x += step
                        }
                    }
                } else Modifier,
            )
            .border(if (selected) 2.dp else 1.dp, borderColor, RoundedCornerShape(4.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center,
    ) {
        if (label.isNotEmpty()) {
            Text(label, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = pieceColor)
        }
    }
}

/**
 * Vera §2.1 AC-T*: 2 filas fijas + scroll horizontal nativo.
 * Slots ≥48×48 dp, gap ≥8 dp, zona ~112–128 dp.
 * Orden: P juntos izq → O por tamaño desc. Placed = ghost ~30%.
 */
@Composable
private fun TrayBar(
    game: GameController,
    onSelect: (String) -> Unit,
    onRotate: (String) -> Unit,
    onReturn: (String) -> Unit,
) {
    val play = game.play
    val pawns = (0 until play.pawnCount).map { play.pawnId(it) }
    val objects = play.objects.sortedByDescending { objectCells(it, 0).size }

    Surface(
        color = MxColors.Surface,
        shape = RoundedCornerShape(10.dp),
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Pawns as 2-row columns
            TwoRowSlotColumns(
                ids = pawns,
                game = game,
                isObject = false,
                onSelect = onSelect,
                onRotate = onRotate,
                onReturn = onReturn,
            )
            // Separator P|O
            Box(
                Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .padding(vertical = 4.dp)
                    .background(MxColors.Line),
            )
            TwoRowSlotColumns(
                ids = objects.map { it.id },
                game = game,
                isObject = true,
                onSelect = onSelect,
                onRotate = onRotate,
                onReturn = onReturn,
            )
        }
    }
}

@Composable
private fun TwoRowSlotColumns(
    ids: List<String>,
    game: GameController,
    isObject: Boolean,
    onSelect: (String) -> Unit,
    onRotate: (String) -> Unit,
    onReturn: (String) -> Unit,
) {
    // Column-major: pairs (top, bottom) scrolling horizontally
    val columns = ids.chunked(2)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (col in columns) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                for (id in col) {
                    val placed = game.state.placementOf(id) != null
                    val selected = game.selectedId == id
                    val rot = if (isObject) game.getObjectRot(id) else 0
                    val shapeSize = if (isObject) {
                        playObjectSize(game, id)
                    } else null
                    TrayChip(
                        label = if (isObject) id.take(3).replaceFirstChar { it.uppercase() } else "P",
                        selected = selected,
                        placed = placed,
                        subtitle = if (isObject) "r$rot · ${shapeSize ?: "?"}" else null,
                        onClick = {
                            when {
                                // tap-tap fallback: piece selected on board → return
                                placed && game.selectedId == id -> onReturn(id)
                                placed -> onSelect(id) // re-select placed instance
                                selected && isObject -> onRotate(id) // 2nd tap O unplaced = rotate
                                else -> onSelect(id)
                            }
                        },
                    )
                }
                if (col.size == 1) {
                    Spacer(Modifier.size(48.dp)) // keep row alignment
                }
            }
        }
    }
}

private fun playObjectSize(game: GameController, id: String): Int {
    val obj = game.play.objectById(id) ?: return 0
    return objectCells(obj, 0).size
}

@Composable
private fun TrayChip(
    label: String,
    selected: Boolean,
    placed: Boolean,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    // States: inTray | selected | placed(ghost~30%)
    val bg = when {
        selected -> MxColors.Select.copy(alpha = 0.22f)
        placed -> MxColors.Surface.copy(alpha = 0.3f)
        else -> MxColors.EdgeO.copy(alpha = 0.55f)
    }
    val contentAlpha = if (placed && !selected) 0.35f else 1f
    Column(
        Modifier
            .size(48.dp)
            .background(bg, RoundedCornerShape(8.dp))
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) MxColors.Select else MxColors.Line,
                RoundedCornerShape(8.dp),
            )
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            if (placed) "✓$label" else label,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = MxColors.Ink.copy(alpha = contentAlpha),
        )
        if (subtitle != null) {
            Text(subtitle, fontSize = 8.sp, color = MxColors.InkMuted.copy(alpha = contentAlpha))
        }
    }
}
