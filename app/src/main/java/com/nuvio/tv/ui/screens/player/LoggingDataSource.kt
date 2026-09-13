package com.nuvio.tv.ui.screens.player

import android.os.SystemClock
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import java.util.concurrent.ConcurrentLinkedDeque

@OptIn(UnstableApi::class)
class LoggingDataSource(
    private val upstream: DataSource,
    private val site: String
) : DataSource by upstream {
    override fun open(dataSpec: DataSpec): Long {
        val t0 = SystemClock.elapsedRealtime()
        val originalUri = dataSpec.uri
        val resolvedUri = PlayerPlaybackNetworking.getResolvedUri(originalUri)
        val effectiveSpec = if (resolvedUri != originalUri) {
            dataSpec.buildUpon().setUri(resolvedUri).build()
        } else {
            dataSpec
        }
        val uriName = effectiveSpec.uri.path?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "stream"
        return try {
            val len = upstream.open(effectiveSpec)
            upstream.uri?.let { PlayerPlaybackNetworking.recordRedirect(originalUri, it) }
            val elapsed = (SystemClock.elapsedRealtime() - t0).coerceAtLeast(0L)
            val line = "[$site] file=$uriName pos=${effectiveSpec.position} reqLen=${effectiveSpec.length} -> len=$len ms=$elapsed host=${effectiveSpec.uri.host}"
            Log.i("DS_OPEN", line)
            recordEvent(line)
            len
        } catch (e: Exception) {
            val elapsed = (SystemClock.elapsedRealtime() - t0).coerceAtLeast(0L)
            val line = "[$site] file=$uriName pos=${effectiveSpec.position} FAILED ${e.javaClass.simpleName}: ${e.message} ms=$elapsed host=${effectiveSpec.uri.host}"
            Log.w("DS_OPEN", line)
            recordEvent(line)
            throw e
        }
    }
    override fun close() = upstream.close()

    companion object {
        private val recentLogs = ConcurrentLinkedDeque<String>()
        private const val MAX_RECENT_LOGS = 50

        fun recordEvent(msg: String) {
            recentLogs.addLast(msg)
            while (recentLogs.size > MAX_RECENT_LOGS) {
                recentLogs.pollFirst()
            }
        }

        fun recentEvents(): List<String> = recentLogs.toList()
        fun clear() = recentLogs.clear()
    }
}

@OptIn(UnstableApi::class)
class LoggingDataSourceFactory(
    private val upstream: DataSource.Factory,
    private val site: String
) : DataSource.Factory {
    override fun createDataSource(): DataSource = LoggingDataSource(upstream.createDataSource(), site)
}
