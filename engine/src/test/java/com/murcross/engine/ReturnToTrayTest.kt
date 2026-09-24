package com.murcross.engine

import com.murcross.data.LevelLoader
import com.murcross.domain.model.Cell
import com.murcross.domain.model.PieceKind
import com.murcross.domain.model.Placement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * AC smoke Dani MURCROSS-quitar-pieza.md §6 + undo atómico Remove.
 */
class ReturnToTrayTest {

    private val n1 = LevelLoader.loadBundled("n1_cafe")

    @Test
    fun acQ1_longPress_pawn_returnsSlot_cellsEmpty_noNewX() {
        val game = GameController(n1)
        assertTrue(game.placePawn("pawn_0", 4, 0))
        val xBefore = game.state.marksX.size
        assertTrue(game.longPressCell(4, 0))
        assertNull(game.state.placementOf("pawn_0"))
        assertNull(game.pieceAt(4, 0))
        assertEquals(xBefore, game.state.marksX.size)
        assertFalse(game.hasX(4, 0))
    }

    @Test
    fun acQ2_longPress_object_wholeToTray_keepsRotation() {
        val game = GameController(n1)
        // banqueta is 2-cell; place then rotate on board
        assertTrue(game.placeObject("banqueta", 0, 1))
        assertTrue(game.rotateObject("banqueta"))
        val placed = game.state.placementOf("banqueta")!!
        assertEquals(1, placed.rot)
        // long-press any occupied cell of the object
        assertTrue(game.longPressCell(placed.r, placed.c))
        assertNull(game.state.placementOf("banqueta"))
        assertEquals(1, game.getObjectRot("banqueta"))
    }

    @Test
    fun acQ3_undo_afterRemove_restoresAnchorAndRotation() {
        val game = GameController(n1)
        assertTrue(game.placeObject("banqueta", 0, 1))
        assertTrue(game.rotateObject("banqueta"))
        val before = game.state.placementOf("banqueta")!!
        assertEquals(1, before.rot)
        assertTrue(game.returnToTray("banqueta"))
        assertNull(game.state.placementOf("banqueta"))
        assertTrue(game.undo())
        val after = game.state.placementOf("banqueta")
        assertNotNull(after)
        assertEquals(before.r, after!!.r)
        assertEquals(before.c, after.c)
        assertEquals(before.rot, after.rot)
    }

    @Test
    fun acQ4_remove_withPiecesInTray_resolverDisabled() {
        val game = GameController(n1)
        game.loadPlacements(
            listOf(
                Placement("banqueta", PieceKind.OBJECT, 0, 1, 0),
                Placement("estanteria", PieceKind.OBJECT, 1, 2, 0),
                Placement("sofa", PieceKind.OBJECT, 0, 3, 0),
                Placement("pawn_0", PieceKind.PAWN, 4, 0, 0),
                Placement("pawn_1", PieceKind.PAWN, 4, 3, 0),
            ),
        )
        // may or may not be legal depending on sofa pose; force remove one
        assertTrue(game.returnToTray("pawn_1"))
        assertFalse(game.canResolve())
        assertTrue(game.validation().softReasons.any { it.contains("falta_pieza") })
    }

    @Test
    fun acQ5_acQ8_longPress_keepsModeX_clearsSelection_noRotate() {
        val game = GameController(n1)
        assertTrue(game.placePawn("pawn_0", 4, 0))
        game.setModeX()
        assertTrue(game.modeX)
        assertTrue(game.longPressCell(4, 0))
        assertTrue(game.modeX) // kept
        assertNull(game.selectedId)
        assertNull(game.state.placementOf("pawn_0"))
        assertFalse(game.hasX(4, 0))
    }

    @Test
    fun acQ6_tapTap_fallback_selectThenReturnViaTray() {
        val game = GameController(n1)
        assertTrue(game.placePawn("pawn_0", 4, 0))
        game.tapCell(4, 0) // select on board
        assertEquals("pawn_0", game.selectedId)
        assertTrue(game.returnToTray("pawn_0")) // tray slot fallback
        assertNull(game.state.placementOf("pawn_0"))
        assertNull(game.selectedId)
    }

    @Test
    fun acQ7_longPress_victim_noop() {
        val game = GameController(n1)
        val v = n1.play.victim
        assertFalse(game.longPressCell(v.r, v.c))
        assertTrue(game.isVictim(v.r, v.c))
    }

    @Test
    fun acQ9_longPress_emptyOrX_noop() {
        val game = GameController(n1)
        assertFalse(game.longPressCell(0, 0))
        game.setModeX()
        game.toggleX(2, 2)
        assertTrue(game.hasX(2, 2))
        assertFalse(game.longPressCell(2, 2))
        assertTrue(game.hasX(2, 2))
        assertTrue(game.modeX)
    }

    @Test
    fun remove_isAtomic_doesNotCollapsePriorHistory() {
        val game = GameController(n1)
        assertTrue(game.placePawn("pawn_0", 4, 0))
        assertTrue(game.placePawn("pawn_1", 4, 3))
        assertTrue(game.returnToTray("pawn_0"))
        assertTrue(game.undo()) // undo remove → pawn_0 back
        assertNotNull(game.state.placementOf("pawn_0"))
        assertNotNull(game.state.placementOf("pawn_1"))
        assertTrue(game.undo()) // undo place pawn_1
        assertNull(game.state.placementOf("pawn_1"))
        assertNotNull(game.state.placementOf("pawn_0"))
    }

    @Test
    fun selectPiece_cancelsModeX() {
        val game = GameController(n1)
        game.setModeX()
        assertTrue(game.modeX)
        game.selectPiece("pawn_0")
        assertFalse(game.modeX)
        assertEquals("pawn_0", game.selectedId)
    }
}
