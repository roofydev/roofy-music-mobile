package com.metrolist.music.pairing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RoofyPairingLinksTest {
    @Test
    fun parseSubsonicPairQuery_validParams() {
        val params =
            RoofyPairingLinks.parseSubsonicPairQuery(
                serverUrl = "http://192.168.1.2:4533",
                username = "roofy",
                password = "secret",
            )
        assertNotNull(params)
        assertEquals("http://192.168.1.2:4533", params!!.serverUrl)
        assertEquals("roofy", params.username)
        assertEquals("secret", params.password)
    }

    @Test
    fun parseSubsonicPairQuery_trimsWhitespace() {
        val params =
            RoofyPairingLinks.parseSubsonicPairQuery(
                serverUrl = "  http://localhost  ",
                username = " user ",
                password = "pass",
            )
        assertNotNull(params)
        assertEquals("http://localhost", params!!.serverUrl)
        assertEquals("user", params.username)
    }

    @Test
    fun parseSubsonicPairQuery_rejectsMissingPassword() {
        assertNull(
            RoofyPairingLinks.parseSubsonicPairQuery(
                serverUrl = "http://localhost",
                username = "roofy",
                password = "",
            ),
        )
    }

    @Test
    fun parseImportPairQuery_validParams() {
        val params =
            RoofyPairingLinks.parseImportPairQuery(
                endpointUrl = "http://192.168.1.2:8765",
                token = "abc123",
            )
        assertNotNull(params)
        assertEquals("http://192.168.1.2:8765", params!!.endpointUrl)
        assertEquals("abc123", params.token)
    }
}
