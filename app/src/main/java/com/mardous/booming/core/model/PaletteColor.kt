package com.mardous.booming.core.model

class PaletteColor(
    val backgroundColor: Int,
    val primaryTextColor: Int,
    val secondaryTextColor: Int
) {
    companion object {
        val Error = PaletteColor(
            backgroundColor = -15724528,
            primaryTextColor = -6974059,
            secondaryTextColor = -8684677
        )
    }
}