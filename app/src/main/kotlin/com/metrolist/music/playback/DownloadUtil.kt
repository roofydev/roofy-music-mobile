/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback

import android.content.Context
import android.net.ConnectivityManager
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.media3.database.DatabaseProvider
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import com.metrolist.innertube.YouTube
import com.metrolist.music.constants.AudioQuality
import com.metrolist.music.constants.AudioQualityKey
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.FormatEntity
import com.metrolist.music.db.entities.SongEntity
import com.metrolist.music.di.DownloadCache
import com.metrolist.music.di.PlayerCache
import com.metrolist.music.utils.YTPlayerUtils
import com.metrolist.music.utils.enumPreference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import timber.log.Timber
import java.time.LocalDateTime
import java.util.Collections
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadUtil
@Inject
constructor(
    @ApplicationContext context: Context,
    val database: MusicDatabase,
    val databaseProvider: DatabaseProvider,
    @DownloadCache val downloadCache: SimpleCache,
    @PlayerCache val playerCache: SimpleCache,
) {
    private val TAG = "DownloadUtil"
    private val connectivityManager = context.getSystemService<ConnectivityManager>()!!
    private val audioQuality by enumPreference(context, AudioQualityKey, AudioQuality.AUTO)
    private data class StreamUrlCacheEntry(
        val url: String,
        val expiresAt: Long,
        val requestHeaders: Map<String, String>,
    )
    private val songUrlCache = HashMap<String, StreamUrlCacheEntry>()
    private val streamRequestHeadersByUrl = Collections.synchronizedMap(
        object : LinkedHashMap<String, StreamUrlCacheEntry>(0, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, StreamUrlCacheEntry>): Boolean {
                return size > 500
            }
        }
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val downloads = MutableStateFlow<Map<String, Download>>(emptyMap())

    private val dataSourceFactory =
        ResolvingDataSource.Factory(
            CacheDataSource
                .Factory()
                .setCache(playerCache)
                .setUpstreamDataSourceFactory(
                    OkHttpDataSource.Factory(
                        OkHttpClient.Builder()
                            .proxy(YouTube.proxy)
                            .addInterceptor { chain ->
                                val request = chain.request()
                                val host = request.url.host
                                val isYouTubeStream =
                                    host.contains("googlevideo", ignoreCase = true) ||
                                        host.contains("youtube", ignoreCase = true)
                                if (isYouTubeStream) {
                                    val headers = streamRequestHeadersForUrl(request.url.toString())
                                    val requestWithHeaders =
                                        request
                                            .newBuilder()
                                            .apply {
                                                headers.forEach { (name, value) ->
                                                    header(name, value)
                                                }
                                            }.build()
                                    chain.proceed(requestWithHeaders)
                                } else {
                                    chain.proceed(request)
                                }
                            }
                            .proxyAuthenticator { _, response ->
                                YouTube.proxyAuth?.let { auth ->
                                    response.request.newBuilder()
                                        .header("Proxy-Authorization", auth)
                                        .build()
                                } ?: response.request
                            }
                            .build(),
                    ),
                ),
        ) { dataSpec ->
            val mediaId = dataSpec.key ?: error("No media id")
            val sourceMediaId = sourceMediaIdFromCacheKey(mediaId)
            val variant = playbackVariantFromCacheKey(mediaId)
            val length = if (dataSpec.length >= 0) dataSpec.length else 1

            if (playerCache.isCached(mediaId, dataSpec.position, length)) {
                return@Factory dataSpec
            }

            songUrlCache[mediaId]?.takeIf { it.expiresAt > System.currentTimeMillis() }?.let {
                cacheStreamRequestHeaders(it.url, it.requestHeaders, it.expiresAt)
                return@Factory dataSpec.withUri(it.url.toUri())
            }

            val playbackData = runBlocking(Dispatchers.IO) {
                YTPlayerUtils.playerResponseForPlayback(
                    sourceMediaId,
                    audioQuality = audioQuality,
                    connectivityManager = connectivityManager,
                    playbackVariant = variant,
                )
            }.getOrThrow()
            val format = playbackData.format

            database.query {
                upsert(
                    FormatEntity(
                        id = mediaId,
                        itag = format.itag,
                        mimeType = format.mimeType.split(";")[0],
                        codecs = format.mimeType.substringAfter("codecs=", "unknown").removeSurrounding("\""),
                        bitrate = format.bitrate,
                        sampleRate = format.audioSampleRate,
                        contentLength = format.contentLength ?: 0L,
                        loudnessDb = playbackData.audioConfig?.loudnessDb,
                        perceptualLoudnessDb = playbackData.audioConfig?.perceptualLoudnessDb,
                        playbackUrl = playbackData.playbackTracking?.videostatsPlaybackUrl?.baseUrl
                    ),
                )

                val now = LocalDateTime.now()
                val existing = getSongByIdBlocking(sourceMediaId)?.song

                val updatedSong = if (existing != null) {
                    if (existing.dateDownload == null) {
                        existing.copy(dateDownload = now)
                    } else {
                        existing
                    }
                } else {
                    SongEntity(
                        id = sourceMediaId,
                        title = playbackData.videoDetails?.title ?: "Unknown",
                        duration = playbackData.videoDetails?.lengthSeconds?.toIntOrNull() ?: 0,
                        thumbnailUrl = playbackData.videoDetails?.thumbnail?.thumbnails?.lastOrNull()?.url,
                        dateDownload = now,
                        isDownloaded = false
                    )
                }

                upsert(updatedSong)
            }

            val streamUrl =
                format.contentLength?.let { contentLength ->
                    "${playbackData.streamUrl}&range=0-$contentLength"
                } ?: playbackData.streamUrl

            songUrlCache[mediaId] =
                StreamUrlCacheEntry(
                    url = streamUrl,
                    expiresAt = System.currentTimeMillis() + playbackData.streamExpiresInSeconds * 1000L,
                    requestHeaders = playbackData.requestHeaders,
                )
            cacheStreamRequestHeaders(
                streamUrl,
                playbackData.requestHeaders,
                System.currentTimeMillis() + playbackData.streamExpiresInSeconds * 1000L,
            )
            dataSpec.withUri(streamUrl.toUri())
        }

    private fun cacheStreamRequestHeaders(
        streamUrl: String,
        requestHeaders: Map<String, String>,
        expiresAt: Long,
    ) {
        streamRequestHeadersByUrl[streamUrl] =
            StreamUrlCacheEntry(
                url = streamUrl,
                expiresAt = expiresAt,
                requestHeaders = requestHeaders,
            )
    }

    private fun streamRequestHeadersForUrl(streamUrl: String): Map<String, String> {
        val now = System.currentTimeMillis()
        streamRequestHeadersByUrl[streamUrl]
            ?.takeIf { it.expiresAt > now }
            ?.let { return it.requestHeaders }

        streamRequestHeadersByUrl.remove(streamUrl)
        return YTPlayerUtils.streamRequestHeaders()
    }

    val downloadNotificationHelper =
        DownloadNotificationHelper(context, ExoDownloadService.CHANNEL_ID)

    @OptIn(DelicateCoroutinesApi::class)
    val downloadManager: DownloadManager =
        DownloadManager(
            context,
            databaseProvider,
            downloadCache,
            dataSourceFactory,
            Executor(Runnable::run)
        ).apply {
            maxParallelDownloads = 3
            addListener(
                object : DownloadManager.Listener {
                    override fun onDownloadChanged(
                        downloadManager: DownloadManager,
                        download: Download,
                        finalException: Exception?,
                    ) {
                        downloads.update { map ->
                            map.toMutableMap().apply {
                                set(download.request.id, download)
                            }
                        }

                        scope.launch {
                            val sourceDownloadId = sourceMediaIdFromCacheKey(download.request.id)
                            val downloadVariant = playbackVariantFromCacheKey(download.request.id)
                            when (download.state) {
                                Download.STATE_COMPLETED -> {
                                    database.updateDownloadedInfo(sourceDownloadId, true, LocalDateTime.now())
                                }
                                Download.STATE_FAILED,
                                Download.STATE_STOPPED,
                                Download.STATE_REMOVING -> {
                                    if (downloadVariant == PlaybackVariant.AUDIO) {
                                        database.updateDownloadedInfo(sourceDownloadId, false, null)
                                    }
                                }
                                else -> {
                                }
                            }
                        }
                    }

                    override fun onDownloadRemoved(
                        downloadManager: DownloadManager,
                        download: Download,
                    ) {
                        val downloadId = download.request.id
                        val sourceDownloadId = sourceMediaIdFromCacheKey(downloadId)
                        val downloadVariant = playbackVariantFromCacheKey(downloadId)

                        runCatching {
                            if (downloadVariant == PlaybackVariant.AUDIO) {
                                database.updateDownloadedInfo(sourceDownloadId, false, null)
                            }
                        }.onSuccess {
                            downloads.update { map ->
                                map.toMutableMap().apply {
                                    remove(downloadId)
                                }
                            }
                            Timber.tag(TAG).d("Successfully removed download $downloadId from in-memory map")
                        }.onFailure { error ->
                            Timber.tag(TAG).e(error, "Failed to update database for removed download $downloadId, keeping in-memory entry")
                        }
                    }
                }
            )
        }

    init {
        val result = mutableMapOf<String, Download>()
        val cursor = downloadManager.downloadIndex.getDownloads()
        while (cursor.moveToNext()) {
            result[cursor.download.request.id] = cursor.download
        }
        downloads.value = result
    }

    fun getDownload(songId: String): Flow<Download?> = downloads.map { it[songId] }

    fun release() {
        scope.cancel()
    }
}
