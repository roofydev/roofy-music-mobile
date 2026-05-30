package com.metrolist.music.desktopimport

import com.metrolist.music.desktopimport.HandoffPlayback.resolveYoutubeVideoId
import com.metrolist.music.models.MediaMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HandoffPlaybackVideoIdTest {
    @Test
    fun prefersSetVideoIdWhenBrowseIdIsNotAVideoId() {
        val metadata =
            MediaMetadata(
                id = "OLAK5uy_njVBa8w1YQ",
                title = "Track",
                artists = listOf(MediaMetadata.Artist(id = null, name = "Artist")),
                duration = 200,
                setVideoId = "dQw4w9WgXcQ",
            )

        assertEquals("dQw4w9WgXcQ", metadata.resolveYoutubeVideoId())
    }

    @Test
    fun usesIdWhenItIsAlreadyAVideoId() {
        val metadata =
            MediaMetadata(
                id = "dQw4w9WgXcQ",
                title = "Track",
                artists = listOf(MediaMetadata.Artist(id = null, name = "Artist")),
                duration = 200,
            )

        assertEquals("dQw4w9WgXcQ", metadata.resolveYoutubeVideoId())
    }

    @Test
    fun returnsNullForLocalPlaylistIdsWithoutVideoId() {
        val metadata =
            MediaMetadata(
                id = "LPSomeLocalId12345",
                title = "Track",
                artists = listOf(MediaMetadata.Artist(id = null, name = "Artist")),
                duration = 200,
            )

        assertNull(metadata.resolveYoutubeVideoId())
    }
}
