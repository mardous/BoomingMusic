package com.mardous.booming.core.model.equalizer

import android.content.Context
import com.mardous.booming.R
import com.mardous.booming.core.model.audiodevice.AudioDeviceType
import com.mardous.booming.extensions.utilities.DEFAULT_INFO_DELIMITER
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EqProfile(
    @SerialName("name")
    val name: String,
    @SerialName("levels")
    val levels: FloatArray,
    @SerialName("associations")
    val associations: Set<AudioDeviceType> = emptySet(),
    val isCustom: Boolean = false,
    val isAutoEq: Boolean = false
) {

    val isValid: Boolean
        get() = name.isNotBlank() && levels.isNotEmpty()

    val numberOfBands: Int
        get() = levels.size

    fun getName(context: Context): String {
        if (isCustom && name == CUSTOM_PRESET_NAME) {
            return context.getString(R.string.custom)
        }
        return name
    }

    fun getDescription(context: Context): String {
        val description = mutableListOf(context.getString(R.string.graphic_eq_band_count, numberOfBands))
        if (isAutoEq) {
            description.add(context.getString(R.string.autoeq_label))
        }
        if (isCustom && name != CUSTOM_PRESET_NAME) {
            description.add(context.getString(R.string.custom))
        }
        return description.joinToString(DEFAULT_INFO_DELIMITER)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EqProfile

        if (name != other.name) return false
        if (!levels.contentEquals(other.levels)) return false
        if (associations != other.associations) return false
        if (isCustom != other.isCustom) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + levels.contentHashCode()
        result = 31 * result + associations.hashCode()
        result = 31 * result + isCustom.hashCode()
        return result
    }

    companion object {
        const val CUSTOM_PRESET_NAME = "Custom"
    }
}