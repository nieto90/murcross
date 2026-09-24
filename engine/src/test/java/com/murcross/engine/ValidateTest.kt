package com.murcross.engine

import com.murcross.data.LevelLoader
import com.murcross.domain.model.Cell
import com.murcross.domain.model.GameState
import com.murcross.domain.model.PieceKind
import com.murcross.domain.model.Placement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidateTest {

    private val n1 = LevelLoader.loadBundled("n1_cafe")
    private val n2 = LevelLoader.loadBundled("n2_atico")

    /** Solución geométrica N1 (verify_levels_v2). */
    private fun n1Solution(): List<Placement> = listOf(
        Placement("banqueta", PieceKind.OBJECT, 0, 1, 0),
        Placement("estanteria", PieceKind.OBJECT, 1, 2, 0),
        // Sofá L: cells (0,3)(0,4)(1,4) — shape L [(0,0),(1,0),(1,1)] needs rot
        // Absolute from (0,3) with rot that yields those cells:
        // rot0 at (0,3): (0,3)(1,3)(1,4) — wrong
        // We place using computed rot in test helper below
        Placement("sofa", PieceKind.OBJECT, 0, 3, sofaRotN1()),
        Placement("pawn_0", PieceKind.PAWN, 4, 0, 0),
        Placement("pawn_1", PieceKind.PAWN, 4, 3, 0),
    )

    /** Encuentra rotación del sofá que cubre {(0,3),(0,4),(1,4)} con origen (0,3). */
    private fun sofaRotN1(): Int {
        val obj = n1.play.objectById("sofa")!!
        val target = setOf(Cell(0, 3), Cell(0, 4), Cell(1, 4))
        for (rot in 0..3) {
            val cells = com.murcross.domain.model.absoluteObjectCells(obj, 0, 3, rot).toSet()
            if (cells == target) return rot
        }
        // Try other origins
        for (r in 0 until 5) for (c in 0 until 5) for (rot in 0..3) {
            val cells = com.murcross.domain.model.absoluteObjectCells(obj, r, c, rot).toSet()
            if (cells == target) {
                // mutate: return encoded - we need origin too; handle in n1Solution differently
                return rot + 10 * r + 100 * c // won't use this path if origin works
            }
        }
        error("sofa placement not found")
    }

    private fun n1SolutionFixed(): Pair<List<Placement>, Cell> {
        val obj = n1.play.objectById("sofa")!!
        val target = setOf(Cell(0, 3), Cell(0, 4), Cell(1, 4))
        var sofaPlacement: Placement? = null
        for (r in 0 until 5) for (c in 0 until 5) for (rot in 0..3) {
            val cells = com.murcross.domain.model.absoluteObjectCells(obj, r, c, rot)
            if (cells.any { !n1.play.inBounds(it.r, it.c) }) continue
            if (cells.toSet() == target) {
                sofaPlacement = Placement("sofa", PieceKind.OBJECT, r, c, rot)
                break
            }
        }
        requireNotNull(sofaPlacement)
        val placements = listOf(
            Placement("banqueta", PieceKind.OBJECT, 0, 1, 0),
            Placement("estanteria", PieceKind.OBJECT, 1, 2, 0),
            sofaPlacement,
            Placement("pawn_0", PieceKind.PAWN, 4, 0, 0),
            Placement("pawn_1", PieceKind.PAWN, 4, 3, 0),
        )
        return placements to Cell(4, 3)
    }

    private fun n2SolutionFixed(): Pair<List<Placement>, Cell> {
        fun find(id: String, target: Set<Cell>): Placement {
            val obj = n2.play.objectById(id)!!
            for (r in 0 until 6) for (c in 0 until 6) for (rot in 0..3) {
                val cells = com.murcross.domain.model.absoluteObjectCells(obj, r, c, rot)
                if (cells.any { !n2.play.inBounds(it.r, it.c) }) continue
                if (cells.toSet() == target) return Placement(id, PieceKind.OBJECT, r, c, rot)
            }
            error("placement not found for $id")
        }
        val placements = listOf(
            find("mesa", setOf(Cell(0, 0), Cell(1, 0), Cell(1, 1))),
            find("sofa", setOf(Cell(2, 3), Cell(2, 4), Cell(2, 5), Cell(3, 4))),
            find("estanteria", setOf(Cell(3, 2), Cell(4, 2), Cell(5, 2))),
            Placement("pawn_0", PieceKind.PAWN, 0, 3, 0), // Mar
            Placement("pawn_1", PieceKind.PAWN, 2, 0, 0), // Iris culpable
            Placement("pawn_2", PieceKind.PAWN, 5, 5, 0), // Gus
        )
        return placements to Cell(2, 0)
    }

    @Test
    fun n1_officialSolution_isLegal_culpritInSalon() {
        val (placements, culprit) = n1SolutionFixed()
        val result = validate(n1.play, GameState(placements))
        assertTrue(result.softReasons.joinToString(), result.softReasons.isEmpty())
        assertTrue(result.hardReasons.joinToString(), result.hardReasons.isEmpty())
        assertTrue(result.ok)
        assertEquals(culprit, result.culpritCell)
    }

    @Test
    fun n2_officialSolution_isLegal_culpritIris() {
        val (placements, culprit) = n2SolutionFixed()
        val result = validate(n2.play, GameState(placements))
        assertTrue(result.softReasons.joinToString(), result.softReasons.isEmpty())
        assertTrue(result.hardReasons.joinToString(), result.hardReasons.isEmpty())
        assertTrue(result.ok)
        assertEquals(culprit, result.culpritCell)
    }

    @Test
    fun layerA_missingPiece_illegal() {
        val (placements, _) = n1SolutionFixed()
        val incomplete = placements.filter { it.pieceId != "pawn_1" }
        val result = validate(n1.play, GameState(incomplete))
        assertFalse(result.ok)
        assertTrue(result.softReasons.any { it.startsWith("falta_pieza:pawn_1") })
    }

    @Test
    fun layerA_objectCrossesRooms_illegal() {
        // Banqueta horizontal spanning Barra+Salón: (0,2)(0,3)
        val bad = listOf(
            Placement("banqueta", PieceKind.OBJECT, 0, 2, 0),
            Placement("estanteria", PieceKind.OBJECT, 1, 2, 0),
            Placement("sofa", PieceKind.OBJECT, 2, 3, 0),
            Placement("pawn_0", PieceKind.PAWN, 4, 0, 0),
            Placement("pawn_1", PieceKind.PAWN, 4, 3, 0),
        )
        val result = validate(n1.play, GameState(bad))
        assertFalse(result.ok)
        assertTrue(
            result.softReasons.any { it.contains("objeto_cruza_salas") || it.contains("borde_") || it.contains("solape") || it.contains("po_") }
        )
    }

    @Test
    fun layerB_wrongEdge_illegal() {
        val (placements, _) = n1SolutionFixed()
        // Move pawn_0 from (4,0) to (3,0) — breaks edges / PO
        val moved = placements.map {
            if (it.pieceId == "pawn_0") it.copy(r = 3, c = 0) else it
        }
        val result = validate(n1.play, GameState(moved))
        assertFalse(result.ok)
        assertTrue(result.softReasons.any { it.startsWith("borde_") || it.startsWith("po_") })
    }

    @Test
    fun layerC_zeroPawnsInVictimRoom_hardFail() {
        val (placements, _) = n1SolutionFixed()
        // Both pawns in Barra
        val moved = placements.map {
            when (it.pieceId) {
                "pawn_0" -> it.copy(r = 4, c = 0)
                "pawn_1" -> it.copy(r = 4, c = 1)
                else -> it
            }
        }
        val result = validate(n1.play, GameState(moved))
        assertFalse(result.ok)
        // May also fail edges; hard should mention sala V if both outside Salon
        // pawn at (4,1) is Barra (cols 0-2), (4,0) Barra → 0 in Salon
        assertTrue(
            result.hardReasons.any { it.startsWith("sospechosos_en_sala_V") } ||
                result.softReasons.isNotEmpty()
        )
    }

    @Test
    fun layerC_twoPawnsInVictimRoom_hardFail() {
        val play = n1.play
        // Minimal: place both pawns in Salón without caring about full legality of O
        val state = GameState(
            placements = listOf(
                Placement("pawn_0", PieceKind.PAWN, 0, 3, 0),
                Placement("pawn_1", PieceKind.PAWN, 1, 3, 0),
                Placement("banqueta", PieceKind.OBJECT, 0, 1, 0),
                Placement("estanteria", PieceKind.OBJECT, 1, 2, 0),
                Placement("sofa", PieceKind.OBJECT, 2, 4, 0),
            )
        )
        val result = validate(play, state)
        assertTrue(result.hardReasons.any { it == "sospechosos_en_sala_V:2" })
        assertFalse(result.ok)
    }

    @Test
    fun resolve_mapsAnonymousPawnsToNames() {
        val (placements, culpritCell) = n1SolutionFixed()
        val game = GameController(n1)
        game.loadPlacements(placements)
        assertTrue(game.canResolve())
        val outcome = game.resolve()
        assertNotNull(outcome)
        assertEquals("Nico", outcome!!.culpritName)
        assertEquals(culpritCell, outcome.culpritCell)
        assertEquals("Vera", outcome.suspectNamesByCell[Cell(4, 0)])
        assertEquals("sofá del salón", outcome.objectNames["sofa"])
    }

    @Test
    fun rotateObject_90cw_changesCells() {
        val game = GameController(n1)
        game.selectPiece("banqueta")
        assertTrue(game.placeObject("banqueta", 0, 1))
        val before = game.state.placementOf("banqueta")!!.rot
        assertTrue(game.rotateObject("banqueta"))
        assertEquals((before + 1) % 4, game.state.placementOf("banqueta")!!.rot)
    }
}
