package com.mardous.booming.core.model.equalizer

import android.content.Context
import com.mardous.booming.R
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @author Christians M. A. (mardous)
 */
@Serializable
data class EQPreset(
    @SerialName("name")
    val name: String,
    @SerialName("levels")
    val levels: IntArray,
    @SerialName("effects")
    val effects: HashMap<String, Float> = hashMapOf(),
    val isCustom: Boolean = false
) {

    constructor(source: EQPreset, name: String, isCustom: Boolean) :
            this(name, source.levels.copyOf(), HashMap(source.effects), isCustom)

    val isValid: Boolean
        get() = name.isNotBlank() && levels.isNotEmpty()

    val numberOfBands: Int
        get() = levels.size

    fun getName(context: Context): String {
        if (isCustom) {
            return context.getString(R.string.custom)
        }
        return name
    }

    fun hasEffect(effect: String): Boolean {
        if (!isValid) return false

        return effects.containsKey(effect)
    }

    fun getEffect(effect: String, default: Float = 0f): Float {
        if (!isValid) return 0f

        return effects.getOrDefault(effect, default)
    }

    fun setEffect(effect: String, value: Float) {
        if (!isValid) return

        effects[effect] = value
    }

    fun removeEffect(effect: String): Boolean {
        if (!isValid) return false

        return effects.remove(effect) != null
    }

    fun getLevelShort(band: Int): Short {
        if (!isValid) return 0

        if (band >= 0 && band < levels.size) {
            return levels[band].toShort()
        }
        throw IllegalArgumentException("Invalid band: $band")
    }

    fun setBandLevel(band: Int, level: Int) {
        if (!isValid) return

        if (band >= 0 && band < levels.size) {
            levels[band] = level
        } else {
            throw IllegalArgumentException("Invalid band: $band")
        }
    }

    fun areSameLevels(other: EQPreset?): Boolean {
        if (other == null) return false
        return levels.contentEquals(other.levels)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EQPreset

        if (name != other.name) return false
        if (!levels.contentEquals(other.levels)) return false
        if (effects != other.effects) return false
        if (isCustom != other.isCustom) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + levels.contentHashCode()
        result = 31 * result + effects.hashCode()
        result = 31 * result + isCustom.hashCode()
        return result
    }

    companion object {
        fun getEmptyPreset(name: String, isCustom: Boolean, numBands: Int): EQPreset {
            val flatLevels = IntArray(numBands) { 0 }
            return EQPreset(name, flatLevels, isCustom = isCustom)
        }
    }
}