package com.mardous.booming.extensions.resources

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.mardous.booming.extensions.dp
import com.mardous.booming.transform.CarouselPagerTransformer
import com.mardous.booming.transform.ParallaxPagerTransformer

fun ViewPager2.setCarouselEffect() {
    val metrics = resources.displayMetrics
    val ratio = metrics.heightPixels.toFloat() / metrics.widthPixels.toFloat()
    val horizontalPadding = if (ratio >= 1.777f) 40.dp(resources) else 100.dp(resources)

    val recyclerView = getChildAt(0) as RecyclerView
    recyclerView.apply {
        setPadding(horizontalPadding, 0, horizontalPadding, 0)
        clipToPadding = false
    }

    val pageMargin = 8.dp(resources)
    offscreenPageLimit = 1
    setPageTransformer(CarouselPagerTransformer(context, pageMargin))
}

fun ViewPager2.setParallaxEffect(viewId: Int) {
    val transformer = ParallaxPagerTransformer(viewId)
    transformer.setSpeed(0.3f)
    offscreenPageLimit = 2
    setPageTransformer(transformer)
}

fun ViewPager2.disableNestedScrolling() {
    (getChildAt(0) as? RecyclerView)?.apply {
        isNestedScrollingEnabled = false
        overScrollMode = View.OVER_SCROLL_NEVER
    }
}