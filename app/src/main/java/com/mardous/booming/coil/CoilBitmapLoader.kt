package com.mardous.booming.coil

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.Uri
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.toBitmap
import com.google.common.util.concurrent.ListenableFuture
import com.mardous.booming.coil.model.AudioCover
import com.mardous.booming.util.IGNORE_MEDIA_STORE
import com.mardous.booming.util.USE_FOLDER_ART

@UnstableApi
class CoilBitmapLoader(private val context: Context, private val preferences: SharedPreferences) :
    BitmapLoader {

    override fun supportsMimeType(mimeType: String): Boolean {
        return Util.isBitmapFactorySupportedMimeType(mimeType)
    }

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> {
        throw UnsupportedOperationException("Cannot decode Bitmap using ByteArray")
    }

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
        return CallbackToFutureAdapter.getFuture { completer ->
            SingletonImageLoader.get(context).enqueue(
                ImageRequest.Builder(context)
                    .data(
                        AudioCover(
                            uri = uri,
                            isIgnoreMediaStore = preferences.getBoolean(IGNORE_MEDIA_STORE, true),
                            isUseFolderArt = preferences.getBoolean(USE_FOLDER_ART, false)
                        )
                    )
                    .target(
                        onError = {
                            completer.setException(
                                Exception("Coil failed to load the image")
                            )
                        },
                        onSuccess = { completer.set(it.toBitmap()) },
                    )
                    .build()
            ).also {
                completer.addCancellationListener(
                    { it.dispose() },
                    ContextCompat.getMainExecutor(context)
                )
            }
        }
    }

    override fun loadBitmapFromMetadata(metadata: MediaMetadata): ListenableFuture<Bitmap>? {
        return loadBitmap(metadata.artworkUri ?: Uri.EMPTY)
    }
}