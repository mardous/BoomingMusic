package com.mardous.booming.playback.shuffle

import android.util.Log
import com.mardous.booming.core.model.shuffle.GroupShuffleMode
import com.mardous.booming.core.model.shuffle.SpecialShuffleMode
import com.mardous.booming.core.sort.SongSortMode
import com.mardous.booming.data.SongProvider
import com.mardous.booming.data.local.repository.Repository
import com.mardous.booming.data.model.ExpandedSong
import com.mardous.booming.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.math.ln

class ShuffleManager : KoinComponent {

    private val repository: Repository by inject()

    suspend fun applySmartShuffle(songs: List<Song>, mode: SpecialShuffleMode): List<Song> {
        if (songs.isEmpty()) return emptyList()

        val expandedSongs = songs.expandSongsBatch()
        val now = System.currentTimeMillis()

        return try {
            val shuffledExpanded = when (mode) {
                SpecialShuffleMode.PureRandom -> expandedSongs.shuffled()

                SpecialShuffleMode.MostPlayed -> weightedShuffle(expandedSongs) {
                    ln(it.playCount.toDouble() + 1.0) + 1.0
                }

                SpecialShuffleMode.MostPlayedArtists -> {
                    val artistWeights = expandedSongs
                        .groupBy { it.albumArtistName ?: it.artistId }
                        .mapValues { entry ->
                            ln(entry.value.sumOf { s -> s.playCount }.toDouble() + 1.0)
                        }

                    weightedShuffle(expandedSongs) { song ->
                        val key = song.albumArtistName ?: song.artistId
                        1.0 + (artistWeights[key] ?: 0.0)
                    }
                }

                SpecialShuffleMode.MostPlayedAlbums -> {
                    val albumWeights = expandedSongs
                        .groupBy { it.albumId }
                        .mapValues { entry ->
                            ln(entry.value.sumOf { s -> s.playCount }.toDouble() + 1.0)
                        }

                    weightedShuffle(expandedSongs) { song ->
                        1.0 + (albumWeights[song.albumId] ?: 0.0)
                    }
                }

                SpecialShuffleMode.FavoriteSongs -> weightedShuffle(expandedSongs) { song ->
                    if (song.isFavorite) 6.0 else 1.0
                }

                SpecialShuffleMode.ForgottenSongs -> weightedShuffle(expandedSongs) { song ->
                    val days = ((now - song.lastPlayedAt).coerceAtLeast(0L)) / MILLIS_IN_DAY
                    ln(days + 1.0).coerceAtLeast(0.1)
                }

                SpecialShuffleMode.RecentlyAdded -> weightedShuffle(expandedSongs) { song ->
                    val daysOld = ((now - song.dateAdded).coerceAtLeast(0L)) / MILLIS_IN_DAY
                    val weight = 10.0 / (daysOld + 1.0)
                    weight.coerceAtLeast(0.1)
                }

                SpecialShuffleMode.Combined -> calculateCombinedShuffle(expandedSongs, now)
            }

            shuffledExpanded.map { it }
        } catch (e: Exception) {
            Log.e("ShuffleManager", "applySmartShuffle() failed in mode: $mode", e)
            songs.shuffled()
        }
    }

    private fun calculateCombinedShuffle(items: List<ExpandedSong>, now: Long): List<ExpandedSong> {
        val maxPlayCount = items.maxOfOrNull { it.playCount }?.coerceAtLeast(1) ?: 1
        val maxDaysSincePlayed = items.maxOfOrNull { now - it.lastPlayedAt }?.coerceAtLeast(1L) ?: 1L
        val maxAge = items.maxOfOrNull { now - it.dateAdded }?.coerceAtLeast(1L) ?: 1L

        return weightedShuffle(items) { song ->
            val playCountScore = song.playCount.toDouble() / maxPlayCount.toDouble()

            val daysSincePlayed = (now - song.lastPlayedAt).coerceAtLeast(0L)
            val forgottenScore = daysSincePlayed.toDouble() / maxDaysSincePlayed.toDouble()

            val age = (now - song.dateAdded).coerceAtLeast(0L)
            val recencyScore = 1.0 - (age.toDouble() / maxAge.toDouble())

            val favMultiplier = if (song.isFavorite) FAVORITE_MULTIPLIER else 1.0
            val finalWeight = (
                    (WEIGHT_PLAY_COUNT * playCountScore) +
                            (WEIGHT_FORGOTTENNESS * forgottenScore) +
                            (WEIGHT_RECENCY * recencyScore)
                    ) * favMultiplier

            finalWeight.coerceAtLeast(0.01)
        }
    }

    private suspend fun List<Song>.expandSongsBatch(): List<ExpandedSong> {
        val playCountsMap = repository.findSongsInPlayCount(this).associateBy { it.id }
        val favorites = repository.findSongsInFavorites(this).map { it.id }.toSet()
        return this.map { song ->
            val stats = playCountsMap[song.id]
            ExpandedSong(
                song = song,
                playCount = stats?.playCount ?: 0,
                skipCount = stats?.skipCount ?: 0,
                lastPlayedAt = stats?.timePlayed ?: 0L,
                isFavorite = favorites.contains(song.id)
            )
        }
    }

    private fun <T> weightedShuffle(items: List<T>, weightFunc: (T) -> Double): List<T> {
        if (items.isEmpty()) return emptyList()

        val rng = java.util.Random(System.nanoTime())

        return items
            .map { item ->
                val weight = weightFunc(item)
                val safeWeight = if (weight.isFinite() && weight > 0) weight else 1.0

                val r = rng.nextDouble()
                val score = ln(r) / safeWeight

                Pair(item, score)
            }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    suspend fun <T : SongProvider> shuffleByProvider(
        providers: List<T>?,
        mode: GroupShuffleMode,
        sortMode: SongSortMode
    ): List<Song> = withContext(Dispatchers.IO) {
        if (providers.isNullOrEmpty()) {
            emptyList()
        } else {
            val mutableProviders = providers.toMutableList()
            when (mode) {
                GroupShuffleMode.ByGroup -> {
                    mutableProviders.shuffle()
                    mutableProviders.flatMap { group ->
                        with(sortMode) { group.songs.sorted() }
                    }
                }

                GroupShuffleMode.BySong -> {
                    mutableProviders.flatMap { it.songs.shuffled() }
                }

                GroupShuffleMode.FullRandom -> {
                    mutableProviders.shuffle()
                    mutableProviders.flatMap { it.songs.shuffled() }
                }
            }
        }
    }

    companion object {
        private const val MILLIS_IN_DAY = 86400000.0
        private const val WEIGHT_PLAY_COUNT = 0.4
        private const val WEIGHT_FORGOTTENNESS = 0.3
        private const val WEIGHT_RECENCY = 0.3
        private const val FAVORITE_MULTIPLIER = 1.2
    }
}