package com.toolbot.mobile.feature.pdfdocuments.splitter

import android.content.ContentResolver
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import java.io.IOException

fun getPdfPageCount(uri: Uri, contentResolver: ContentResolver): Result<Int> {
    return runCatching {
        contentResolver.openInputStream(uri)?.use { inputStream ->
            PDDocument.load(inputStream).use { document ->
                document.numberOfPages
            }
        } ?: throw IOException("Unable to open selected PDF.")
    }
}

fun splitPdfRange(
    inputUri: Uri,
    startPage: Int,
    endPage: Int,
    outputUri: Uri,
    contentResolver: ContentResolver,
): Result<Unit> {
    return runCatching {
        require(startPage >= 1) { "Start page must be 1 or greater." }
        require(endPage >= startPage) { "End page must be greater than or equal to the start page." }

        val totalPages = getPdfPageCount(inputUri, contentResolver).getOrThrow()
        require(endPage <= totalPages) { "End page must not exceed the total pages." }

        contentResolver.openInputStream(inputUri)?.use { inputStream ->
            PDDocument.load(inputStream).use { sourceDocument ->
                val outputDocument = PDDocument()
                try {
                    for (pageIndex in (startPage - 1) until endPage) {
                        outputDocument.importPage(sourceDocument.getPage(pageIndex))
                    }

                    contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                        outputDocument.save(outputStream)
                    } ?: throw IOException("Unable to create output PDF.")
                } finally {
                    outputDocument.close()
                }
            }
        } ?: throw IOException("Unable to open selected PDF.")
    }
}
