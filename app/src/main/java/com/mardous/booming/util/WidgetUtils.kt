package com.mardous.booming.util

import android.graphics.*
import android.graphics.drawable.Drawable
import androidx.core.graphics.createBitmap

object WidgetUtils {

    fun createRoundedBitmap(drawable: Drawable?, size: Int, radius: Float) =
        createRoundedBitmap(drawable, size, size, radius, radius, radius, radius)

    fun createRoundedBitmap(
        drawable: Drawable?,
        width: Int,
        height: Int,
        tl: Float,
        tr: Float,
        bl: Float,
        br: Float
    ): Bitmap? {
        if (drawable == null)
            return null

        val path = composeRoundedRectPath(RectF(0f, 0f, width.toFloat(), height.toFloat()), tl, tr, bl, br)
        val bitmap = createBitmap(width, height)
        val c = Canvas(bitmap)
        drawable.setBounds(0, 0, width, height)
        drawable.draw(c)
        val rounded = createBitmap(width, height)
        val canvas = Canvas(rounded)
        val paint = Paint()
        paint.shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        paint.isAntiAlias = true
        canvas.drawPath(path, paint)
        return rounded
    }

    private fun composeRoundedRectPath(
        rect: RectF,
        tl: Float,
        tr: Float,
        bl: Float,
        br: Float
    ): Path {
        val topLeft = tl.coerceAtLeast(0f)
        val topRight = tr.coerceAtLeast(0f)
        val bottomLeft = bl.coerceAtLeast(0f)
        val bottomRight = br.coerceAtLeast(0f)

        val path = Path()
        path.moveTo(rect.left + topLeft, rect.top)
        path.lineTo(rect.right - topRight, rect.top)
        path.quadTo(rect.right, rect.top, rect.right, rect.top + topRight)
        path.lineTo(rect.right, rect.bottom - bottomRight)
        path.quadTo(rect.right, rect.bottom, rect.right - bottomRight, rect.bottom)
        path.lineTo(rect.left + bottomLeft, rect.bottom)
        path.quadTo(rect.left, rect.bottom, rect.left, rect.bottom - bottomLeft)
        path.lineTo(rect.left, rect.top + topLeft)
        path.quadTo(rect.left, rect.top, rect.left + topLeft, rect.top)
        path.close()
        return path
    }
}