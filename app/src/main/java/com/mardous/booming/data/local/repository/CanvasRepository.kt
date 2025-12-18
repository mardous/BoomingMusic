package com.mardous.booming.data.local.repository

import android.content.Context
import android.util.Log
import com.mardous.booming.data.local.room.CanvasDao
import com.mardous.booming.data.local.room.CanvasEntity
import com.mardous.booming.data.remote.canvas.CanvasService
import com.mardous.booming.extensions.isAllowedToDownloadMetadata
import com.mardous.booming.extensions.media.isArtistNameUnknown

interface CanvasRepository {
    suspend fun canvas(id: Long): CanvasEntity?
}

class RealCanvasRepository(
    private val context: Context,
    private val songRepository: SongRepository,
    private val canvasService: CanvasService,
    private val canvasDao: CanvasDao
) : CanvasRepository {

    override suspend fun canvas(id: Long): CanvasEntity? {
        try {
            val song = songRepository.song(id)
            val canvas = canvasDao.getCanvas(song.id)
            val currentTimeMs = System.currentTimeMillis()
            if (canvas == null || (currentTimeMs - canvas.fetchTimeMs) > CANVAS_VALIDITY_MS) {
                canvas?.let { canvasDao.deleteCanvas(it) }

                if (song.isArtistNameUnknown() || !context.isAllowedToDownloadMetadata())
                    return null

                val onlineResult = canvasService.canvas(song.artistName, song.title)
                if (onlineResult.isNotEmpty()) {
                    val data = onlineResult.first()
                    val entity = CanvasEntity(
                        id = song.id,
                        url = data.url,
                        fetchTimeMs = currentTimeMs
                    )
                    canvasDao.insertCanvas(entity)
                    return entity
                }
            }
            return canvas
        } catch (e: Exception) {
            Log.e(TAG, "The canvas data could not be obtained", e)
            return null
        }
    }

    companion object {
        private const val TAG = "CanvasRepository"
        private const val CANVAS_VALIDITY_MS = ((3600 * 24) * 30) * 1000L
    }
}