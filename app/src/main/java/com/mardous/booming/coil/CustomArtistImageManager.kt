package com.mardous.booming.coil

import android.content.Context
import android.net.Uri
import android.provider.MediaStore.Audio.Artists
import android.util.Log
import androidx.core.content.edit
import coil3.SingletonImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.toBitmap
import com.mardous.booming.coil.model.ArtistImage
import com.mardous.booming.data.model.Artist
import com.mardous.booming.extensions.resources.toJPG
import com.mardous.booming.util.FileUtil
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CustomArtistImageManager(private val context: Context) {

    private val coroutineScope = MainScope()
    private val contentResolver get() = context.contentResolver
    private val imagesPreferences by lazy {
        context.getSharedPreferences("custom_artist_images", Context.MODE_PRIVATE)
    }
    private val signaturesPreferences by lazy {
        context.getSharedPreferences("artist_signatures", Context.MODE_PRIVATE)
    }

    // shared prefs saves us many IO operations
    fun hasCustomImage(image: ArtistImage) =
        imagesPreferences.getBoolean(image.getFileName(), false)

    fun getSignature(image: ArtistImage) =
        signaturesPreferences.getLong(image.name, 0).toString()

    fun getCustomImageFile(image: ArtistImage) =
        FileUtil.customArtistImagesDirectory()?.let { dir ->
            File(dir, image.getFileName())
        }

    fun getCustomImageFile(artist: Artist) =
        FileUtil.customArtistImagesDirectory()?.let { dir ->
            File(dir, artist.getFileName())
        }

    suspend fun setCustomImage(artist: Artist, uri: Uri): Boolean {
        return suspendCancellableCoroutine { continuation ->
            SingletonImageLoader.get(context).enqueue(
                ImageRequest.Builder(context)
                    .data(uri)
                    .memoryCachePolicy(CachePolicy.DISABLED)
                    .size(2048)
                    .target(
                        onSuccess = { drawable ->
                            coroutineScope.launch(Dispatchers.IO) {
                                val imageFile = getCustomImageFile(artist)
                                if (imageFile == null) {
                                    continuation.resume(false)
                                } else {
                                    try {
                                        val imageCreated = imageFile.outputStream()
                                            .buffered()
                                            .use { stream ->
                                                drawable.toBitmap().toJPG(100, stream)
                                            }

                                        artist.updateHasImage(imageCreated)
                                        contentResolver.notifyChange(Artists.EXTERNAL_CONTENT_URI, null)

                                        if (!imageCreated) {
                                            imageFile.deleteQuietly()
                                        }

                                        continuation.resume(imageCreated)
                                    } catch (t: Throwable) {
                                        imageFile.deleteQuietly()
                                        continuation.resumeWithException(t)
                                    }
                                }
                                continuation.invokeOnCancellation {
                                    imageFile?.deleteQuietly()
                                }
                            }
                        }
                    )
                    .build()
            )
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

    private fun Artist.updateHasImage(hasImage: Boolean) {
        imagesPreferences.edit(true) {
            putBoolean(getFileName(), hasImage)
        }
        signaturesPreferences.edit(true) {
            putLong(name, System.currentTimeMillis())
        }
    }

    private fun Artist.getFileName(): String {
        return String.format(Locale.US, "#%d#%s.jpeg", id, name)
    }

    private fun ArtistImage.getFileName(): String {
        return String.format(Locale.US, "#%d#%s.jpeg", id, name)
    }

    private fun File.deleteQuietly() = try {
        this.delete()
    } catch (e: IOException) {
        Log.e("CustomArtistImageManager", "Unable to delete file $this", e)
        false
    }
}