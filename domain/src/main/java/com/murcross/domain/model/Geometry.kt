package com.murcross.domain.model

/** Rotación 90° CW de celdas relativas: (dr,dc) → (dc, -dr), normalizada al origen. */
fun rotateRelCells(cells: List<RelCell>, times: Int = 1): List<RelCell> {
    val n = ((times % 4) + 4) % 4
    var out = cells
    repeat(n) {
        out = out.map { RelCell(dr = it.dc, dc = -it.dr) }
    }
    return normalize(out)
}

fun normalize(cells: List<RelCell>): List<RelCell> {
    val minR = cells.minOf { it.dr }
    val minC = cells.minOf { it.dc }
    return cells.map { RelCell(it.dr - minR, it.dc - minC) }.sortedWith(compareBy({ it.dr }, { it.dc }))
}

fun objectCells(obj: GameObject, rot: Int): List<RelCell> =
    rotateRelCells(obj.cells, rot)

fun absoluteObjectCells(obj: GameObject, placement: Placement): List<Cell> =
    objectCells(obj, placement.rot).map { Cell(placement.r + it.dr, placement.c + it.dc) }

fun absoluteObjectCells(obj: GameObject, originR: Int, originC: Int, rot: Int): List<Cell> =
    objectCells(obj, rot).map { Cell(originR + it.dr, originC + it.dc) }
