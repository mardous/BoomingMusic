package com.mardous.booming.core.model.equalizer

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
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
        type = UUID.fromString("0bed4300-ddd6-11db-8f34-0002a5d5c51b"),
        titleRes = R.string.eq_engine_basic_title,
        descriptionRes = R.string.eq_engine_basic_description,
        defaultBandCount = 5
    ),
    DynamicsProcessing(
        type = UUID.fromString("7261676f-6d75-7369-6364-28e2fd3ac39e"),
        titleRes = R.string.eq_engine_precision_title,
        descriptionRes = R.string.eq_engine_precision_description,
        defaultBandCount = 10
    );

    companion object {
        val Auto = if (isSwitchingSupported()) DynamicsProcessing else Basic

        @ChecksSdkIntAtLeast(Build.VERSION_CODES.P)
        fun isSwitchingSupported() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
    }
}