package com.abhi.ganasuno.data

import org.json.JSONArray
import org.json.JSONObject

/** Saves the play queue + position so the app can resume where you left off. */
object QueueStore {
    class Saved(
        val songs: List<Song>,
        val index: Int,
        val positionMs: Long,
        val sources: List<String?> = emptyList(),
    )

    fun save(
        songs: List<Song>,
        index: Int,
        positionMs: Long,
        sources: List<String?> = emptyList(),
    ) {
        val arr = JSONArray()
        songs.forEachIndexed { index, it ->
            arr.put(
                JSONObject()
                    .put("id", it.id)
                    .put("t", it.title)
                    .put("a", it.artist)
                    .put("d", it.duration)
                    .put("th", it.thumb)
                    .put("l", it.live).put("uri", it.localUri).put("album",it.album)
                    .put("source", sources.getOrNull(index)),
            )
        }
        Prefs.queueJson =
            JSONObject()
                .put("i", index)
                .put("p", positionMs)
                .put("s", arr)
                .toString()
    }

    fun load(): Saved? {
        val raw = Prefs.queueJson ?: return null
        return try {
            val root = JSONObject(raw)
            val arr = root.getJSONArray("s")
            val list = ArrayList<Song>()
            val sources = ArrayList<String?>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                sources.add(o.optString("source", "").takeIf { it.isNotBlank() })
                list.add(
                    Song(
                        o.getString("id"),
                        o.optString("t"),
                        o.optString("a"),
                        o.optString("d"),
                        o.optString("th"),
                        o.optBoolean("l", false),
                        o.optString("uri"),
                        o.optString("album"),
                    ),
                )
            }
            if (list.isEmpty()) null else Saved(list, root.optInt("i", 0), root.optLong("p", 0L), sources)
        } catch (e: Exception) {
            null
        }
    }
}
