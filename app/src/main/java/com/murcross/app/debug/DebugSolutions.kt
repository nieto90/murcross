package com.murcross.app.debug

import com.murcross.domain.model.Cell
import com.murcross.domain.model.GameObject
import com.murcross.domain.model.Level
import com.murcross.domain.model.PieceKind
import com.murcross.domain.model.Placement
import com.murcross.domain.model.absoluteObjectCells

/**
 * Authored N=1 solutions for DEBUG «Ver solución» (Dani).
 * Release UI must not expose this; GameScreen gates on BuildConfig.DEBUG.
 */
object DebugSolutions {

    fun placementsFor(level: Level): List<Placement>? = when {
        level.id.contains("n1_cafe") -> n1(level)
        level.id.contains("n2_atico") -> n2(level)
        level.id.contains("v3a_mercado") -> v3a(level)
        level.id.contains("v3b_biblioteca") -> v3b(level)
        else -> null
    }

    private fun n1(level: Level): List<Placement> {
        val sofa = level.play.objectById("sofa")!!
        return listOf(
            Placement("banqueta", PieceKind.OBJECT, 0, 1, 0),
            Placement("estanteria", PieceKind.OBJECT, 1, 2, 0),
            findObjectPlacement(level, sofa, setOf(Cell(0, 3), Cell(0, 4), Cell(1, 4))),
            Placement("pawn_0", PieceKind.PAWN, 4, 0, 0),
            Placement("pawn_1", PieceKind.PAWN, 4, 3, 0),
        )
    }

    private fun n2(level: Level): List<Placement> {
        fun find(id: String, want: Set<Cell>) =
            findObjectPlacement(level, level.play.objectById(id)!!, want)
        return listOf(
            find("mesa", setOf(Cell(0, 0), Cell(1, 0), Cell(1, 1))),
            find("sofa", setOf(Cell(2, 3), Cell(2, 4), Cell(2, 5), Cell(3, 4))),
            find("estanteria", setOf(Cell(3, 2), Cell(4, 2), Cell(5, 2))),
            Placement("pawn_0", PieceKind.PAWN, 0, 3, 0),
            Placement("pawn_1", PieceKind.PAWN, 5, 5, 0),
            Placement("pawn_2", PieceKind.PAWN, 2, 0, 0),
        )
    }

    private fun v3a(level: Level): List<Placement> = listOf(
        findObj(level, "caja", setOf(Cell(3, 0), Cell(4, 0), Cell(4, 1))),
        findObj(level, "banco", setOf(Cell(0, 4), Cell(1, 4), Cell(2, 4))),
        findObj(level, "cesta", setOf(Cell(1, 0), Cell(2, 0))),
        Placement("pawn_0", PieceKind.PAWN, 0, 0, 0),
        Placement("pawn_1", PieceKind.PAWN, 4, 3, 0),
    )

    private fun v3b(level: Level): List<Placement> = listOf(
        findObj(level, "estanteria", setOf(Cell(1, 2), Cell(2, 2), Cell(3, 2))),
        findObj(level, "baul", setOf(Cell(4, 0), Cell(4, 1), Cell(5, 0), Cell(5, 1))),
        findObj(level, "sofa", setOf(Cell(3, 4), Cell(4, 3), Cell(4, 4), Cell(5, 4))),
        Placement("pawn_0", PieceKind.PAWN, 0, 2, 0),
        Placement("pawn_1", PieceKind.PAWN, 0, 3, 0),
        Placement("pawn_2", PieceKind.PAWN, 2, 5, 0),
    )

    private fun findObj(level: Level, objId: String, target: Set<Cell>): Placement =
        findObjectPlacement(level, level.play.objectById(objId)!!, target)

    private fun findObjectPlacement(level: Level, obj: GameObject, want: Set<Cell>): Placement {
        val n = level.play.size
        for (r in -2 until n + 2) for (c in -2 until n + 2) for (rot in 0..3) {
            val cells = absoluteObjectCells(obj, r, c, rot)
            if (cells.any { !level.play.inBounds(it.r, it.c) }) continue
            if (cells.toSet() == want) return Placement(obj.id, PieceKind.OBJECT, r, c, rot)
        }
        error("no placement for ${obj.id} covering $want")
    }
}
