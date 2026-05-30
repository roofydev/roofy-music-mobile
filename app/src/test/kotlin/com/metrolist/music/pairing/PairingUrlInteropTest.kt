package com.metrolist.music.pairing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.net.URI
import java.net.URLDecoder

/**
 * Ensures mobile parsing accepts the same query shape the desktop builds
 * (`pairing-urls.ts` → `buildSubsonicPairingUrl` / `buildImportPairingUrl`).
 */
class PairingUrlInteropTest {
    @Test
    fun parseSubsonicPairQuery_acceptsDesktopStyleDeepLink() {
        val deepLink =
            "roofymusic://pair/subsonic" +
                "?name=Roofy+Local+Library" +
                "&serverUrl=http%3A%2F%2F192.168.1.10%3A4533" +
                "&username=roofy" +
                "&password=secret%26token"

        val params =
            RoofyPairingLinks.parseSubsonicPairQuery(
                serverUrl = queryParam(deepLink, "serverUrl"),
                username = queryParam(deepLink, "username"),
                password = queryParam(deepLink, "password"),
            )

        assertNotNull(params)
        assertEquals("http://192.168.1.10:4533", params!!.serverUrl)
        assertEquals("roofy", params.username)
        assertEquals("secret&token", params.password)
    }

    @Test
    fun parseDevicePairQuery_acceptsDesktopStyleDeepLink() {
        val deepLink =
            "roofymusic://pair/device" +
                "?serverUrl=http%3A%2F%2F192.168.1.10%3A4533" +
                "&username=roofy" +
                "&password=secret" +
                "&endpointUrl=http%3A%2F%2F192.168.1.10%3A8765" +
                "&token=import-token"

        val params =
            RoofyPairingLinks.parseDevicePair(android.net.Uri.parse(deepLink))

        assertNotNull(params)
        assertEquals("http://192.168.1.10:4533", params!!.serverUrl)
        assertEquals("roofy", params.username)
        assertEquals("import-token", params.token)
    }

    @Test
    fun parseImportPairQuery_acceptsDesktopStyleDeepLink() {
        val deepLink =
            "roofymusic://pair/import" +
                "?endpointUrl=http%3A%2F%2F192.168.1.10%3A8765" +
                "&token=import-token"

        val params =
            RoofyPairingLinks.parseImportPairQuery(
                endpointUrl = queryParam(deepLink, "endpointUrl"),
                token = queryParam(deepLink, "token"),
            )

        assertNotNull(params)
        assertEquals("http://192.168.1.10:8765", params!!.endpointUrl)
        assertEquals("import-token", params.token)
    }

    private fun queryParam(deepLink: String, name: String): String? {
        val query = URI(deepLink).rawQuery ?: return null
        return query
            .split('&')
            .mapNotNull { pair ->
                val parts = pair.split('=', limit = 2)
                if (parts.size == 2 && parts[0] == name) {
                    URLDecoder.decode(parts[1], Charsets.UTF_8.name())
                } else {
                    null
                }
            }
            .firstOrNull()
    }
}
