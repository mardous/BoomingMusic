package com.mardous.booming.data.local

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.os.Parcelable
import android.util.Log
import com.kyant.taglib.Picture
import com.kyant.taglib.TagLib
import com.mardous.booming.data.model.Album
import com.mardous.booming.data.model.Artist
import com.mardous.booming.data.model.Song
import com.mardous.booming.extensions.media.createAlbumArtThumbFile
import com.mardous.booming.extensions.media.deleteAlbumArt
import com.mardous.booming.extensions.media.insertAlbumArt
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import kotlinx.parcelize.Parcelize
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.coroutines.resume

class MetadataWriter : KoinComponent {

    private val contentResolver: ContentResolver by inject()
    private val lock = Any()

    private var properties = mapOf<String, String?>()
    private var pictureBitmap: Bitmap? = null
    private var pictureDeleted = false

    fun picture(pictureBitmap: Bitmap?) = synchronized(lock) {
        this.pictureBitmap = pictureBitmap
        this.pictureDeleted = false
    }

    fun pictureDeleted(pictureDeleted: Boolean) = synchronized(lock) {
        this.pictureDeleted = pictureDeleted
        this.pictureBitmap = if (pictureDeleted) null else pictureBitmap
    }

    fun propertyMap(propertyMap: Map<String, String?>) = synchronized(lock) {
        this.properties = propertyMap
    }

    suspend fun write(context: Context, target: EditTarget) = withContext(IO) {
        val results = mutableListOf<WriteResultInternal>()
        val picture = createPicture(target)
        val pictureThumbFile = createPictureThumbFile(picture)
        for (content in target.contents) {
            val result = runCatching {
                contentResolver.openFileDescriptor(content.uri, "rw")?.use { fd ->
                    WriteResultInternal(
                        content = content,
                        pictureResult = writePicture(picture, fd),
                        propertiesResult = writePropertyMap(fd)
                    )
                } ?: WriteResultInternal(content)
            }
            if (result.isSuccess) {
                results.add(result.getOrThrow())
                contentResolver.notifyChange(content.uri, null)
            } else {
                Log.e("MetadataWriter", "Failed to write metadata for ${content.uri}", result.exceptionOrNull())
            }
        }
        if (target.hasArtwork) {
            val wrotePicture = results.any { it.pictureResult == Result.Wrote }
            val deletedPicture = results.any { it.pictureResult == Result.Deleted }
            if (wrotePicture) {
                if (pictureThumbFile != null) {
                    contentResolver.insertAlbumArt(target.artworkId, pictureThumbFile.path)
                }
            } else if (deletedPicture) {
                contentResolver.deleteAlbumArt(target.artworkId)
            }
        }
        val successContents = results.filter { it.isSuccess }
            .map { it -> it.content }
        val paths = successContents.map { content -> content.path }.toTypedArray()
        if (paths.isNotEmpty()) {
            val total = paths.size
            val scanned = suspendCancellableCoroutine { continuation ->
                var progress = 0
                MediaScannerConnection.scanFile(context, paths, null) { _, _ ->
                    progress++
                    if (progress == total && continuation.isActive) {
                        continuation.resume(total)
                    }
                }
            }
            WriteResult(successContents, failed = total - scanned, scanned = scanned)
        } else {
            WriteResult(emptyList())
        }
    }

    private fun createPicture(target: EditTarget): Picture? {
        val pictureBitmap = this.pictureBitmap
        if (target.artworkId == -1L || pictureBitmap == null) {
            return null
        }
        val byteArray = ByteArrayOutputStream(pictureBitmap.byteCount).use {
            pictureBitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
            it.toByteArray()
        }
        return Picture(
            data = byteArray,
            description = "Embedded Front Cover - Booming Music",
            pictureType = "Front Cover",
            mimeType = "image/jpeg"
        )
    }

    private fun createPictureThumbFile(picture: Picture?): File? {
        if (picture != null) {
            val result = createAlbumArtThumbFile().canonicalFile
            try {
                return result.also { it.writeBytes(picture.data) }
            } catch (e: IOException) {
                Log.e("MetadataWriter", "Failed to create album thumb file", e)
                try {
                    if (result.delete()) {
                        Log.v("MetadataWriter", "Deleted empty album thumb file")
                    }
                } catch (e: Exception) {
                    Log.e("MetadataWriter", "Failed to delete empty album thumb file", e)
                }
            }
        }
        return null
    }

    private fun writePicture(picture: Picture?, fd: ParcelFileDescriptor): Result {
        if (picture == null) {
            if (pictureDeleted) {
                if (TagLib.savePictures(fd.dup().detachFd(), arrayOf())) {
                    return Result.Deleted
                }
            } else {
                return Result.None
            }
        } else {
            if (TagLib.savePictures(fd.dup().detachFd(), arrayOf(picture))) {
                return Result.Wrote
            }
        }
        return Result.Failed
    }

    private fun writePropertyMap(fd: ParcelFileDescriptor): Result {
        val currentProperties = TagLib.getMetadata(fd.dup().detachFd(), false)
                ?.propertyMap ?: hashMapOf()

        for ((key, newValueRaw) in properties) {
            val newValue = if (newValueRaw.isNullOrBlank()) {
                emptyArray()
            } else {
                arrayOf(newValueRaw)
            }
            currentProperties[key] = newValue
        }

        val newProperties = currentProperties
            .filterValues { it.isNotEmpty() }
            .mapValuesTo(hashMapOf()) { it.value }

        return if (TagLib.savePropertyMap(fd.dup().detachFd(), newProperties)) {
            Result.Wrote
        } else {
            Result.Failed
        }
    }

    class WriteResult(
        val contents: List<EditTarget.Content>,
        val failed: Int = 0,
        val scanned: Int = 0
    ) {
        val isSuccess: Boolean
            get() = contents.isNotEmpty() && scanned == contents.size
    }

    private class WriteResultInternal(
        val content: EditTarget.Content,
        val pictureResult: Result = Result.Failed,
        val propertiesResult: Result = Result.Failed
    ) {
        val isSuccess: Boolean
            get() = pictureResult != Result.Failed && propertiesResult != Result.Failed
    }

    private enum class Result {
        None, Wrote, Deleted, Failed
    }
}

@Parcelize
data class EditTarget(
    val type: Type,
    val name: String,
    val id: Long,
    val artworkId: Long,
    val contents: List<Content>
) : Parcelable {

    val hasContent get() = contents.isNotEmpty()

    val hasArtwork get() = artworkId > -1

    val first get() = contents.first()

    @Parcelize
    data class Content(val id: Long, val uri: Uri, val path: String) : Parcelable

    enum class Type {
        Song, Artist, AlbumArtist, Album
    }

    companion object {
        val Empty = EditTarget(Type.Song, "", -1, -1, emptyList())

        fun song(song: Song) = EditTarget(
            type = Type.Song,
            name = song.title,
            id = song.id,
            artworkId = song.albumId,
            contents = listOf(Content(song.id, song.uri, song.data))
        )

        fun album(album: Album) = EditTarget(
            type = Type.Album,
            name = album.name,
            id = album.id,
            artworkId = album.id,
            contents = album.songs.map { Content(it.id, it.uri, it.data) }
        )

        fun artist(artist: Artist) = EditTarget(
            type = if (artist.isAlbumArtist) Type.AlbumArtist else Type.Artist,
            name = artist.name,
            id = artist.id,
            artworkId = -1,
            contents = artist.songs.map { Content(it.id, it.uri, it.data) }
        )
    }
}