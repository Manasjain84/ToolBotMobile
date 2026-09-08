package com.toolbot.mobile.feature.imagecamera.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri

object ImageUtils {

    fun getFileName(context: Context, uri: Uri): String {
        val resolver = context.contentResolver
        resolver.query(uri, null, null, null, null)?.use { cursor ->
            val displayNameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (displayNameIndex >= 0 && cursor.moveToFirst()) {
                return cursor.getString(displayNameIndex)
            }
        }
        return uri.lastPathSegment ?: "image"
    }

    fun getFileSize(context: Context, uri: Uri): Long {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.statSize ?: -1L
        } catch (e: Exception) {
            -1L
        }
    }

    fun getBitmapDimensions(context: Context, uri: Uri): Pair<Int, Int> {
        var input = context.contentResolver.openInputStream(uri)
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(input, null, options)
            Pair(options.outWidth, options.outHeight)
        } catch (e: Exception) {
            Pair(0, 0)
        } finally {
            try { input?.close() } catch (_: Exception) {}
        }
    }

    fun loadBitmapSafely(context: Context, uri: Uri, maxSide: Int = 2048): Bitmap? {
        var input = context.contentResolver.openInputStream(uri)
        try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(input, null, options)
            input?.close()

            var sampleSize = 1
            val maxOriginal = options.outWidth.coerceAtLeast(options.outHeight)
            if (maxOriginal > maxSide && maxSide > 0) {
                sampleSize = Integer.highestOneBit(maxOriginal / maxSide).coerceAtLeast(1)
            }

            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            input = context.contentResolver.openInputStream(uri)
            return BitmapFactory.decodeStream(input, null, decodeOptions)
        } catch (e: OutOfMemoryError) {
            return null
        } catch (e: Exception) {
            return null
        } finally {
            try { input?.close() } catch (_: Exception) {}
        }
    }

    fun resizeBitmap(bitmap: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    fun compressAndSave(
        context: Context,
        bitmap: Bitmap,
        outputUri: Uri,
        format: Bitmap.CompressFormat,
        quality: Int = 90,
    ): Boolean {
        var out = context.contentResolver.openOutputStream(outputUri)
        if (out == null) return false
        return try {
            val bmpToWrite = if (format == Bitmap.CompressFormat.JPEG && bitmap.hasAlpha()) {
                val composited = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(composited)
                canvas.drawColor(Color.WHITE)
                canvas.drawBitmap(bitmap, 0f, 0f, null)
                composited
            } else {
                bitmap
            }

            val success = bmpToWrite.compress(format, quality.coerceIn(1, 100), out)
            out.flush()
            success
        } catch (e: Exception) {
            false
        } finally {
            try { out.close() } catch (_: Exception) {}
        }
    }
}
