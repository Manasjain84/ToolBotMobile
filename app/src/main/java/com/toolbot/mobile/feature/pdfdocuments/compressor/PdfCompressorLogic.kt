package com.toolbot.mobile.feature.pdfdocuments.compressor

import android.content.ContentResolver
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import java.io.IOException

enum class CompressionLevel(val label: String, val value: Int) {
    LOW("Low", 3),
    MEDIUM("Medium", 6),
    HIGH("High", 9),
}

fun compressPdfFile(
    inputUri: Uri,
    outputUri: Uri,
    level: CompressionLevel,
    contentResolver: ContentResolver,
): Result<Unit> {
    return runCatching {
        val inputStream = contentResolver.openInputStream(inputUri)
            ?: throw IOException("Unable to open selected PDF.")

        val sourceDocument = PDDocument.load(inputStream)
        val optimizedDocument = PDDocument()

        try {
            for (pageIndex in 0 until sourceDocument.numberOfPages) {
                optimizedDocument.importPage(sourceDocument.getPage(pageIndex))
            }

            contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                optimizedDocument.save(outputStream)
            } ?: throw IOException("Unable to create compressed output PDF.")
        } finally {
            sourceDocument.close()
            optimizedDocument.close()
        }
    }
}
