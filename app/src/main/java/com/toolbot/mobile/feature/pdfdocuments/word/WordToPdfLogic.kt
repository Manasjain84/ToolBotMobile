package com.toolbot.mobile.feature.pdfdocuments.word

import android.content.ContentResolver
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import java.io.File
import java.io.IOException
import java.util.zip.ZipException
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Node

fun isDocxUri(uri: Uri, resolver: ContentResolver): Boolean {
    val mimeType = resolver.getType(uri)?.lowercase()
    val lowerUri = uri.toString().lowercase()
    return mimeType == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ||
        lowerUri.endsWith(".docx")
}

fun convertDocxToPdf(
    inputUri: Uri,
    outputUri: Uri,
    contentResolver: ContentResolver,
): Result<Unit> {
    return runCatching {
        val xmlText = extractDocxText(inputUri, contentResolver)
            .getOrElse { throw it }

        if (xmlText.isBlank()) {
            throw IOException("The selected DOCX file is empty or contains no readable text.")
        }

        val tempFile = createTempFile("toolbot_word_to_pdf_", ".pdf")
        try {
            tempFile.outputStream().use { stream ->
                createPdfFromText(xmlText, stream)
            }
            validatePdfFile(tempFile)
            contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                tempFile.inputStream().use { input -> input.copyTo(outputStream) }
            } ?: throw IOException("Unable to create the output PDF file.")
        } finally {
            tempFile.delete()
        }
    }
}

fun extractDocxText(
    inputUri: Uri,
    contentResolver: ContentResolver,
): Result<String> {
    return runCatching {
        val inputStream = contentResolver.openInputStream(inputUri)
            ?: throw IOException("Unable to open the selected Word document.")

        inputStream.use { stream ->
            ZipInputStream(stream).use { zip ->
                var entry = zip.nextEntry
                var documentXml: ByteArray? = null
                while (entry != null) {
                    if (entry.name == "word/document.xml") {
                        documentXml = zip.readBytes()
                        break
                    }
                    entry = zip.nextEntry
                }

                if (documentXml == null) {
                    throw IOException("The DOCX file is missing word/document.xml.")
                }

                val factory = DocumentBuilderFactory.newInstance().apply {
                    isNamespaceAware = true
                    isIgnoringComments = true
                    isIgnoringElementContentWhitespace = true
                }

                val builder = factory.newDocumentBuilder()
                val document = builder.parse(documentXml.inputStream())
                val paragraphNodes = document.getElementsByTagNameNS("*", "p")
                val paragraphs = mutableListOf<String>()

                for (i in 0 until paragraphNodes.length) {
                    val paragraphNode = paragraphNodes.item(i)
                    val paragraphText = extractParagraphText(paragraphNode)
                    if (paragraphText.isNotBlank()) {
                        paragraphs.add(paragraphText)
                    }
                }

                if (paragraphs.isEmpty()) {
                    ""
                } else {
                    paragraphs.joinToString(separator = "\n\n")
                }
            }
        }
    }.recoverCatching { throwable ->
        when (throwable) {
            is ZipException -> throw IOException("The selected file is not a valid DOCX package.", throwable)
            is IOException -> throw throwable
            else -> throw IOException("Unable to extract text from the DOCX file.", throwable)
        }
    }
}

private fun extractParagraphText(node: Node): String {
    val builder = StringBuilder()
    val childNodes = node.childNodes
    for (i in 0 until childNodes.length) {
        val child = childNodes.item(i)
        when {
            child.nodeType == Node.TEXT_NODE -> builder.append(child.textContent)
            child.nodeType == Node.ELEMENT_NODE -> {
                val name = child.localName ?: child.nodeName
                when (name) {
                    "t" -> builder.append(child.textContent)
                    "tab" -> builder.append("\t")
                    "br", "cr" -> builder.append("\n")
                    else -> builder.append(extractParagraphText(child))
                }
            }
        }
    }
    return builder.toString()
        .replace("\u00A0", " ")
        .replace("\t", " ")
        .replace("\r", "")
        .replace("\n", " ")
        .trim()
}

private fun createPdfFromText(text: String, outputStream: java.io.OutputStream) {
    val document = PDDocument()
    val font = PDType1Font.HELVETICA
    val fontSize = 12f
    val lineHeight = 16f
    val marginLeft = 54f
    val marginTop = 780f
    val marginBottom = 54f

    val paragraphs = text
        .replace("\r\n", "\n")
        .replace("\r", "\n")
        .split("\n\n")
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    if (paragraphs.isEmpty()) {
        throw IOException("The selected DOCX file is empty or contains no readable text.")
    }

    val firstPage = PDPage(PDRectangle.A4)
    document.addPage(firstPage)
    var currentPage = firstPage
    var currentY = marginTop
    var contentStream = PDPageContentStream(document, currentPage)

    try {
        contentStream.beginText()
        contentStream.setFont(font, fontSize)
        contentStream.newLineAtOffset(marginLeft, marginTop)

        for (paragraph in paragraphs) {
            val wrappedLines = wrapParagraph(paragraph, font, fontSize, firstPage.mediaBox.width - marginLeft - 54f)
            for (line in wrappedLines) {
                if (currentY - lineHeight < marginBottom) {
                    contentStream.endText()
                    contentStream.close()
                    val newPage = PDPage(PDRectangle.A4)
                    document.addPage(newPage)
                    currentPage = newPage
                    currentY = marginTop
                    contentStream = PDPageContentStream(document, currentPage)
                    contentStream.beginText()
                    contentStream.setFont(font, fontSize)
                    contentStream.newLineAtOffset(marginLeft, marginTop)
                }
                contentStream.newLineAtOffset(0f, -lineHeight)
                contentStream.showText(line)
                currentY -= lineHeight
            }
            contentStream.newLineAtOffset(0f, -lineHeight)
            currentY -= lineHeight
        }

        contentStream.endText()
        contentStream.close()
        document.save(outputStream)
        outputStream.flush()
    } catch (throwable: Throwable) {
        try {
            contentStream.close()
        } catch (_: Exception) {
        }
        throw IOException("Failed to generate a valid PDF from the DOCX content.", throwable)
    } finally {
        document.close()
    }
}

private fun wrapParagraph(
    paragraph: String,
    font: com.tom_roush.pdfbox.pdmodel.font.PDFont,
    fontSize: Float,
    maxWidth: Float,
): List<String> {
    val words = paragraph.trim().split(Regex("\\s+"))
    if (words.isEmpty()) return emptyList()
    val result = mutableListOf<String>()
    var line = ""

    for (word in words) {
        val candidate = if (line.isEmpty()) word else "$line $word"
        val width = font.getStringWidth(candidate) / 1000f * fontSize
        if (width <= maxWidth || line.isEmpty()) {
            line = candidate
        } else {
            result.add(line)
            line = word
        }
    }
    if (line.isNotEmpty()) result.add(line)
    return result
}

private fun validatePdfFile(file: File) {
    val bytes = file.readBytes()
    if (bytes.size < 5 || !bytes.copyOfRange(0, 5).contentEquals(byteArrayOf(0x25.toByte(), 0x50.toByte(), 0x44.toByte(), 0x46.toByte(), 0x2D.toByte()))) {
        throw IOException("Generated PDF is missing a valid PDF header.")
    }
    PDDocument.load(file.inputStream()).use { document ->
        if (document.numberOfPages <= 0) {
            throw IOException("Generated PDF does not contain any pages.")
        }
    }
}
