package com.murcross.domain

/** Contrato v0 — sin dependencias Android. */

data class Cell(val r: Int, val c: Int)

data class RelCell(val dr: Int, val dc: Int)

data class EdgeSegment(val roomId: Int, val count: Int)

data class EdgeLine(
    val segments: List<EdgeSegment>,
    val people: Int,
    val objects: Int,
)

data class EdgeHints(val rows: List<EdgeLine>, val cols: List<EdgeLine>)

data class RoomMeta(val id: Int, val name: String, val short: String, val token: String? = null)

/** Solo geometría de partida. Sin nombres de sospechosos. */
data class LevelPlay(
    val size: Int,
    val rooms: List<List<Int>>,
    val roomMeta: List<RoomMeta> = emptyList(),
    val victim: Cell,
    val pawnCount: Int,
    val objects: List<GameObject>,
    val edge: EdgeHints,
)

data class GameObject(
    val id: String,
    val shapeId: String,
    val cells: List<RelCell>,
)

/** Solo reveal. El validador de partida no lo lee. */
data class LevelReveal(
    val suspects: List<SuspectReveal>,
    val objectNames: Map<String, String>,
    val culpritPawnIndex: Int? = null,
)

data class SuspectReveal(
    val pawnIndex: Int,
    val name: String,
    val cell: Cell,
    /** Metadato reveal-only. Nunca entra en validate(). */
    val mustRoom: Int? = null,
    val trait: String? = null,
    /** Rol genérico si no hay ancla (V3-6). */
    val role: String? = null,
)

data class Level(
    val schemaVersion: Int,
    val id: String,
    val title: String? = null,
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
)

data class ValidationResult(
    val ok: Boolean,
    val softReasons: List<String> = emptyList(),
    val hardReasons: List<String> = emptyList(),
    /** Índice de peón anónimo en sala de V cuando ok; nombres vía LevelReveal. */
    val culpritPawnIndex: Int? = null,
)
