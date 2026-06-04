/*
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback

import android.content.Context
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService

enum class OfflineSaveMode {
    AUDIO_ONLY,
    AUDIO_AND_VIDEO,
}

fun offlineDownloadRequest(
    mediaId: String,
    title: String,
    variant: PlaybackVariant,
): DownloadRequest {
    val cacheKey = playbackVariantCacheKey(mediaId, variant)
    return DownloadRequest
        .Builder(cacheKey, cacheKey.toUri())
        .setCustomCacheKey(cacheKey)
        .setData(title.toByteArray())
        .build()
}

fun sendAddOfflineDownload(
    context: Context,
    mediaId: String,
    title: String,
    mode: OfflineSaveMode,
) {
    DownloadService.sendAddDownload(
        context,
        ExoDownloadService::class.java,
        offlineDownloadRequest(mediaId, title, PlaybackVariant.AUDIO),
        false,
    )
    if (mode == OfflineSaveMode.AUDIO_AND_VIDEO) {
        DownloadService.sendAddDownload(
            context,
            ExoDownloadService::class.java,
            offlineDownloadRequest(mediaId, title, PlaybackVariant.VIDEO),
            false,
        )
    }
}

fun sendRemoveOfflineDownload(
    context: Context,
    mediaId: String,
) {
    DownloadService.sendRemoveDownload(
        context,
        ExoDownloadService::class.java,
        playbackVariantCacheKey(mediaId, PlaybackVariant.AUDIO),
        false,
    )
    DownloadService.sendRemoveDownload(
        context,
        ExoDownloadService::class.java,
        playbackVariantCacheKey(mediaId, PlaybackVariant.VIDEO),
        false,
    )
}
