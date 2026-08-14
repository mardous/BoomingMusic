package com.mardous.booming.ui.screen.tageditor

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.kyant.taglib.Picture
import com.mardous.booming.coil.CustomArtistImageManager
import com.mardous.booming.data.local.EditTarget
import com.mardous.booming.data.local.MetadataReader
import com.mardous.booming.data.local.MetadataWriter
import com.mardous.booming.data.repository.Repository
import com.mardous.booming.data.model.Artist
import com.mardous.booming.data.remote.musicbrainz.artworkUrl
import com.mardous.booming.data.remote.musicbrainz.model.MusicBrainzRecording
import com.mardous.booming.extensions.utilities.capitalize
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @author Christians M. A. (mardous)
 */
class TagEditorViewModel(
    private val repository: Repository,
    private val customArtistImageManager: CustomArtistImageManager,
    private val target: EditTarget
) : ViewModel() {

    private val ioHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Failed to read file tags", throwable)
    }

    private var artist: Artist? = null
    private val metadataWriter = MetadataWriter()

    private val _tagResult = MutableLiveData<TagEditorResult>()
    val tagResult: LiveData<TagEditorResult> = _tagResult

    private val _artworkResult = MutableLiveData<Picture?>()
    val artworkResult: LiveData<Picture?> = _artworkResult

    val uris get() = target.contents.map { it.uri }

    fun setPictureBitmap(pictureBitmap: Bitmap?) {
        metadataWriter.picture(pictureBitmap)
    }

    fun setPictureDeleted(pictureDeleted: Boolean) {
        metadataWriter.pictureDeleted(pictureDeleted)
    }

    fun write(context: Context, propertyMap: Map<String, String?>) = liveData(Dispatchers.IO) {
        emit(SaveTagsResult(isLoading = true, isSuccess = false))
        val result = runCatching {
            metadataWriter.propertyMap(propertyMap)
            metadataWriter.write(context, target)
        }
        if (result.isSuccess) {
            val writeResult = result.getOrThrow()
            if (writeResult.successIds.isNotEmpty()) {
                repository.updatePlaylistsContainingIds(writeResult.successIds)
            }
            emit(
                SaveTagsResult(
                    isLoading = false,
                    isSuccess = writeResult.isSuccess
                )
            )
        } else {
            emit(SaveTagsResult(isLoading = false, isSuccess = false))
        }

    }

    fun loadContent() = viewModelScope.launch(Dispatchers.IO + ioHandler) {
        if (target.hasContent) {
            val metadataReader = MetadataReader(target.first.uri, target.hasArtwork)
            if (metadataReader.hasMetadata) {
                val newValue = TagEditorResult(
                    title = metadataReader.first(MetadataReader.TITLE),
                    album = metadataReader.first(MetadataReader.ALBUM),
                    artist = metadataReader.merge(MetadataReader.ARTIST),
                    albumArtist = metadataReader.first(MetadataReader.ALBUM_ARTIST),
                    composer = metadataReader.merge(MetadataReader.COMPOSER),
                    conductor = metadataReader.merge(MetadataReader.PRODUCER),
                    publisher = metadataReader.merge(MetadataReader.COPYRIGHT),
                    genre = metadataReader.merge(MetadataReader.GENRE),
                    year = metadataReader.first(MetadataReader.YEAR),
                    trackNumber = metadataReader.value(MetadataReader.TRACK_NUMBER),
                    trackTotal = metadataReader.value(MetadataReader.TRACK_TOTAL),
                    discNumber = metadataReader.value(MetadataReader.DISC_NUMBER),
                    discTotal = metadataReader.value(MetadataReader.DISC_TOTAL),
                    lyrics = metadataReader.value(MetadataReader.LYRICS),
                    lyricist = metadataReader.merge(MetadataReader.LYRICIST),
                    arranger = metadataReader.merge(MetadataReader.ARRANGER),
                    comment = metadataReader.value(MetadataReader.COMMENT)
                )
                _tagResult.postValue(newValue)
            }
            _artworkResult.postValue(metadataReader.frontCover())
        }
    }

    fun loadArtwork() = viewModelScope.launch(Dispatchers.IO + ioHandler) {
        if (target.hasArtwork) {
            val metadataReader = MetadataReader(target.first.uri, readPictures = true)
            val picture = metadataReader.frontCover()
            if (picture != null) {
                _artworkResult.postValue(picture)
            }
        }
    }

    fun setArtistImage(uri: Uri) = liveData(Dispatchers.IO) {
        val artist = fetchArtist()
        if (artist != Artist.empty) {
            emit(customArtistImageManager.setCustomImage(artist, uri))
        } else {
            emit(false)
        }
    }

    fun resetArtistImage() = liveData(Dispatchers.IO) {
        val artist = fetchArtist()
        if (artist != Artist.empty) {
            emit(customArtistImageManager.removeCustomImage(artist))
        } else {
            emit(false)
        }
    }

    fun getAlbumInfo(artistName: String, albumName: String) = liveData(Dispatchers.IO) {
        emit(repository.deezerAlbum(artistName, albumName))
    }

    fun getTrackInfo(artistName: String, title: String) = liveData(Dispatchers.IO) {
        emit(repository.deezerTrack(artistName, title))
    }

    fun fetchMusicBrainzTags(title: String, artist: String) = liveData(Dispatchers.IO) {
        emit(repository.musicBrainzRecording(title, artist))
    }

    fun fetchMusicBrainzRecordingDetails(recording: MusicBrainzRecording) = liveData(Dispatchers.IO) {
        val details = repository.musicBrainzRecordingDetails(recording.id)
        emit(mapRecordingToTags(details ?: recording))
    }

    private suspend fun mapRecordingToTags(recording: MusicBrainzRecording) = withContext(Dispatchers.Default) {
        val title = recording.title
        val artist = recording.artistCredit.joinToString("; ") { it.name }

        val firstRelease = recording.releases.firstOrNull()
        val genres = recording.genres.ifEmpty { firstRelease?.genres.orEmpty() }
            .map { it.name.capitalize() }
            .distinct()
            .take(5)
            .joinToString("; ")

        val composers = mutableListOf<String>()
        val lyricists = mutableListOf<String>()

        recording.relations.forEach { rel ->
            if (rel.targetType == "artist") {
                when (rel.type) {
                    "composer" -> rel.artist?.name?.let { composers.add(it) }
                    "lyricist" -> rel.artist?.name?.let { lyricists.add(it) }
                }
            } else if (rel.targetType == "work") {
                rel.work?.relations?.forEach { workRel ->
                    if (workRel.targetType == "artist") {
                        when (workRel.type) {
                            "composer" -> workRel.artist?.name?.let { composers.add(it) }
                            "lyricist" -> workRel.artist?.name?.let { lyricists.add(it) }
                        }
                    }
                }
            }
        }

        var album: String? = null
        var albumArtist: String? = null
        var year: String? = null
        var trackTotal: String? = null
        var discNumber: String? = null
        var discTotal: String? = null
        var trackNumber: String? = null

        if (firstRelease != null) {
            album = firstRelease.title
            albumArtist = firstRelease.artistCredit.joinToString("; ") { it.name }

            val date = firstRelease.date ?: recording.firstReleaseDate
            if (!date.isNullOrBlank()) {
                year = date.split("-").firstOrNull()
            }

            val firstMedia = firstRelease.media.firstOrNull()
            if (firstMedia != null) {
                discNumber = firstMedia.position?.toString()
                discTotal = firstRelease.media.size.toString()
                trackNumber = firstMedia.tracks.firstOrNull()?.number
                trackTotal = firstMedia.trackCount?.toString()
            }
        }

        firstRelease?.artworkUrl to TagEditorResult(
            title = title,
            album = album,
            artist = artist,
            albumArtist = if (!albumArtist.isNullOrBlank()) albumArtist else null,
            composer = if (composers.isNotEmpty()) composers.distinct().joinToString("; ") else null,
            genre = genres.ifBlank { null },
            year = year,
            trackNumber = trackNumber,
            trackTotal = trackTotal,
            discNumber = discNumber,
            discTotal = discTotal,
            lyricist = if (lyricists.isNotEmpty()) lyricists.distinct().joinToString("; ") else null,
            comment = if (!recording.disambiguation.isNullOrBlank()) recording.disambiguation else null
        )
    }

    fun requestArtist(): LiveData<Artist> = liveData(Dispatchers.IO) {
        val artist = fetchArtist()
        if (artist != Artist.empty) {
            emit(artist)
        }
    }

    private fun fetchArtist(): Artist {
        if (artist == null) {
            artist = if (target.type == EditTarget.Type.AlbumArtist) {
                repository.albumArtistByName(target.name)
            } else {
                repository.artistById(target.id)
            }
        }
        return artist!!
    }

    companion object {
        val TAG: String = TagEditorViewModel::class.java.simpleName
    }
}