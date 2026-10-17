
package dev.dozingduckling.besur

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.LruCache
import android.util.Log
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AlbumArtLoader {
    private const val CACHE_SIZE_KB = 8 * 1024

    private val cache = object : LruCache<String, Bitmap>(CACHE_SIZE_KB) {
        override fun sizeOf(key: String, value: Bitmap): Int =
                (value.byteCount / 1024).coerceAtLeast(1)
    }

    suspend fun load(
        context: Context,
        audioUri: String?,
        albumId: Long?,
        targetSize: Int
    ): Bitmap? {
        if (audioUri.isNullOrBlank() || targetSize <= 0) return null

        val key = "${albumId ?: audioUri}|$targetSize"
        cache.get(key)?.let { return it }

        return withContext(Dispatchers.IO) {
            cache.get(key)?.let { return@withContext it }

            val retriever = MediaMetadataRetriever()

            try {
                retriever.setDataSource(
                        context.applicationContext,
                        Uri.parse(audioUri)
                )

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
            } catch (e: Exception) {
                null
            } finally {
                retriever.release()
            }
        }
    }
}
