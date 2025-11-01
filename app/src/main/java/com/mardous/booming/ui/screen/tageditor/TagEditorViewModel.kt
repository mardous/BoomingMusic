package com.mardous.booming.ui.screen.tageditor

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.*
import com.kyant.taglib.Picture
import com.mardous.booming.coil.CustomArtistImageManager
import com.mardous.booming.core.model.task.Result
import com.mardous.booming.data.local.EditTarget
import com.mardous.booming.data.local.MetadataReader
import com.mardous.booming.data.local.MetadataWriter
import com.mardous.booming.data.local.repository.Repository
import com.mardous.booming.data.model.Artist
import com.mardous.booming.data.remote.deezer.model.DeezerAlbum
import com.mardous.booming.data.remote.deezer.model.DeezerTrack
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
            emit(
                SaveTagsResult(
                    isLoading = false,
                    isSuccess = writeResult.isSuccess,
                    scanned = writeResult.scanned,
                    failed = writeResult.failed
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

    fun getAlbumInfo(artistName: String, albumName: String): LiveData<Result<DeezerAlbum>> =
        liveData(Dispatchers.IO) {
            emit(repository.deezerAlbum(artistName, albumName))
        }

    fun getTrackInfo(artistName: String, title: String): LiveData<Result<DeezerTrack>> =
        liveData(Dispatchers.IO) {
            emit(repository.deezerTrack(artistName, title))
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