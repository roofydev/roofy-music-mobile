/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.metrolist.music.R
import com.metrolist.music.playback.PlayerConnection
import com.metrolist.music.constants.CONTENT_TYPE_LIST
import com.metrolist.music.db.entities.Album
import com.metrolist.music.db.entities.Artist
import com.metrolist.music.db.entities.Playlist
import com.metrolist.music.db.entities.Song
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.ui.component.MenuState
import com.metrolist.music.ui.menu.SongMenu
import com.metrolist.music.ui.theme.RetroListItem
import com.metrolist.music.ui.theme.RetroTokens
import com.metrolist.music.viewmodels.LocalFilter
import com.metrolist.music.viewmodels.LocalSearchResult

@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.localSearchResultsSection(
    query: String,
    result: LocalSearchResult,
    navController: NavController,
    playerConnection: PlayerConnection,
    menuState: MenuState,
) {

    if (query.isBlank() || result.map.values.all { it.isEmpty() }) {
        return
    }

    item(key = "local_search_header") {
        Text(
            text = stringResource(R.string.search_in_library_section),
            style = MaterialTheme.typography.labelLarge,
            color = RetroTokens.TextHot,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }

    result.map.forEach { (filter, items) ->
        if (items.isEmpty()) return@forEach

        val preview = items.take(3)
        item(key = "local_search_group_$filter") {
            Text(
                text =
                    stringResource(
                        when (filter) {
                            LocalFilter.SONG -> R.string.filter_songs
                            LocalFilter.ALBUM -> R.string.filter_albums
                            LocalFilter.ARTIST -> R.string.filter_artists
                            LocalFilter.PLAYLIST -> R.string.filter_playlists
                            LocalFilter.ALL -> R.string.filter_all
                        },
                    ),
                style = MaterialTheme.typography.labelMedium,
                color = RetroTokens.TextSoft,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        items(
            items = preview,
            key = { item -> "local_search_${filter.name}_${item.id}" },
            contentType = { CONTENT_TYPE_LIST },
        ) { item ->
            when (item) {
                is Song -> {
                    val songs = preview.filterIsInstance<Song>()
                    RetroListItem(
                        title = item.song.title,
                        subtitle = item.artists.joinToString { it.name },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        playerConnection.playQueue(
                                            ListQueue(
                                                title = query,
                                                items = songs.map { it.toMediaItem() },
                                                startIndex = songs.indexOf(item),
                                            ),
                                        )
                                    },
                                    onLongClick = {
                                        menuState.show {
                                            SongMenu(
                                                originalSong = item,
                                                navController = navController,
                                                onDismiss = menuState::dismiss,
                                            )
                                        }
                                    },
                                ),
                    )
                }

                is Album -> {
                    RetroListItem(
                        title = item.album.title,
                        subtitle = item.artists.joinToString { it.name },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { navController.navigate("album/${item.id}") },
                                ),
                    )
                }

                is Artist -> {
                    RetroListItem(
                        title = item.artist.name,
                        subtitle = null,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { navController.navigate("artist/${item.id}") },
                                ),
                    )
                }

                is Playlist -> {
                    RetroListItem(
                        title = item.playlist.name,
                        subtitle = null,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        navController.navigate("local_playlist/${item.id}")
                                    },
                                ),
                    )
                }
            }
        }
    }
}
