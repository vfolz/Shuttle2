package com.simplecityapps.mediaprovider.repository

import com.simplecityapps.mediaprovider.MediaProvider
import com.simplecityapps.mediaprovider.model.Song
import com.simplecityapps.mediaprovider.model.removeArticles
import kotlinx.coroutines.flow.Flow
import java.io.Serializable
import java.text.Collator
import java.util.*
import kotlin.Comparator

interface SongRepository {
    fun getSongs(query: SongQuery): Flow<List<Song>>
    suspend fun insert(songs: List<Song>, mediaProviderType: MediaProvider.Type)
    suspend fun update(song: Song): Int
    suspend fun update(songs: List<Song>)
    suspend fun remove(song: Song)
    suspend fun removeAll(mediaProviderType: MediaProvider.Type)
    suspend fun insertUpdateAndDelete(inserts: List<Song>, updates: List<Song>, deletes: List<Song>, mediaProviderType: MediaProvider.Type): Triple<Int, Int, Int>
    suspend fun incrementPlayCount(song: Song)
    suspend fun setPlaybackPosition(song: Song, playbackPosition: Int)
    suspend fun setExcluded(songs: List<Song>, excluded: Boolean)
    suspend fun clearExcludeList()
}

open class SongQuery(
    val predicate: ((Song) -> Boolean),
    val sortOrder: SongSortOrder = SongSortOrder.Default,
    val includeExcluded: Boolean = false,
    val providerType: MediaProvider.Type? = null
) : Serializable {

    class All(includeExcluded: Boolean = false, sortOrder: SongSortOrder = SongSortOrder.Default, providerType: MediaProvider.Type? = null) :
        SongQuery(
            predicate = { true },
            sortOrder = sortOrder,
            includeExcluded = includeExcluded,
            providerType = providerType
        )

    class AlbumArtist(val name: String) :
        SongQuery(
            predicate = { song -> song.albumArtist.equals(name, ignoreCase = true) }
        )

    class AlbumArtists(val albumArtists: List<AlbumArtist>) :
        SongQuery(
            predicate = { song -> albumArtists.any { albumArtist -> albumArtist.predicate(song) } },
            sortOrder = SongSortOrder.Track
        )

    class Album(val name: String, val albumArtistName: String) :
        SongQuery(
            predicate = { song -> song.album.equals(name, ignoreCase = true) && song.albumArtist.equals(albumArtistName, ignoreCase = true) }
        )

    class Albums(val albums: List<Album>) :
        SongQuery(
            predicate = { song -> albums.any { album -> Album(name = album.name, albumArtistName = album.albumArtistName).predicate(song) } },
            sortOrder = SongSortOrder.Track
        )

    class SongIds(val songIds: List<Long>) :
        SongQuery(
            predicate = { song -> songIds.contains(song.id) }
        )

    class LastPlayed(val after: Date) :
        SongQuery(
            predicate = { song -> song.lastPlayed?.after(after) ?: false },
            sortOrder = SongSortOrder.RecentlyPlayed
        )

    class LastCompleted(val after: Date) :
        SongQuery(
            predicate = { song -> song.lastCompleted?.after(after) ?: false },
            sortOrder = SongSortOrder.RecentlyPlayed
        )

    class Search(val query: String) :
        SongQuery(
            predicate = { song -> song.name.contains(query, true) || song.album.contains(query, true) || song.albumArtist.contains(query, true) }
        )

    class PlayCount(val count: Int, sortOrder: SongSortOrder) :
        SongQuery(
            predicate = { song -> song.playCount >= count },
            sortOrder = sortOrder
        )

    // Todo: This isn't really 'recently added', any songs which have had their contents modified will show up here.
    //   Best to add a 'dateAdded' column.
    class RecentlyAdded :
        SongQuery(
            predicate = { song -> (Date().time - song.lastModified.time < (2 * 7 * 24 * 60 * 60 * 1000L)) },
            sortOrder = SongSortOrder.RecentlyAdded
        ) // 2 weeks
}

enum class SongSortOrder : Serializable {
    Default, SongName, ArtistName, AlbumName, Year, Duration, Track, PlayCount, RecentlyAdded, RecentlyPlayed;

    val comparator: Comparator<Song>
        get() {
            return when (this) {
                Default -> defaultComparator
                SongName -> songNameComparator
                ArtistName -> artistNameComparator
                AlbumName -> albumNameComparator
                Year -> yearComparator
                Duration -> durationComparator
                Track -> trackComparator
                PlayCount -> playCountComparator
                RecentlyAdded -> recentlyAddedComparator
                RecentlyPlayed -> recentlyPlayedComparator
            }
        }

    companion object {
        private val collator: Collator by lazy { Collator.getInstance().apply { strength = Collator.TERTIARY } }
        val defaultComparator: Comparator<Song> by lazy { compareBy({ song -> song.album }, { song -> song.disc }, { song -> song.track }) }
        val songNameComparator: Comparator<Song> by lazy { Comparator<Song> { a, b -> collator.compare(a.name, b.name) }.then(defaultComparator) }
        val artistNameComparator: Comparator<Song> by lazy {
            Comparator<Song> { a, b ->
                collator.compare(
                    a.albumArtist.removeArticles(),
                    b.albumArtist.removeArticles()
                )
            }.then(compareBy { song -> song.album }).then(defaultComparator)
        }
        val albumNameComparator: Comparator<Song> by lazy { Comparator<Song> { a, b -> collator.compare(a.album.removeArticles(), b.album.removeArticles()) }.then(defaultComparator) }
        val yearComparator: Comparator<Song> by lazy {
            Comparator<Song> { a, b -> zeroLastComparator.compare(a.year, b.year) }.then(compareBy({ song -> song.year }, { song -> song.album })).then(defaultComparator)
        }
        val durationComparator: Comparator<Song> by lazy { compareBy<Song> { song -> song.duration }.then(defaultComparator) }
        val trackComparator: Comparator<Song> by lazy { compareBy<Song>({ song -> song.disc }, { song -> song.track }).then(defaultComparator) }
        val playCountComparator: Comparator<Song> by lazy { compareByDescending<Song> { song -> song.playCount }.then(defaultComparator) }
        val recentlyAddedComparator: Comparator<Song> by lazy { compareByDescending<Song> { song -> song.lastModified.time / 1000 / 60 }.then(defaultComparator) } // Round to the nearest minute
        val recentlyPlayedComparator: Comparator<Song> by lazy { compareByDescending<Song> { song -> song.lastCompleted }.then(defaultComparator) }
    }
}