/*
 * Copyright (c) 2020 Hemanth Savarla.
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 */
package com.mardous.booming.glide

import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.request.transition.Transition
import com.mardous.booming.appContext
import com.mardous.booming.extensions.resources.defaultFooterColor
import com.mardous.booming.glide.palette.BitmapPaletteTarget
import com.mardous.booming.glide.palette.BitmapPaletteWrapper
import com.mardous.booming.util.color.MediaNotificationProcessor

abstract class BoomingColoredTarget(view: ImageView) : BitmapPaletteTarget(view) {

    protected val defaultFooterColor: Int
        get() = getView().context.defaultFooterColor()

    abstract fun onColorReady(colors: MediaNotificationProcessor)

    override fun onLoadFailed(errorDrawable: Drawable?) {
        super.onLoadFailed(errorDrawable)
        onColorReady(MediaNotificationProcessor.errorColor(appContext()))
    }

    override fun onResourceReady(resource: BitmapPaletteWrapper, transition: Transition<in BitmapPaletteWrapper>?) {
        super.onResourceReady(resource, transition)
        MediaNotificationProcessor(appContext()).getPaletteAsync({
            onColorReady(it)
        }, resource.bitmap)
    }
}
