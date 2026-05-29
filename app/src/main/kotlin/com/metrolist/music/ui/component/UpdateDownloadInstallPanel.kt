/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.metrolist.music.R
import com.metrolist.music.update.AppUpdateDownloadService
import com.metrolist.music.utils.AppUpdateManager
import com.metrolist.music.utils.AppUpdateState
import com.metrolist.music.utils.InstallResult

@Composable
fun UpdateDownloadInstallPanel(
    downloadUrl: String,
    versionLabel: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val uriHandler = LocalUriHandler.current
    val updateState by AppUpdateManager.state.collectAsStateWithLifecycle()

    val unknownSourcesLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (activity != null) {
                when (val installResult = AppUpdateManager.installUpdate(activity)) {
                    InstallResult.Success -> Unit
                    InstallResult.BlockedUnknownSources ->
                        Toast
                            .makeText(
                                context,
                                context.getString(R.string.install_unknown_apps_required),
                                Toast.LENGTH_LONG,
                            ).show()
                    is InstallResult.Failed ->
                        Toast
                            .makeText(context, installResult.message, Toast.LENGTH_LONG)
                            .show()
                }
            }
        }

    fun launchInstall() {
        if (activity == null) return
        when (val result = AppUpdateManager.installUpdate(activity)) {
            InstallResult.Success -> Unit
            InstallResult.BlockedUnknownSources ->
                unknownSourcesLauncher.launch(
                    AppUpdateManager.createUnknownSourcesSettingsIntent(context),
                )
            is InstallResult.Failed ->
                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
        }
    }

    fun startDownload() {
        AppUpdateDownloadService.startDownload(context, downloadUrl, versionLabel)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (val state = updateState) {
            is AppUpdateState.Downloading -> {
                if (state.totalBytes > 0) {
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                Text(
                    text = stringResource(R.string.downloading_update),
                    style = MaterialTheme.typography.bodySmall,
                )
                TextButton(
                    onClick = { AppUpdateManager.cancelDownload() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.cancel_download))
                }
            }
            is AppUpdateState.ReadyToInstall -> {
                Button(
                    onClick = { launchInstall() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.install_update))
                }
            }
            is AppUpdateState.Error -> {
                Text(
                    text = stringResource(R.string.download_failed) + ": " + state.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                Button(
                    onClick = { startDownload() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.download_update))
                }
            }
            is AppUpdateState.Cancelled, AppUpdateState.Idle -> {
                Button(
                    onClick = { startDownload() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.download_update))
                }
            }
            AppUpdateState.Installing -> {
                Text(
                    text = stringResource(R.string.install_update),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        TextButton(
            onClick = { uriHandler.openUri(downloadUrl) },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        ) {
            Text(stringResource(R.string.open_in_browser))
        }
    }
}
