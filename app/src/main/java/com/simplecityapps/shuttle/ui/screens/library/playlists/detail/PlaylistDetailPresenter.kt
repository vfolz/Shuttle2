package com.simplecityapps.shuttle.ui.screens.library.playlists.detail

import android.content.Context
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.simplecityapps.mediaprovider.model.Playlist
import com.simplecityapps.mediaprovider.model.Song
import com.simplecityapps.mediaprovider.repository.PlaylistQuery
import com.simplecityapps.mediaprovider.repository.PlaylistRepository
import com.simplecityapps.mediaprovider.repository.SongRepository
import com.simplecityapps.playback.PlaybackManager
import com.simplecityapps.shuttle.ui.common.error.UserFriendlyError
import com.simplecityapps.shuttle.ui.common.mvp.BaseContract
import com.simplecityapps.shuttle.ui.common.mvp.BasePresenter
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber

interface PlaylistDetailContract {

    interface View {
        fun setData(songs: List<Song>)
        fun showLoadError(error: Error)
        fun onAddedToQueue(song: Song)
        fun setPlaylist(playlist: Playlist)
        fun showDeleteError(error: Error)
    }

    interface Presenter : BaseContract.Presenter<View> {
        fun loadData()
        fun onSongClicked(song: Song)
        fun shuffle()
        fun addToQueue(song: Song)
        fun playNext(song: Song)
        fun exclude(song: Song)
        fun remove(song: Song)
        fun delete(song: Song)
    }
}

class PlaylistDetailPresenter @AssistedInject constructor(
    private val context: Context,
    private val playlistRepository: PlaylistRepository,
    private val songRepository: SongRepository,
    private val playbackManager: PlaybackManager,
    @Assisted private val playlist: Playlist
) : BasePresenter<PlaylistDetailContract.View>(),
    PlaylistDetailContract.Presenter {

    @AssistedInject.Factory
    interface Factory {
        fun create(playlist: Playlist): PlaylistDetailPresenter
    }

    private var songs: List<Song> = emptyList()

    override fun bindView(view: PlaylistDetailContract.View) {
        super.bindView(view)

        view.setPlaylist(playlist)

        launch {
            playlistRepository.getPlaylists(PlaylistQuery.PlaylistId(playlist.id))
                .collect { playlists ->
                    playlists.firstOrNull()?.let { playlist ->
                        view.setPlaylist(playlist)
                    }
                }
        }
    }

    override fun loadData() {
        launch {
            playlistRepository.getSongsForPlaylist(playlist.id)
                .collect { songs ->
                    this@PlaylistDetailPresenter.songs = songs
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
        if (songs.isNotEmpty()) {
            launch {
                playbackManager.shuffle(songs) { result ->
                    result.onSuccess { playbackManager.play() }
                    result.onFailure { error -> view?.showLoadError(error as Error) }
                }
            }
        } else {
            Timber.i("Shuffle failed: Songs list empty")
        }
    }

    override fun addToQueue(song: Song) {
        launch {
            playbackManager.addToQueue(listOf(song))
            view?.onAddedToQueue(song)
        }
    }

    override fun playNext(song: Song) {
        launch {
            playbackManager.playNext(listOf(song))
            view?.onAddedToQueue(song)
        }
    }

    override fun exclude(song: Song) {
        launch {
            songRepository.setExcluded(listOf(song), true)
        }
    }

    override fun remove(song: Song) {
        launch {
            playlistRepository.removeFromPlaylist(playlist, listOf(song))
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