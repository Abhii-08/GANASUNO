package com.abhi.ganasuno.data

object Shuffler {
    /** Random order, but never two songs of the same artist back to back (when it can be avoided). */
    fun smart(list: List<Song>): List<Song> {
        val rest = list.shuffled().toMutableList()
        val out = ArrayList<Song>(rest.size)
        while (rest.isNotEmpty()) {
            val lastArtist = out.lastOrNull()?.artist
            var idx = rest.indexOfFirst { it.artist != lastArtist }
            if (idx < 0) idx = 0
            out.add(rest.removeAt(idx))
        }
        return out
    }
}
