package com.murcross.data

import com.murcross.domain.model.Cell
import com.murcross.domain.model.EdgeHints
import com.murcross.domain.model.EdgeLine
import com.murcross.domain.model.EdgeSegment
import com.murcross.domain.model.GameObject
import com.murcross.domain.model.Level
import com.murcross.domain.model.LevelPlay
import com.murcross.domain.model.LevelReveal
import com.murcross.domain.model.RelCell
import com.murcross.domain.model.RevealSuspect
import com.murcross.domain.model.RoomMeta
import kotlinx.serialization.json.Json
import java.io.InputStream

object LevelLoader {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun fromJson(text: String): Level = toDomain(json.decodeFromString(LevelDto.serializer(), text))

    fun fromStream(stream: InputStream): Level =
        fromJson(stream.bufferedReader().use { it.readText() })

    /** Carga desde classpath: `/levels/<id>.json`. */
    fun fromClasspath(resourcePath: String): Level {
        val stream = LevelLoader::class.java.classLoader.getResourceAsStream(resourcePath)
            ?: error("Level resource not found: $resourcePath")
        return fromStream(stream)
    }

    fun listBundledIds(): List<String> = listOf("n1_cafe", "n2_atico")

    fun loadBundled(id: String): Level = fromClasspath("levels/$id.json")

    private fun toDomain(dto: LevelDto): Level {
        require(dto.schemaVersion == 1) { "Unsupported schemaVersion=${dto.schemaVersion}" }
        val play = dto.play
        require(play.rooms.size == play.size) { "rooms rows != size" }
        require(play.rooms.all { it.size == play.size }) { "rooms not square" }
        require(play.edge.rows.size == play.size) { "edge.rows != size" }
        require(play.edge.cols.size == play.size) { "edge.cols != size" }

        return Level(
            schemaVersion = dto.schemaVersion,
            id = dto.id,
            title = dto.title,
            play = LevelPlay(
                size = play.size,
                rooms = play.rooms,
                roomMeta = play.roomMeta.map {
                    RoomMeta(it.id, it.name, it.short, it.token)
                },
                victim = Cell(play.victim.r, play.victim.c),
                pawnCount = play.pawns.count,
                objects = play.objects.map {
                    GameObject(
                        id = it.id,
                        shapeId = it.shapeId,
                        cells = it.cells.map { c -> RelCell(c.dr, c.dc) },
                    )
                },
                edge = EdgeHints(
                    rows = play.edge.rows.map { line ->
                        EdgeLine(
                            segments = line.segments.map { EdgeSegment(it.roomId, it.count) },
                            people = line.people,
                            objects = line.objects,
                        )
                    },
                    cols = play.edge.cols.map { line ->
                        EdgeLine(
                            segments = line.segments.map { EdgeSegment(it.roomId, it.count) },
                            people = line.people,
                            objects = line.objects,
                        )
                    },
                ),
            ),
            reveal = LevelReveal(
                suspects = dto.reveal.suspects.map {
                    RevealSuspect(
                        pawnIndex = it.pawnIndex,
                        name = it.name,
                        cell = Cell(it.cell.r, it.cell.c),
                        mustRoom = it.mustRoom,
                    )
                },
                objectNames = dto.reveal.objects,
                culpritPawnIndex = dto.reveal.culpritPawnIndex,
            ),
        )
    }
}
