package com.mardous.booming.coil

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore.Audio.Artists
import android.util.Log
import androidx.core.content.edit
import coil3.SingletonImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import com.mardous.booming.coil.model.ArtistImage
import com.mardous.booming.data.model.Artist
import com.mardous.booming.extensions.resources.toJPG
import com.mardous.booming.extensions.utilities.sanitize
import com.mardous.booming.util.FileUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.Locale

class CustomArtistImageManager(private val context: Context) {

    private val contentResolver get() = context.contentResolver
    private val imagesPreferences by lazy {
        context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
    }
    private val signaturesPreferences by lazy {
        context.getSharedPreferences("artist_signatures", Context.MODE_PRIVATE)
    }

    // shared prefs saves us many IO operations
    fun hasCustomImage(image: ArtistImage): Boolean {
        if (imagesPreferences.getBoolean(image.getFileName(), false)) return true
        return imagesPreferences.getBoolean(image.getLegacyFileName(), false)
    }

    fun getSignature(image: ArtistImage) =
        signaturesPreferences.getLong(image.name, 0).toString()

    fun getCustomImageFile(image: ArtistImage) =
        FileUtil.customArtistImagesDirectory()?.let { dir ->
            val file = File(dir, image.getFileName())
            if (file.exists()) file else File(dir, image.getLegacyFileName())
        }

    private fun getCustomImageFile(artist: Artist) =
        FileUtil.customArtistImagesDirectory()?.let { dir ->
            val file = File(dir, artist.getFileName())
            if (file.exists()) file else File(dir, artist.getLegacyFileName())
        }

    suspend fun setCustomImage(artist: Artist, uri: Uri): Boolean {
        return try {
            val result = SingletonImageLoader.get(context).execute(
                ImageRequest.Builder(context)
                    .data(uri)
                    .memoryCachePolicy(CachePolicy.DISABLED)
                    .size(MAX_BITMAP_SIZE)
                    .build()
            )
            if (result is SuccessResult) {
                setCustomImage(artist, result.image.toBitmap(MAX_BITMAP_SIZE, MAX_BITMAP_SIZE))
            } else false
        } catch (t: Throwable) {
            Log.e("CustomArtistImageManager", "Cannot set artist image", t)
            false
        }
    }

    private suspend fun setCustomImage(
        artist: Artist,
        bitmap: Bitmap
    ) = withContext(Dispatchers.IO) {
        val imageFile = getCustomImageFile(artist)
        if (imageFile == null) {
            false
        } else try {
            imageFile.outputStream()
                .buffered()
                .use { stream -> bitmap.toJPG(100, stream) }
                .also { imageCreated ->
                    artist.updateHasImage(imageCreated)
                    contentResolver.notifyChange(Artists.EXTERNAL_CONTENT_URI, null)
                    if (!imageCreated) {
                        imageFile.deleteQuietly()
                    }
                }
        } catch (t: Throwable) {
            imageFile.deleteQuietly()
            Log.e("CustomArtistImageManager", "Cannot set artist image", t)
            false
        }
    }

    suspend fun removeCustomImage(artist: Artist): Boolean = withContext(Dispatchers.IO) {
        artist.updateHasImage(false)

        // trigger media store changed to force artist image reload
        contentResolver.notifyChange(Artists.EXTERNAL_CONTENT_URI, null)

        getCustomImageFile(artist)?.let { file ->
            file.exists() && file.deleteQuietly()
        } ?: false
    }

    fun getImageFiles(): List<File> {
        val imagesDir = FileUtil.customArtistImagesDirectory()
        if (imagesDir != null) {
            return imagesDir.listFiles { it.extension == IMAGE_EXTENSION }
                ?.toList() ?: emptyList()
        }
        return emptyList()
    }

    private fun Artist.updateHasImage(hasImage: Boolean) {
        imagesPreferences.edit(true) {
            putBoolean(getFileName(), hasImage)
        }
        signaturesPreferences.edit(true) {
            putLong(name, System.currentTimeMillis())
        }
    }

    private fun Artist.getFileName(): String {
        return "${name}.$IMAGE_EXTENSION".sanitize()
    }

    private fun Artist.getLegacyFileName(): String {
        return String.format(Locale.US, "#%d#%s.$IMAGE_EXTENSION", id, name).sanitize()
    }

    private fun ArtistImage.getFileName(): String {
        return "${name}.$IMAGE_EXTENSION".sanitize()
    }

    private fun ArtistImage.getLegacyFileName(): String {
        return String.format(Locale.US, "#%d#%s.$IMAGE_EXTENSION", id, name).sanitize()
    }

    private fun File.deleteQuietly() = try {
        this.delete()
    } catch (e: IOException) {
        Log.e("CustomArtistImageManager", "Unable to delete file $this", e)
        false
    }

    companion object {
        private const val MAX_BITMAP_SIZE = 2048
        const val PREFERENCE_NAME = "custom_artist_images"
        private const val IMAGE_EXTENSION = "jpeg"
    }
}