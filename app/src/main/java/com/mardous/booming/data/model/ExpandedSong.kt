package com.mardous.booming.data.model

import kotlinx.parcelize.Parcelize

@Parcelize
class ExpandedSong(
    override val id: Long,
    override val data: String,
    override val title: String,
    override val trackNumber: Int,
    override val year: Int,
    override val size: Long,
    override val duration: Long,
    override val dateAdded: Long,
    override val dateModified: Long,
    override val albumId: Long,
    override val albumName: String,
    override val artistId: Long,
    override val artistName: String,
    override val albumArtistName: String?,
    override val genreName: String?,
    val playCount: Int,
    val skipCount: Int,
    val lastPlayedAt: Long,
    val isFavorite: Boolean
) : Song(
    id,
    data,
    title,
    trackNumber,
    year,
    size,
    duration,
    dateAdded,
    dateModified,
    albumId,
    albumName,
    artistId,
    artistName,
    albumArtistName,
    genreName
) {

    constructor(
        song: Song,
        playCount: Int,
        skipCount: Int,
        lastPlayedAt: Long,
        isFavorite: Boolean
    ) : this(
        song.id,
        song.data,
        song.title,
        song.trackNumber,
        song.year,
        song.size,
        song.duration,
        song.dateAdded,
        song.dateModified,
        song.albumId,
        song.albumName,
        song.artistId,
        song.artistName,
        song.albumArtistName,
        song.genreName,
        playCount,
        skipCount,
        lastPlayedAt,
        isFavorite
    )
}