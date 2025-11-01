package com.mardous.booming.core.model.sort

enum class SortKey(val value: String) {
    AZ("az_key"),
    Album("album_key"),
    Artist("artist_key"),
    Duration("duration_key"),
    Track("track_key"),
    Year("year_key"),
    DateAdded("added_key"),
    DateModified("modified_key"),
    SongCount("songs_key"),
    AlbumCount("albums_key"),
    FileName("file_name_key")
}