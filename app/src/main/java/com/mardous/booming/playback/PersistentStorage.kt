package com.mardous.booming.playback

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import com.mardous.booming.data.local.repository.Repository
import com.mardous.booming.data.local.room.QueueDao
import com.mardous.booming.data.local.room.QueueEntity
import kotlinx.coroutines.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

typealias RestorationListener = (MediaSession.MediaItemsWithStartPosition) -> Unit

@UnstableApi
@OptIn(ExperimentalAtomicApi::class)
class PersistentStorage(
    context: Context,
    private val coroutineScope: CoroutineScope,
    private val player: AdvancedForwardingPlayer
) : KoinComponent {

    // Synchronization lock for listener sets
    private val lock = Any()

    private val queueDao: QueueDao by inject()
    private val repository: Repository by inject()
    private val preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)

    // Tracks the latest save operation to prevent overlapping writes
    private var saveJob: Job? = null

    private val simpleListeners = LinkedHashSet<Runnable>()
    private val mediaItemsListeners = LinkedHashSet<RestorationListener>()

    // Atomic reference for restoration state management
    private var state = AtomicReference(RestorationState.Awaiting)
    val restorationState get() = state.load()

    /**
     * Represents the internal state of playback restoration.
     */
    enum class RestorationState(val isRestored: Boolean = false) {
        Awaiting(false),   // Not started yet
        Restoring(false),  // Currently restoring
        Restored(true)     // Completed (either failed or successful)
    }

    /**
     * Adds a listener that will run once restoration is completed.
     * If restoration is already done, the listener runs immediately.
     */
    fun waitForRestoration(listener: Runnable) {
        if (restorationState.isRestored) {
            listener.run()
        } else {
            synchronized(lock) {
                simpleListeners.add(listener)
            }
        }
    }

    /**
     * Adds a listener that will receive the restored media items once available.
     * Ignored if restoration has already completed.
     */
    fun waitForMediaItems(listener: RestorationListener) {
        if (!restorationState.isRestored) {
            synchronized(lock) {
                mediaItemsListeners.add(listener)
            }
        }
    }

    /**
     * Restores the last known player state:
     * - Reads saved playlist and playback state from disk.
     * - Reconstructs MediaItems using Repository.
     * - Applies repeat/shuffle modes and playback position.
     * - Notifies all waiting listeners once complete.
     */
    fun restoreState(
        callback: (MediaSession.MediaItemsWithStartPosition) -> Unit
    ) = coroutineScope.launch(Dispatchers.IO) {
        try {
            // Ensure player is empty before restoring
            val emptyTimeline = withContext(Dispatchers.Main) { player.currentTimeline.isEmpty }

            // Only one restoration allowed
            val movedToRestoringState = state.compareAndSet(RestorationState.Awaiting, RestorationState.Restoring)
            if (movedToRestoringState && emptyTimeline) {
                // Load saved queue from database
                val savedMediaItems = queueDao.savedItems().map {
                    MediaItem.Builder()
                        .setMediaId(it.id)
                        .build()
                }

                // Resolve valid items from repository
                val (restoredMediaItems) = repository.songsByMediaItems(savedMediaItems)
                    .let { (songs, missingMediaItems) ->
                        songs.toMediaItems() to missingMediaItems
                    }

                // Build session state object
                val items = if (restoredMediaItems.isNotEmpty()) {
                    var startPosition = preferences.getInt(LAST_INDEX, C.INDEX_UNSET)
                    val startPositionMs = preferences.getLong(POSITION_IN_TRACK, C.TIME_UNSET)

                    // Validate index if playlist changed
                    if (restoredMediaItems.size != savedMediaItems.size) {
                        val savedLastMediaItem = savedMediaItems.getOrNull(startPosition)
                        if (savedLastMediaItem == null) {
                            startPosition = 0
                        } else {
                            val restoredLastMediaItem = restoredMediaItems.getOrNull(startPosition)
                            if (restoredLastMediaItem == null ||
                                restoredLastMediaItem.mediaId != savedLastMediaItem.mediaId
                            ) {
                                startPosition = restoredMediaItems.indexOfFirst {
                                    it.mediaId == savedLastMediaItem.mediaId
                                }
                            }
                        }
                        startPosition = startPosition.coerceIn(0, restoredMediaItems.lastIndex)
                    }

                    MediaSession.MediaItemsWithStartPosition(
                        restoredMediaItems,
                        startPosition,
                        startPositionMs
                    )
                } else {
                    // No items found, return an empty session state
                    MediaSession.MediaItemsWithStartPosition(
                        emptyList(),
                        C.INDEX_UNSET,
                        C.TIME_UNSET
                    )
                }

                withContext(Dispatchers.Main) {
                    // Apply repeat/shuffle modes on main thread
                    player.repeatMode = preferences.getInt(REPEAT_MODE, Player.REPEAT_MODE_OFF)
                    player.shuffleModeEnabled = preferences.getBoolean(SHUFFLE_MODE, false)

                    synchronized(lock) {
                        if (!mediaItemsListeners.contains(callback)) {
                            callback(items)
                        }

                        mediaItemsListeners.forEach { it(items) }
                        mediaItemsListeners.clear()

                        simpleListeners.forEach { it.run() }
                        simpleListeners.clear()
                    }
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "State restoration failed", t)
        } finally {
            // Always mark as restored to unblock future listeners
            state.store(RestorationState.Restored)
        }
    }

    /**
     * Saves the current playback state to disk.
     * Optionally persists the playlist order when [savePlaylist] is true.
     *
     * This operation is debounced to avoid writing too frequently.
     */
    fun saveState(savePlaylist: Boolean = false) {
        saveJob?.cancel()
        saveJob = coroutineScope.launch {
            // Avoid writing while restoring
            if (restorationState == RestorationState.Restoring)
                return@launch

            try {
                // Small delay to prevent race conditions (e.g. fast track changes)
                delay(500)

                // Read player state (on main thread)
                val repeatMode = player.repeatMode
                val shuffleModeEnabled = player.shuffleModeEnabled
                val position = player.currentMediaItemIndex
                val positionInTrack = player.currentPosition
                val mediaItems = player.mediaItems

                // Write state asynchronously
                withContext(Dispatchers.IO) {
                    preferences.edit(commit = true) {
                        putInt(REPEAT_MODE, repeatMode)
                        putBoolean(SHUFFLE_MODE, shuffleModeEnabled)
                        putInt(LAST_INDEX, position)
                        putLong(POSITION_IN_TRACK, positionInTrack)
                    }

                    // Optionally save playlist order
                    if (savePlaylist) {
                        val queueItems = mediaItems.mapIndexed { index, item ->
                            QueueEntity(id = item.mediaId, order = index)
                        }
                        if (isActive) {
                            queueDao.replaceQueue(queueItems)
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to write current state to disk", e)
            }
        }
    }

    companion object {
        private const val TAG = "PersistentStorage"
        private const val PREFERENCE_NAME = "playback_state"

        const val REPEAT_MODE = "repeat_mode"
        const val SHUFFLE_MODE = "shuffle_mode"
        const val SHUFFLE_ORDER = "shuffle_order"
        const val POSITION_IN_TRACK = "position_in_track"
        const val LAST_INDEX = "last_index"
    }
}
