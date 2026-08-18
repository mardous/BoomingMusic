package com.mardous.booming.playback

object Playback {
    /** Package name used for playback-related command */
    const val PACKAGE_NAME = "com.mardous.booming"

    // Custom commands
    /** Action to toggle shuffle */
    const val TOGGLE_SHUFFLE = "$PACKAGE_NAME.command.shuffle.toggle"
    /** Action to cycle repeat */
    const val CYCLE_REPEAT = "$PACKAGE_NAME.command.repeat.cycle"
    /** Action to toggle the current playing song as favorite */
    const val TOGGLE_FAVORITE = "$PACKAGE_NAME.command.toggle_favorite"
    /** Action to restore the previous playback state */
    const val RESTORE_PLAYBACK = "$PACKAGE_NAME.command.restore_playback"
    /** Action to set UnshuffledShuffleOrder as the new shuffle order */
    const val SET_UNSHUFFLED_ORDER = "$PACKAGE_NAME.command.set.unshuffled_order"
    /** Action to set the new stop position */
    const val SET_STOP_POSITION = "$PACKAGE_NAME.command.set.stop_position"

    // Result extras reported back by TOGGLE_SHUFFLE and CYCLE_REPEAT
    /** Extra indicating the new shuffle mode */
    const val EXTRA_SHUFFLE_MODE = "$PACKAGE_NAME.extra.shuffle_mode"
    /** Extra indicating the new repeat mode */
    const val EXTRA_REPEAT_MODE = "$PACKAGE_NAME.extra.repeat_mode"

    // Custom events
    const val EVENT_MEDIA_CONTENT_CHANGED = "$PACKAGE_NAME.event.media_content_changed"
    const val EVENT_FAVORITE_CONTENT_CHANGED = "$PACKAGE_NAME.event.favorite_content_changed"
    const val EVENT_PLAYBACK_RESTORED = "$PACKAGE_NAME.event.playback_restored"
    const val EVENT_PLAYBACK_STARTED = "$PACKAGE_NAME.event.playback_started"
}