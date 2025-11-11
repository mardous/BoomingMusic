package com.mardous.booming.core.model.theme

import androidx.annotation.DrawableRes
import com.mardous.booming.R

enum class NowPlayingButtonStyle(
    @param:DrawableRes val play: Int,
    @param:DrawableRes val pause: Int,
    @param:DrawableRes val skipNext: Int,
    @param:DrawableRes val skipPrevious: Int
) {
    Normal(
        play = R.drawable.ic_play_24dp,
        pause = R.drawable.ic_pause_24dp,
        skipNext = R.drawable.ic_next_24dp,
        skipPrevious = R.drawable.ic_previous_24dp
    ),
    Material3(
        play = R.drawable.ic_play_m3_24dp,
        pause = R.drawable.ic_pause_m3_24dp,
        skipNext = R.drawable.ic_next_m3_24dp,
        skipPrevious = R.drawable.ic_previous_m3_24dp
    );
}