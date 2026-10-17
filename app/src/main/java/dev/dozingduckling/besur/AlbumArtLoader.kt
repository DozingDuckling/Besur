package dev.dozingduckling.besur

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.LruCache
import androidx.core.net.toUri
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object AlbumArtLoader {
    private const val CACHE_SIZE_KB = 8 * 1024

    private val cache = object : LruCache<String, Bitmap>(CACHE_SIZE_KB) {
        override fun sizeOf(key: String, value: Bitmap): Int =
                (value.byteCount / 1024).coerceAtLeast(1)
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pendingLoads = mutableMapOf<String, Deferred<Bitmap?>>()

    suspend fun load(
        context: Context,
        audioUri: String?,
        albumId: Long?,
        targetSize: Int
    ): Bitmap? {
        if (audioUri.isNullOrBlank() || targetSize <= 0) return null

        val key = "${albumId ?: audioUri}|$targetSize"
        cache.get(key)?.let { return it }

        val result: Deferred<Bitmap?> = synchronized(pendingLoads) {
            pendingLoads[key] ?: CompletableDeferred<Bitmap?>().also {
                pendingLoads[key] = it

                scope.launch {
                    try {
                        it.complete(decodeArtwork(context.applicationContext, audioUri, key, targetSize))
                    } catch (_: Exception) {
                        it.complete(null)
                    } finally {
                        synchronized(pendingLoads) {
                            if (pendingLoads[key] === it) {
                                pendingLoads.remove(key)?.let { }
                            }
                        }
                    }
                }
            }
        }

        return result.await()
    }

    private suspend fun decodeArtwork(
        context: Context,
        audioUri: String,
        key: String,
        targetSize: Int
    ): Bitmap? = withContext(Dispatchers.IO) {
        cache.get(key)?.let { return@withContext it }

        val retriever = MediaMetadataRetriever()

        try {
            retriever.setDataSource(context, audioUri.toUri())

            val data = retriever.embeddedPicture
                ?: return@withContext null

            val bounds = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(data, 0, data.size, bounds)

            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                return@withContext null
            }

            var sampleSize = 1
            while (
                bounds.outWidth / sampleSize > targetSize * 2 ||
                bounds.outHeight / sampleSize > targetSize * 2
            ) {
                sampleSize *= 2
            }

            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }

            val bitmap = BitmapFactory.decodeByteArray(
                    data, 0, data.size, options
            ) ?: return@withContext null

            cache.put(key, bitmap)
            bitmap
        } catch (_: Exception) {
            null
        } finally {
            retriever.release()
        }
    }
}