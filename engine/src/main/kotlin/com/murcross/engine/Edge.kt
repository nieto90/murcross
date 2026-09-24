package com.murcross.engine

import com.murcross.domain.Cell
import com.murcross.domain.GameObject
import com.murcross.domain.GameState
import com.murcross.domain.LevelPlay
import com.murcross.domain.PieceKind
import com.murcross.domain.absoluteObjectCells

enum class OccupKind { V, P, O, X }

data class OccupCell(val kind: OccupKind, val id: String? = null)

data class RowSeg(val roomId: Int, val c0: Int, val c1: Int)
data class ColSeg(val roomId: Int, val r0: Int, val r1: Int)

fun rowSegments(rooms: List<List<Int>>, r: Int): List<RowSeg> {
    val size = rooms[r].size
    val segs = mutableListOf<RowSeg>()
    var i = 0
    while (i < size) {
        val roomId = rooms[r][i]
        var j = i + 1
        while (j < size && rooms[r][j] == roomId) j++
        segs += RowSeg(roomId, i, j - 1)
        i = j
    }
    return segs
}

fun colSegments(rooms: List<List<Int>>, c: Int): List<ColSeg> {
    val size = rooms.size
    val segs = mutableListOf<ColSeg>()
    var i = 0
    while (i < size) {
        val roomId = rooms[i][c]
        var j = i + 1
        while (j < size && rooms[j][c] == roomId) j++
        segs += ColSeg(roomId, i, j - 1)
        i = j
    }
    return segs
}

fun buildOccupancy(
    play: LevelPlay,
    state: GameState,
): Pair<Array<Array<OccupCell?>>, List<String>> {
    val n = play.size
    val grid = Array(n) { arrayOfNulls<OccupCell>(n) }
    val errors = mutableListOf<String>()

    fun set(r: Int, c: Int, cell: OccupCell): Boolean {
        if (r !in 0 until n || c !in 0 until n) {
            errors += "fuera_tablero:${cell.kind}:$r,$c"
            return false
        }
        if (grid[r][c] != null) {
            errors += "solape:$r,$c"
            return false
        }
        grid[r][c] = cell
        return true
    }

    set(play.victim.r, play.victim.c, OccupCell(OccupKind.V, "victim"))

    for (x in state.marksX) {
        val existing = grid.getOrNull(x.r)?.getOrNull(x.c)
        when {
            existing?.kind == OccupKind.V -> errors += "x_sobre_v:${x.r},${x.c}"
            existing != null -> errors += "solape:${x.r},${x.c}"
            x.r !in 0 until n || x.c !in 0 until n -> errors += "fuera_tablero:X:${x.r},${x.c}"
            else -> grid[x.r][x.c] = OccupCell(OccupKind.X)
        }
    }

    val objectsById = play.objects.associateBy { it.id }
    for (p in state.placements) {
        when (p.kind) {
            PieceKind.PAWN -> set(p.r, p.c, OccupCell(OccupKind.P, p.pieceId))
            PieceKind.OBJECT -> {
                val obj = objectsById[p.pieceId]
                if (obj == null) {
                    errors += "objeto_desconocido:${p.pieceId}"
                } else {
                    for (cell in absoluteObjectCells(obj, p)) {
                        set(cell.r, cell.c, OccupCell(OccupKind.O, p.pieceId))
                    }
                }
            }
        }
    }
    return grid to errors
}

fun isOccupied(cell: OccupCell?): Boolean =
    cell != null && (cell.kind == OccupKind.P || cell.kind == OccupKind.O || cell.kind == OccupKind.V)

fun isPerson(cell: OccupCell?): Boolean =
    cell != null && (cell.kind == OccupKind.P || cell.kind == OccupKind.V)

fun isObject(cell: OccupCell?): Boolean = cell?.kind == OccupKind.O

fun countRowSegment(grid: Array<Array<OccupCell?>>, r: Int, c0: Int, c1: Int): Int {
    var n = 0
    for (c in c0..c1) if (isOccupied(grid[r][c])) n++
    return n
}

fun countColSegment(grid: Array<Array<OccupCell?>>, c: Int, r0: Int, r1: Int): Int {
    var n = 0
    for (r in r0..r1) if (isOccupied(grid[r][c])) n++
    return n
}

fun linePeopleObjectsRow(grid: Array<Array<OccupCell?>>, r: Int): Pair<Int, Int> {
    var people = 0
    var objects = 0
    for (c in grid[r].indices) {
        if (isPerson(grid[r][c])) people++
        if (isObject(grid[r][c])) objects++
    }
    return people to objects
}

fun linePeopleObjectsCol(grid: Array<Array<OccupCell?>>, c: Int): Pair<Int, Int> {
    var people = 0
    var objects = 0
    for (r in grid.indices) {
        if (isPerson(grid[r][c])) people++
        if (isObject(grid[r][c])) objects++
    }
    return people to objects
}
