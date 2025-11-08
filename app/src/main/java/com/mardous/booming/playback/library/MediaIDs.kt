package com.mardous.booming.playback.library

object MediaIDs {
    const val ROOT = "ROOT"
    const val SONGS = "SONGS"
    const val ALBUMS = "ALBUMS"
    const val ARTISTS = "ARTISTS"
    const val ALBUM_ARTISTS = "ALBUM_ARTISTS"
    const val PLAYLISTS = "PLAYLISTS"
    const val GENRES = "GENRES"
    const val TOP_TRACKS = "TOP_TRACKS"
    const val LAST_ADDED = "LAST_ADDED"
    const val RECENT_SONGS = "HISTORY"
    const val FAVORITES = "FAVORITES"

    private const val SEPARATOR = ":"

    fun getPathId(parentId: String, mediaId: Long) = getPathId(parentId, mediaId.toString())

    fun getPathId(parentId: String, mediaId: String) = parentId + SEPARATOR + mediaId

    fun splitPath(pathId: String) = pathId.split(SEPARATOR)

    fun isPath(id: String) = id.split(SEPARATOR).size > 1
}