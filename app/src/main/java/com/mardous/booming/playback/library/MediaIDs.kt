package com.mardous.booming.playback.library

object MediaIDs {
    const val ROOT = "ROOT"
    const val ALBUMS = "ALBUMS"
    const val ARTISTS = "ARTISTS"
    const val ALBUM_ARTISTS = "ALBUM_ARTISTS"
    const val PLAYLISTS = "PLAYLISTS"
    const val GENRES = "GENRES"
    const val TOP_TRACKS = "TOP_TRACKS"
    const val RECENT_SONGS = "HISTORY"

    private const val SEPARATOR = ":"

    fun getPathId(parentId: String, mediaId: Long) = getPathId(parentId, mediaId.toString())

    fun getPathId(parentId: String, mediaId: String) = parentId + SEPARATOR + mediaId

    fun getParentId(path: String): String? {
        return path.split(SEPARATOR, limit = 2).takeIf { it.size == 2 }?.get(0)
    }

    fun getChildId(path: String): String? {
        return path.split(SEPARATOR, limit = 2).takeIf { it.size == 2 }?.get(1)
    }

    fun isPath(id: String) = id.split(SEPARATOR).size == 2
}