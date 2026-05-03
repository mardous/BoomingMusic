package com.mardous.booming.coil

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.core.content.ContextCompat
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import coil3.SingletonImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.toBitmap
import com.google.common.util.concurrent.ListenableFuture

@UnstableApi
class CoilBitmapLoader(
    private val context: Context
) : BitmapLoader {

    override fun supportsMimeType(mimeType: String): Boolean {
        return Util.isBitmapFactorySupportedMimeType(mimeType)
    }

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> {
        return loadImageUsingData(data)
    }

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
        return loadImageUsingData(uri)
    }

    private fun loadImageUsingData(data: Any): ListenableFuture<Bitmap> {
        return CallbackToFutureAdapter.getFuture { completer ->
            SingletonImageLoader.get(context).enqueue(
                ImageRequest.Builder(context)
                    .data(data)
                    .target(
                        onError = {
                            completer.setException(Exception("Coil failed to load the image"))
                        },
                        onSuccess = {
                            completer.set(it.toBitmap())
                        }
                    )
                    .memoryCachePolicy(CachePolicy.DISABLED)
                    .size(MAX_BITMAP_SIZE)
                    .build()
            ).also {
                completer.addCancellationListener(
                    { it.dispose() },
                    ContextCompat.getMainExecutor(context)
                )
            }
        }
    }

    companion object {
        private const val MAX_BITMAP_SIZE = 1024
    }
}