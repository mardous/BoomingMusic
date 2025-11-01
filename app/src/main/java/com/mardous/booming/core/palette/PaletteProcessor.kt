package com.mardous.booming.core.palette

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.palette.graphics.Palette
import com.mardous.booming.core.model.PaletteColor
import com.mardous.booming.extensions.resources.isColorLight
import com.mardous.booming.util.color.NotificationColorUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.sqrt

object PaletteProcessor {

    /**
     * The fraction below which we select the vibrant instead of the light/dark vibrant color
     */
    private const val POPULATION_FRACTION_FOR_MORE_VIBRANT = 1.0f

    /**
     * Minimum saturation that a muted color must have if there exists if deciding between two colors
     */
    private const val MIN_SATURATION_WHEN_DECIDING = 0.19f

    /**
     * Minimum fraction that any color must have to be picked up as a text color
     */
    private const val MINIMUM_IMAGE_FRACTION = 0.002

    /**
     * The population fraction to select the dominant color as the text color over a the colored ones.
     */
    private const val POPULATION_FRACTION_FOR_DOMINANT = 0.01f

    /**
     * The population fraction to select a white or black color as the background over a color.
     */
    private const val POPULATION_FRACTION_FOR_WHITE_OR_BLACK = 2.5f

    private const val BLACK_MAX_LIGHTNESS = 0.08f
    private const val WHITE_MIN_LIGHTNESS = 0.90f
    private const val RESIZE_BITMAP_AREA = 150 * 150

    /**
     * The lightness difference that has to be added to the primary text color to obtain the secondary
     * text color when the background is light.
     */
    private const val LIGHTNESS_TEXT_DIFFERENCE_LIGHT = 20

    /**
     * The lightness difference that has to be added to the primary text color to obtain the secondary
     * text color when the background is dark. A bit less then the above value, since it looks better
     * on dark backgrounds.
     */
    private const val LIGHTNESS_TEXT_DIFFERENCE_DARK = -10

    private val mBlackWhiteFilter = Palette.Filter { rgb: Int, hsl: FloatArray ->
        !isWhiteOrBlack(hsl)
    }

    suspend fun getPaletteColor(context: Context, bitmap: Bitmap): PaletteColor =
        getPaletteColor(context, bitmap.toDrawable(context.resources))

    private suspend fun getPaletteColor(context: Context, drawable: Drawable) = withContext(Dispatchers.Default) {
        if (!isRecycled(drawable)) {
            var width = drawable.intrinsicWidth
            var height = drawable.intrinsicHeight
            val area = width * height
            if (area > RESIZE_BITMAP_AREA) {
                val factor = sqrt((RESIZE_BITMAP_AREA.toFloat() / area).toDouble())
                width = (factor * width).toInt()
                height = (factor * height).toInt()
            }

            val bitmap = createBitmap(width, height)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, width, height)
            drawable.draw(canvas)

            // for the background we only take the left side of the image to ensure
            // a smooth transition
            val paletteBuilder = Palette.from(bitmap)
                .setRegion(0, 0, bitmap.getWidth() / 2, bitmap.getHeight())
                .clearFilters() // we want all colors, red / white / black ones too!
                .resizeBitmapArea(RESIZE_BITMAP_AREA)

            var palette = paletteBuilder.generate()
            val backgroundColorAndFilter = findBackgroundColorAndFilter(palette)

            // we want most of the full region again, slightly shifted to the right
            paletteBuilder.setRegion(
                (bitmap.getWidth() * 0.4f).toInt(),
                0,
                bitmap.getWidth(),
                bitmap.getHeight()
            )

            backgroundColorAndFilter.second?.let { backgroundHsl ->
                paletteBuilder.addFilter { rgb: Int, hsl: FloatArray ->
                    // at least 10 degrees hue difference
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

            val foregroundColors = ensureColors(backgroundColor, foregroundColor)
            PaletteColor(backgroundColor, foregroundColors.first, foregroundColors.second)
        } else {
            PaletteColor.errorColor(context)
        }
    }

    fun findBackgroundColorAndFilter(palette: Palette): Pair<Int, FloatArray?> {
        // by default we use the dominant palette
        val dominantSwatch = palette.dominantSwatch
        if (dominantSwatch == null) {
            // We're not filtering on white or black
            return Color.WHITE to null
        }

        if (!isWhiteOrBlack(dominantSwatch.hsl)) {
            return dominantSwatch.rgb to dominantSwatch.hsl
        }

        // Oh well, we selected black or white. Lets look at the second color!
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
            // We're not filtering on white or black
            return dominantSwatch.rgb to null
        }

        return if (dominantSwatch.population / highestNonWhitePopulation > POPULATION_FRACTION_FOR_WHITE_OR_BLACK) {
            // The dominant swatch is very dominant, lets take it!
            // We're not filtering on white or black
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
        // We want a fraction that is at least 1% of the image
        return swatch.population / RESIZE_BITMAP_AREA.toFloat() > MINIMUM_IMAGE_FRACTION
    }

    private fun isRecycled(drawable: Drawable?): Boolean {
        if (drawable is BitmapDrawable) {
            val wrappedBitmap = drawable.bitmap
            return wrappedBitmap == null || wrappedBitmap.isRecycled
        }
        return false
    }

    private fun isWhiteOrBlack(hsl: FloatArray): Boolean {
        return isBlack(hsl) || isWhite(hsl)
    }

    /**
     * @return true if the color represents a color which is close to black.
     */
    private fun isBlack(hslColor: FloatArray): Boolean {
        return hslColor[2] <= BLACK_MAX_LIGHTNESS
    }

    /**
     * @return true if the color represents a color which is close to white.
     */
    private fun isWhite(hslColor: FloatArray): Boolean {
        return hslColor[2] >= WHITE_MIN_LIGHTNESS
    }

    private fun ensureColors(backgroundColor: Int, mForegroundColor: Int): Pair<Int, Int> {
        var primaryTextColor: Int
        var secondaryTextColor: Int
        val backLum = NotificationColorUtil.calculateLuminance(backgroundColor)
        val textLum = NotificationColorUtil.calculateLuminance(mForegroundColor)
        val contrast = NotificationColorUtil.calculateContrast(mForegroundColor, backgroundColor)
        // We only respect the given colors if worst case Black or White still has
        // contrast
        val backgroundLight = (backLum > textLum && NotificationColorUtil.satisfiesTextContrast(backgroundColor, Color.BLACK)) ||
                (backLum <= textLum && !NotificationColorUtil.satisfiesTextContrast(backgroundColor, Color.WHITE))
        if (contrast < 4.5f) {
            if (backgroundLight) {
                secondaryTextColor = NotificationColorUtil.findContrastColor(mForegroundColor, backgroundColor, true,  /* findFG */4.5)
                primaryTextColor = NotificationColorUtil.changeColorLightness(secondaryTextColor, -LIGHTNESS_TEXT_DIFFERENCE_LIGHT)
            } else {
                secondaryTextColor = NotificationColorUtil.findContrastColorAgainstDark(mForegroundColor, backgroundColor, true,  /* findFG */4.5)
                primaryTextColor = NotificationColorUtil.changeColorLightness(secondaryTextColor, -LIGHTNESS_TEXT_DIFFERENCE_DARK)
            }
        } else {
            primaryTextColor = mForegroundColor
            secondaryTextColor = NotificationColorUtil.changeColorLightness(
                primaryTextColor, if (backgroundLight) LIGHTNESS_TEXT_DIFFERENCE_LIGHT else LIGHTNESS_TEXT_DIFFERENCE_DARK
            )
            if (NotificationColorUtil.calculateContrast(secondaryTextColor, backgroundColor) < 4.5f) {
                // oh well the secondary is not good enough
                secondaryTextColor = if (backgroundLight) {
                    NotificationColorUtil.findContrastColor(secondaryTextColor, backgroundColor, true,  /* findFG */4.5)
                } else {
                    NotificationColorUtil.findContrastColorAgainstDark(secondaryTextColor, backgroundColor, true,  /* findFG */4.5)
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