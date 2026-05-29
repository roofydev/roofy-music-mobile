/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.update

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.metrolist.music.MainActivity
import com.metrolist.music.R
import com.metrolist.music.utils.AppUpdateManager
import com.metrolist.music.utils.AppUpdateState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

class AppUpdateDownloadService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private var stateJob: Job? = null

    private var downloadUrl: String = ""
    private var versionLabel: String = ""

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CANCEL -> {
                AppUpdateManager.cancelDownload()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_INSTALL -> {
                val installIntent =
                    Intent(this, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        putExtra(MainActivity.EXTRA_TRIGGER_UPDATE_INSTALL, true)
                    }
                startActivity(installIntent)
                return START_NOT_STICKY
            }
            else -> {
                downloadUrl = intent?.getStringExtra(EXTRA_DOWNLOAD_URL).orEmpty()
                versionLabel = intent?.getStringExtra(EXTRA_VERSION_LABEL).orEmpty()
                if (downloadUrl.isBlank()) {
                    stopSelf()
                    return START_NOT_STICKY
                }
            }
        }

        if (!startInForeground()) return START_NOT_STICKY

        when (intent?.action) {
            ACTION_CANCEL, ACTION_INSTALL -> Unit
            else -> {
                observeState()
                AppUpdateManager.startDownload(downloadUrl, versionLabel)
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stateJob?.cancel()
        serviceScope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private fun observeState() {
        stateJob?.cancel()
        stateJob =
            serviceScope.launch {
                AppUpdateManager.state.collectLatest { state ->
                    when (state) {
                        is AppUpdateState.Downloading -> {
                            updateProgressNotification(state)
                        }
                        is AppUpdateState.ReadyToInstall -> {
                            showReadyNotification(state.versionLabel)
                            stopSelf()
                        }
                        is AppUpdateState.Error -> {
                            showErrorNotification(state.message)
                            stopSelf()
                        }
                        is AppUpdateState.Cancelled -> {
                            stopSelf()
                        }
                        else -> Unit
                    }
                }
            }
    }

    private fun startInForeground(): Boolean {
        val notification = buildDownloadingNotification(0, false)
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
                )
            } else {
                @Suppress("DEPRECATION")
                startForeground(NOTIFICATION_ID, notification)
            }
            true
        } catch (e: SecurityException) {
            Timber.w(e, "Unable to start app update download foreground service")
            stopSelf()
            false
        } catch (e: RuntimeException) {
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                e::class.java.name == "android.app.ForegroundServiceStartNotAllowedException"
            ) {
                Timber.w(e, "Unable to start app update download foreground service")
                stopSelf()
                false
            } else {
                throw e
            }
        }
    }

    private fun updateProgressNotification(state: AppUpdateState.Downloading) {
        val indeterminate = state.totalBytes <= 0
        val progress = (state.progress * 100).toInt().coerceIn(0, 100)
        val notification = buildDownloadingNotification(progress, !indeterminate)
        val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun buildDownloadingNotification(progress: Int, determinate: Boolean): Notification {
        val builder =
            NotificationCompat
                .Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.update)
                .setContentTitle(getString(R.string.downloading_update))
                .setContentText(versionLabel.ifBlank { getString(R.string.update_available_title) })
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(openUpdaterPendingIntent())
                .addAction(
                    R.drawable.close,
                    getString(R.string.cancel_download),
                    cancelPendingIntent(),
                )

        if (determinate) {
            builder.setProgress(100, progress, false)
        } else {
            builder.setProgress(0, 0, true)
        }
        return builder.build()
    }

    private fun showReadyNotification(version: String) {
        val notification =
            NotificationCompat
                .Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.update)
                .setContentTitle(getString(R.string.update_ready_to_install))
                .setContentText(version)
                .setAutoCancel(true)
                .setContentIntent(openUpdaterPendingIntent())
                .addAction(
                    R.drawable.download,
                    getString(R.string.install_update),
                    installPendingIntent(),
                )
                .build()
        val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun showErrorNotification(message: String) {
        val notification =
            NotificationCompat
                .Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.update)
                .setContentTitle(getString(R.string.download_failed))
                .setContentText(message)
                .setAutoCancel(true)
                .setContentIntent(openUpdaterPendingIntent())
                .build()
        val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun openUpdaterPendingIntent(): PendingIntent {
        val intent =
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(MainActivity.EXTRA_OPEN_UPDATER, true)
            }
        return PendingIntent.getActivity(
            this,
            REQUEST_OPEN_UPDATER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun cancelPendingIntent(): PendingIntent {
        val intent =
            Intent(this, AppUpdateDownloadService::class.java).apply {
                action = ACTION_CANCEL
            }
        return PendingIntent.getService(
            this,
            REQUEST_CANCEL,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun installPendingIntent(): PendingIntent {
        val intent =
            Intent(this, AppUpdateDownloadService::class.java).apply {
                action = ACTION_INSTALL
            }
        return PendingIntent.getService(
            this,
            REQUEST_INSTALL,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val CHANNEL_ID = "updates"
        private const val NOTIFICATION_ID = 1001

        private const val EXTRA_DOWNLOAD_URL = "download_url"
        private const val EXTRA_VERSION_LABEL = "version_label"

        const val ACTION_CANCEL = "com.metrolist.music.update.CANCEL"
        const val ACTION_INSTALL = "com.metrolist.music.update.INSTALL"

        private const val REQUEST_OPEN_UPDATER = 2001
        private const val REQUEST_CANCEL = 2002
        private const val REQUEST_INSTALL = 2003

        fun startDownload(context: Context, downloadUrl: String, versionLabel: String) {
            val intent = createDownloadIntent(context, downloadUrl, versionLabel)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun createDownloadPendingIntent(
            context: Context,
            downloadUrl: String,
            versionLabel: String,
        ): PendingIntent {
            val intent = createDownloadIntent(context, downloadUrl, versionLabel)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                PendingIntent.getForegroundService(context, REQUEST_DOWNLOAD, intent, flags)
            } else {
                PendingIntent.getService(context, REQUEST_DOWNLOAD, intent, flags)
            }
        }

        private fun createDownloadIntent(
            context: Context,
            downloadUrl: String,
            versionLabel: String,
        ): Intent =
            Intent(context, AppUpdateDownloadService::class.java).apply {
                putExtra(EXTRA_DOWNLOAD_URL, downloadUrl)
                putExtra(EXTRA_VERSION_LABEL, versionLabel)
            }

        private const val REQUEST_DOWNLOAD = 2004
    }
}
