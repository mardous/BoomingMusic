package com.mardous.booming.data.model

data class Queue(
    val songs: List<Song>,
    val position: Int,
    val nextPosition: Int
) {
    val currentSong: Song = songs.getOrElse(position) { Song.emptySong }
    val nextSong: Song? = songs.getOrNull(nextPosition)
    val isEmpty: Boolean = songs.isEmpty()
    val isNotEmpty: Boolean = !isEmpty

    private constructor() : this(emptyList(), -1, -1)

    companion object {
        val Empty = Queue()
    }
}