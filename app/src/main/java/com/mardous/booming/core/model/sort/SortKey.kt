package com.mardous.booming.core.model.sort

enum class SortKey(val value: String) {
    Name("az_key"), // keep user preference, we can clean this up at some point, but im not gonna be making that call :D
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