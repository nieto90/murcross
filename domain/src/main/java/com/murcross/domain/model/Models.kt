package com.murcross.domain.model

/** Celda 0-index (fila, columna). */
data class Cell(val r: Int, val c: Int)

/** Offset relativo de poliminó. */
data class RelCell(val dr: Int, val dc: Int)

data class RoomMeta(
    val id: Int,
    val name: String,
    val short: String,
    val token: String = "room-$id",
)

data class EdgeSegment(val roomId: Int, val count: Int)

data class EdgeLine(
    val segments: List<EdgeSegment>,
    val people: Int,
    val objects: Int,
)

data class EdgeHints(
    val rows: List<EdgeLine>,
    val cols: List<EdgeLine>,
)

/** Objeto de bandeja (forma base; rotación en Placement). */
data class GameObject(
    val id: String,
    val shapeId: String,
    val cells: List<RelCell>,
)

/**
 * Datos de partida (sin identidad). Peones anónimos.
 * Nombres / must_room viven solo en [LevelReveal].
 */
data class LevelPlay(
    val size: Int,
    val rooms: List<List<Int>>,
    val roomMeta: List<RoomMeta>,
    val victim: Cell,
    val pawnCount: Int,
    val objects: List<GameObject>,
    val edge: EdgeHints,
) {
    fun roomOf(r: Int, c: Int): Int = rooms[r][c]
    fun victimRoom(): Int = roomOf(victim.r, victim.c)
    fun inBounds(r: Int, c: Int): Boolean = r in 0 until size && c in 0 until size
    fun pawnId(index: Int): String = "pawn_$index"
    fun objectById(id: String): GameObject? = objects.find { it.id == id }
}

data class RevealSuspect(
    val pawnIndex: Int,
    val name: String,
    val cell: Cell,
    val mustRoom: Int? = null,
)

/** Metadatos de reveal — no entran en validate de partida. */
data class LevelReveal(
    val suspects: List<RevealSuspect>,
    val objectNames: Map<String, String>,
    val culpritPawnIndex: Int? = null,
)

data class Level(
    val schemaVersion: Int,
    val id: String,
    val title: String,
    val play: LevelPlay,
    val reveal: LevelReveal,
)

enum class PieceKind { PAWN, OBJECT }

data class Placement(
    val pieceId: String,
    val kind: PieceKind,
    val r: Int,
    val c: Int,
    val rot: Int = 0,
)

data class GameState(
    val placements: List<Placement> = emptyList(),
    val marksX: List<Cell> = emptyList(),
) {
    fun placementOf(pieceId: String): Placement? = placements.find { it.pieceId == pieceId }
}

/** Resultado de validate por capas A/B/C. */
data class ValidationResult(
    val ok: Boolean,
    val softReasons: List<String> = emptyList(),
    val hardReasons: List<String> = emptyList(),
    /** Celda del peón culpable si hard OK. */
    val culpritCell: Cell? = null,
) {
    val allReasons: List<String> get() = softReasons + hardReasons
}

data class RevealOutcome(
    val culpritName: String,
    val culpritCell: Cell,
    val suspectNamesByCell: Map<Cell, String>,
    val objectNames: Map<String, String>,
)
