package com.murcross.engine

import com.murcross.domain.GameState
import com.murcross.domain.LevelPlay
import com.murcross.domain.PieceKind
import com.murcross.domain.ValidationResult
import com.murcross.domain.absoluteObjectCells
import com.murcross.domain.pawnId
import com.murcross.domain.roomOf

/**
 * Validador puro en capas A (colocación) → B (bordes) → C (regla dura).
 *
 * Firma: validate(play, state) — NUNCA recibe LevelReveal / must_room / nombres.
 * Web MVP must_room-in-play NO se porta.
 */
fun validate(play: LevelPlay, state: GameState): ValidationResult {
    val soft = mutableListOf<String>()
    val hard = mutableListOf<String>()
    val n = play.size

    // --- Capa A: colocación ---
    val placedIds = mutableMapOf<String, com.murcross.domain.Placement>()
    for (p in state.placements) {
        if (placedIds.containsKey(p.pieceId)) soft += "pieza_duplicada:${p.pieceId}"
        placedIds[p.pieceId] = p
    }

    for (i in 0 until play.pawnCount) {
        val id = pawnId(i)
        val p = placedIds[id]
        when {
            p == null -> soft += "falta_pieza:$id"
            p.kind != PieceKind.PAWN -> soft += "tipo_incorrecto:$id"
        }
    }
    for (o in play.objects) {
        val p = placedIds[o.id]
        when {
            p == null -> soft += "falta_pieza:${o.id}"
            p.kind != PieceKind.OBJECT -> soft += "tipo_incorrecto:${o.id}"
        }
    }
    val known = (0 until play.pawnCount).map { pawnId(it) }.toSet() + play.objects.map { it.id }.toSet()
    for (id in placedIds.keys) {
        if (id !in known) soft += "pieza_desconocida:$id"
    }

    val (grid, occErrors) = buildOccupancy(play, state)
    soft += occErrors

    for (p in state.placements) {
        if (p.kind != PieceKind.OBJECT) continue
        val obj = play.objects.find { it.id == p.pieceId } ?: continue
        val cells = absoluteObjectCells(obj, p)
        val rooms = mutableSetOf<Int>()
        var out = false
        for (cell in cells) {
            if (cell.r !in 0 until n || cell.c !in 0 until n) {
                out = true
                continue
            }
            rooms += roomOf(play, cell.r, cell.c)
        }
        if (!out && rooms.size > 1) soft += "objeto_cruza_salas:${p.pieceId}"
    }

    // --- Capa B: bordes ---
    for (r in 0 until n) {
        val segs = rowSegments(play.rooms, r)
        val expected = play.edge.rows.getOrNull(r)?.segments.orEmpty()
        if (segs.size != expected.size) {
            soft += "borde_fila_tramos_mismatch:$r"
            continue
        }
        for (i in segs.indices) {
            val got = countRowSegment(grid, r, segs[i].c0, segs[i].c1)
            val want = expected[i].count
            if (segs[i].roomId != expected[i].roomId) soft += "borde_fila_room_mismatch:$r:$i"
            if (got != want) soft += "borde_tramo_fila:$r:$i:got=$got:want=$want"
        }
    }
    for (c in 0 until n) {
        val segs = colSegments(play.rooms, c)
        val expected = play.edge.cols.getOrNull(c)?.segments.orEmpty()
        if (segs.size != expected.size) {
            soft += "borde_col_tramos_mismatch:$c"
            continue
        }
        for (i in segs.indices) {
            val got = countColSegment(grid, c, segs[i].r0, segs[i].r1)
            val want = expected[i].count
            if (segs[i].roomId != expected[i].roomId) soft += "borde_col_room_mismatch:$c:$i"
            if (got != want) soft += "borde_tramo_col:$c:$i:got=$got:want=$want"
        }
    }
    for (r in 0 until n) {
        val (people, objects) = linePeopleObjectsRow(grid, r)
        val er = play.edge.rows.getOrNull(r)
        if (er == null) {
            soft += "borde_fila_ausente:$r"
            continue
        }
        if (people != er.people) soft += "po_fila_P:$r:got=$people:want=${er.people}"
        if (objects != er.objects) soft += "po_fila_O:$r:got=$objects:want=${er.objects}"
    }
    for (c in 0 until n) {
        val (people, objects) = linePeopleObjectsCol(grid, c)
        val ec = play.edge.cols.getOrNull(c)
        if (ec == null) {
            soft += "borde_col_ausente:$c"
            continue
        }
        if (people != ec.people) soft += "po_col_P:$c:got=$people:want=${ec.people}"
        if (objects != ec.objects) soft += "po_col_O:$c:got=$objects:want=${ec.objects}"
    }

    // --- Capa C: exactamente 1 peón en sala de V (hard) ---
    // NO must_room aquí.
    val vRoom = roomOf(play, play.victim.r, play.victim.c)
    val inV = mutableListOf<String>()
    for (p in state.placements) {
        if (p.kind != PieceKind.PAWN) continue
        if (p.r !in 0 until n || p.c !in 0 until n) continue
        if (roomOf(play, p.r, p.c) == vRoom) inV += p.pieceId
    }
    var culpritIndex: Int? = null
    when {
        inV.isEmpty() -> hard += "sospechosos_en_sala_V:0"
        inV.size > 1 -> hard += "sospechosos_en_sala_V:${inV.size}"
        else -> {
            val id = inV[0]
            culpritIndex = id.removePrefix("pawn_").toIntOrNull()
        }
    }

    val softU = soft.distinct()
    val hardU = hard.distinct()
    val ok = softU.isEmpty() && hardU.isEmpty()
    return ValidationResult(
        ok = ok,
        softReasons = softU,
        hardReasons = hardU,
        culpritPawnIndex = if (ok) culpritIndex else null,
    )
}
