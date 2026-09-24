package com.murcross.ui.game

import com.murcross.domain.Cell
import com.murcross.domain.GameState
import com.murcross.domain.Level
import com.murcross.domain.PieceKind
import com.murcross.domain.Placement
import com.murcross.domain.ValidationResult
import com.murcross.domain.absoluteObjectCells
import com.murcross.domain.inBounds
import com.murcross.domain.pawnId
import com.murcross.domain.roomOf
import com.murcross.engine.validate

enum class ToolMode { PLACE, MARK_X }

data class GameUiState(
    val level: Level,
    val state: GameState = GameState(),
    val selectedId: String? = null,
    val trayRot: Map<String, Int> = emptyMap(),
    val mode: ToolMode = ToolMode.PLACE,
    val validation: ValidationResult = ValidationResult(ok = false),
    val revealed: Boolean = false,
    val history: List<GameState> = emptyList(),
    val toast: String? = null,
    /** Lucas: highlight rooms when a piece is selected (not permanent must_room badge). */
    val highlightRoomIds: Set<Int> = emptySet(),
)

class GameController(private val level: Level) {
    var ui: GameUiState = GameUiState(level = level)
        private set

    private fun revalidate(base: GameUiState = ui): GameUiState {
        val v = validate(base.level.play, base.state)
        return base.copy(validation = v)
    }

    private fun push(next: GameState): GameUiState {
        val hist = ui.history + ui.state
        return revalidate(ui.copy(state = next, history = hist, toast = null, revealed = false))
    }

    fun selectPiece(id: String) {
        val highlight = highlightFor(id)
        ui = if (ui.selectedId == id && ui.mode == ToolMode.PLACE) {
            // tap again on O → rotate
            val obj = level.play.objects.find { it.id == id }
            if (obj != null) {
                rotateSelected()
                return
            }
            ui.copy(selectedId = id, mode = ToolMode.PLACE, highlightRoomIds = highlight)
        } else {
            ui.copy(selectedId = id, mode = ToolMode.PLACE, highlightRoomIds = highlight)
        }
    }

    private fun highlightFor(id: String): Set<Int> {
        // Highlight all rooms (soft) when selecting — helps packing without must_room spoiler.
        // For objects: highlight rooms that can fit is complex; v0 = all room ids for spatial cue.
        return level.play.roomMeta.map { it.id }.toSet().ifEmpty {
            level.play.rooms.flatten().toSet()
        }
    }

    fun toggleModeX() {
        ui = ui.copy(
            mode = if (ui.mode == ToolMode.MARK_X) ToolMode.PLACE else ToolMode.MARK_X,
            selectedId = if (ui.mode == ToolMode.MARK_X) ui.selectedId else null,
            highlightRoomIds = emptySet(),
        )
    }

    fun tapCell(r: Int, c: Int) {
        if (!inBounds(level.play.size, r, c)) return
        if (ui.mode == ToolMode.MARK_X) {
            toggleX(r, c)
            return
        }
        val id = ui.selectedId ?: return
        val existing = ui.state.placements.find { it.pieceId == id }
        // Tap on own piece → remove
        if (existing != null && existing.kind == PieceKind.PAWN && existing.r == r && existing.c == c) {
            undoableRemove(id)
            return
        }
        if (id.startsWith("pawn_")) {
            placePawn(id, r, c)
        } else {
            placeObject(id, r, c)
        }
    }

    private fun toggleX(r: Int, c: Int) {
        if (r == level.play.victim.r && c == level.play.victim.c) return
        val cell = Cell(r, c)
        val occ = occupiedCells()
        if (cell in occ && ui.state.marksX.none { it == cell }) return
        val marks = if (ui.state.marksX.any { it == cell }) {
            ui.state.marksX.filterNot { it == cell }
        } else {
            ui.state.marksX + cell
        }
        ui = push(ui.state.copy(marksX = marks))
    }

    private fun occupiedCells(): Set<Cell> {
        val set = mutableSetOf(level.play.victim)
        for (p in ui.state.placements) {
            when (p.kind) {
                PieceKind.PAWN -> set += Cell(p.r, p.c)
                PieceKind.OBJECT -> {
                    val obj = level.play.objects.find { it.id == p.pieceId } ?: continue
                    set += absoluteObjectCells(obj, p)
                }
            }
        }
        return set
    }

    private fun placePawn(id: String, r: Int, c: Int) {
        if (r == level.play.victim.r && c == level.play.victim.c) {
            ui = ui.copy(toast = "No encaja")
            return
        }
        if (ui.state.marksX.any { it.r == r && it.c == c }) {
            ui = ui.copy(toast = "No encaja")
            return
        }
        val others = occupiedCells().filter {
            val owner = ui.state.placements.find { pl ->
                if (pl.pieceId == id) false
                else when (pl.kind) {
                    PieceKind.PAWN -> pl.r == it.r && pl.c == it.c
                    PieceKind.OBJECT -> {
                        val obj = level.play.objects.find { o -> o.id == pl.pieceId }
                        obj != null && absoluteObjectCells(obj, pl).any { c2 -> c2 == it }
                    }
                }
            }
            owner != null || (it == level.play.victim)
        }
        // simpler collision
        val collision = ui.state.placements.any { pl ->
            if (pl.pieceId == id) return@any false
            when (pl.kind) {
                PieceKind.PAWN -> pl.r == r && pl.c == c
                PieceKind.OBJECT -> {
                    val obj = level.play.objects.find { it.id == pl.pieceId } ?: return@any false
                    absoluteObjectCells(obj, pl).any { it.r == r && it.c == c }
                }
            }
        } || (r == level.play.victim.r && c == level.play.victim.c)
        if (collision) {
            ui = ui.copy(toast = "No encaja")
            return
        }
        val nextPlacements = ui.state.placements.filter { it.pieceId != id } +
            Placement(id, PieceKind.PAWN, r, c)
        val nextMarks = ui.state.marksX.filterNot { it.r == r && it.c == c }
        ui = push(ui.state.copy(placements = nextPlacements, marksX = nextMarks))
            .copy(selectedId = id, highlightRoomIds = highlightFor(id))
    }

    private fun placeObject(id: String, r: Int, c: Int) {
        val obj = level.play.objects.find { it.id == id } ?: return
        val rot = ui.trayRot[id] ?: ui.state.placements.find { it.pieceId == id }?.rot ?: 0
        val probe = Placement(id, PieceKind.OBJECT, r, c, rot)
        val cells = absoluteObjectCells(obj, probe)
        if (cells.any { !inBounds(level.play.size, it.r, it.c) }) {
            ui = ui.copy(toast = "No encaja")
            return
        }
        if (cells.any { it.r == level.play.victim.r && it.c == level.play.victim.c }) {
            ui = ui.copy(toast = "No encaja")
            return
        }
        val rooms = cells.map { roomOf(level.play, it.r, it.c) }.toSet()
        if (rooms.size > 1) {
            ui = ui.copy(toast = "No encaja")
            return
        }
        val collision = cells.any { cell ->
            ui.state.placements.any { pl ->
                if (pl.pieceId == id) return@any false
                when (pl.kind) {
                    PieceKind.PAWN -> pl.r == cell.r && pl.c == cell.c
                    PieceKind.OBJECT -> {
                        val o = level.play.objects.find { it.id == pl.pieceId } ?: return@any false
                        absoluteObjectCells(o, pl).any { it == cell }
                    }
                }
            }
        }
        if (collision) {
            ui = ui.copy(toast = "No encaja")
            return
        }
        val keys = cells.toSet()
        val nextPlacements = ui.state.placements.filter { it.pieceId != id } + probe
        val nextMarks = ui.state.marksX.filterNot { it in keys }
        ui = push(ui.state.copy(placements = nextPlacements, marksX = nextMarks))
            .copy(selectedId = id, trayRot = ui.trayRot + (id to rot), highlightRoomIds = highlightFor(id))
    }

    fun rotateSelected() {
        val id = ui.selectedId ?: return
        val obj = level.play.objects.find { it.id == id } ?: return
        val p = ui.state.placements.find { it.pieceId == id }
        val curRot = p?.rot ?: ui.trayRot[id] ?: 0
        val next = (curRot + 1) % 4
        if (p != null && p.r >= 0) {
            val probe = p.copy(rot = next)
            val cells = absoluteObjectCells(obj, probe)
            val illegal = cells.any { !inBounds(level.play.size, it.r, it.c) } ||
                cells.any { it.r == level.play.victim.r && it.c == level.play.victim.c } ||
                cells.map { roomOf(level.play, it.r, it.c) }.toSet().size > 1 ||
                cells.any { cell ->
                    ui.state.placements.any { pl ->
                        pl.pieceId != id && when (pl.kind) {
                            PieceKind.PAWN -> pl.r == cell.r && pl.c == cell.c
                            PieceKind.OBJECT -> {
                                val o = level.play.objects.find { it.id == pl.pieceId } ?: return@any false
                                absoluteObjectCells(o, pl).any { it == cell }
                            }
                        }
                    }
                }
            if (illegal) {
                // detach
                ui = push(ui.state.copy(placements = ui.state.placements.filter { it.pieceId != id }))
                    .copy(selectedId = id, trayRot = ui.trayRot + (id to next), highlightRoomIds = highlightFor(id))
                return
            }
            val keys = cells.toSet()
            ui = push(
                ui.state.copy(
                    placements = ui.state.placements.map { if (it.pieceId == id) probe else it },
                    marksX = ui.state.marksX.filterNot { it in keys },
                ),
            ).copy(trayRot = ui.trayRot + (id to next), selectedId = id, highlightRoomIds = highlightFor(id))
        } else {
            ui = ui.copy(trayRot = ui.trayRot + (id to next))
        }
    }

    private fun undoableRemove(id: String) {
        ui = push(ui.state.copy(placements = ui.state.placements.filter { it.pieceId != id }))
            .copy(selectedId = id, highlightRoomIds = highlightFor(id))
    }

    fun undo() {
        val hist = ui.history
        if (hist.isEmpty()) return
        val prev = hist.last()
        ui = revalidate(
            ui.copy(
                state = prev,
                history = hist.dropLast(1),
                revealed = false,
                toast = null,
            ),
        )
    }

    fun resolve(): Boolean {
        if (!ui.validation.ok) {
            ui = ui.copy(toast = "Aún no encaja del todo")
            return false
        }
        ui = ui.copy(revealed = true, toast = null)
        return true
    }

    fun clearToast() {
        ui = ui.copy(toast = null)
    }

    fun trayPieceIds(): List<String> {
        val pawns = (0 until level.play.pawnCount).map { pawnId(it) }
        val objs = level.play.objects.map { it.id }
        return pawns + objs
    }

    fun isInTray(id: String): Boolean = ui.state.placements.none { it.pieceId == id }
}
