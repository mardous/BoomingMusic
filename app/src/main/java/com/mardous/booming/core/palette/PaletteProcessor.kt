package com.mardous.booming.core.palette

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.scale
import androidx.palette.graphics.Palette
import com.kyant.m3color.quantize.QuantizerCelebi
import com.kyant.m3color.score.Score
import com.mardous.booming.core.model.PaletteColor
import com.mardous.booming.extensions.resources.isColorLight
import com.mardous.booming.util.color.NotificationColorUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.sqrt

object PaletteProcessor {

    private const val POPULATION_FRACTION_FOR_MORE_VIBRANT = 1.0f
    private const val MIN_SATURATION_WHEN_DECIDING = 0.19f
    private const val MINIMUM_IMAGE_FRACTION = 0.002
    private const val POPULATION_FRACTION_FOR_DOMINANT = 0.01f
    private const val POPULATION_FRACTION_FOR_WHITE_OR_BLACK = 2.5f

    private const val BLACK_MAX_LIGHTNESS = 0.08f
    private const val WHITE_MIN_LIGHTNESS = 0.90f
    private const val RESIZE_BITMAP_AREA = 150 * 150

    private const val LIGHTNESS_TEXT_DIFFERENCE_LIGHT = 20
    private const val LIGHTNESS_TEXT_DIFFERENCE_DARK = -10

    private val mBlackWhiteFilter = Palette.Filter { _: Int, hsl: FloatArray ->
        !isWhiteOrBlack(hsl)
    }

    fun getVibrantColor(bitmap: Bitmap): Int {
        val width = bitmap.width
        val height = bitmap.height
        val bitmapPixels = IntArray(width * height)
        bitmap.getPixels(bitmapPixels, 0, width, 0, 0, width, height)
        return Score.score(QuantizerCelebi.quantize(bitmapPixels, 128))[0]
    }

    suspend fun getPaletteColor(context: Context, bitmap: Bitmap): PaletteColor = withContext(Dispatchers.Default) {
        if (bitmap.isRecycled) {
            return@withContext PaletteColor.errorColor(context)
        }

        val area = bitmap.width * bitmap.height
        val workingBitmap = if (area > RESIZE_BITMAP_AREA) {
            val factor = sqrt(RESIZE_BITMAP_AREA.toFloat() / area)
            bitmap.scale((bitmap.width * factor).toInt(), (bitmap.height * factor).toInt())
        } else {
            bitmap
        }

        val bitmapWidth = workingBitmap.width
        val bitmapHeight = workingBitmap.height
        if (bitmapWidth <= 0 || bitmapHeight <= 0) {
            if (workingBitmap !== bitmap) workingBitmap.recycle()
            return@withContext PaletteColor.errorColor(context)
        }

        // extraction for background
        val paletteBuilder = Palette.from(workingBitmap)
            .setRegion(0, 0, bitmapWidth / 2, bitmapHeight)
            .clearFilters()
            .resizeBitmapArea(RESIZE_BITMAP_AREA)

        var palette = paletteBuilder.generate()
        val backgroundColorAndFilter = findBackgroundColorAndFilter(palette)

        // extraction for foreground
        paletteBuilder.setRegion(
            (bitmapWidth * 0.4f).toInt(),
            0,
            bitmapWidth,
            bitmapHeight
        )

        backgroundColorAndFilter.second?.let { backgroundHsl ->
            paletteBuilder.addFilter { _: Int, hsl: FloatArray ->
                val diff = abs(hsl[0] - backgroundHsl[0])
                diff > 10 && diff < 350
            }
        }

        paletteBuilder.addFilter(mBlackWhiteFilter)
        palette = paletteBuilder.generate()

        val backgroundColor = backgroundColorAndFilter.first
        val foregroundColor = if (backgroundColor.isColorLight) {
            selectForegroundColorForSwatches(
                palette.darkVibrantSwatch,
                palette.vibrantSwatch,
                palette.darkMutedSwatch,
                palette.mutedSwatch,
                palette.dominantSwatch,
                Color.BLACK
            )
        } else {
            selectForegroundColorForSwatches(
                palette.lightVibrantSwatch,
                palette.vibrantSwatch,
                palette.lightMutedSwatch,
                palette.mutedSwatch,
                palette.dominantSwatch,
                Color.WHITE
            )
        }

        val vibrantColor = getVibrantColor(workingBitmap)
        val foregroundColors = ensureColors(backgroundColor, foregroundColor)

        if (workingBitmap !== bitmap) {
            workingBitmap.recycle()
        }

        PaletteColor(
            backgroundColor = backgroundColor,
            primaryColor = vibrantColor,
            primaryTextColor = foregroundColors.first,
            secondaryTextColor = foregroundColors.second
        )
    }

    private fun findBackgroundColorAndFilter(palette: Palette): Pair<Int, FloatArray?> {
        val dominantSwatch = palette.dominantSwatch ?: return Color.WHITE to null

        if (!isWhiteOrBlack(dominantSwatch.hsl)) {
            return dominantSwatch.rgb to dominantSwatch.hsl
        }

        val swatches = palette.swatches
        var highestNonWhitePopulation = -1f
        var second: Palette.Swatch? = null
        for (swatch in swatches) {
            if (swatch !== dominantSwatch && swatch.population > highestNonWhitePopulation && !isWhiteOrBlack(swatch.hsl)) {
                second = swatch
                highestNonWhitePopulation = swatch.population.toFloat()
            }
        }

        if (second == null) {
            return dominantSwatch.rgb to null
        }

        return if (dominantSwatch.population / highestNonWhitePopulation > POPULATION_FRACTION_FOR_WHITE_OR_BLACK) {
            dominantSwatch.rgb to null
        } else {
            second.rgb to second.hsl
        }
    }

    private fun selectForegroundColorForSwatches(
        moreVibrant: Palette.Swatch?,
        vibrant: Palette.Swatch?,
        moreMutedSwatch: Palette.Swatch?,
        mutedSwatch: Palette.Swatch?,
        dominantSwatch: Palette.Swatch?,
        fallbackColor: Int
    ): Int {
        var coloredCandidate = selectVibrantCandidate(moreVibrant, vibrant)
        if (coloredCandidate == null) {
            coloredCandidate = selectMutedCandidate(mutedSwatch, moreMutedSwatch)
        }
        return if (dominantSwatch != null && coloredCandidate != null) {
            if (dominantSwatch === coloredCandidate) {
                coloredCandidate.rgb
            } else if ((coloredCandidate.population.toFloat() / dominantSwatch.population < POPULATION_FRACTION_FOR_DOMINANT) &&
                dominantSwatch.hsl[1] > MIN_SATURATION_WHEN_DECIDING
            ) {
                dominantSwatch.rgb
            } else {
                coloredCandidate.rgb
            }
        } else if (dominantSwatch != null && hasEnoughPopulation(dominantSwatch)) {
            dominantSwatch.rgb
        } else {
            fallbackColor
        }
    }

    private fun selectMutedCandidate(first: Palette.Swatch?, second: Palette.Swatch?): Palette.Swatch? {
        val firstValid = first != null && hasEnoughPopulation(first)
        val secondValid = second != null && hasEnoughPopulation(second)
        if (firstValid && secondValid) {
            val firstSaturation = first.hsl[1]
            val secondSaturation = second.hsl[1]
            val populationFraction = (first.population / second.population).toFloat()
            return if (firstSaturation * populationFraction > secondSaturation) first else second
        } else if (firstValid) {
            return first
        } else if (secondValid) {
            return second
        }
        return null
    }

    private fun selectVibrantCandidate(first: Palette.Swatch?, second: Palette.Swatch?): Palette.Swatch? {
        val firstValid = first != null && hasEnoughPopulation(first)
        val secondValid = second != null && hasEnoughPopulation(second)
        return if (firstValid && secondValid) {
            val firstPopulation = first.population
            val secondPopulation = second.population
            if (firstPopulation / secondPopulation.toFloat() < POPULATION_FRACTION_FOR_MORE_VIBRANT) second else first
        } else if (firstValid) {
            first
        } else if (secondValid) {
            second
        } else null
    }

    private fun hasEnoughPopulation(swatch: Palette.Swatch): Boolean {
        return swatch.population / RESIZE_BITMAP_AREA.toFloat() > MINIMUM_IMAGE_FRACTION
    }

    private fun isWhiteOrBlack(hsl: FloatArray): Boolean {
        return isBlack(hsl) || isWhite(hsl)
    }

    private fun isBlack(hslColor: FloatArray): Boolean {
        return hslColor[2] <= BLACK_MAX_LIGHTNESS
    }

    private fun isWhite(hslColor: FloatArray): Boolean {
        return hslColor[2] >= WHITE_MIN_LIGHTNESS
    }

    private fun ensureColors(backgroundColor: Int, mForegroundColor: Int): Pair<Int, Int> {
        var primaryTextColor: Int
        var secondaryTextColor: Int
        val backLum = NotificationColorUtil.calculateLuminance(backgroundColor)
        val textLum = NotificationColorUtil.calculateLuminance(mForegroundColor)
        val contrast = NotificationColorUtil.calculateContrast(mForegroundColor, backgroundColor)

        val backgroundLight = (backLum > textLum && NotificationColorUtil.satisfiesTextContrast(backgroundColor, Color.BLACK)) ||
                (backLum <= textLum && !NotificationColorUtil.satisfiesTextContrast(backgroundColor, Color.WHITE))

        if (contrast < 4.5f) {
            if (backgroundLight) {
                secondaryTextColor = NotificationColorUtil.findContrastColor(mForegroundColor, backgroundColor, true, 4.5)
                primaryTextColor = NotificationColorUtil.changeColorLightness(secondaryTextColor, -LIGHTNESS_TEXT_DIFFERENCE_LIGHT)
            } else {
                secondaryTextColor = NotificationColorUtil.findContrastColorAgainstDark(mForegroundColor, backgroundColor, true, 4.5)
                primaryTextColor = NotificationColorUtil.changeColorLightness(secondaryTextColor, -LIGHTNESS_TEXT_DIFFERENCE_DARK)
            }
        } else {
            primaryTextColor = mForegroundColor
            secondaryTextColor = NotificationColorUtil.changeColorLightness(
                primaryTextColor, if (backgroundLight) LIGHTNESS_TEXT_DIFFERENCE_LIGHT else LIGHTNESS_TEXT_DIFFERENCE_DARK
            )
            if (NotificationColorUtil.calculateContrast(secondaryTextColor, backgroundColor) < 4.5f) {
                secondaryTextColor = if (backgroundLight) {
                    NotificationColorUtil.findContrastColor(secondaryTextColor, backgroundColor, true, 4.5)
                } else {
                    NotificationColorUtil.findContrastColorAgainstDark(secondaryTextColor, backgroundColor, true, 4.5)
                }
                primaryTextColor = NotificationColorUtil.changeColorLightness(
                    secondaryTextColor,
                    if (backgroundLight) -LIGHTNESS_TEXT_DIFFERENCE_LIGHT else -LIGHTNESS_TEXT_DIFFERENCE_DARK
                )
            }
        }
        return primaryTextColor to secondaryTextColor
    }
}