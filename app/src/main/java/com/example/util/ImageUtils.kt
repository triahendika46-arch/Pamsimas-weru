package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream
import kotlin.math.max

object ImageUtils {
  fun encodeUriToBase64Jpeg(
    context: Context,
    uri: Uri,
    maxDimensionPx: Int = 720,
    maxBase64Length: Int = 280_000
  ): String? {
    return try {
      val inputBounds = context.contentResolver.openInputStream(uri)?.use { stream ->
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeStream(stream, null, opts)
        opts
      } ?: return null

      val rawMax = max(inputBounds.outWidth, inputBounds.outHeight)
      var sampleSize = 1
      while (rawMax / sampleSize > maxDimensionPx * 2) {
        sampleSize *= 2
      }

      val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
        val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        BitmapFactory.decodeStream(stream, null, opts)
      } ?: return null

      val scaled = scaleDownBitmap(decoded, maxDimensionPx)
      if (scaled !== decoded) decoded.recycle()

      var quality = 75
      var base64 = ""
      while (quality >= 30) {
        val baos = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
        val encoded = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
        if (encoded.length <= maxBase64Length) {
          base64 = encoded
          break
        }
        quality -= 15
      }
      scaled.recycle()
      base64.ifBlank { null }
    } catch (_: Exception) {
      null
    }
  }

  private fun scaleDownBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    if (width <= maxDimension && height <= maxDimension) return bitmap
    val ratio = maxDimension.toFloat() / max(width, height).toFloat()
    val newW = (width * ratio).toInt().coerceAtLeast(1)
    val newH = (height * ratio).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
  }

  fun decodeBase64ToImageBitmap(base64: String): ImageBitmap? {
    if (base64.isBlank()) return null
    return try {
      val bytes = Base64.decode(base64, Base64.DEFAULT)
      BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    } catch (_: Exception) {
      null
    }
  }
}
