package com.murcross.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LevelDto(
    val schemaVersion: Int,
    val id: String,
    val title: String = "",
    val play: PlayDto,
    val reveal: RevealDto,
)

@Serializable
data class PlayDto(
    val size: Int,
    val rooms: List<List<Int>>,
    val roomMeta: List<RoomMetaDto> = emptyList(),
    val victim: CellDto,
    val pawns: PawnsDto,
    val objects: List<ObjectDto>,
    val edge: EdgeDto,
)

@Serializable
data class PawnsDto(val count: Int)

@Serializable
data class RoomMetaDto(
    val id: Int,
    val name: String,
    val short: String,
    val token: String = "room-$id",
)

@Serializable
data class CellDto(val r: Int, val c: Int)

@Serializable
data class RelCellDto(val dr: Int, val dc: Int)

@Serializable
data class ObjectDto(
    val id: String,
    val shapeId: String,
    val cells: List<RelCellDto>,
)

@Serializable
data class EdgeSegmentDto(val roomId: Int, val count: Int)

@Serializable
data class EdgeLineDto(
    val segments: List<EdgeSegmentDto>,
    val people: Int,
    val objects: Int,
)

@Serializable
data class EdgeDto(
    val rows: List<EdgeLineDto>,
    val cols: List<EdgeLineDto>,
)

@Serializable
data class RevealSuspectDto(
    val pawnIndex: Int,
    val name: String,
    val cell: CellDto,
    @SerialName("must_room") val mustRoom: Int? = null,
)

@Serializable
data class RevealDto(
    val suspects: List<RevealSuspectDto>,
    val objects: Map<String, String>,
    val culpritPawnIndex: Int? = null,
)
