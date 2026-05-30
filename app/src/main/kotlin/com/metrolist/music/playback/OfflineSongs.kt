/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback

import android.content.Context
import androidx.media3.datasource.cache.SimpleCache
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.constants.HideVideoSongsKey
import com.metrolist.music.constants.SongSortType
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.Song
import com.metrolist.music.extensions.filterExplicit
import com.metrolist.music.extensions.filterVideoSongs
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import java.time.LocalDateTime

/**
 * Saved offline downloads plus songs in the streaming buffer (formerly a separate "Cached" playlist).
 */
fun offlineSongsFlow(
    database: MusicDatabase,
    context: Context,
    playerCache: SimpleCache,
    downloadCache: SimpleCache,
    sortType: SongSortType,
    descending: Boolean,
): Flow<List<Song>> =
    combine(
        database.downloadedSongs(sortType, descending),
        streamBufferSongsFlow(database, playerCache, downloadCache),
        context.dataStore.data,
    ) { downloaded, streamBuffered, prefs ->
        val hideExplicit = prefs[HideExplicitKey] ?: false
        val hideVideoSongs = prefs[HideVideoSongsKey] ?: false

        mergeOfflineSongs(
            downloaded = downloaded,
            streamBuffered = streamBuffered,
            sortType = sortType,
            descending = descending,
        )
            .filterExplicit(hideExplicit)
            .filterVideoSongs(hideVideoSongs)
    }

private fun streamBufferSongsFlow(
    database: MusicDatabase,
    playerCache: SimpleCache,
    downloadCache: SimpleCache,
): Flow<List<Song>> =
    flow {
        while (true) {
            val cachedIds = playerCache.keys.toSet()
            val downloadedIds = downloadCache.keys.toSet()
            val streamOnlyIds = cachedIds.subtract(downloadedIds)

            val songs =
                if (streamOnlyIds.isNotEmpty()) {
                    database.getSongsByIds(streamOnlyIds.toList())
                } else {
                    emptyList()
                }

            val completeSongs =
                songs.filter { song ->
                    val contentLength = song.format?.contentLength
                    contentLength != null &&
                        playerCache.isCached(song.song.id, 0, contentLength)
                }

            if (completeSongs.isNotEmpty()) {
                database.query {
                    completeSongs.forEach { song ->
                        if (song.song.dateDownload == null) {
                            update(song.song.copy(dateDownload = LocalDateTime.now()))
                        }
                    }
                }
            }

            emit(
                completeSongs
                    .filter { it.song.dateDownload != null }
                    .distinctBy { it.id },
            )
            delay(1_000)
        }
    }

private fun mergeOfflineSongs(
    downloaded: List<Song>,
    streamBuffered: List<Song>,
    sortType: SongSortType,
    descending: Boolean,
): List<Song> {
    val merged =
        (downloaded + streamBuffered)
            .distinctBy { it.id }

    return when (sortType) {
        SongSortType.CREATE_DATE ->
            merged.sortedBy { it.song.dateDownload ?: LocalDateTime.MIN }
        SongSortType.NAME ->
            merged.sortedBy { it.song.title.lowercase() }
        SongSortType.ARTIST ->
            merged.sortedBy { song ->
                song.artists.joinToString { it.name }.lowercase()
            }
        SongSortType.PLAY_TIME ->
            merged.sortedBy { it.song.totalPlayTime }
    }.let { sorted ->
        if (descending) sorted.reversed() else sorted
    }
}
