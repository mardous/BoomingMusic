package com.mardous.booming.core.appwidgets.component

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.LruCache
import androidx.core.graphics.createBitmap
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/** Separate layers let Glance tint played and remaining track differently. */
object WaveRenderer {

    private const val WAVELENGTH_DP = 20f

    private const val AMPLITUDE_DP = 2.6f
    private const val STROKE_DP = 3f
    private const val HANDLE_WIDTH_DP = 5f
    private const val SAMPLE_STEP_DP = 1f

    private enum class Part { Played, Remaining }

    private data class Key(
        val w: Int,
        val h: Int,
        val handle: Int,
        val wavy: Boolean,
        val part: Part
    )

    private val cache = object : LruCache<Key, Bitmap>(1024 * 1024) {
        override fun sizeOf(key: Key, value: Bitmap) = value.allocationByteCount
    }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    fun played(
        widthPx: Int,
        heightPx: Int,
        progress: Float,
        isPlaying: Boolean,
        density: Float
    ): Bitmap = render(Part.Played, widthPx, heightPx, progress, isPlaying, density)

    fun remaining(
        widthPx: Int,
        heightPx: Int,
        progress: Float,
        density: Float
    ): Bitmap = render(Part.Remaining, widthPx, heightPx, progress, false, density)

    @Synchronized
    private fun render(
        part: Part,
        widthPx: Int,
        heightPx: Int,
        progress: Float,
        isPlaying: Boolean,
        density: Float
    ): Bitmap {
        val width = widthPx.coerceAtLeast(1)
        val height = heightPx.coerceAtLeast(1)
        val fraction = progress.coerceIn(0f, 1f)

        val handleWidth = HANDLE_WIDTH_DP * density
        val stroke = STROKE_DP * density
        val inset = handleWidth / 2f + stroke / 2f
        val handleX = inset + (width - 2 * inset) * fraction

        // Shorten the remaining stroke so its round cap meets the handle.
        val ink = handleWidth / 2f + stroke / 2f
        val playedEnd = handleX - ink
        val remainingStart = handleX + ink

        // Keep the track flat while paused or too short for a wave.
        val wavy = isPlaying && playedEnd - inset > WAVELENGTH_DP * density * 0.5f

        val key = Key(width, height, handleX.roundToInt(), wavy && part == Part.Played, part)
        cache.get(key)?.let { return it }

        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)
        val centreY = height / 2f
        strokePaint.strokeWidth = STROKE_DP * density

        when (part) {
            Part.Played -> {
                val path = Path()
                if (wavy) {
                    val wavelength = WAVELENGTH_DP * density
                    val amplitude = AMPLITUDE_DP * density
                    val frequency = (2 * PI / wavelength).toFloat()
                    val step = SAMPLE_STEP_DP * density
                    fun yAt(x: Float) = centreY + amplitude * sin(frequency * x)

                    var previousX = inset
                    var previousY = yAt(previousX)
                    path.moveTo(previousX, previousY)
                    var x = previousX + step
                    while (x < playedEnd) {
                        val y = yAt(x)
                        path.quadTo(previousX, previousY, (previousX + x) / 2f, (previousY + y) / 2f)
                        previousX = x
                        previousY = y
                        x += step
                    }
                    path.lineTo(playedEnd, yAt(playedEnd))
                } else {
                    path.moveTo(inset, centreY)
                    path.lineTo(playedEnd.coerceAtLeast(inset), centreY)
                }
                canvas.drawPath(path, strokePaint)

                val handleHeight = height - stroke
                canvas.drawRoundRect(
                    RectF(
                        handleX - handleWidth / 2f,
                        centreY - handleHeight / 2f,
                        handleX + handleWidth / 2f,
                        centreY + handleHeight / 2f
                    ),
                    handleWidth / 2f,
                    handleWidth / 2f,
                    fillPaint
                )
            }

            Part.Remaining -> {
                if (remainingStart < width - inset) {
                    canvas.drawLine(remainingStart, centreY, width - inset, centreY, strokePaint)
                }
            }
        }

        cache.put(key, bitmap)
        return bitmap
    }

}
