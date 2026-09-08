package com.toolbot.mobile.feature.pdfdocuments.merger

import android.content.ContentResolver
import android.net.Uri
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import java.io.IOException

fun mergePdfFiles(inputUris: List<Uri>, outputUri: Uri, contentResolver: ContentResolver): Result<Unit> {
    return runCatching {
        if (inputUris.size < 2) {
            throw IllegalArgumentException("At least two PDFs are required.")
        }

        val sourceStreams = mutableListOf<java.io.InputStream>()
        val merger = PDFMergerUtility()

        try {
            inputUris.forEach { uri ->
                val inputStream = contentResolver.openInputStream(uri)
                    ?: throw IOException("Unable to open PDF: $uri")
                sourceStreams.add(inputStream)
                merger.addSource(inputStream)
            }

            contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                merger.setDestinationStream(outputStream)
                merger.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly())
                outputStream.flush()
            } ?: throw IOException("Unable to create output PDF.")
        } finally {
            sourceStreams.forEach { stream ->
                try {
                    stream.close()
                } catch (_: IOException) {
                }
            }
        }
    }
}
