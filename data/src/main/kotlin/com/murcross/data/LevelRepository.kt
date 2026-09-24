package com.murcross.data

import android.content.Context
import com.murcross.domain.Level

class LevelRepository(private val context: Context) {
    /** Order: v3 examples first (priority), then v2 stubs. */
    private val assetFiles = listOf(
        "levels/v3a_mercado.json",
        "levels/v3b_biblioteca.json",
        "levels/n1_cafe.json",
        "levels/n2_atico.json",
    )

    fun listLevels(): List<Level> = assetFiles.mapNotNull { path ->
        runCatching { loadAsset(path) }.getOrNull()
    }

    fun loadById(id: String): Level? = listLevels().find { it.id == id }

    private fun loadAsset(path: String): Level {
        val json = context.assets.open(path).bufferedReader().use { it.readText() }
        return LevelJsonParser.parse(json)
    }
}
