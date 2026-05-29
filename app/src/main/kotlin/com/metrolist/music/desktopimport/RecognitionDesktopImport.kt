/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.desktopimport

import com.metrolist.innertube.YouTube
import com.metrolist.shazamkit.models.RecognitionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object RecognitionDesktopImport {
    suspend fun queueRecognizedTrack(
        endpointUrl: String,
        token: String,
        result: RecognitionResult,
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            val videoId =
                result.youtubeVideoId?.takeIf { it.isNotBlank() }
                    ?: resolveYoutubeVideoId(result)
                    ?: return@withContext Result.failure(
                        IllegalStateException("Could not find a YouTube match for this track."),
                    )

            val trackUrl = "https://music.youtube.com/watch?v=$videoId"
            DesktopImportClient.sendImport(
                endpointUrl = endpointUrl,
                token = token,
                track =
                    DesktopImportTrack(
                        artist = result.artist,
                        artists = listOf(result.artist),
                        thumbnailUrl = result.coverArtHqUrl ?: result.coverArtUrl,
                        title = result.title,
                        url = trackUrl,
                        videoId = videoId,
                    ),
            )
        }

    private suspend fun resolveYoutubeVideoId(result: RecognitionResult): String? {
        val query = "${result.title} ${result.artist}".trim()
        if (query.isBlank()) return null
        val search =
            YouTube
                .search(query, YouTube.SearchFilter.FILTER_SONG)
                .getOrNull()
                ?.items
                ?.firstOrNull()
        return search?.id
    }
}
