package com.metrolist.music.subsonic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubsonicModelsTest {
    @Test
    fun mediaId_roundTrip() {
        val raw = "song-abc"
        val mediaId = SubsonicClient.mediaId(raw)
        assertEquals("subsonic:song-abc", mediaId)
        assertEquals(raw, SubsonicClient.localIdFromMediaId(mediaId))
    }

    @Test
    fun localIdFromMediaId_rejectsNonSubsonicIds() {
        assertNull(SubsonicClient.localIdFromMediaId("LPdQw4v9Ywk"))
        assertNull(SubsonicClient.localIdFromMediaId("youtube:abc"))
    }

    @Test
    fun playlistBrowseId_roundTrip() {
        val remoteId = "pl-42"
        val browseId = subsonicPlaylistBrowseId(remoteId)
        assertEquals("subsonic:playlist:pl-42", browseId)
        assertEquals(remoteId, subsonicRemoteIdFromBrowseId(browseId))
    }

    @Test
    fun subsonicRemoteIdFromBrowseId_rejectsPendingPrefix() {
        assertNull(subsonicRemoteIdFromBrowseId("subsonic:pending:temp"))
    }

    @Test
    fun personalLibraryCredentials_isConfigured() {
        assertFalse(
            PersonalLibraryCredentials("", "user", "pass").isConfigured,
        )
        assertTrue(
            PersonalLibraryCredentials("http://localhost", "user", "pass").isConfigured,
        )
    }
}
