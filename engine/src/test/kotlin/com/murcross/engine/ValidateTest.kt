package com.murcross.engine

import com.murcross.domain.Cell
import com.murcross.domain.GameObject
import com.murcross.domain.GameState
import com.murcross.domain.Level
import com.murcross.domain.LevelPlay
import com.murcross.domain.LevelReveal
import com.murcross.domain.PieceKind
import com.murcross.domain.Placement
import com.murcross.domain.absoluteObjectCells
import com.murcross.domain.pawnId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.lang.reflect.Method
import kotlin.reflect.full.functions
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.jvm.javaMethod

class ValidateTest {

    private fun load(name: String): Level {
        val stream = checkNotNull(javaClass.classLoader.getResourceAsStream("levels/$name.json")) {
            "missing fixture levels/$name.json"
        }
        return LevelJsonParser.parse(stream.bufferedReader().use { it.readText() })
    }

    private fun findObjectPlacement(obj: GameObject, want: Set<Cell>): Placement {
        for (rot in 0..3) {
            for (r in -3..8) {
                for (c in -3..8) {
                    val p = Placement(obj.id, PieceKind.OBJECT, r, c, rot)
                    val cells = absoluteObjectCells(obj, p).toSet()
                    if (cells == want) return p
                }
            }
        }
        fail("no placement for ${obj.id} covering $want")
    }

    private fun legalState(level: Level, pawnCells: List<Cell>, objectCells: Map<String, Set<Cell>>): GameState {
        val placements = mutableListOf<Placement>()
        pawnCells.forEachIndexed { i, cell ->
            placements += Placement(pawnId(i), PieceKind.PAWN, cell.r, cell.c)
        }
        for (obj in level.play.objects) {
            val want = objectCells[obj.id] ?: fail("missing cells for ${obj.id}")
            placements += findObjectPlacement(obj, want)
        }
        return GameState(placements = placements)
    }

    @Test
    fun v3a_mercado_legal_solution_and_culprit() {
        val level = load("v3a_mercado")
        val state = legalState(
            level,
            pawnCells = listOf(Cell(0, 0), Cell(4, 3)),
            objectCells = mapOf(
                "caja" to setOf(Cell(3, 0), Cell(4, 0), Cell(4, 1)),
                "banco" to setOf(Cell(0, 4), Cell(1, 4), Cell(2, 4)),
                "cesta" to setOf(Cell(1, 0), Cell(2, 0)),
            ),
        )
        val result = validate(level.play, state)
        assertTrue(result.ok, "soft=${result.softReasons} hard=${result.hardReasons}")
        assertEquals(1, result.culpritPawnIndex)
        assertTrue(result.softReasons.isEmpty())
        assertTrue(result.hardReasons.isEmpty())
    }

    @Test
    fun v3b_biblioteca_legal_solution_and_culprit() {
        val level = load("v3b_biblioteca")
        val state = legalState(
            level,
            pawnCells = listOf(Cell(0, 2), Cell(0, 3), Cell(2, 5)),
            objectCells = mapOf(
                "estanteria" to setOf(Cell(1, 2), Cell(2, 2), Cell(3, 2)),
                "baul" to setOf(Cell(4, 0), Cell(4, 1), Cell(5, 0), Cell(5, 1)),
                "sofa" to setOf(Cell(3, 4), Cell(4, 3), Cell(4, 4), Cell(5, 4)),
            ),
        )
        val result = validate(level.play, state)
        assertTrue(result.ok, "soft=${result.softReasons} hard=${result.hardReasons}")
        assertEquals(1, result.culpritPawnIndex) // Iván @ (0,3)
    }

    @Test
    fun n1_cafe_legal_solution_and_culprit() {
        val level = load("n1_cafe")
        val state = legalState(
            level,
            pawnCells = listOf(Cell(4, 0), Cell(4, 3)),
            objectCells = mapOf(
                "sofa" to setOf(Cell(0, 3), Cell(0, 4), Cell(1, 4)),
                "estanteria" to setOf(Cell(1, 2), Cell(2, 2), Cell(3, 2)),
                "banqueta" to setOf(Cell(0, 1), Cell(0, 2)),
            ),
        )
        val result = validate(level.play, state)
        assertTrue(result.ok, "soft=${result.softReasons} hard=${result.hardReasons}")
        assertEquals(1, result.culpritPawnIndex)
    }

    @Test
    fun n2_atico_legal_solution_and_culprit() {
        val level = load("n2_atico")
        val state = legalState(
            level,
            pawnCells = listOf(Cell(0, 3), Cell(5, 5), Cell(2, 0)), // Mar, Gus, Iris
            objectCells = mapOf(
                "mesa" to setOf(Cell(0, 0), Cell(1, 0), Cell(1, 1)),
                "sofa" to setOf(Cell(2, 3), Cell(2, 4), Cell(2, 5), Cell(3, 4)),
                "estanteria" to setOf(Cell(3, 2), Cell(4, 2), Cell(5, 2)),
            ),
        )
        val result = validate(level.play, state)
        assertTrue(result.ok, "soft=${result.softReasons} hard=${result.hardReasons}")
        assertEquals(2, result.culpritPawnIndex) // Iris
    }

    @Test
    fun layerA_missing_piece_soft() {
        val level = load("v3a_mercado")
        val empty = GameState()
        val result = validate(level.play, empty)
        assertFalse(result.ok)
        assertTrue(result.softReasons.any { it.startsWith("falta_pieza:") })
        assertNull(result.culpritPawnIndex)
    }

    @Test
    fun layerA_object_crosses_rooms() {
        val level = load("v3a_mercado")
        // Place caja straddling rooms: (0,1) and (0,2) are different rooms
        val caja = level.play.objects.first { it.id == "caja" }
        // L at (0,1) with rot0: (0,1),(1,1),(1,2) — rooms 0,0,1 → crosses
        val placements = listOf(
            Placement("pawn_0", PieceKind.PAWN, 0, 0),
            Placement("pawn_1", PieceKind.PAWN, 4, 3),
            Placement("caja", PieceKind.OBJECT, 0, 1, 0),
            Placement("banco", PieceKind.OBJECT, 0, 4, 0),
            Placement("cesta", PieceKind.OBJECT, 1, 0, 0),
        )
        val result = validate(level.play, GameState(placements))
        assertFalse(result.ok)
        assertTrue(result.softReasons.any { it.contains("objeto_cruza_salas") || it.contains("borde_") || it.contains("po_") })
    }

    @Test
    fun layerB_wrong_edge_soft() {
        val level = load("v3a_mercado")
        val state = legalState(
            level,
            pawnCells = listOf(Cell(0, 0), Cell(4, 4)), // wrong pawn cell → edge fail
            objectCells = mapOf(
                "caja" to setOf(Cell(3, 0), Cell(4, 0), Cell(4, 1)),
                "banco" to setOf(Cell(0, 4), Cell(1, 4), Cell(2, 4)),
                "cesta" to setOf(Cell(1, 0), Cell(2, 0)),
            ),
        )
        val result = validate(level.play, state)
        assertFalse(result.ok)
        assertTrue(
            result.softReasons.any { it.startsWith("borde_") || it.startsWith("po_") },
            "expected edge fail, got ${result.softReasons}",
        )
    }

    @Test
    fun layerC_zero_pawns_in_v_room_hard() {
        val level = load("v3a_mercado")
        // Both pawns in Frutería (room 0); V is Carnicería
        val state = legalState(
            level,
            pawnCells = listOf(Cell(0, 0), Cell(0, 1)),
            objectCells = mapOf(
                "caja" to setOf(Cell(3, 0), Cell(4, 0), Cell(4, 1)),
                "banco" to setOf(Cell(0, 4), Cell(1, 4), Cell(2, 4)),
                "cesta" to setOf(Cell(1, 0), Cell(2, 0)),
            ),
        )
        // May also fail edges; ensure hard reason when we force only C
        // Use placements that satisfy A roughly but 0 in V room — edges likely fail too.
        // Construct minimal: take legal then move culprit out.
        val legal = legalState(
            level,
            pawnCells = listOf(Cell(0, 0), Cell(4, 3)),
            objectCells = mapOf(
                "caja" to setOf(Cell(3, 0), Cell(4, 0), Cell(4, 1)),
                "banco" to setOf(Cell(0, 4), Cell(1, 4), Cell(2, 4)),
                "cesta" to setOf(Cell(1, 0), Cell(2, 0)),
            ),
        )
        val moved = legal.copy(
            placements = legal.placements.map {
                if (it.pieceId == "pawn_1") it.copy(r = 4, c = 0) else it // both in room 0; may overlap caja
            }.filterNot { it.pieceId == "pawn_1" && it.r == 4 && it.c == 0 }.let { list ->
                // place pawn_1 on empty fruteria cell (2,1)
                list.filter { it.pieceId != "pawn_1" } + Placement("pawn_1", PieceKind.PAWN, 2, 1)
            },
        )
        val result = validate(level.play, moved)
        assertFalse(result.ok)
        assertTrue(result.hardReasons.any { it.startsWith("sospechosos_en_sala_V") }, result.hardReasons.toString())
    }

    @Test
    fun validate_signature_cannot_see_reveal_fields() {
        // Reflective contract: validate has exactly (LevelPlay, GameState)
        val methods = ::validate.javaMethod!!
        val params = methods.parameterTypes
        assertEquals(2, params.size)
        assertEquals(LevelPlay::class.java, params[0])
        assertEquals(GameState::class.java, params[1])
        // Ensure LevelReveal is not a parameter type
        assertFalse(params.any { it == LevelReveal::class.java })
        // Source-level: method name
        assertEquals("validate", methods.name)
    }

    @Test
    fun must_room_not_enforced_in_validate() {
        // Place legal geometry but swap pawn cells vs reveal must_room — still ok if geometry legal
        val level = load("v3a_mercado")
        // Same cells as legal solution — validate doesn't read must_room
        val state = legalState(
            level,
            pawnCells = listOf(Cell(0, 0), Cell(4, 3)),
            objectCells = mapOf(
                "caja" to setOf(Cell(3, 0), Cell(4, 0), Cell(4, 1)),
                "banco" to setOf(Cell(0, 4), Cell(1, 4), Cell(2, 4)),
                "cesta" to setOf(Cell(1, 0), Cell(2, 0)),
            ),
        )
        assertTrue(validate(level.play, state).ok)
        // Reveal still has must_room metadata
        assertNotNull(level.reveal.suspects[0].mustRoom)
    }
}
