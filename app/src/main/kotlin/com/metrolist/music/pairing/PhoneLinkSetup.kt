/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.pairing

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.metrolist.music.R
import com.metrolist.music.constants.DesktopImportEndpointUrlKey
import com.metrolist.music.constants.DesktopImportTokenKey
import com.metrolist.music.constants.DesktopWebControlUrlKey
import com.metrolist.music.constants.PersonalLibraryEnabledKey
import com.metrolist.music.constants.PersonalLibraryPasswordKey
import com.metrolist.music.constants.PersonalLibraryServerUrlKey
import com.metrolist.music.constants.PersonalLibraryUsernameKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.metrolist.music.desktopimport.DesktopConnect

object PhoneLinkSetup {
    suspend fun applyDevicePair(
        dataStore: DataStore<Preferences>,
        params: DevicePairingParams,
    ) {
        dataStore.edit { settings ->
            settings[PersonalLibraryEnabledKey] = true
            settings[PersonalLibraryServerUrlKey] = params.serverUrl
            settings[PersonalLibraryUsernameKey] = params.username
            settings[PersonalLibraryPasswordKey] = params.password
            settings[DesktopImportEndpointUrlKey] = params.endpointUrl
            settings[DesktopImportTokenKey] = params.token
            if (params.webControlUrl != null) {
                settings[DesktopWebControlUrlKey] = params.webControlUrl
            } else {
                settings.remove(DesktopWebControlUrlKey)
            }
        }
    }

    suspend fun applyPairingUri(
        context: Context,
        dataStore: DataStore<Preferences>,
        uri: Uri,
    ): Boolean {
        val params =
            when {
                RoofyPairingLinks.isDevicePairLink(uri) -> RoofyPairingLinks.parseDevicePair(uri)
                RoofyPairingLinks.isSubsonicPairLink(uri) -> {
                    val subsonic = RoofyPairingLinks.parseSubsonicPair(uri) ?: return false
                    dataStore.edit { settings ->
                        settings[PersonalLibraryEnabledKey] = true
                        settings[PersonalLibraryServerUrlKey] = subsonic.serverUrl
                        settings[PersonalLibraryUsernameKey] = subsonic.username
                        settings[PersonalLibraryPasswordKey] = subsonic.password
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, R.string.personal_library_paired, Toast.LENGTH_SHORT).show()
                    }
                    return true
                }
                RoofyPairingLinks.isImportPairLink(uri) -> {
                    val import = RoofyPairingLinks.parseImportPair(uri) ?: return false
                    dataStore.edit { settings ->
                        settings[DesktopImportEndpointUrlKey] = import.endpointUrl
                        settings[DesktopImportTokenKey] = import.token
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, R.string.desktop_import_paired, Toast.LENGTH_SHORT).show()
                    }
                    return true
                }
                else -> null
            } ?: return false

        applyDevicePair(dataStore, params)
        notifyDesktopPhonePaired(params)
        withContext(Dispatchers.Main) {
            Toast.makeText(context, R.string.phone_link_paired, Toast.LENGTH_SHORT).show()
        }
        return true
    }

    private suspend fun notifyDesktopPhonePaired(params: DevicePairingParams) {
        val endpoint = params.endpointUrl.trim().trimEnd('/')
        val token = params.token.trim()
        if (endpoint.isBlank() || token.isBlank()) return

        withContext(Dispatchers.IO) {
            runCatching {
                DesktopConnect.resolveLiveEndpoint(endpoint, token).getOrThrow()
            }
        }
    }
}
