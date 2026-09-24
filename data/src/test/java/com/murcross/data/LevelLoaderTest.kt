package com.murcross.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelLoaderTest {
    @Test
    fun loadN1_hasCorrectMeta() {
        val level = LevelLoader.loadBundled("n1_cafe")
        assertEquals("n1_cafe", level.id)
        assertEquals(5, level.play.size)
        assertEquals(2, level.play.pawnCount)
        assertEquals(3, level.play.objects.size)
        assertEquals(3, level.play.victim.r)
        assertEquals(4, level.play.victim.c)
        assertEquals(2, level.reveal.suspects.size)
        assertEquals("Nico", level.reveal.suspects.find { it.cell.r == 4 && it.cell.c == 3 }?.name)
    }

    @Test
    fun loadN2_hasCorrectMeta() {
        val level = LevelLoader.loadBundled("n2_atico")
        assertEquals(6, level.play.size)
        assertEquals(3, level.play.pawnCount)
        assertEquals(3, level.play.objects.size)
        assertEquals(3, level.play.victim.r)
        assertEquals(1, level.play.victim.c)
        assertEquals(
            "Iris",
            level.reveal.suspects.find {
                it.cell == com.murcross.domain.model.Cell(2, 0)
            }?.name,
        )
    }

    @Test
    fun bundledIds_matchResources() {
        val ids = LevelLoader.listBundledIds()
        assertTrue("n1_cafe" in ids)
        assertTrue("n2_atico" in ids)
        for (id in ids) {
            val level = LevelLoader.loadBundled(id)
            assertTrue(level.play.size in 5..7)
            assertEquals(level.play.size, level.play.edge.rows.size)
            assertEquals(level.play.size, level.play.edge.cols.size)
        }
    }

    @Test
    fun playHasNoIdentityInObjects() {
        val level = LevelLoader.loadBundled("n1_cafe")
        assertTrue(level.reveal.objectNames.containsKey("sofa"))
        assertTrue(level.play.objects.none { it.id.contains("Vera") })
    }
}
