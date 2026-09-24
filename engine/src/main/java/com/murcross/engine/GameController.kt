package com.murcross.engine

import com.murcross.domain.model.Cell
import com.murcross.domain.model.GameState
import com.murcross.domain.model.Level
import com.murcross.domain.model.LevelPlay
import com.murcross.domain.model.PieceKind
import com.murcross.domain.model.Placement
import com.murcross.domain.model.RevealOutcome
import com.murcross.domain.model.ValidationResult
import com.murcross.domain.model.absoluteObjectCells

/**
 * Loop de partida: colocar/quitar, X, rotar O 90° CW, undo, Resolver.
 * Peones anónimos hasta [resolve].
 */
class GameController(val level: Level) {
    val play: LevelPlay get() = level.play

    var state: GameState = GameState()
        private set
    private val history = ArrayDeque<GameState>()

    var selectedId: String? = null
        private set
    var modeX: Boolean = false
        private set
    /** Rotación en bandeja (pieceId → 0..3). */
    private val trayRot = mutableMapOf<String, Int>()

    var resolved: RevealOutcome? = null
        private set
    var lastIllegalReason: String? = null
        private set

    fun selectPiece(pieceId: String) {
        selectedId = pieceId
        modeX = false
    }

    fun setModeX() {
        modeX = true
        selectedId = null
    }

    fun clearSelection() {
        selectedId = null
        modeX = false
    }

    fun getObjectRot(pieceId: String): Int {
        val p = state.placementOf(pieceId)
        if (p != null) return p.rot
        return trayRot[pieceId] ?: 0
    }

    fun validation(): ValidationResult = validate(play, state)

    fun canResolve(): Boolean = validation().ok

    fun pieceAt(r: Int, c: Int): String? {
        for (p in state.placements) {
            when (p.kind) {
                PieceKind.PAWN -> if (p.r == r && p.c == c) return p.pieceId
                PieceKind.OBJECT -> {
                    val obj = play.objectById(p.pieceId) ?: continue
                    if (absoluteObjectCells(obj, p).any { it.r == r && it.c == c }) return p.pieceId
                }
            }
        }
        return null
    }

    fun isVictim(r: Int, c: Int): Boolean =
        r == play.victim.r && c == play.victim.c

    fun hasX(r: Int, c: Int): Boolean =
        state.marksX.any { it.r == r && it.c == c }

    private fun snapshot() {
        history.addLast(state.copy(
            placements = state.placements.map { it.copy() },
            marksX = state.marksX.map { it.copy() },
        ))
    }

    fun undo(): Boolean {
        if (history.isEmpty()) return false
        state = history.removeLast()
        resolved = null
        return true
    }

    fun returnToTray(pieceId: String): Boolean {
        val p = state.placementOf(pieceId) ?: return false
        snapshot()
        if (p.kind == PieceKind.OBJECT) trayRot[pieceId] = p.rot
        state = state.copy(placements = state.placements.filter { it.pieceId != pieceId })
        resolved = null
        return true
    }

    fun tapCell(r: Int, c: Int) {
        if (!play.inBounds(r, c)) return
        if (modeX) {
            toggleX(r, c)
            return
        }
        val sel = selectedId
        if (sel == null) {
            pieceAt(r, c)?.let { selectedId = it }
            return
        }
        val hit = pieceAt(r, c)
        if (hit == sel) {
            if (play.objectById(sel) != null) rotateObject(sel)
            return
        }
        if (play.objects.any { it.id == sel }) {
            placeObject(sel, r, c)
        } else {
            placePawn(sel, r, c)
        }
    }

    fun toggleX(r: Int, c: Int) {
        if (isVictim(r, c)) {
            lastIllegalReason = "x_sobre_v"
            return
        }
        if (pieceAt(r, c) != null) {
            lastIllegalReason = "x_sobre_pieza"
            return
        }
        snapshot()
        state = if (hasX(r, c)) {
            state.copy(marksX = state.marksX.filterNot { it.r == r && it.c == c })
        } else {
            state.copy(marksX = state.marksX + Cell(r, c))
        }
        resolved = null
        lastIllegalReason = null
    }

    fun placePawn(pieceId: String, r: Int, c: Int): Boolean {
        if (!play.inBounds(r, c)) return false
        if (isVictim(r, c)) {
            lastIllegalReason = "sobre_v"
            return false
        }
        if (hasX(r, c)) {
            lastIllegalReason = "sobre_x"
            return false
        }
        val other = pieceAt(r, c)
        if (other != null && other != pieceId) {
            lastIllegalReason = "solape"
            return false
        }
        snapshot()
        val without = state.placements.filter { it.pieceId != pieceId }
        // Quitar X si hubiera
        val marks = state.marksX.filterNot { it.r == r && it.c == c }
        state = state.copy(
            placements = without + Placement(pieceId, PieceKind.PAWN, r, c, 0),
            marksX = marks,
        )
        resolved = null
        lastIllegalReason = null
        return true
    }

    fun placeObject(pieceId: String, r: Int, c: Int): Boolean {
        val obj = play.objectById(pieceId) ?: return false
        val rot = getObjectRot(pieceId)
        val cells = absoluteObjectCells(obj, r, c, rot)
        if (cells.any { !play.inBounds(it.r, it.c) }) {
            lastIllegalReason = "fuera_tablero"
            return false
        }
        if (cells.any { isVictim(it.r, it.c) }) {
            lastIllegalReason = "sobre_v"
            return false
        }
        if (cells.any { hasX(it.r, it.c) }) {
            lastIllegalReason = "sobre_x"
            return false
        }
        val rooms = cells.map { play.roomOf(it.r, it.c) }.toSet()
        if (rooms.size > 1) {
            lastIllegalReason = "cruza_salas"
            return false
        }
        for (cell in cells) {
            val hit = pieceAt(cell.r, cell.c)
            if (hit != null && hit != pieceId) {
                lastIllegalReason = "solape"
                return false
            }
        }
        snapshot()
        val without = state.placements.filter { it.pieceId != pieceId }
        val marks = state.marksX.filterNot { x -> cells.any { it.r == x.r && it.c == x.c } }
        state = state.copy(
            placements = without + Placement(pieceId, PieceKind.OBJECT, r, c, rot),
            marksX = marks,
        )
        resolved = null
        lastIllegalReason = null
        return true
    }

    /** Tap 90° CW; si colocada y la nueva pose es ilegal, rechaza. */
    fun rotateObject(pieceId: String): Boolean {
        val obj = play.objectById(pieceId) ?: return false
        val p = state.placementOf(pieceId)
        if (p == null) {
            trayRot[pieceId] = ((trayRot[pieceId] ?: 0) + 1) % 4
            return true
        }
        val newRot = (p.rot + 1) % 4
        val cells = absoluteObjectCells(obj, p.r, p.c, newRot)
        if (cells.any { !play.inBounds(it.r, it.c) }) {
            lastIllegalReason = "rot_fuera"
            return false
        }
        if (cells.any { isVictim(it.r, it.c) }) {
            lastIllegalReason = "rot_sobre_v"
            return false
        }
        if (cells.any { hasX(it.r, it.c) }) {
            lastIllegalReason = "rot_sobre_x"
            return false
        }
        val rooms = cells.map { play.roomOf(it.r, it.c) }.toSet()
        if (rooms.size > 1) {
            lastIllegalReason = "rot_cruza_salas"
            return false
        }
        for (cell in cells) {
            val hit = pieceAt(cell.r, cell.c)
            if (hit != null && hit != pieceId) {
                lastIllegalReason = "rot_solape"
                return false
            }
        }
        snapshot()
        state = state.copy(
            placements = state.placements.map {
                if (it.pieceId == pieceId) it.copy(rot = newRot) else it
            }
        )
        resolved = null
        lastIllegalReason = null
        return true
    }

    /**
     * Resolver: solo si legal. Mapea peones→nombres por celda de autoría.
     */
    fun resolve(): RevealOutcome? {
        val v = validation()
        if (!v.ok || v.culpritCell == null) {
            lastIllegalReason = "no_legal"
            return null
        }
        val byCell = level.reveal.suspects.associate { it.cell to it.name }
        val culpritName = byCell[v.culpritCell]
            ?: level.reveal.suspects.getOrNull(level.reveal.culpritPawnIndex ?: -1)?.name
            ?: "Sospechoso"
        val outcome = RevealOutcome(
            culpritName = culpritName,
            culpritCell = v.culpritCell,
            suspectNamesByCell = byCell,
            objectNames = level.reveal.objectNames,
        )
        resolved = outcome
        return outcome
    }

    /** Carga una solución conocida (tests / debug). */
    fun loadPlacements(placements: List<Placement>) {
        snapshot()
        state = GameState(placements = placements, marksX = emptyList())
        resolved = null
    }
}
