/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.devices

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metrolist.music.R
import com.metrolist.music.constants.DesktopWebControlUrlKey
import com.metrolist.music.utils.rememberPreference

@Composable
fun ColumnScope.ListenOnWebControlSection(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val webControlUrl by rememberPreference(DesktopWebControlUrlKey, "")

    if (webControlUrl.isBlank()) {
        return
    }

    Spacer(modifier = Modifier.height(8.dp))

    ListenOnDeviceRow(
        title = stringResource(R.string.devices_web_control_title),
        subtitle = stringResource(R.string.devices_web_control_subtitle),
        iconRes = R.drawable.language,
        isActive = false,
        enabled = true,
        onClick = {
            val url = webControlUrl.trim()
            if (url.isBlank()) {
                Toast.makeText(
                    context,
                    R.string.phone_link_web_control_unavailable,
                    Toast.LENGTH_LONG,
                ).show()
                return@ListenOnDeviceRow
            }
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            onDismiss()
        },
    )
}
