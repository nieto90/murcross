package com.murcross.domain

/** Rotación 90° CW: (dr,dc) → (dc, -dr). Sin normalizar (ref. web model.js). */
fun rotateRelCells(cells: List<RelCell>, times: Int = 1): List<RelCell> {
    val n = ((times % 4) + 4) % 4
    var out = cells
    repeat(n) {
        out = out.map { RelCell(dr = it.dc, dc = -it.dr) }
    }
    return out
}

fun objectRelCells(obj: GameObject, rot: Int): List<RelCell> =
    rotateRelCells(obj.cells, rot)

fun absoluteObjectCells(obj: GameObject, placement: Placement): List<Cell> =
    objectRelCells(obj, placement.rot).map { Cell(placement.r + it.dr, placement.c + it.dc) }

fun inBounds(size: Int, r: Int, c: Int): Boolean =
    r in 0 until size && c in 0 until size

fun roomOf(play: LevelPlay, r: Int, c: Int): Int = play.rooms[r][c]

fun pawnId(index: Int): String = "pawn_$index"
