package com.mardous.booming.ui.screen.player

import androidx.viewpager.widget.ViewPager
import com.mardous.booming.ui.component.transform.*

enum class PlayerTransition(val id: String) {
    SIMPLE("simple"),
    CASCADING("cascading"),
    DEPTH("depth"),
    HINGE("hinge"),
    HORIZONTAL_FLIP("horizontal_flip"),
    VERTICAL_FLIP("vertical_flip"),
    STACK("stack"),
    ZOOM_OUT("zoom_out"),
    PARALLAX("parallax");

    val transformerFactory: (Int) -> ViewPager.PageTransformer?
        get() = when (this) {
            SIMPLE -> { _ -> SimplePageTransformer() }
            CASCADING -> { _ -> CascadingPageTransformer() }
            DEPTH -> { _ -> DepthTransformation() }
            HINGE -> { _ -> HingeTransformation() }
            HORIZONTAL_FLIP -> { _ -> HorizontalFlipTransformation() }
            VERTICAL_FLIP -> { _ -> VerticalFlipTransformation() }
            STACK -> { _ -> VerticalStackTransformer() }
            ZOOM_OUT -> { _ -> ZoomOutPageTransformer() }
            PARALLAX -> { id -> ParallaxPagerTransformer(id).apply { setSpeed(0.3f) } }
    }

    companion object {
        fun fromId(id: String?): PlayerTransition? =
            entries.find { it.id == id }
    }
}