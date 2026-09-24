package com.murcross.engine

import com.murcross.data.LevelLoader
import com.murcross.domain.model.Cell
import com.murcross.domain.model.GameObject
import com.murcross.domain.model.GameState
import com.murcross.domain.model.Level
import com.murcross.domain.model.PieceKind
import com.murcross.domain.model.Placement
import com.murcross.domain.model.absoluteObjectCells
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidateTest {

    private val n1 = LevelLoader.loadBundled("n1_cafe")
    private val n2 = LevelLoader.loadBundled("n2_atico")

    private fun findObjectPlacement(level: Level, obj: GameObject, want: Set<Cell>): Placement {
        val n = level.play.size
        for (r in 0 until n) for (c in 0 until n) for (rot in 0..3) {
            val cells = absoluteObjectCells(obj, r, c, rot)
            if (cells.any { !level.play.inBounds(it.r, it.c) }) continue
            if (cells.toSet() == want) return Placement(obj.id, PieceKind.OBJECT, r, c, rot)
        }
        error("no placement for ${obj.id} covering $want")
    }

    private fun n1Solution(): Pair<List<Placement>, Cell> {
        val sofa = n1.play.objectById("sofa")!!
        val placements = listOf(
            Placement("banqueta", PieceKind.OBJECT, 0, 1, 0),
            Placement("estanteria", PieceKind.OBJECT, 1, 2, 0),
            findObjectPlacement(n1, sofa, setOf(Cell(0, 3), Cell(0, 4), Cell(1, 4))),
            Placement("pawn_0", PieceKind.PAWN, 4, 0, 0),
            Placement("pawn_1", PieceKind.PAWN, 4, 3, 0),
        )
        return placements to Cell(4, 3)
    }

    private fun n2Solution(): Pair<List<Placement>, Cell> {
        fun find(id: String, want: Set<Cell>) =
            findObjectPlacement(n2, n2.play.objectById(id)!!, want)
        val placements = listOf(
            find("mesa", setOf(Cell(0, 0), Cell(1, 0), Cell(1, 1))),
            find("sofa", setOf(Cell(2, 3), Cell(2, 4), Cell(2, 5), Cell(3, 4))),
            find("estanteria", setOf(Cell(3, 2), Cell(4, 2), Cell(5, 2))),
            Placement("pawn_0", PieceKind.PAWN, 0, 3, 0), // Mar
            Placement("pawn_1", PieceKind.PAWN, 5, 5, 0), // Gus
            Placement("pawn_2", PieceKind.PAWN, 2, 0, 0), // Iris culpable
        )
        return placements to Cell(2, 0)
    }

    @Test
    fun n1_officialSolution_isLegal() {
        val (placements, culprit) = n1Solution()
        val result = validate(n1.play, GameState(placements))
        assertTrue("soft=${result.softReasons} hard=${result.hardReasons}", result.ok)
        assertEquals(culprit, result.culpritCell)
    }

    @Test
    fun n2_officialSolution_isLegal() {
        val (placements, culprit) = n2Solution()
        val result = validate(n2.play, GameState(placements))
        assertTrue("soft=${result.softReasons} hard=${result.hardReasons}", result.ok)
        assertEquals(culprit, result.culpritCell)
    }

    @Test
    fun layerA_missingPiece_illegal() {
        val (placements, _) = n1Solution()
        val incomplete = placements.filter { it.pieceId != "pawn_1" }
        val result = validate(n1.play, GameState(incomplete))
        assertFalse(result.ok)
        assertTrue(result.softReasons.any { it.startsWith("falta_pieza:pawn_1") })
    }

    @Test
    fun layerA_objectCrossesRooms_orFailsPacking() {
        val bad = listOf(
            Placement("banqueta", PieceKind.OBJECT, 0, 2, 0), // spans Barra|Salón
            Placement("estanteria", PieceKind.OBJECT, 1, 2, 0),
            Placement("sofa", PieceKind.OBJECT, 2, 3, 0),
            Placement("pawn_0", PieceKind.PAWN, 4, 0, 0),
            Placement("pawn_1", PieceKind.PAWN, 4, 3, 0),
        )
        val result = validate(n1.play, GameState(bad))
        assertFalse(result.ok)
        assertTrue(result.softReasons.isNotEmpty())
    }

    @Test
    fun layerB_wrongEdge_illegal() {
        val (placements, _) = n1Solution()
        val moved = placements.map {
            if (it.pieceId == "pawn_0") it.copy(r = 3, c = 0) else it
        }
        val result = validate(n1.play, GameState(moved))
        assertFalse(result.ok)
        assertTrue(result.softReasons.any { it.startsWith("borde_") || it.startsWith("po_") })
    }

    @Test
    fun layerC_twoPawnsInVictimRoom_hardFail() {
        val state = GameState(
            placements = listOf(
                Placement("pawn_0", PieceKind.PAWN, 0, 3, 0),
                Placement("pawn_1", PieceKind.PAWN, 1, 3, 0),
                Placement("banqueta", PieceKind.OBJECT, 0, 1, 0),
                Placement("estanteria", PieceKind.OBJECT, 1, 2, 0),
                Placement("sofa", PieceKind.OBJECT, 2, 4, 0),
            )
        )
        val result = validate(n1.play, state)
        assertTrue(result.hardReasons.any { it == "sospechosos_en_sala_V:2" })
        assertFalse(result.ok)
    }

    @Test
    fun resolve_mapsAnonymousPawnsToNames() {
        val (placements, culpritCell) = n1Solution()
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
    fun rotateObject_90cw() {
        val game = GameController(n1)
        game.selectPiece("banqueta")
        assertTrue(game.placeObject("banqueta", 0, 1))
        val before = game.state.placementOf("banqueta")!!.rot
        assertTrue(game.rotateObject("banqueta"))
        assertEquals((before + 1) % 4, game.state.placementOf("banqueta")!!.rot)
    }

    private val v3a = LevelLoader.loadBundled("v3a_mercado")
    private val v3b = LevelLoader.loadBundled("v3b_biblioteca")

    private fun findObj(levelId: String, objId: String, target: Set<Cell>): Placement {
        val level = LevelLoader.loadBundled(levelId)
        val obj = level.play.objectById(objId)!!
        val n = level.play.size
        for (r in -2 until n + 2) for (c in -2 until n + 2) for (rot in 0..3) {
            val cells = com.murcross.domain.model.absoluteObjectCells(obj, r, c, rot)
            if (cells.any { !level.play.inBounds(it.r, it.c) }) continue
            if (cells.toSet() == target) return Placement(objId, PieceKind.OBJECT, r, c, rot)
        }
        error("no placement $objId in $levelId for $target")
    }

    @Test
    fun v3a_mercado_officialSolution_ok() {
        val placements = listOf(
            findObj("v3a_mercado", "caja", setOf(Cell(3, 0), Cell(4, 0), Cell(4, 1))),
            findObj("v3a_mercado", "banco", setOf(Cell(0, 4), Cell(1, 4), Cell(2, 4))),
            findObj("v3a_mercado", "cesta", setOf(Cell(1, 0), Cell(2, 0))),
            Placement("pawn_0", PieceKind.PAWN, 0, 0, 0),
            Placement("pawn_1", PieceKind.PAWN, 4, 3, 0),
        )
        val result = validate(v3a.play, GameState(placements))
        assertTrue(result.softReasons.joinToString(), result.ok)
        assertEquals(Cell(4, 3), result.culpritCell)
    }

    @Test
    fun v3b_biblioteca_officialSolution_ok() {
        val placements = listOf(
            findObj("v3b_biblioteca", "estanteria", setOf(Cell(1, 2), Cell(2, 2), Cell(3, 2))),
            findObj("v3b_biblioteca", "baul", setOf(Cell(4, 0), Cell(4, 1), Cell(5, 0), Cell(5, 1))),
            findObj("v3b_biblioteca", "sofa", setOf(Cell(3, 4), Cell(4, 3), Cell(4, 4), Cell(5, 4))),
            Placement("pawn_0", PieceKind.PAWN, 0, 2, 0),
            Placement("pawn_1", PieceKind.PAWN, 0, 3, 0),
            Placement("pawn_2", PieceKind.PAWN, 2, 5, 0),
        )
        val result = validate(v3b.play, GameState(placements))
        assertTrue(result.softReasons.joinToString(), result.ok)
        assertEquals(Cell(0, 3), result.culpritCell)
    }

    @Test
    fun validate_signature_is_play_and_state_only() {
        val m = Class.forName("com.murcross.engine.ValidateKt")
            .methods.first { it.name == "validate" && it.parameterCount == 2 }
        assertEquals(com.murcross.domain.model.LevelPlay::class.java, m.parameterTypes[0])
        assertEquals(GameState::class.java, m.parameterTypes[1])
        // must_room never consulted: legal geometry still ok with reveal must_room present
        assertNotNull(v3a.reveal.suspects[0].mustRoom)
        val placements = listOf(
            findObj("v3a_mercado", "caja", setOf(Cell(3, 0), Cell(4, 0), Cell(4, 1))),
            findObj("v3a_mercado", "banco", setOf(Cell(0, 4), Cell(1, 4), Cell(2, 4))),
            findObj("v3a_mercado", "cesta", setOf(Cell(1, 0), Cell(2, 0))),
            Placement("pawn_0", PieceKind.PAWN, 0, 0, 0),
            Placement("pawn_1", PieceKind.PAWN, 4, 3, 0),
        )
        assertTrue(validate(v3a.play, GameState(placements)).ok)
    }
}
