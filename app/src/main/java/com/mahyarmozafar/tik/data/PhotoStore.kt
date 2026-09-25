package com.mahyarmozafar.tik.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import com.mahyarmozafar.tik.model.newId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Keeps task photos as small JPEG files in the app's own folder. */
class PhotoStore(context: Context) {
    private val resolver = context.contentResolver
    private val folder = File(context.filesDir, "photos")

    fun file(name: String): File = File(folder, name)

    /**
     * Copies a picked photo into the app, shrunk so its longest side is at most [maxSide] pixels.
     * Returns the new file's name, or null if the photo could not be read.
     */
    suspend fun import(uri: Uri, maxSide: Int = 1600): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = decode(uri, maxSide)
            folder.mkdirs()
            val name = "${newId()}.jpg"
            FileOutputStream(file(name)).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 80, it) }
            bitmap.recycle()
            name
        }.getOrNull()
    }

    suspend fun deleteAllExcept(keep: Set<String>) = withContext(Dispatchers.IO) {
        folder.listFiles()?.filter { it.name !in keep }?.forEach { it.delete() }
    }

    private fun decode(uri: Uri, maxSide: Int): Bitmap {
        if (Build.VERSION.SDK_INT >= 28) {
            val source = ImageDecoder.createSource(resolver, uri)
            return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val scale = min(1f, maxSide.toFloat() / max(info.size.width, info.size.height))
                decoder.setTargetSize(
                    (info.size.width * scale).roundToInt().coerceAtLeast(1),
                    (info.size.height * scale).roundToInt().coerceAtLeast(1),
                )
                // Hardware bitmaps can't be written back to a file.
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val decoded = resolver.openInputStream(uri).use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error("Could not read the photo")

        // Before Android 9, the camera's rotation has to be applied by hand.
        val orientation = resolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        val scale = min(1f, maxSide.toFloat() / max(decoded.width, decoded.height))
        if (degrees == 0f && scale == 1f) return decoded
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postRotate(degrees)
        }
        return Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
    }
}
