package com.murcross.app.ui.screens

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.murcross.app.BuildConfig
import com.murcross.app.debug.DebugSolutions
import com.murcross.app.ui.theme.GlyphO
import com.murcross.app.ui.theme.GlyphP
import com.murcross.app.ui.theme.GlyphV
import com.murcross.app.ui.theme.GlyphX
import com.murcross.app.ui.theme.MxCardRadius
import com.murcross.app.ui.theme.MxChipRadius
import com.murcross.app.ui.theme.MxColors
import com.murcross.app.ui.theme.RoomColors
import com.murcross.app.ui.theme.RoomPill
import com.murcross.app.ui.theme.drawObjectHatch
import com.murcross.app.ui.theme.drawRoomPattern
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

    var showRoomPills by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MxColors.Bg)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Vera HUD: short title + mute/back only — no subtitle
        Row(
            Modifier
                .fillMaxWidth()
                .height(40.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) { Text("←", color = MxColors.Brand) }
            Text(
                level.title.ifBlank { level.id },
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                color = MxColors.Ink,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { toggleMute(); refresh() }) {
                Text(if (muted) "🔇" else "🔊")
            }
        }

        if (showRemoveCoach) {
            Surface(
                color = MxColors.Surface,
                shape = RoundedCornerShape(MxCardRadius),
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
                        color = MxColors.Ink,
                    )
                    TextButton(onClick = {
                        prefs.edit().putBoolean("ftue_remove_longpress_done", true).apply()
                        showRemoveCoach = false
                        sfx.play(Sfx.SfxCoachDismiss)
                    }) { Text("OK", color = MxColors.Brand) }
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
            showRoomPills = showRoomPills,
            onLongPress = { cell ->
                val ok = game.longPressCell(cell.r, cell.c)
                if (ok) {
                    sfx.play(Sfx.SfxUndo)
                    if (showRemoveCoach) {
                        prefs.edit().putBoolean("ftue_remove_longpress_done", true).apply()
                        showRemoveCoach = false
                    }
                } else {
                    // Vera: long-press empty → toggle room pills
                    showRoomPills = !showRoomPills
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

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = {
                    val outcome = game.resolve()
                    refresh()
                    if (outcome != null) {
                        sfx.play(Sfx.SfxReveal)
                        bgm.start(Bgm.BgmVictory)
                        showReveal = outcome
                        showRoomPills = true
                    } else {
                        sfx.play(Sfx.SfxIllegal)
                    }
                },
                enabled = canResolve,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MxColors.Brand,
                    contentColor = MxColors.Surface,
                    disabledContainerColor = MxColors.Line,
                    disabledContentColor = MxColors.InkMuted,
                ),
                shape = RoundedCornerShape(MxCardRadius),
            ) { Text("Resolver") }
            if (BuildConfig.DEBUG) {
                OutlinedButton(
                    onClick = {
                        val placements = DebugSolutions.placementsFor(level)
                        if (placements == null) {
                            sfx.play(Sfx.SfxIllegal)
                            refresh()
                            return@OutlinedButton
                        }
                        val outcome = game.applyAuthoredSolution(placements)
                        refresh()
                        if (outcome != null) {
                            sfx.play(Sfx.SfxReveal)
                            bgm.start(Bgm.BgmVictory)
                            showReveal = outcome
                            showRoomPills = true
                        } else {
                            // authored layout failed validate — never count as victory
                            sfx.play(Sfx.SfxIllegal)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(MxCardRadius),
                ) { Text("Ver solución") }
            }
        }
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
                        color = MxColors.AccentRed,
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
    showRoomPills: Boolean = false,
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
    // Room pill anchors: top-left cell of each room id (cheap AC-C4)
    val roomAnchors = remember(play) {
        val first = mutableMapOf<Int, Cell>()
        for (r in 0 until n) for (c in 0 until n) {
            val id = play.roomOf(r, c)
            if (id !in first) first[id] = Cell(r, c)
        }
        first
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Bottom) {
            Spacer(Modifier.width(52.dp))
            for (c in 0 until n) {
                Column(
                    Modifier.width(cellSize),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val line = play.edge.cols[c]
                    PoBadge(line.people, line.objects)
                    TramoBoxes(line.segments.map { it.count })
                }
            }
        }
        for (r in 0 until n) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    Modifier.width(52.dp),
                    horizontalAlignment = Alignment.End,
                ) {
                    val line = play.edge.rows[r]
                    PoBadge(line.people, line.objects)
                    TramoBoxes(line.segments.map { it.count }, endPad = true)
                }
                for (c in 0 until n) {
                    val roomId = play.roomOf(r, c)
                    val showPill = roomAnchors[roomId] == Cell(r, c)
                    val pillText = play.roomMeta.getOrNull(roomId)?.short
                        ?: play.roomMeta.getOrNull(roomId)?.name?.take(4)
                        ?: "R${roomId + 1}"
                    BoardCell(
                        game = game,
                        r = r,
                        c = c,
                        validAnchor = Cell(r, c) in validAnchors,
                        hasSelection = game.selectedId != null && !game.modeX,
                        roomPill = if (showRoomPills && showPill) pillText else null,
                        modifier = Modifier.size(cellSize),
                        onClick = { onCell(Cell(r, c)) },
                        onLongClick = { onLongPress(Cell(r, c)) },
                    )
                }
            }
        }
    }
}

/** V3-2: omit tramo 0; empty → single 0. Vera: no '|' — separate boxes with gap. */
@Composable
private fun TramoBoxes(counts: List<Int>, endPad: Boolean = false) {
    val visible = counts.filter { it > 0 }.ifEmpty { listOf(0) }
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = if (endPad) Modifier.padding(end = 4.dp) else Modifier,
    ) {
        for (n in visible) {
            Text(
                "$n",
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                color = MxColors.InkMuted,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .background(MxColors.Surface, RoundedCornerShape(3.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp),
            )
        }
    }
}

/** Vera: glyph P/O + digit — never strings "P1"/"O2". */
@Composable
private fun PoBadge(people: Int, objects: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        if (people > 0) {
            Row(
                Modifier
                    .background(MxColors.EdgeP, RoundedCornerShape(3.dp))
                    .padding(horizontal = 2.dp, vertical = 1.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                GlyphP(Modifier.size(10.dp), fill = MxColors.Ink)
                Text("$people", fontSize = 9.sp, color = MxColors.Ink, fontWeight = FontWeight.Bold)
            }
        }
        if (objects > 0) {
            Row(
                Modifier
                    .background(MxColors.EdgeO, RoundedCornerShape(3.dp))
                    .padding(horizontal = 2.dp, vertical = 1.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                GlyphO(Modifier.size(10.dp), fill = MxColors.Ink, hatch = MxColors.Ink.copy(alpha = 0.25f))
                Text("$objects", fontSize = 9.sp, color = MxColors.Ink, fontWeight = FontWeight.Bold)
            }
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
    roomPill: String?,
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
    val isVictim = game.isVictim(r, c)
    val hasX = game.hasX(r, c)

    // Iris: P mono / O masa+hatch / V badge. Never Murdoku-colored identity pawns.
    val pieceFill = when {
        isVictim -> MxColors.VictimFill
        isPawn -> MxColors.PersonFill
        isObj -> MxColors.ObjectFill
        else -> null
    }

    // AC-S5: legales = room tint 100%; ilegales ~40%; idle = full pastel (no legal outlines)
    val roomBg = when {
        hasSelection && validAnchor -> baseRoom
        hasSelection && !validAnchor -> baseRoom.copy(alpha = 0.4f)
        else -> baseRoom
    }
    val bg = pieceFill ?: roomBg

    // AC-S5: legal cells = Select outline 2dp; idle = no legal outlines
    val borderWidth = when {
        selected -> 2.dp
        hasSelection && validAnchor -> 2.dp
        else -> 1.dp
    }
    val borderColor = when {
        selected -> MxColors.Select
        hasSelection && validAnchor -> MxColors.Select
        isVictim -> MxColors.VictimFill
        else -> MxColors.Line
    }
    val patternAlpha = if (hasSelection && !validAnchor && pieceFill == null) 0.4f else 1f

    Box(
        modifier
            .padding(1.dp)
            .background(bg, RoundedCornerShape(4.dp))
            .drawBehind {
                if (pieceFill == null) {
                    drawRoomPattern(roomId, MxColors.Ink.copy(alpha = 0.12f * patternAlpha))
                }
                if (isObj) {
                    drawObjectHatch()
                }
            }
            .border(borderWidth, borderColor, RoundedCornerShape(4.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center,
    ) {
        when {
            isVictim -> GlyphV(Modifier.size(22.dp))
            isPawn -> GlyphP(Modifier.size(26.dp), fill = MxColors.Surface)
            isObj -> { /* masa+hatch is the O channel; no identity label */ }
            hasX -> GlyphX(Modifier.size(18.dp), color = MxColors.InkMuted)
        }
        // Room pill only on empty anchor cell (no piece/V/X), idle-safe
        if (roomPill != null && pieceFill == null && !hasX) {
            RoomPill(
                roomPill,
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 2.dp),
            )
        }
    }
}

/**
 * Vera §2.1 AC-T* / Iris AC-T5: 2 filas + scroll H.
 * Chips ≥48×48 dp, corner 12–16 dp, placed = ghost ~30% (dashed).
 * Hard ban: no must_room badges.
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
        color = MxColors.TraySurface,
        shape = RoundedCornerShape(MxCardRadius),
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
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TwoRowSlotColumns(
                ids = pawns,
                game = game,
                isObject = false,
                onSelect = onSelect,
                onRotate = onRotate,
                onReturn = onReturn,
            )
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
    val columns = ids.chunked(2)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        for (col in columns) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                for (id in col) {
                    val placed = game.state.placementOf(id) != null
                    val selected = game.selectedId == id
                    TrayChip(
                        isObject = isObject,
                        selected = selected,
                        placed = placed,
                        onClick = {
                            when {
                                placed && game.selectedId == id -> onReturn(id)
                                placed -> onSelect(id)
                                selected && isObject -> onRotate(id)
                                else -> onSelect(id)
                            }
                        },
                    )
                }
                if (col.size == 1) {
                    Spacer(Modifier.size(48.dp))
                }
            }
        }
    }
}

@Composable
private fun TrayChip(
    isObject: Boolean,
    selected: Boolean,
    placed: Boolean,
    onClick: () -> Unit,
) {
    // States: inTray | selected | placed(ghost~30% dashed)
    val shape = RoundedCornerShape(MxChipRadius) // 14 dp within 12–16
    val bg = when {
        selected -> MxColors.Select.copy(alpha = 0.22f)
        placed -> MxColors.TraySurface.copy(alpha = 0.3f)
        else -> MxColors.TraySurface
    }
    val contentAlpha = if (placed && !selected) 0.3f else 1f
    val borderColor = when {
        selected -> MxColors.Select
        else -> MxColors.Line
    }
    Box(
        Modifier
            .size(48.dp)
            .background(bg, shape)
            .then(
                if (placed && !selected) {
                    Modifier.drawWithContent {
                        drawContent()
                        drawRoundRect(
                            color = MxColors.Line,
                            style = Stroke(
                                width = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
                            ),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                                MxChipRadius.toPx(),
                                MxChipRadius.toPx(),
                            ),
                        )
                    }
                } else {
                    Modifier.border(if (selected) 2.dp else 1.5.dp, borderColor, shape)
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (isObject) {
            GlyphO(
                Modifier.size(28.dp),
                fill = MxColors.ObjectFill.copy(alpha = contentAlpha),
                hatch = MxColors.ObjectHatch.copy(alpha = 0.22f * contentAlpha),
            )
        } else {
            GlyphP(
                Modifier.size(28.dp),
                fill = MxColors.PersonFill.copy(alpha = contentAlpha),
            )
        }
    }
}
