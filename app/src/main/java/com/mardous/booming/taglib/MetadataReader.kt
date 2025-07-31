package com.mardous.booming.taglib

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.kyant.taglib.AudioProperties
import com.kyant.taglib.Metadata
import com.kyant.taglib.Picture
import com.kyant.taglib.TagLib
import org.jaudiotagger.tag.reference.GenreTypes
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

fun Picture.toBitmap(): Bitmap? = BitmapFactory.decodeByteArray(data, 0, data.size)

class MetadataReader(uri: Uri, readPictures: Boolean = false) : KoinComponent {

    private var metadata: Metadata? = null
    private var audioProperties: AudioProperties? = null

    private val pictures get() = metadata?.pictures
    private val tags get() = metadata?.propertyMap

    val hasMetadata get() = metadata != null
    val hasPictures get() = pictures?.isNotEmpty() == true

    init {
        try {
            get<ContentResolver>().openFileDescriptor(uri, "r")?.use {
                metadata = TagLib.getMetadata(it.dup().detachFd(), readPictures = readPictures)
                audioProperties = TagLib.getAudioProperties(it.dup().detachFd())
            }
        } catch (t: Throwable) {
            Log.e("MetadataWorker", "Error reading file $uri", t)
        }
    }

    fun bitrate(): String? {
        if (audioProperties == null || audioProperties!!.bitrate <= 0)
            return null

        return "${audioProperties!!.bitrate} kbps"
    }

    fun sampleRate(): String? {
        if (audioProperties == null)
            return null

        return if (audioProperties!!.sampleRate >= 1000) {
            "%.1f kHz".format(audioProperties!!.sampleRate / 1000.0)
        } else {
            "${audioProperties!!.sampleRate} Hz"
        }
    }

    fun channels() = audioProperties?.channels ?: -1
    fun channelName(): String? {
        val channels = channels()
        return CHANNEL_MAP[channels] ?: "$channels channels"
    }

    /**
     * Returns a map containing all the metadata present in the file.
     *
     * If there is no metadata or if the file read failed, this returns an empty map.
     */
    fun all() = tags ?: emptyMap()

    /**
     * Returns all possible values associated with a particular tag.
     *
     * This takes an array of delimiters as a parameter, which can be used to
     * force multiple values to be read.
     */
    fun all(key: String, delimiters: Array<String> = DEFAULT_DELIMITERS): Array<String>? {
        val values = tags?.get(key.uppercase())
        if (values?.size == 1 && delimiters.isNotEmpty()) {
            val single = values.single()
            for (delimiter in delimiters) {
                if (single.contains(delimiter)) {
                    return single.split(delimiter).toTypedArray()
                }
            }
        }
        return values
    }

    fun get(key: String, index: Int, delimiters: Array<String> = DEFAULT_DELIMITERS) =
        all(key, delimiters = delimiters)?.getOrNull(index)

    fun first(key: String) = get(key, 0)

    fun value(key: String) = get(key, 0, delimiters = emptyArray())

    fun frontCover() = pictures?.find { it.pictureType == "Front Cover" } ?: pictures?.firstOrNull()

    fun allPictures() = pictures

    fun merge(key: String, separator: String = "; "): String? {
        val values = all(key)
        if (values.isNullOrEmpty()) {
            return null
        }
        return if (values.size == 1) {
            values.single()
        } else {
            values.joinToString(separator = separator)
        }
    }

    fun genre() = first(GENRE)?.normalizeGenre()
    fun genres() = all(GENRE)?.map { it.normalizeGenre() }

    private fun String.normalizeGenre(): String {
        val genreAsNumber = toIntOrNull()
        if (genreAsNumber != null) {
            val normalizedGenreName = GenreTypes.getInstanceOf().getValueForId(genreAsNumber)
            if (!normalizedGenreName.isNullOrEmpty()) {
                return normalizedGenreName
            }
        }
        return this
    }

    companion object {
        private val DEFAULT_DELIMITERS = arrayOf("/", ";")
        private val CHANNEL_MAP = mapOf(
            1 to "Mono",
            2 to "Stereo",
            3 to "2.1 Stereo",
            4 to "Quadraphonic",
            5 to "5.0 Surround",
            6 to "5.1 Surround",
            7 to "6.1 Surround",
            8 to "7.1 Surround",
            10 to "9.1 Surround",
            12 to "11.1 Surround",
            14 to "13.1 Surround"
        )

        const val TITLE = "TITLE"
        const val ARTIST = "ARTIST"
        const val ALBUM = "ALBUM"
        const val ALBUM_ARTIST = "ALBUMARTIST"
        const val GENRE = "GENRE"
        const val TRACK_NUMBER = "TRACKNUMBER"
        const val TRACK_TOTAL = "TRACKTOTAL"
        const val DISC_NUMBER = "DISCNUMBER"
        const val DISC_TOTAL = "DISCTOTAL"
        const val YEAR = "DATE"
        const val LYRICS = "LYRICS"
        const val LYRICIST = "LYRICIST"
        const val COMPOSER = "COMPOSER"
        const val PRODUCER = "PRODUCER"
        const val COMMENT = "COMMENT"
        const val ENCODER = "ENCODER"
        const val COPYRIGHT = "COPYRIGHT"
    }
}
