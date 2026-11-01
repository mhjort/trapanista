package app.trapanista.llm

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

private const val MAX_LONG_EDGE = 2576.0
private const val MAX_PIXELS = 3_750_000.0

// The API limits the base64 form to 5 MB, which is about 3.75 MB of JPEG.
private const val MAX_JPEG_BYTES = 3_700_000

private const val UNREADABLE = "Couldn't read that image. Try another photo."

/**
 * Loads a photo as base64 JPEG, upright and within Claude's image limits. Keeps as much
 * resolution as the API accepts, because small print in the book has to stay legible.
 * Blocking; call from a background dispatcher.
 */
fun prepareImage(resolver: ContentResolver, uri: Uri): String {
    val jpeg = try {
        encode(decode(resolver, uri), exifRotation(resolver, uri))
    } catch (e: IOException) {
        throw ClaudeException(UNREADABLE, e)
    } catch (e: SecurityException) {
        throw ClaudeException(UNREADABLE, e)
    } catch (e: OutOfMemoryError) {
        throw ClaudeException(UNREADABLE, e)
    }
    return Base64.encodeToString(jpeg, Base64.NO_WRAP)
}

/** Decodes close to the final size, so a 50 MP photo doesn't exhaust memory. */
private fun decode(resolver: ContentResolver, uri: Uri): Bitmap {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    open(resolver, uri).use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw ClaudeException(UNREADABLE)

    val scale = targetScale(bounds.outWidth, bounds.outHeight)
    var sample = 1
    while (sample * 2 * scale <= 1) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    return open(resolver, uri).use { BitmapFactory.decodeStream(it, null, options) }
        ?: throw ClaudeException(UNREADABLE)
}

private fun open(resolver: ContentResolver, uri: Uri) =
    resolver.openInputStream(uri) ?: throw IOException("No stream for image")

private fun targetScale(width: Int, height: Int): Double = minOf(
    1.0,
    MAX_LONG_EDGE / max(width, height),
    sqrt(MAX_PIXELS / (width.toLong() * height)),
)

/** A missing or unreadable EXIF block just means the image is treated as upright. */
private fun exifRotation(resolver: ContentResolver, uri: Uri): Float = runCatching {
    open(resolver, uri).use { stream ->
        when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
    }
}.getOrDefault(0f)

private fun encode(decoded: Bitmap, rotation: Float): ByteArray {
    val scale = targetScale(decoded.width, decoded.height).toFloat()
    val matrix = Matrix().apply {
        postScale(scale, scale)
        postRotate(rotation)
    }
    val bitmap = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
    for (quality in listOf(85, 70, 55)) {
        val bytes = ByteArrayOutputStream()
            .also { bitmap.compress(Bitmap.CompressFormat.JPEG, quality, it) }
            .toByteArray()
        if (bytes.size <= MAX_JPEG_BYTES) return bytes
    }
    throw ClaudeException("That photo is too large to send. Try another one.")
}
