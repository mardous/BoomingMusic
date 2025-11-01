package com.mardous.booming.data.model.lyrics

enum class LyricsActor(val value: String, val isBackground: Boolean) {
    Voice1("v1", false), Voice1Background("v1", true),
    Voice2("v2", false), Voice2Background("v2", true),
    Group("v3", false), GroupBackground("v3", true),
    Duet("D", false), DuetBackground("D", true),
    Male("M", false), MaleBackground("M", true),
    Female("F", false), FemaleBackground("F", true);

    fun asBackground(asBackground: Boolean): LyricsActor {
        return if (asBackground) {
            if (this.isBackground) {
                return this
            }
            return LyricsActor.entries.first {
                it.value == this.value && it.isBackground
            }
        } else this
    }

    companion object {
        fun getActorFromValue(value: String?): LyricsActor? =
            LyricsActor.entries.firstOrNull { it.value == value }
    }
}