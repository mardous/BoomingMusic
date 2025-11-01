package com.mardous.booming.playback

object Playback {
    // Custom commands
    const val TOGGLE_SHUFFLE = "com.mardous.booming.command.shuffle.toggle"
    const val SHUFFLE_OFF = "com.mardous.booming.command.shuffle.off"
    const val SHUFFLE_ON = "com.mardous.booming.command.shuffle.on"

    const val CYCLE_REPEAT = "com.mardous.booming.command.repeat.cycle"
    const val REPEAT_OFF = "com.mardous.booming.command.repeat.off"
    const val REPEAT_ALL = "com.mardous.booming.command.repeat.all"
    const val REPEAT_ONE = "com.mardous.booming.command.repeat.one"

    const val TOGGLE_FAVORITE = "com.mardous.booming.command.toggle_favorite"

    const val RESTORE_PLAYBACK = "com.mardous.booming.command.restore_playback"

    const val SET_UNSHUFFLED_ORDER = "com.mardous.booming.command.set.unshuffled_order"
    const val SET_STOP_POSITION = "com.mardous.booming.command.set.stop_position"

    // Custom events
    const val EVENT_MEDIA_CONTENT_CHANGED = "com.mardous.booming.event.media_content_changed"
    const val EVENT_FAVORITE_CONTENT_CHANGED = "com.mardous.booming.event.favorite_content_changed"
    const val EVENT_PLAYBACK_RESTORED = "com.mardous.booming.event.playback_restored"
    const val EVENT_PLAYBACK_STARTED = "com.mardous.booming.event.playback_started"
}