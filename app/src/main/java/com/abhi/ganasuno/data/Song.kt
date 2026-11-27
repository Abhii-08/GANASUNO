package com.abhi.ganasuno.data

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val duration: String,
    val thumb: String,
    val live: Boolean = false,
    val localUri: String = "",
    val album: String = "",
)
