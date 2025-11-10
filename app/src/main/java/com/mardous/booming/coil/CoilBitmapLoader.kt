package com.mardous.booming.coil

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.toBitmap
import com.google.common.util.concurrent.ListenableFuture
import com.mardous.booming.R
import com.mardous.booming.coil.model.AudioCover
import com.mardous.booming.extensions.resources.getDrawableCompat
import com.mardous.booming.util.IGNORE_MEDIA_STORE
import com.mardous.booming.util.USE_FOLDER_ART
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.future

@UnstableApi
class CoilBitmapLoader(
    private val scope: CoroutineScope,
    private val context: Context,
    private val preferences: SharedPreferences
) : BitmapLoader {

    override fun supportsMimeType(mimeType: String): Boolean {
        return Util.isBitmapFactorySupportedMimeType(mimeType)
    }

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> {
        return scope.future(Dispatchers.IO) {
            BitmapFactory.decodeByteArray(data, 0, data.size)
                ?: context.getDrawableCompat(R.drawable.default_audio_art)
                    ?.toBitmap(1080, 1080, Bitmap.Config.ARGB_8888)
                ?: error("Failed to decode from compressed binary data")
        }
    }

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
        return CallbackToFutureAdapter.getFuture { completer ->
            SingletonImageLoader.get(context).enqueue(
                ImageRequest.Builder(context)
                    .data(
                        if (uri.toString().contains("/albumart/")) uri else {
                            AudioCover(
                                uri = uri,
                                isIgnoreMediaStore = preferences.getBoolean(IGNORE_MEDIA_STORE, true),
                                isUseFolderArt = preferences.getBoolean(USE_FOLDER_ART, false)
                            )
                        }
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
}