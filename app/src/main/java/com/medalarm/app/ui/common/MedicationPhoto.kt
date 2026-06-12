package com.medalarm.app.ui.common

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.medalarm.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Loads a medication box photo off the main thread, sampled down to roughly the
 * size it will be shown at. Returns null while loading, when [path] is null, or
 * when the file is missing (e.g. backup restored on another device) — callers
 * show the letter/icon avatar in that case.
 *
 * Stored photos are rotation-normalized JPEGs (see MedicationPhotoStore), so a
 * plain file decode is all that's needed here.
 */
@Composable
fun rememberMedicationPhoto(path: String?, displaySize: Dp): ImageBitmap? {
    val targetPx = with(LocalDensity.current) { displaySize.roundToPx() }
    val bitmap by produceState<ImageBitmap?>(initialValue = null, path, targetPx) {
        value = if (path == null) null else withContext(Dispatchers.IO) {
            decodeSampled(path, targetPx)?.asImageBitmap()
        }
    }
    return bitmap
}

/**
 * Shows a medication photo at its natural aspect ratio, capped at [maxHeight]
 * and the available width — never a full-screen takeover, never distorted.
 * Renders nothing when [path] is null or the file is missing.
 */
@Composable
fun MedicationPhotoBox(
    path: String?,
    maxHeight: Dp,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val photo = rememberMedicationPhoto(path, displaySize = maxHeight) ?: return
    if (photo.width <= 0 || photo.height <= 0) return
    val ratio = photo.width.toFloat() / photo.height.toFloat()
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Image(
            bitmap = photo,
            contentDescription = stringResource(R.string.med_photo_content_desc),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .heightIn(max = maxHeight)
                .aspectRatio(ratio)
                .clip(RoundedCornerShape(16.dp))
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        )
    }
}

private fun decodeSampled(path: String, targetPx: Int): Bitmap? {
    if (!File(path).exists()) return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sampleSize = 1
    if (targetPx > 0) {
        while (
            bounds.outWidth / (sampleSize * 2) >= targetPx &&
            bounds.outHeight / (sampleSize * 2) >= targetPx
        ) {
            sampleSize *= 2
        }
    }
    val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    return BitmapFactory.decodeFile(path, opts)
}
