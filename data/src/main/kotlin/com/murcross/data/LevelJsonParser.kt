package com.murcross.data

import com.murcross.domain.Cell
import com.murcross.domain.EdgeHints
import com.murcross.domain.EdgeLine
import com.murcross.domain.EdgeSegment
import com.murcross.domain.GameObject
import com.murcross.domain.Level
import com.murcross.domain.LevelPlay
import com.murcross.domain.LevelReveal
import com.murcross.domain.RelCell
import com.murcross.domain.RoomMeta
import com.murcross.domain.SuspectReveal
import org.json.JSONObject

object LevelJsonParser {
    fun parse(json: String): Level {
        val root = JSONObject(json)
        require(root.getInt("schemaVersion") == 1) { "unsupported schemaVersion" }
        val playObj = root.getJSONObject("play")
        // Spoiler lint: play must not contain identity fields
        require(!playObj.has("must_room")) { "play must not contain must_room" }
        require(!playObj.has("suspects")) { "play must not contain suspects names" }
        require(!playObj.has("suspectNames")) { "play must not contain suspectNames" }

        val size = playObj.getInt("size")
        val roomsArr = playObj.getJSONArray("rooms")
        val rooms = List(roomsArr.length()) { r ->
            val row = roomsArr.getJSONArray(r)
            List(row.length()) { c -> row.getInt(c) }
        }
        val roomMeta = if (playObj.has("roomMeta")) {
            val arr = playObj.getJSONArray("roomMeta")
            List(arr.length()) { i ->
                val m = arr.getJSONObject(i)
                RoomMeta(
                    id = m.getInt("id"),
                    name = m.getString("name"),
                    short = m.getString("short"),
                    token = if (m.has("token") && !m.isNull("token")) m.getString("token") else null,
                )
            }
        } else emptyList()
        val victim = playObj.getJSONObject("victim").let {
            Cell(it.getInt("r"), it.getInt("c"))
        }
        val pawnCount = playObj.getJSONObject("pawns").getInt("count")
        val objectsArr = playObj.getJSONArray("objects")
        val objects = List(objectsArr.length()) { i ->
            val o = objectsArr.getJSONObject(i)
            val cellsArr = o.getJSONArray("cells")
            GameObject(
                id = o.getString("id"),
                shapeId = o.getString("shapeId"),
                cells = List(cellsArr.length()) { j ->
                    val c = cellsArr.getJSONObject(j)
                    RelCell(c.getInt("dr"), c.getInt("dc"))
                },
            )
        }
        val edgeObj = playObj.getJSONObject("edge")
        fun parseLines(key: String): List<EdgeLine> {
            val arr = edgeObj.getJSONArray(key)
            return List(arr.length()) { i ->
                val line = arr.getJSONObject(i)
                val segs = line.getJSONArray("segments")
                EdgeLine(
                    segments = List(segs.length()) { s ->
                        val seg = segs.getJSONObject(s)
                        EdgeSegment(seg.getInt("roomId"), seg.getInt("count"))
                    },
                    people = line.getInt("people"),
                    objects = line.getInt("objects"),
                )
            }
        }
        val play = LevelPlay(
            size = size,
            rooms = rooms,
            roomMeta = roomMeta,
            victim = victim,
            pawnCount = pawnCount,
            objects = objects,
            edge = EdgeHints(rows = parseLines("rows"), cols = parseLines("cols")),
        )

        val revealObj = root.getJSONObject("reveal")
        val suspectsArr = revealObj.getJSONArray("suspects")
        val suspects = List(suspectsArr.length()) { i ->
            val s = suspectsArr.getJSONObject(i)
            val cell = s.getJSONObject("cell")
            SuspectReveal(
                pawnIndex = s.getInt("pawnIndex"),
                name = s.getString("name"),
                cell = Cell(cell.getInt("r"), cell.getInt("c")),
                mustRoom = if (s.has("must_room") && !s.isNull("must_room")) s.getInt("must_room") else null,
                trait = if (s.has("trait") && !s.isNull("trait")) s.getString("trait") else null,
                role = if (s.has("role") && !s.isNull("role")) s.getString("role") else null,
            )
        }
        val objectsMap = mutableMapOf<String, String>()
        val objNames = revealObj.getJSONObject("objects")
        val keys = objNames.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            objectsMap[k] = objNames.getString(k)
        }
        val reveal = LevelReveal(
            suspects = suspects,
            objectNames = objectsMap,
            culpritPawnIndex = if (revealObj.has("culpritPawnIndex") && !revealObj.isNull("culpritPawnIndex")) {
                revealObj.getInt("culpritPawnIndex")
            } else null,
        )
        return Level(
            schemaVersion = root.getInt("schemaVersion"),
            id = root.getString("id"),
            title = if (root.has("title") && !root.isNull("title")) root.getString("title") else null,
            play = play,
            reveal = reveal,
        )
    }
}
