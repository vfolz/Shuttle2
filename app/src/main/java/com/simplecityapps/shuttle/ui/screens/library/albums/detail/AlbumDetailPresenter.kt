package com.simplecityapps.shuttle.ui.screens.library.albums.detail

import android.content.Context
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.simplecityapps.mediaprovider.model.Album
import com.simplecityapps.mediaprovider.model.Song
import com.simplecityapps.mediaprovider.repository.AlbumQuery
import com.simplecityapps.mediaprovider.repository.AlbumRepository
import com.simplecityapps.mediaprovider.repository.SongQuery
import com.simplecityapps.mediaprovider.repository.SongRepository
import com.simplecityapps.playback.PlaybackManager
import com.simplecityapps.shuttle.ui.common.error.UserFriendlyError
import com.simplecityapps.shuttle.ui.common.mvp.BaseContract
import com.simplecityapps.shuttle.ui.common.mvp.BasePresenter
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

interface AlbumDetailContract {

    interface View {
        fun setData(songs: List<Song>)
        fun showLoadError(error: Error)
        fun onAddedToQueue(name: String)
        fun setAlbum(album: Album)
        fun showDeleteError(error: Error)
    }

    interface Presenter : BaseContract.Presenter<View> {
        fun loadData()
        fun onSongClicked(song: Song)
        fun shuffle()
        fun addToQueue(album: Album)
        fun addToQueue(song: Song)
        fun playNext(album: Album)
        fun playNext(song: Song)
        fun exclude(song: Song)
        fun delete(song: Song)
    }
}

class AlbumDetailPresenter @AssistedInject constructor(
    private val context: Context,
    private val albumRepository: AlbumRepository,
    private val songRepository: SongRepository,
    private val playbackManager: PlaybackManager,
    @Assisted private val album: Album
) : BasePresenter<AlbumDetailContract.View>(),
    AlbumDetailContract.Presenter {

    @AssistedInject.Factory
    interface Factory {
        fun create(album: Album): AlbumDetailPresenter
    }

    private var songs: List<Song> = emptyList()

    override fun bindView(view: AlbumDetailContract.View) {
        super.bindView(view)

        view.setAlbum(album)

        launch {
            albumRepository.getAlbums(AlbumQuery.Album(name = album.name, albumArtistName = album.albumArtist))
                .collect { albums ->
                    albums.firstOrNull()?.let { album ->
                        view.setAlbum(album)
                    }
                }
        }
    }

    override fun loadData() {
        launch {
            songRepository.getSongs(SongQuery.Album(name = album.name, albumArtistName = album.albumArtist))
                .collect { songs ->
                    this@AlbumDetailPresenter.songs = songs
                    view?.setData(songs)
                }
        }
    }

    override fun onSongClicked(song: Song) {
        launch {
            playbackManager.load(songs, songs.indexOf(song)) { result ->
                result.onSuccess { playbackManager.play() }
                result.onFailure { error -> view?.showLoadError(error as Error) }
            }
        }
    }

    override fun shuffle() {
        launch {
            playbackManager.shuffle(songs) { result ->
                result.onSuccess { playbackManager.play() }
                result.onFailure { error -> view?.showLoadError(error as Error) }
            }
        }
    }

    override fun addToQueue(album: Album) {
        launch {
            val songs = songRepository.getSongs(SongQuery.Albums(listOf(SongQuery.Album(name = album.name, albumArtistName = album.albumArtist)))).firstOrNull().orEmpty()
            playbackManager.addToQueue(songs)
            view?.onAddedToQueue(album.name)
        }
    }

    override fun addToQueue(song: Song) {
        launch {
            playbackManager.addToQueue(listOf(song))
            view?.onAddedToQueue(song.name)
        }
    }

    override fun playNext(album: Album) {
        launch {
            val songs = songRepository.getSongs(SongQuery.Albums(listOf(SongQuery.Album(name = album.name, albumArtistName = album.albumArtist)))).firstOrNull().orEmpty()
            playbackManager.playNext(songs)
            view?.onAddedToQueue(album.name)
        }
    }

    override fun playNext(song: Song) {
        launch {
            playbackManager.playNext(listOf(song))
            view?.onAddedToQueue(song.name)
        }
    }

    override fun exclude(song: Song) {
        launch {
            songRepository.setExcluded(listOf(song), true)
        }
    }

    override fun delete(song: Song) {
        val uri = song.path.toUri()
        val documentFile = DocumentFile.fromSingleUri(context, uri)
        if (documentFile?.delete() == true) {
            launch {
                songRepository.removeSong(song)
            }
        } else {
            view?.showDeleteError(UserFriendlyError("The song couldn't be deleted"))
        }
    }
}