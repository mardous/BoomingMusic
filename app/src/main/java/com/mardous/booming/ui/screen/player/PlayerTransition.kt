package com.mardous.booming.ui.screen.player

import androidx.viewpager.widget.ViewPager
import com.mardous.booming.R
import com.mardous.booming.ui.component.transform.*

enum class PlayerTransition(val nameRes: Int) {
    Simple(R.string.simple),
    Cascading(R.string.cascading),
    Depth(R.string.depth),
    Hinge(R.string.hinge),
    HorizontalFlip(R.string.horizontal_flip),
    VerticalFlip(R.string.vertical_flip),
    Stack(R.string.stack),
    ZoomOut(R.string.zoom_out),
    Parallax(R.string.parallax);

    val transformerFactory: (Int) -> Pair<ViewPager.PageTransformer?, Boolean>
        get() = when (this) {
            Cascading -> { _ -> CascadingPageTransformer() to true }
            Depth -> { _ -> DepthTransformation() to true }
            Hinge -> { _ -> HingeTransformation() to true }
            HorizontalFlip -> { _ -> HorizontalFlipTransformation() to true }
            VerticalFlip -> { _ -> VerticalFlipTransformation() to true }
            Stack -> { _ -> VerticalStackTransformer() to true }
            ZoomOut -> { _ -> ZoomOutPageTransformer() to true }
            Parallax -> { id -> ParallaxPagerTransformer(id).apply { setSpeed(0.3f) } to false}
            Simple -> { _ -> Pair(null, true) }
    }
}