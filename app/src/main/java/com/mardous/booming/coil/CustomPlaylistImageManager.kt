package com.mardous.booming.coil

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toFile
import androidx.core.net.toUri
import coil3.SingletonImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import com.mardous.booming.extensions.resources.toJPG
import com.mardous.booming.util.FileUtil
import java.io.File

class CustomPlaylistImageManager(private val context: Context) {

    suspend fun createPlaylistImage(selectedUri: String?): Uri? {
        if (selectedUri == null) return null

        val imagesDir = FileUtil.customPlaylistImagesDirectory() ?: return null
        val imageFile = File(imagesDir, "${System.currentTimeMillis()}.jpg")
        if (!imageFile.createNewFile()) return null

        try {
            val result = SingletonImageLoader.get(context)
                .execute(
                    ImageRequest.Builder(context)
                        .data(selectedUri)
                        .memoryCachePolicy(CachePolicy.DISABLED)
                        .size(1080)
                        .build()
                )
            if (result is SuccessResult) {
                val success = imageFile.outputStream()
                    .buffered()
                    .use {
                        result.image.toBitmap().toJPG(100, it)
                    }
                if (success) {
                    return imageFile.toUri()
                } else {
                    imageFile.delete()
                }
            }
        } catch (e: Exception) {
            if (imageFile.exists()) {
                imageFile.delete()
            }
            Log.e(TAG, "Cannot create a custom image file for selectedUri=$selectedUri", e)
        }
        return null
    }

    fun deleteImage(imageUri: Uri): Boolean {
        try {
            val imageFile = imageUri.toFile()
            return imageFile.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Cannot delete custom image: $imageUri", e)
        }
        return false
    }

    companion object {
        private const val TAG = "CustomPlaylistImageManager"
    }
}