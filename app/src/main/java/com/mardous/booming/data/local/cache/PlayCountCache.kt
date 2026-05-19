package com.mardous.booming.data.local.cache

import com.mardous.booming.core.model.filesystem.FileSystemItem
import com.mardous.booming.data.local.room.PlayCountDao
import com.mardous.booming.data.local.room.PlayCountEntity
import com.mardous.booming.data.model.Album
import com.mardous.booming.data.model.Artist
import com.mardous.booming.data.model.Folder
import com.mardous.booming.data.model.Genre
import com.mardous.booming.data.model.ReleaseYear
import com.mardous.booming.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Locale

class PlayCountCache(playCountDao: PlayCountDao) {

    // Room emits asynchronously, so the cache can be empty during early startup.
    // Koin creates this at app start to make that window unlikely before lists sort.
    @Volatile
    private var byId: Map<Long, Int> = emptyMap()

    @Volatile
    private var byGenreName: Map<String, Int> = emptyMap()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        playCountDao.allPlayCountsFlow()
            .onEach { applyEntries(it) }
            .launchIn(scope)
    }

    private fun applyEntries(entries: List<PlayCountEntity>) {
        byId = entries.associate { it.id to it.playCount }
        byGenreName = entries
            .filter { !it.genreName.isNullOrEmpty() }
            .groupBy { it.genreName!!.normalizeGenreKey() }
            .mapValues { entry -> entry.value.sumOf { it.playCount } }
    }

    fun forSong(id: Long): Int = byId[id] ?: 0

    fun forGenreName(name: String): Int = byGenreName[name.normalizeGenreKey()] ?: 0

    private fun String.normalizeGenreKey(): String = trim().lowercase(Locale.ROOT)
}

private object PlayCountCacheHolder : KoinComponent {
    val cache: PlayCountCache by inject()
}

val Song.playCount: Int
    get() = PlayCountCacheHolder.cache.forSong(id)

val Album.playCount: Int
    get() = songs.sumOf { it.playCount }

val Artist.playCount: Int
    get() = songs.sumOf { it.playCount }

val Genre.playCount: Int
    get() = PlayCountCacheHolder.cache.forGenreName(name)

val ReleaseYear.playCount: Int
    get() = songs.sumOf { it.playCount }

val Folder.playCount: Int
    get() = songs.sumOf { it.playCount }

val FileSystemItem.playCount: Int
    get() = when (this) {
        is Song -> playCount
        is Folder -> playCount
        else -> 0
    }
