package com.murcross.engine

import com.murcross.domain.model.Cell
import com.murcross.domain.model.GameState
import com.murcross.domain.model.LevelPlay
import com.murcross.domain.model.PieceKind
import com.murcross.domain.model.absoluteObjectCells

enum class OccKind { V, P, O, X }

data class OccCell(val kind: OccKind, val id: String? = null)

data class Occupancy(
    val grid: Array<Array<OccCell?>>,
    val errors: List<String>,
) {
    fun get(r: Int, c: Int): OccCell? = grid[r][c]
}

fun isOccupied(cell: OccCell?): Boolean =
    cell != null && (cell.kind == OccKind.P || cell.kind == OccKind.O || cell.kind == OccKind.V)

fun isPerson(cell: OccCell?): Boolean =
    cell != null && (cell.kind == OccKind.P || cell.kind == OccKind.V)

fun isObject(cell: OccCell?): Boolean = cell != null && cell.kind == OccKind.O

fun buildOccupancy(play: LevelPlay, state: GameState): Occupancy {
    val n = play.size
    val grid = Array(n) { arrayOfNulls<OccCell>(n) }
    val errors = mutableListOf<String>()

    fun set(r: Int, c: Int, cell: OccCell): Boolean {
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

    set(play.victim.r, play.victim.c, OccCell(OccKind.V, "victim"))

    for (x in state.marksX) {
        if (x.r !in 0 until n || x.c !in 0 until n) {
            errors += "fuera_tablero:X:${x.r},${x.c}"
            continue
        }
        val existing = grid[x.r][x.c]
        when {
            existing?.kind == OccKind.V -> errors += "x_sobre_v:${x.r},${x.c}"
            existing != null -> errors += "solape:${x.r},${x.c}"
            else -> grid[x.r][x.c] = OccCell(OccKind.X)
        }
    }

    for (p in state.placements) {
        when (p.kind) {
            PieceKind.PAWN -> set(p.r, p.c, OccCell(OccKind.P, p.pieceId))
            PieceKind.OBJECT -> {
                val obj = play.objectById(p.pieceId)
                if (obj == null) {
                    errors += "objeto_desconocido:${p.pieceId}"
                    continue
                }
                for (cell in absoluteObjectCells(obj, p)) {
                    set(cell.r, cell.c, OccCell(OccKind.O, p.pieceId))
                }
            }
        }
    }

    return Occupancy(grid, errors)
}

fun countRowSegment(grid: Array<Array<OccCell?>>, r: Int, c0: Int, c1: Int): Int {
    var n = 0
    for (c in c0..c1) if (isOccupied(grid[r][c])) n++
    return n
}

fun countColSegment(grid: Array<Array<OccCell?>>, c: Int, r0: Int, r1: Int): Int {
    var n = 0
    for (r in r0..r1) if (isOccupied(grid[r][c])) n++
    return n
}

fun linePeopleObjectsRow(grid: Array<Array<OccCell?>>, r: Int): Pair<Int, Int> {
    var people = 0
    var objects = 0
    for (c in grid[r].indices) {
        if (isPerson(grid[r][c])) people++
        if (isObject(grid[r][c])) objects++
    }
    return people to objects
}

fun linePeopleObjectsCol(grid: Array<Array<OccCell?>>, c: Int): Pair<Int, Int> {
    var people = 0
    var objects = 0
    for (r in grid.indices) {
        if (isPerson(grid[r][c])) people++
        if (isObject(grid[r][c])) objects++
    }
    return people to objects
}

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
