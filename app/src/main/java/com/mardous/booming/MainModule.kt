/*
 * Copyright (c) 2024 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mardous.booming

import androidx.preference.PreferenceManager
import androidx.room.Room
import com.mardous.booming.coil.CustomArtistImageManager
import com.mardous.booming.coil.CustomPlaylistImageManager
import com.mardous.booming.core.BoomingDatabase
import com.mardous.booming.core.audio.AudioOutputObserver
import com.mardous.booming.core.audio.SoundSettings
import com.mardous.booming.data.local.AlbumCoverSaver
import com.mardous.booming.data.local.EditTarget
import com.mardous.booming.data.local.MediaStoreWriter
import com.mardous.booming.data.local.repository.*
import com.mardous.booming.data.model.Genre
import com.mardous.booming.data.remote.deezer.DeezerService
import com.mardous.booming.data.remote.github.GitHubService
import com.mardous.booming.data.remote.jsonHttpClient
import com.mardous.booming.data.remote.lastfm.LastFmService
import com.mardous.booming.data.remote.lyrics.LyricsDownloadService
import com.mardous.booming.data.remote.provideOkHttp
import com.mardous.booming.playback.SleepTimer
import com.mardous.booming.playback.equalizer.EqualizerManager
import com.mardous.booming.ui.screen.about.AboutViewModel
import com.mardous.booming.ui.screen.equalizer.EqualizerViewModel
import com.mardous.booming.ui.screen.info.InfoViewModel
import com.mardous.booming.ui.screen.library.LibraryViewModel
import com.mardous.booming.ui.screen.library.albums.AlbumDetailViewModel
import com.mardous.booming.ui.screen.library.artists.ArtistDetailViewModel
import com.mardous.booming.ui.screen.library.folders.FolderDetailViewModel
import com.mardous.booming.ui.screen.library.genres.GenreDetailViewModel
import com.mardous.booming.ui.screen.library.playlists.PlaylistDetailViewModel
import com.mardous.booming.ui.screen.library.search.SearchViewModel
import com.mardous.booming.ui.screen.library.years.YearDetailViewModel
import com.mardous.booming.ui.screen.lyrics.LyricsViewModel
import com.mardous.booming.ui.screen.player.PlayerViewModel
import com.mardous.booming.ui.screen.sound.SoundSettingsViewModel
import com.mardous.booming.ui.screen.tageditor.TagEditorViewModel
import com.mardous.booming.ui.screen.update.UpdateViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

val networkModule = module {
    factory {
        jsonHttpClient(okHttpClient = get())
    }
    factory {
        provideOkHttp(context = get())
    }
    single {
        GitHubService(context = androidContext(), client = get())
    }
    single {
        DeezerService(client = get())
    }
    single {
        LastFmService(client = get())
    }
    single {
        LyricsDownloadService(client = get())
    }
}

private val mainModule = module {
    single {
        androidContext().contentResolver
    }
    single {
        PreferenceManager.getDefaultSharedPreferences(androidContext())
    }
    single {
        SleepTimer(context = androidContext())
    }
    single {
        EqualizerManager(context = androidContext())
    }
    single {
        SoundSettings(context = androidContext())
    }
    single {
        MediaStoreWriter(context = androidContext(), contentResolver = get())
    }
    single {
        AlbumCoverSaver(context = androidContext(), mediaStoreWriter = get())
    }
    single {
        CustomArtistImageManager(context = androidContext())
    }
    single {
        CustomPlaylistImageManager(context = androidContext())
    }
    factory {
        AudioOutputObserver(context = androidContext())
    }
}

private val roomModule = module {
    single {
        Room.databaseBuilder(androidContext(), BoomingDatabase::class.java, "music_database.db")
            .addMigrations(BoomingDatabase.MIGRATION_1_2, BoomingDatabase.MIGRATION_2_3)
            .build()
    }

    factory {
        get<BoomingDatabase>().playlistDao()
    }

    factory {
        get<BoomingDatabase>().playCountDao()
    }

    factory {
        get<BoomingDatabase>().historyDao()
    }

    factory {
        get<BoomingDatabase>().queueDao()
    }

    factory {
        get<BoomingDatabase>().inclExclDao()
    }

    factory {
        get<BoomingDatabase>().lyricsDao()
    }
}

private val dataModule = module {
    single {
        RealRepository(
            context = androidContext(),
            deezerService = get(),
            lastFmService = get(),
            songRepository = get(),
            albumRepository = get(),
            artistRepository = get(),
            genreRepository = get(),
            smartRepository = get(),
            specialRepository = get(),
            playlistRepository = get(),
            searchRepository = get()
        )
    } bind Repository::class

    single {
        RealSongRepository(context = get(), inclExclDao = get())
    } bind SongRepository::class

    single {
        RealAlbumRepository(songRepository = get())
    } bind AlbumRepository::class

    single {
        RealArtistRepository(songRepository = get(), albumRepository = get())
    } bind ArtistRepository::class

    single {
        RealPlaylistRepository(
            context = androidContext(),
            songRepository = get(),
            playlistDao = get()
        )
    } bind PlaylistRepository::class

    single {
        RealGenreRepository(contentResolver = get(), songRepository = get())
    } bind GenreRepository::class

    single {
        RealSearchRepository(
            albumRepository = get(),
            songRepository = get(),
            artistRepository = get(),
            playlistRepository = get(),
            genreRepository = get(),
            specialRepository = get()
        )
    } bind SearchRepository::class

    single {
        RealSmartRepository(
            context = androidContext(),
            songRepository = get(),
            albumRepository = get(),
            artistRepository = get(),
            historyDao = get(),
            playCountDao = get()
        )
    } bind SmartRepository::class

    single {
        RealSpecialRepository(songRepository = get())
    } bind SpecialRepository::class

    single {
        RealLyricsRepository(
            context = androidContext(),
            preferences = get(),
            contentResolver = get(),
            lyricsDownloadService = get(),
            lyricsDao = get()
        )
    } bind LyricsRepository::class
}

private val viewModule = module {
    viewModel {
        LibraryViewModel(repository = get(), inclExclDao = get(), customPlaylistImageManager = get())
    }

    viewModel {
        PlayerViewModel(preferences = get(), repository = get(), albumCoverSaver = get())
    }

    viewModel {
        EqualizerViewModel(
            contentResolver = get(),
            equalizerManager = get(),
            mediaStoreWriter = get()
        )
    }

    viewModel { (albumId: Long) ->
        AlbumDetailViewModel(repository = get(), albumId = albumId)
    }

    viewModel { (artistId: Long, artistName: String?) ->
        ArtistDetailViewModel(repository = get(), artistId = artistId, artistName = artistName)
    }

    viewModel { (playlistId: Long) ->
        PlaylistDetailViewModel(playlistRepository = get(), playlistId = playlistId)
    }

    viewModel { (genre: Genre) ->
        GenreDetailViewModel(repository = get(), genre = genre)
    }

    viewModel { (year: Int) ->
        YearDetailViewModel(repository = get(), year = year)
    }

    viewModel { (path: String) ->
        FolderDetailViewModel(repository = get(), folderPath = path)
    }

    viewModel {
        SearchViewModel(repository = get())
    }

    viewModel { (target: EditTarget) ->
        TagEditorViewModel(
            repository = get(),
            customArtistImageManager = get(),
            target = target
        )
    }

    viewModel {
        LyricsViewModel(preferences = get(), lyricsRepository = get())
    }

    viewModel {
        InfoViewModel(repository = get())
    }

    viewModel {
        SoundSettingsViewModel(audioOutputObserver = get(), soundSettings = get())
    }

    viewModel {
        UpdateViewModel(updateService = get())
    }

    viewModel {
        AboutViewModel(repository = get())
    }
}

val appModules = listOf(networkModule, mainModule, roomModule, dataModule, viewModule)