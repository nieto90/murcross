package com.murcross.engine

import com.murcross.domain.model.Cell
import com.murcross.domain.model.GameState
import com.murcross.domain.model.LevelPlay
import com.murcross.domain.model.PieceKind
import com.murcross.domain.model.ValidationResult
import com.murcross.domain.model.absoluteObjectCells

/**
 * Valida estado de partida por capas A/B/C.
 * No usa must_room ni nombres (reveal-only).
 */
fun validate(play: LevelPlay, state: GameState): ValidationResult {
    val soft = mutableListOf<String>()
    val hard = mutableListOf<String>()
    val n = play.size

    // --- Capa A: piezas exactas, solapes, O⊂sala ---
    val placedIds = mutableMapOf<String, Int>()
    for (p in state.placements) {
        placedIds[p.pieceId] = (placedIds[p.pieceId] ?: 0) + 1
    }
    for ((id, count) in placedIds) {
        if (count > 1) soft += "pieza_duplicada:$id"
    }

    for (i in 0 until play.pawnCount) {
        val id = play.pawnId(i)
        val p = state.placements.find { it.pieceId == id }
        if (p == null) soft += "falta_pieza:$id"
        else if (p.kind != PieceKind.PAWN) soft += "tipo_incorrecto:$id"
    }
    for (obj in play.objects) {
        val p = state.placements.find { it.pieceId == obj.id }
        if (p == null) soft += "falta_pieza:${obj.id}"
        else if (p.kind != PieceKind.OBJECT) soft += "tipo_incorrecto:${obj.id}"
    }
    for (id in placedIds.keys) {
        val knownPawn = (0 until play.pawnCount).any { play.pawnId(it) == id }
        val knownObj = play.objects.any { it.id == id }
        if (!knownPawn && !knownObj) soft += "pieza_desconocida:$id"
    }

    val occ = buildOccupancy(play, state)
    soft += occ.errors

    for (p in state.placements) {
        if (p.kind != PieceKind.OBJECT) continue
        val obj = play.objectById(p.pieceId) ?: continue
        val cells = absoluteObjectCells(obj, p)
        val rooms = mutableSetOf<Int>()
        var out = false
        for (cell in cells) {
            if (!play.inBounds(cell.r, cell.c)) {
                out = true
                continue
            }
            rooms += play.roomOf(cell.r, cell.c)
        }
        if (!out && rooms.size > 1) soft += "objeto_cruza_salas:${p.pieceId}"
    }

    // --- Capa B: bordes tramo + P/O ---
    for (r in 0 until n) {
        val segs = rowSegments(play.rooms, r)
        val expected = play.edge.rows.getOrNull(r)
        if (expected == null) {
            soft += "borde_fila_ausente:$r"
            continue
        }
        if (segs.size != expected.segments.size) {
            soft += "borde_fila_tramos_mismatch:$r"
        } else {
            for (i in segs.indices) {
                val got = countRowSegment(occ.grid, r, segs[i].c0, segs[i].c1)
                val want = expected.segments[i].count
                if (segs[i].roomId != expected.segments[i].roomId) {
                    soft += "borde_fila_room_mismatch:$r:$i"
                }
                if (got != want) soft += "borde_tramo_fila:$r:$i:got=$got:want=$want"
            }
        }
        val (people, objects) = linePeopleObjectsRow(occ.grid, r)
        if (people != expected.people) soft += "po_fila_P:$r:got=$people:want=${expected.people}"
        if (objects != expected.objects) soft += "po_fila_O:$r:got=$objects:want=${expected.objects}"
    }
    for (c in 0 until n) {
        val segs = colSegments(play.rooms, c)
        val expected = play.edge.cols.getOrNull(c)
        if (expected == null) {
            soft += "borde_col_ausente:$c"
            continue
        }
        if (segs.size != expected.segments.size) {
            soft += "borde_col_tramos_mismatch:$c"
        } else {
            for (i in segs.indices) {
                val got = countColSegment(occ.grid, c, segs[i].r0, segs[i].r1)
                val want = expected.segments[i].count
                if (segs[i].roomId != expected.segments[i].roomId) {
                    soft += "borde_col_room_mismatch:$c:$i"
                }
                if (got != want) soft += "borde_tramo_col:$c:$i:got=$got:want=$want"
            }
        }
        val (people, objects) = linePeopleObjectsCol(occ.grid, c)
        if (people != expected.people) soft += "po_col_P:$c:got=$people:want=${expected.people}"
        if (objects != expected.objects) soft += "po_col_O:$c:got=$objects:want=${expected.objects}"
    }

    // --- Capa C: exactamente 1 peón en sala de V ---
    val vRoom = play.victimRoom()
    val inV = mutableListOf<Cell>()
    for (p in state.placements) {
        if (p.kind != PieceKind.PAWN) continue
        if (!play.inBounds(p.r, p.c)) continue
        if (play.roomOf(p.r, p.c) == vRoom) inV += Cell(p.r, p.c)
    }
    var culpritCell: Cell? = null
    when (inV.size) {
        0 -> hard += "sospechosos_en_sala_V:0"
        1 -> culpritCell = inV[0]
        else -> hard += "sospechosos_en_sala_V:${inV.size}"
    }

    val softU = soft.distinct()
    val hardU = hard.distinct()
    val ok = softU.isEmpty() && hardU.isEmpty()
    return ValidationResult(
        ok = ok,
        softReasons = softU,
        hardReasons = hardU,
        culpritCell = if (ok) culpritCell else null,
    )
}

fun isLegal(play: LevelPlay, state: GameState): Boolean = validate(play, state).ok
