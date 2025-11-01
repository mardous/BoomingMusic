package com.mardous.booming.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.mardous.booming.data.model.Song
import com.mardous.booming.extensions.resources.toJPG
import com.mardous.booming.util.FileUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

/**
 * @author Christians M. A. (mardous)
 */
class AlbumCoverSaver(
    private val context: Context,
    private val mediaStoreWriter: MediaStoreWriter
) {

    private val artworkFileName: String
        get() = "Artwork_${System.currentTimeMillis()}.jpg"

    suspend fun saveArtwork(song: Song) = withContext(Dispatchers.IO) {
        val bitmap = extractBitmap(song)
        if (bitmap == null) {
            null
        } else {
            saveBitmap(bitmap) ?: if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                val savedFile = saveBitmapAsFile(bitmap)
                if (savedFile != null) {
                    val scannedUri = runCatching {
                        suspendCancellableCoroutine { continuation ->
                            MediaScannerConnection.scanFile(
                                context,
                                arrayOf(savedFile.absolutePath),
                                arrayOf(ARTWORK_MIME_TYPE)
                            ) { _, uri ->
                                continuation.resume(uri)
                            }
                        }
                    }
                    scannedUri.getOrNull()
                } else {
                    null
                }
            } else {
                null
            }
        }
    }

    private fun extractBitmap(song: Song): Bitmap? {
        if (song.id == Song.Companion.emptySong.id)
            return null

        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, song.uri)
            val bytes = retriever.embeddedPicture
            bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid URI or inaccessible mediaStoreUri: ${song.uri}", e)
            null
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing permission to access mediaStoreUri: ${song.uri}", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract bitmap from ${song.uri}", e)
            null
        } finally {
            retriever.release()
        }
    }

    private fun saveBitmap(bitmap: Bitmap): Uri? {
        val request = MediaStoreWriter.Request.forImage(
            displayName = artworkFileName,
            subFolder = FileUtil.BOOMING_ARTWORK_DIRECTORY_NAME,
            imageMimeType = ARTWORK_MIME_TYPE
        )
        val result = mediaStoreWriter.toMediaStore(
            contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            request = request
        ) { outputStream ->
            bitmap.toJPG(stream = outputStream)
        }
        if (result.resultCode == MediaStoreWriter.Result.Code.SUCCESS) {
            return result.uri
        }
        return null
    }

    private fun saveBitmapAsFile(bitmap: Bitmap): File? {
        return mediaStoreWriter.toFile(
            directory = FileUtil.imagesDirectory(FileUtil.BOOMING_ARTWORK_DIRECTORY_NAME),
            fileName = artworkFileName
        ) { outputStream ->
            bitmap.toJPG(stream = outputStream)
        }
    }

    companion object {
        private const val TAG = "AlbumCoverSaver"
        private const val ARTWORK_MIME_TYPE = "image/jpeg"
    }
}