package com.medalarm.app.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.medalarm.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores medication box photos in app-internal storage (no permissions needed,
 * files are private and survive app updates).
 *
 * Photos are downscaled to [MAX_DIMENSION_PX] and re-encoded as JPEG at import,
 * with EXIF rotation baked in — so display code can decode the file directly
 * without worrying about orientation or memory.
 */
@Singleton
class MedicationPhotoStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Deletions must survive ViewModel scope cancellation (e.g. onCleared cleanup).
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val photosDir: File
        get() = File(context.filesDir, PHOTOS_DIR).apply { mkdirs() }

    private val cameraCacheDir: File
        get() = File(context.cacheDir, CAMERA_CACHE_DIR).apply { mkdirs() }

    /** Content Uri for a fresh camera capture target, served by our FileProvider. */
    fun newCameraCaptureUri(): Uri {
        val file = File(cameraCacheDir, "capture_${UUID.randomUUID()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${BuildConfig.APPLICATION_ID}.fileprovider",
            file
        )
    }

    /**
     * Copies the image behind [source] (gallery pick or camera capture) into
     * internal storage, downscaled and rotation-normalized.
     *
     * @return the absolute path of the stored file, or null if the image could
     *         not be read (corrupt file, revoked Uri, …).
     */
    suspend fun importFromUri(source: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val bitmap = decodeScaled(source) ?: return@withContext null
            val upright = applyExifRotation(source, bitmap)
            if (upright !== bitmap) bitmap.recycle()
            // Sampling only gets within 2x of the target; cap exactly here.
            val final = scaleDownIfNeeded(upright)
            if (final !== upright) upright.recycle()
            val target = File(photosDir, "med_${UUID.randomUUID()}.jpg")
            FileOutputStream(target).use { out ->
                final.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }
            final.recycle()
            target.absolutePath
        } catch (t: Throwable) {
            Timber.e(t, "Medication photo import failed")
            null
        } finally {
            clearCameraCache()
        }
    }

    /** Fire-and-forget file deletion; safe to call with null or a missing file. */
    fun deleteQuietly(path: String?) {
        if (path == null) return
        ioScope.launch {
            runCatching { File(path).delete() }
                .onFailure { Timber.w(it, "Could not delete medication photo %s", path) }
        }
    }

    private fun clearCameraCache() {
        cameraCacheDir.listFiles()?.forEach { runCatching { it.delete() } }
    }

    /** Two-pass decode: bounds first, then sampled to stay near [MAX_DIMENSION_PX]. */
    private fun decodeScaled(source: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        // NOTE: a bounds-only decodeStream always returns null by design, so the
        // stream-open check must NOT be an elvis on the use-block's result — that
        // made every import silently fail. Check the stream itself instead.
        val boundsStream = context.contentResolver.openInputStream(source) ?: return null
        boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sampleSize = 1
        while (
            bounds.outWidth / (sampleSize * 2) >= MAX_DIMENSION_PX ||
            bounds.outHeight / (sampleSize * 2) >= MAX_DIMENSION_PX
        ) {
            sampleSize *= 2
        }

        val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return context.contentResolver.openInputStream(source)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        }
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap): Bitmap {
        val maxDim = maxOf(bitmap.width, bitmap.height)
        if (maxDim <= MAX_DIMENSION_PX) return bitmap
        val scale = MAX_DIMENSION_PX.toFloat() / maxDim
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt().coerceAtLeast(1),
            (bitmap.height * scale).toInt().coerceAtLeast(1),
            true
        )
    }

    private fun applyExifRotation(source: Uri, bitmap: Bitmap): Bitmap {
        val orientation = runCatching {
            context.contentResolver.openInputStream(source)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            }
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private companion object {
        const val PHOTOS_DIR = "medication_photos"
        const val CAMERA_CACHE_DIR = "camera"
        const val MAX_DIMENSION_PX = 1280
        const val JPEG_QUALITY = 85
    }
}
