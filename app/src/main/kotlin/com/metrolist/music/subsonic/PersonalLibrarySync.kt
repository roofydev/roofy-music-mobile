/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.subsonic

import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.PlaylistEntity
import com.metrolist.music.db.entities.PlaylistSongMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

data class PersonalLibraryPlaylistSyncResult(
    val importedPlaylists: Int,
    val updatedPlaylists: Int,
    val pushedPlaylists: Int,
    val remotePlaylists: Int,
)

data class PersonalLibraryHistorySyncResult(
    val pushedScrobbles: Int,
    val skippedEvents: Int,
    val lastSyncedEpochMs: Long,
)

data class PersonalLibraryFavoriteSyncResult(
    val importedFavorites: Int,
    val pushedFavorites: Int,
    val remoteFavorites: Int,
    val updatedFavorites: Int,
)

data class PersonalLibraryRatingSyncResult(
    val importedRatings: Int,
    val pushedRatings: Int,
    val remoteRatings: Int,
)

data class PersonalLibraryFullSyncResult(
    val favorites: PersonalLibraryFavoriteSyncResult,
    val history: PersonalLibraryHistorySyncResult,
    val playlists: PersonalLibraryPlaylistSyncResult,
    val ratings: PersonalLibraryRatingSyncResult,
)

object PersonalLibrarySync {
    suspend fun syncFavorites(
        database: MusicDatabase,
        client: SubsonicClient,
    ): PersonalLibraryFavoriteSyncResult =
        withContext(Dispatchers.IO) {
            val remoteSongs = client.getStarred2().song
            val remoteRawIds = remoteSongs.map { it.id }.toSet()
            val localLikedSongs = database.likedSubsonicSongs()
            val localOnlyRawIds =
                localLikedSongs
                    .mapNotNull { SubsonicClient.localIdFromMediaId(it.id) }
                    .filterNot(remoteRawIds::contains)
                    .distinct()

            localOnlyRawIds.forEach { client.starSong(it) }

            var imported = 0
            var updated = 0
            val now = LocalDateTime.now()

            database.withTransaction {
                remoteSongs.forEach { song ->
                    val metadata = song.toRoofyMetadata(client)
                    val remoteRating = song.userRating?.takeIf { it > 0 }
                    val existing = getSongByIdBlocking(metadata.id)
                    if (existing == null) {
                        insert(metadata) {
                            it.copy(
                                inLibrary = now,
                                liked = true,
                                likedDate = now,
                                subsonicUserRating = remoteRating,
                            )
                        }
                        imported += 1
                    } else {
                        val needsFavoriteUpdate =
                            !existing.song.liked || existing.song.inLibrary == null
                        val needsRatingUpdate = remoteRating != null && existing.song.subsonicUserRating != remoteRating
                        if (needsFavoriteUpdate || needsRatingUpdate) {
                            update(
                                existing.song.copy(
                                    inLibrary = existing.song.inLibrary ?: now,
                                    liked = true,
                                    likedDate = existing.song.likedDate ?: now,
                                    subsonicUserRating = remoteRating ?: existing.song.subsonicUserRating,
                                ),
                            )
                            updated += 1
                        }
                    }
                }
            }

            PersonalLibraryFavoriteSyncResult(
                importedFavorites = imported,
                pushedFavorites = localOnlyRawIds.size,
                remoteFavorites = remoteSongs.size,
                updatedFavorites = updated,
            )
        }

    suspend fun syncPlaylists(
        database: MusicDatabase,
        client: SubsonicClient,
    ): PersonalLibraryPlaylistSyncResult =
        withContext(Dispatchers.IO) {
            val remotePlaylists = client.getPlaylists()
            val remoteIds = remotePlaylists.map { it.id }.toSet()
            val localManaged = database.subsonicManagedPlaylists()
            val localByRemoteId =
                localManaged
                    .mapNotNull { playlist ->
                        subsonicRemoteIdFromBrowseId(playlist.browseId)?.let { it to playlist }
                    }
                    .toMap()

            var imported = 0
            var updated = 0

            remotePlaylists.forEach { remote ->
                val detailed = client.getPlaylist(remote.id)
                val localId = subsonicPlaylistLocalId(remote.id)
                val existing = localByRemoteId[remote.id]

                database.withTransaction {
                    val playlistEntity =
                        PlaylistEntity(
                            id = existing?.id ?: localId,
                            name = detailed.name,
                            browseId = subsonicPlaylistBrowseId(remote.id),
                            isEditable = true,
                            isAutoSync = true,
                            bookmarkedAt = existing?.bookmarkedAt ?: LocalDateTime.now(),
                            lastUpdateTime = LocalDateTime.now(),
                        )
                    if (existing == null) {
                        insert(playlistEntity)
                        imported += 1
                    } else {
                        update(playlistEntity)
                        updated += 1
                    }

                    val maps = playlistSongsBlocking(playlistEntity.id)
                    maps.forEach { delete(it.map) }

                    detailed.entry.forEachIndexed { index, song ->
                        val metadata = song.toRoofyMetadata(client)
                        insert(metadata)
                        insert(
                            PlaylistSongMap(
                                playlistId = playlistEntity.id,
                                songId = metadata.id,
                                position = index,
                            ),
                        )
                    }
                }
            }

            var pushed = 0
            localManaged
                .filter { playlist ->
                    playlist.browseId?.startsWith(SUBSONIC_PENDING_PLAYLIST_BROWSE_PREFIX) == true
                }
                .forEach { playlist ->
                    val songIds =
                        database
                            .playlistSongsBlocking(playlist.id)
                            .mapNotNull { entry ->
                                SubsonicClient.localIdFromMediaId(entry.song.id)
                            }
                            .distinct()
                    if (songIds.isEmpty()) return@forEach

                    client.createPlaylist(playlist.name, songIds)
                    val createdId =
                        client
                            .getPlaylists()
                            .filter { it.name == playlist.name && (it.songCount ?: 0) == songIds.size }
                            .maxByOrNull { it.changed.orEmpty() }
                            ?.id
                    if (createdId != null) {
                        database.withTransaction {
                            update(
                                playlist.copy(
                                    browseId = subsonicPlaylistBrowseId(createdId),
                                    lastUpdateTime = LocalDateTime.now(),
                                ),
                            )
                        }
                        pushed += 1
                    }
                }

            localManaged
                .mapNotNull { playlist ->
                    subsonicRemoteIdFromBrowseId(playlist.browseId)?.let { it to playlist }
                }
                .filter { (remoteId, _) -> remoteId in remoteIds }
                .forEach { (remoteId, playlist) ->
                    val remoteSongIds = client.getPlaylist(remoteId).entry.map { it.id }.toSet()
                    val toAdd =
                        database
                            .playlistSongsBlocking(playlist.id)
                            .mapNotNull { entry ->
                                SubsonicClient.localIdFromMediaId(entry.song.id)
                            }
                            .filterNot { it in remoteSongIds }
                            .distinct()
                    if (toAdd.isNotEmpty()) {
                        client.updatePlaylist(remoteId, toAdd)
                        pushed += 1
                    }
                }

            PersonalLibraryPlaylistSyncResult(
                importedPlaylists = imported,
                updatedPlaylists = updated,
                pushedPlaylists = pushed,
                remotePlaylists = remotePlaylists.size,
            )
        }

    suspend fun syncPlayHistory(
        database: MusicDatabase,
        client: SubsonicClient,
        lastSyncedEpochMs: Long,
    ): PersonalLibraryHistorySyncResult =
        withContext(Dispatchers.IO) {
            val since =
                Instant
                    .ofEpochMilli(lastSyncedEpochMs.coerceAtLeast(0))
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime()
            val events = database.subsonicEventsSince(since, limit = 250)
            var pushed = 0
            var skipped = 0
            var latestEpoch = lastSyncedEpochMs

            events.forEach { event ->
                val rawId = SubsonicClient.localIdFromMediaId(event.songId)
                if (rawId == null) {
                    skipped += 1
                    return@forEach
                }

                val eventEpoch =
                    event.timestamp
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                if (eventEpoch <= lastSyncedEpochMs) {
                    skipped += 1
                    return@forEach
                }

                runCatching {
                    client.scrobble(rawId, eventEpoch)
                }.onSuccess {
                    pushed += 1
                    if (eventEpoch > latestEpoch) {
                        latestEpoch = eventEpoch
                    }
                }.onFailure {
                    skipped += 1
                }
            }

            if (pushed == 0 && events.isNotEmpty()) {
                latestEpoch =
                    events
                        .maxOf {
                            it.timestamp
                                .atZone(ZoneId.systemDefault())
                                .toInstant()
                                .toEpochMilli()
                        }
                        .coerceAtLeast(lastSyncedEpochMs)
            } else if (pushed > 0) {
                latestEpoch = latestEpoch.coerceAtLeast(System.currentTimeMillis())
            }

            PersonalLibraryHistorySyncResult(
                pushedScrobbles = pushed,
                skippedEvents = skipped,
                lastSyncedEpochMs = latestEpoch,
            )
        }

    suspend fun syncRatings(
        database: MusicDatabase,
        client: SubsonicClient,
    ): PersonalLibraryRatingSyncResult =
        withContext(Dispatchers.IO) {
            val remoteSongs = client.getStarred2().song
            val remoteRatingById = remoteSongs.mapNotNull { song ->
                song.userRating?.takeIf { it > 0 }?.let { song.id to it }
            }.toMap()

            var imported = 0
            var pushed = 0

            database.subsonicSongsWithRating().forEach { local ->
                val rawId = SubsonicClient.localIdFromMediaId(local.id) ?: return@forEach
                val localRating = local.song.subsonicUserRating ?: return@forEach
                val remoteRating = remoteRatingById[rawId]
                if (remoteRating == null) {
                    client.setRating(rawId, localRating)
                    pushed += 1
                } else if (remoteRating != localRating) {
                    client.setRating(rawId, localRating)
                    pushed += 1
                }
            }

            database.withTransaction {
                remoteSongs.forEach { song ->
                    val rating = song.userRating?.takeIf { it > 0 } ?: return@forEach
                    val mediaId = SubsonicClient.mediaId(song.id)
                    val existing = getSongByIdBlocking(mediaId) ?: return@forEach
                    if (existing.song.subsonicUserRating != rating) {
                        update(existing.song.copy(subsonicUserRating = rating))
                        imported += 1
                    }
                }
            }

            PersonalLibraryRatingSyncResult(
                importedRatings = imported,
                pushedRatings = pushed,
                remoteRatings = remoteRatingById.size,
            )
        }

    suspend fun syncAll(
        database: MusicDatabase,
        client: SubsonicClient,
        lastSyncedEpochMs: Long,
    ): PersonalLibraryFullSyncResult =
        withContext(Dispatchers.IO) {
            val favorites = syncFavorites(database, client)
            val ratings = syncRatings(database, client)
            val playlists = syncPlaylists(database, client)
            val history = syncPlayHistory(database, client, lastSyncedEpochMs)
            PersonalLibraryFullSyncResult(
                favorites = favorites,
                ratings = ratings,
                playlists = playlists,
                history = history,
            )
        }
}
