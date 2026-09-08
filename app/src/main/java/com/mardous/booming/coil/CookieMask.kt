package com.mardous.booming.coil

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import com.mardous.booming.R

object CookieMask {

    private const val SIZE = 512

    fun apply(context: Context, source: Bitmap): Bitmap {
        val output = createBitmap(SIZE, SIZE)
        val canvas = Canvas(output)
        ContextCompat.getDrawable(context, R.drawable.widget_shape_scallop)?.apply {
            setBounds(0, 0, SIZE, SIZE)
            draw(canvas)
        }
        val side = minOf(source.width, source.height)
        val crop = Rect(
            (source.width - side) / 2,
            (source.height - side) / 2,
            (source.width + side) / 2,
            (source.height + side) / 2
        )
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        }
        canvas.drawBitmap(source, crop, Rect(0, 0, SIZE, SIZE), paint)
        return output
    }
}
