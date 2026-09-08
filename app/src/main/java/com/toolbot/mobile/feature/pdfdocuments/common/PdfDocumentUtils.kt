package com.toolbot.mobile.feature.pdfdocuments.common

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import java.io.IOException

object PdfDocumentUtils {
    const val PDF_MIME_TYPE = "application/pdf"

    fun isPdfUri(uri: Uri, resolver: ContentResolver): Boolean {
        val mimeType = resolver.getType(uri)
        return mimeType != null && (
            mimeType.equals(PDF_MIME_TYPE, ignoreCase = true) ||
                mimeType.endsWith("+pdf", ignoreCase = true)
            )
            || uri.toString().lowercase().endsWith(".pdf")
    }

    fun getDisplayName(uri: Uri, resolver: ContentResolver): String {
        val cursor = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        return cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    it.getString(index)
                } else {
                    "document.pdf"
                }
            } else {
                "document.pdf"
            }
        } ?: "document.pdf"
    }

    fun getFileSize(uri: Uri, resolver: ContentResolver): Long {
        val cursor = resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
        return cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.SIZE)
                if (index >= 0) {
                    it.getLong(index)
                } else {
                    -1L
                }
            } else {
                -1L
            }
        } ?: -1L
    }

    fun copyUri(sourceUri: Uri, targetUri: Uri, resolver: ContentResolver) {
        resolver.openInputStream(sourceUri)?.use { inputStream ->
            resolver.openOutputStream(targetUri)?.use { outputStream ->
                inputStream.copyTo(outputStream)
            } ?: throw IOException("Unable to create output stream for $targetUri")
        } ?: throw IOException("Unable to open input stream for $sourceUri")
    }
}
