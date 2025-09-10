package com.mardous.booming.coil

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.edit
import coil3.SingletonImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.toBitmap
import com.mardous.booming.coil.model.ArtistImage
import com.mardous.booming.data.model.Artist
import com.mardous.booming.extensions.resources.toJPG
import com.mardous.booming.util.FileUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class CustomArtistImageManager(private val context: Context) {

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

    suspend fun setCustomImage(artist: Artist, uri: Uri) {
        val request = ImageRequest.Builder(context)
            .data(uri)
            .diskCachePolicy(CachePolicy.DISABLED)
            .memoryCachePolicy(CachePolicy.DISABLED)
            .size(2048)
            .target(
                onSuccess = { drawable ->
                    MainScope().launch {
                        withContext(Dispatchers.IO) {
                            val result = runCatching {
                                getCustomImageFile(artist)
                                    ?.outputStream()
                                    ?.buffered()
                                    ?.use { stream ->
                                        drawable.toBitmap().toJPG(100, stream)
                                    }
                            }
                            if (result.getOrDefault(false) == true) {
                                artist.updateHasImage(true)
                                contentResolver.notifyChange(
                                    MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                                    null
                                )
                            }
                        }
                    }
                }
            )
            .build()

        SingletonImageLoader.get(context).execute(request)
    }

    suspend fun removeCustomImage(artist: Artist) {
        withContext(Dispatchers.IO) {
            artist.updateHasImage(false)

            // trigger media store changed to force artist image reload
            contentResolver.notifyChange(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, null)

            getCustomImageFile(artist)?.let { file ->
                if (file.exists()) {
                    file.delete()
                }
            }
        }
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
}