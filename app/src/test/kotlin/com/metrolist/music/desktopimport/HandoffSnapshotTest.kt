package com.metrolist.music.desktopimport

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HandoffSnapshotTest {
    private val parser =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

    @Test
    fun decodesDesktopHandoffPayload() {
        val json =
            """
            {
              "positionMs": 12500,
              "playbackStatus": "playing",
              "nowPlaying": {
                "source": "youtube",
                "id": "dQw4w9WgXcQ",
                "title": "Never Gonna Give You Up",
                "artist": "Rick Astley",
                "durationMs": 212000
              },
              "queue": [
                {
                  "source": "subsonic",
                  "id": "song-1",
                  "title": "Owned Track",
                  "artist": "Local Artist"
                }
              ]
            }
            """.trimIndent()

        val snapshot = parser.decodeFromString(HandoffSnapshot.serializer(), json)
        assertEquals(12500, snapshot.positionMs)
        assertEquals("playing", snapshot.playbackStatus)
        assertEquals("youtube", snapshot.nowPlaying?.source)
        assertEquals("dQw4w9WgXcQ", snapshot.nowPlaying?.id)
        assertEquals(1, snapshot.queue.size)
        assertEquals("subsonic", snapshot.queue.first().source)
    }

    @Test
    fun encodesSnapshotForDesktopPush() {
        val snapshot =
            HandoffSnapshot(
                positionMs = 0,
                playbackStatus = "paused",
                nowPlaying =
                    HandoffTrack(
                        source = "subsonic",
                        id = "abc",
                        title = "Title",
                        artist = "Artist",
                    ),
            )

        val encoded = parser.encodeToString(HandoffSnapshot.serializer(), snapshot)
        val roundTrip = parser.decodeFromString(HandoffSnapshot.serializer(), encoded)
        assertEquals(snapshot.nowPlaying?.id, roundTrip.nowPlaying?.id)
        assertNull(roundTrip.queue.firstOrNull())
    }
}
