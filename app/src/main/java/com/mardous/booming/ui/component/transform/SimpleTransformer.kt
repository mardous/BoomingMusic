package com.mardous.booming.ui.component.transform

import android.view.View
import androidx.viewpager.widget.ViewPager

class SimplePageTransformer : ViewPager.PageTransformer {
    override fun transformPage(page: View, position: Float) {
        // no-op: page remains in place, no scaling or translation
    }
}
