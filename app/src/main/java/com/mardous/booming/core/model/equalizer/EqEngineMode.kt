package com.mardous.booming.core.model.equalizer

import android.media.audiofx.AudioEffect
import androidx.annotation.StringRes
import com.mardous.booming.R
import java.util.UUID

enum class EqEngineMode(
    val type: UUID,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val defaultBandCount: Int
) {
    Basic(
        type = AudioEffect.EFFECT_TYPE_EQUALIZER,
        titleRes = R.string.eq_engine_basic_title,
        descriptionRes = R.string.eq_engine_basic_description,
        defaultBandCount = 5
    ),
    DynamicsProcessing(
        type = AudioEffect.EFFECT_TYPE_DYNAMICS_PROCESSING,
        titleRes = R.string.eq_engine_precision_title,
        descriptionRes = R.string.eq_engine_precision_description,
        defaultBandCount = 10
    )
}