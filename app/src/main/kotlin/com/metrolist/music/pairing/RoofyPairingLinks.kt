/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.pairing

import android.net.Uri

data class SubsonicPairingParams(
    val serverUrl: String,
    val username: String,
    val password: String,
)

data class ImportPairingParams(
    val endpointUrl: String,
    val token: String,
)

object RoofyPairingLinks {
    fun isSubsonicPairLink(uri: Uri): Boolean =
        uri.scheme.equals("roofymusic", ignoreCase = true) &&
            uri.host.equals("pair", ignoreCase = true) &&
            uri.pathSegments.firstOrNull().equals("subsonic", ignoreCase = true)

    fun isImportPairLink(uri: Uri): Boolean =
        uri.scheme.equals("roofymusic", ignoreCase = true) &&
            uri.host.equals("pair", ignoreCase = true) &&
            uri.pathSegments.firstOrNull().equals("import", ignoreCase = true)

    fun parseSubsonicPair(uri: Uri): SubsonicPairingParams? {
        if (!isSubsonicPairLink(uri)) return null
        return parseSubsonicPairQuery(
            serverUrl = uri.getQueryParameter("serverUrl"),
            username = uri.getQueryParameter("username"),
            password = uri.getQueryParameter("password"),
        )
    }

    fun parseSubsonicPairQuery(
        serverUrl: String?,
        username: String?,
        password: String?,
    ): SubsonicPairingParams? {
        val normalizedServerUrl = serverUrl?.trim().orEmpty()
        val normalizedUsername = username?.trim().orEmpty()
        val normalizedPassword = password.orEmpty()
        if (normalizedServerUrl.isBlank() || normalizedUsername.isBlank() || normalizedPassword.isBlank()) {
            return null
        }
        return SubsonicPairingParams(normalizedServerUrl, normalizedUsername, normalizedPassword)
    }

    fun parseImportPair(uri: Uri): ImportPairingParams? {
        if (!isImportPairLink(uri)) return null
        return parseImportPairQuery(
            endpointUrl = uri.getQueryParameter("endpointUrl"),
            token = uri.getQueryParameter("token"),
        )
    }

    fun parseImportPairQuery(
        endpointUrl: String?,
        token: String?,
    ): ImportPairingParams? {
        val normalizedEndpoint = endpointUrl?.trim().orEmpty()
        val normalizedToken = token.orEmpty()
        if (normalizedEndpoint.isBlank() || normalizedToken.isBlank()) return null
        return ImportPairingParams(normalizedEndpoint, normalizedToken)
    }
}
