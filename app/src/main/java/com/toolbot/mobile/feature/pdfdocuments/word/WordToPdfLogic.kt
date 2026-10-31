package com.toolbot.mobile.feature.pdfdocuments.word

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.toolbot.mobile.feature.pdfdocuments.common.PdfDocumentUtils
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDFont
import com.tom_roush.pdfbox.pdmodel.font.PDType0Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipException
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.w3c.dom.Node

sealed class DocxContentItem {
    data class Paragraph(val text: String) : DocxContentItem()
    data class Image(val bytes: ByteArray, val name: String) : DocxContentItem() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Image
            if (!bytes.contentEquals(other.bytes)) return false
            if (name != other.name) return false
            return true
        }

        override fun hashCode(): Int {
            var result = bytes.contentHashCode()
            result = 31 * result + name.hashCode()
            return result
        }
    }
}

data class DocxContent(
    val items: List<DocxContentItem>,
) {
    fun hasAnyContent(): Boolean = items.any {
        when (it) {
            is DocxContentItem.Paragraph -> it.text.isNotBlank()
            is DocxContentItem.Image -> it.bytes.isNotEmpty()
        }
    }

    fun getAllText(): String = items.filterIsInstance<DocxContentItem.Paragraph>()
        .map { it.text }
        .filter { it.isNotBlank() }
        .joinToString("\n\n")
}

fun isDocxUri(uri: Uri, resolver: ContentResolver): Boolean {
    val mimeType = resolver.getType(uri)?.lowercase()
    val lowerUri = uri.toString().lowercase()
    if (mimeType == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ||
        lowerUri.endsWith(".docx")) {
        return true
    }

    val displayName = PdfDocumentUtils.getDisplayName(uri, resolver).lowercase()
    if (displayName.endsWith(".docx") || displayName.endsWith(".doc")) {
        return true
    }

    if (mimeType == null || mimeType == "application/zip" || mimeType == "application/octet-stream") {
        return true
    }

    return false
}

fun convertDocxToPdf(
    inputUri: Uri,
    outputUri: Uri,
    context: Context,
): Result<Unit> {
    return runCatching {
        if (!PDFBoxResourceLoader.isReady()) {
            try {
                PDFBoxResourceLoader.init(context)
            } catch (_: Exception) {
            }
        }

        val contentResolver = context.contentResolver
        val docxContent = extractDocxContent(inputUri, contentResolver)
            .getOrElse { throw it }

        if (!docxContent.hasAnyContent()) {
            throw IOException("The selected Word document contains no readable text or supported images.")
        }

        val tempFile = File.createTempFile("toolbot_word_to_pdf_", ".pdf", context.cacheDir)
        try {
            createPdfFromDocxContent(context, docxContent, tempFile)

            validatePdfFile(tempFile)

            val outputStream = contentResolver.openOutputStream(outputUri, "rwt")
                ?: contentResolver.openOutputStream(outputUri)
                ?: throw IOException("Unable to open output destination PDF for writing.")

            try {
                outputStream.use { os ->
                    tempFile.inputStream().use { input -> input.copyTo(os) }
                    os.flush()
                }
            } catch (writeEx: Exception) {
                throw IOException("Failed to write generated PDF to destination: ${writeEx.message}", writeEx)
            }

            // Verify non-zero bytes written to SAF destination where supported
            try {
                contentResolver.openFileDescriptor(outputUri, "r")?.use { pfd ->
                    if (pfd.statSize == 0L) {
                        throw IOException("Destination PDF file is empty (0 bytes) after writing.")
                    }
                }
            } catch (statEx: IOException) {
                throw statEx
            } catch (_: Exception) {
                // Ignore SAF provider stat UnsupportedOperationExceptions
            }

        } finally {
            tempFile.delete()
        }
    }
}

fun extractDocxText(
    inputUri: Uri?,
    contentResolver: ContentResolver,
    inputStreamOverride: InputStream? = null,
): Result<String> {
    return extractDocxContent(inputUri, contentResolver, inputStreamOverride).map { it.getAllText() }
}

fun extractDocxContent(
    inputUri: Uri?,
    contentResolver: ContentResolver,
    inputStreamOverride: InputStream? = null,
): Result<DocxContent> {
    return runCatching {
        val inputStream = inputStreamOverride
            ?: (inputUri?.let { contentResolver.openInputStream(it) })
            ?: throw IOException("Unable to open the selected Word document.")

        val zipEntriesMap = mutableMapOf<String, ByteArray>()

        inputStream.use { stream ->
            ZipInputStream(stream).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val normKey = normalizeZipPath(entry.name)
                        zipEntriesMap[normKey] = zip.readBytes()
                    }
                    entry = zip.nextEntry
                }
            }
        }

        val documentXmlBytes = zipEntriesMap["word/document.xml"]
            ?: zipEntriesMap.entries.firstOrNull { it.key.endsWith("word/document.xml") }?.value
            ?: throw IOException("The selected file is not a valid DOCX document (missing word/document.xml).")

        val relsXmlBytes = zipEntriesMap["word/_rels/document.xml.rels"]
            ?: zipEntriesMap.entries.firstOrNull { it.key.endsWith("word/_rels/document.xml.rels") }?.value

        val rIdToZipPathMap = mutableMapOf<String, String>()
        if (relsXmlBytes != null && relsXmlBytes.isNotEmpty()) {
            parseRelsXml(relsXmlBytes, rIdToZipPathMap)
        }

        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            isIgnoringComments = true
            isIgnoringElementContentWhitespace = true
            try {
                setFeature("http://javax.xml.XMLConstants/feature/secure-processing", true)
                setFeature("http://xml.org/sax/features/external-general-entities", false)
                setFeature("http://xml.org/sax/features/external-parameter-entities", false)
                setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            } catch (_: Exception) {
            }
        }

        val builder = factory.newDocumentBuilder()
        val document = builder.parse(documentXmlBytes.inputStream())

        val items = mutableListOf<DocxContentItem>()
        val paragraphNodes = document.getElementsByTagNameNS("*", "p")

        for (i in 0 until paragraphNodes.length) {
            val pNode = paragraphNodes.item(i)
            extractParagraphAndImages(pNode, rIdToZipPathMap, zipEntriesMap, items)
        }

        DocxContent(items)
    }.recoverCatching { throwable ->
        when (throwable) {
            is ZipException -> throw IOException("The selected file is not a valid DOCX package.", throwable)
            is IOException -> throw throwable
            else -> throw IOException("Unable to extract content from the DOCX file: ${throwable.message}", throwable)
        }
    }
}

private fun normalizeZipPath(path: String): String {
    val clean = path.replace("\\", "/").trim().lowercase().removePrefix("/")
    return if (clean.startsWith("../")) {
        "word/" + clean.removePrefix("../")
    } else if (clean.startsWith("media/")) {
        "word/" + clean
    } else {
        clean
    }
}

private fun parseRelsXml(relsXmlBytes: ByteArray, rIdToZipPathMap: MutableMap<String, String>) {
    try {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(relsXmlBytes.inputStream())
        val relNodes = doc.getElementsByTagNameNS("*", "Relationship")

        for (i in 0 until relNodes.length) {
            val relNode = relNodes.item(i)
            if (relNode is Element) {
                val id = relNode.getAttribute("Id")
                val target = relNode.getAttribute("Target")
                if (id.isNotBlank() && target.isNotBlank()) {
                    var normTarget = target.replace("\\", "/").trim()
                    if (normTarget.startsWith("../")) {
                        normTarget = "word/" + normTarget.removePrefix("../")
                    } else if (normTarget.startsWith("media/")) {
                        normTarget = "word/" + normTarget
                    } else if (!normTarget.startsWith("word/")) {
                        normTarget = "word/" + normTarget.removePrefix("/")
                    }
                    rIdToZipPathMap[id] = normTarget.lowercase()
                }
            }
        }
    } catch (e: Exception) {
        println("Warning: Failed to parse word/_rels/document.xml.rels: ${e.message}")
    }
}

private fun extractParagraphAndImages(
    pNode: Node,
    rIdToZipPathMap: Map<String, String>,
    zipEntriesMap: Map<String, ByteArray>,
    outItems: MutableList<DocxContentItem>,
) {
    val textBuilder = StringBuilder()

    fun flushText() {
        val text = textBuilder.toString()
            .replace("\u00A0", " ")
            .replace("\t", " ")
            .replace("\r", "")
            .replace("\n", " ")
            .trim()
        if (text.isNotBlank()) {
            outItems.add(DocxContentItem.Paragraph(text))
        }
        textBuilder.clear()
    }

    fun traverse(node: Node) {
        val nodeType = node.nodeType
        if (nodeType == Node.TEXT_NODE) {
            textBuilder.append(node.textContent)
            return
        }

        if (nodeType == Node.ELEMENT_NODE) {
            val elem = node as Element
            val name = (elem.localName ?: elem.nodeName).lowercase()

            when (name) {
                "t" -> textBuilder.append(elem.textContent)
                "tab" -> textBuilder.append("\t")
                "br", "cr" -> textBuilder.append("\n")
                "blip", "imagedata" -> {
                    val rId = elem.getAttributeNS("http://schemas.openxmlformats.org/officeDocument/2006/relationships", "embed")
                        .ifEmpty { elem.getAttributeNS("http://schemas.openxmlformats.org/officeDocument/2006/relationships", "link") }
                        .ifEmpty { elem.getAttribute("r:embed") }
                        .ifEmpty { elem.getAttribute("r:link") }
                        .ifEmpty { elem.getAttribute("r:id") }

                    if (rId.isNotBlank()) {
                        val targetZipPath = rIdToZipPathMap[rId] ?: rIdToZipPathMap[rId.lowercase()]
                        if (targetZipPath != null) {
                            val imgBytes = zipEntriesMap[targetZipPath] ?: zipEntriesMap[targetZipPath.lowercase()]
                            if (imgBytes != null && imgBytes.isNotEmpty()) {
                                flushText()
                                outItems.add(DocxContentItem.Image(imgBytes, targetZipPath))
                            } else {
                                println("Warning: Image bytes missing in zip for path $targetZipPath (rId=$rId)")
                            }
                        } else {
                            println("Warning: Unresolved image relationship ID $rId in document.xml.rels")
                        }
                    }
                }
                else -> {
                    val children = node.childNodes
                    for (i in 0 until children.length) {
                        traverse(children.item(i))
                    }
                }
            }
        }
    }

    traverse(pNode)
    flushText()
}

private fun loadUnicodeFont(document: PDDocument, context: Context?): PDFont {
    if (context != null) {
        try {
            if (!PDFBoxResourceLoader.isReady()) {
                PDFBoxResourceLoader.init(context)
            }
            context.assets.open("fonts/app_font.ttf").use { isStream ->
                return PDType0Font.load(document, isStream)
            }
        } catch (e: Exception) {
            println("Asset font load failed: ${e.message}")
        }
    }

    val systemFontPaths = listOf(
        "src/main/assets/fonts/app_font.ttf",
        "app/src/main/assets/fonts/app_font.ttf",
        "../app/src/main/assets/fonts/app_font.ttf",
        "C:/Users/Manas/AndroidStudioProjects/ToolBotMobile/app/src/main/assets/fonts/app_font.ttf",
        "/system/fonts/Roboto-Regular.ttf",
        "/system/fonts/NotoSans-Regular.ttf",
        "/system/fonts/DroidSansFallback.ttf",
        "C:\\Windows\\Fonts\\arial.ttf",
    )

    for (path in systemFontPaths) {
        val file = File(path)
        if (file.exists() && file.length() > 0L) {
            try {
                file.inputStream().use { isStream ->
                    return PDType0Font.load(document, isStream)
                }
            } catch (e: Exception) {
                println("File font load failed for $path: $e")
            }
        }
    }

    throw IOException("Unable to load a suitable TrueType Unicode font for PDF generation.")
}

private fun sanitizeTextForFont(text: String, font: PDFont): String {
    val builder = StringBuilder()
    for (ch in text) {
        val code = ch.code
        if (code in 0..31 && code != 9 && code != 10 && code != 13) {
            continue
        }
        try {
            font.encode(ch.toString())
            builder.append(ch)
        } catch (_: Exception) {
            builder.append(' ')
        }
    }
    return builder.toString()
}

fun createPdfFromText(
    context: Context?,
    text: String,
    outputFile: File,
) {
    val paragraphs = text
        .replace("\r\n", "\n")
        .replace("\r", "\n")
        .split("\n\n")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { DocxContentItem.Paragraph(it) }

    createPdfFromDocxContent(context, DocxContent(paragraphs), outputFile)
}

fun createPdfFromDocxContent(
    context: Context?,
    content: DocxContent,
    outputFile: File,
) {
    val document = PDDocument()
    val font = loadUnicodeFont(document, context)
    val fontSize = 12f
    val lineHeight = 16f
    val marginLeft = 54f
    val marginTop = 780f
    val marginBottom = 54f
    val printableWidth = PDRectangle.A4.width - marginLeft - 54f
    val printableHeight = marginTop - marginBottom

    if (!content.hasAnyContent()) {
        document.close()
        throw IOException("The selected Word document contains no readable content.")
    }

    val firstPage = PDPage(PDRectangle.A4)
    document.addPage(firstPage)
    var currentPage = firstPage
    var currentY = marginTop
    var contentStream = PDPageContentStream(document, currentPage)
    var inTextMode = false

    fun closeTextMode() {
        if (inTextMode) {
            try {
                contentStream.endText()
            } catch (_: Exception) {
            }
            inTextMode = false
        }
    }

    fun openTextMode() {
        if (!inTextMode) {
            contentStream.beginText()
            contentStream.setFont(font, fontSize)
            inTextMode = true
        }
    }

    fun startNewPage() {
        closeTextMode()
        try {
            contentStream.close()
        } catch (_: Exception) {
        }

        val newPage = PDPage(PDRectangle.A4)
        document.addPage(newPage)
        currentPage = newPage
        currentY = marginTop
        contentStream = PDPageContentStream(document, currentPage)
    }

    try {
        for (item in content.items) {
            when (item) {
                is DocxContentItem.Paragraph -> {
                    val sanitized = sanitizeTextForFont(item.text, font)
                    if (sanitized.isBlank()) continue

                    val wrappedLines = wrapParagraph(sanitized, font, fontSize, printableWidth)

                    for (line in wrappedLines) {
                        if (currentY - lineHeight < marginBottom) {
                            startNewPage()
                        }

                        openTextMode()
                        contentStream.newLineAtOffset(marginLeft, currentY)
                        contentStream.showText(line)
                        contentStream.newLineAtOffset(-marginLeft, -currentY)
                        currentY -= lineHeight
                    }

                    currentY -= 8f // Paragraph spacing
                }

                is DocxContentItem.Image -> {
                    closeTextMode()

                    val pdImage: PDImageXObject? = try {
                        val bitmap: Bitmap? = try {
                            BitmapFactory.decodeByteArray(item.bytes, 0, item.bytes.size)
                        } catch (_: Exception) {
                            null
                        }

                        if (bitmap != null) {
                            try {
                                LosslessFactory.createFromImage(document, bitmap)
                            } finally {
                                bitmap.recycle()
                            }
                        } else {
                            // On Android device, if bitmap decoding failed or as fallback for JPEG streams:
                            JPEGFactory.createFromStream(document, ByteArrayInputStream(item.bytes))
                        }
                    } catch (e: Exception) {
                        println("Warning: Failed to create PDImageXObject for ${item.name}: ${e.message}")
                        null
                    }

                    if (pdImage == null) {
                        println("Warning: Corrupt or unsupported image format for ${item.name}")
                        continue
                    }

                    val origW = pdImage.width.toFloat().coerceAtLeast(1f)
                    val origH = pdImage.height.toFloat().coerceAtLeast(1f)
                    val maxW = printableWidth
                    val maxH = (printableHeight * 0.7f).coerceAtLeast(100f)

                    var drawW = origW
                    var drawH = origH

                    if (drawW > maxW) {
                        drawH = drawH * (maxW / drawW)
                        drawW = maxW
                    }
                    if (drawH > maxH) {
                        drawW = drawW * (maxH / drawH)
                        drawH = maxH
                    }

                    if (currentY - drawH < marginBottom) {
                        startNewPage()
                    }

                    contentStream.drawImage(pdImage, marginLeft, currentY - drawH, drawW, drawH)
                    currentY -= (drawH + 12f) // Spacing after image
                }
            }
        }

        closeTextMode()
        contentStream.close()

        outputFile.outputStream().use { os ->
            document.save(os)
            os.flush()
        }
    } catch (e: Exception) {
        closeTextMode()
        try {
            contentStream.close()
        } catch (_: Exception) {
        }
        throw IOException("Failed to generate PDF from DOCX content: ${e.message}", e)
    } finally {
        document.close()
    }
}

private fun wrapParagraph(
    paragraph: String,
    font: PDFont,
    fontSize: Float,
    maxWidth: Float,
): List<String> {
    val words = paragraph.trim().split(Regex("\\s+"))
    if (words.isEmpty()) return emptyList()
    val result = mutableListOf<String>()
    var line = ""

    for (word in words) {
        val candidate = if (line.isEmpty()) word else "$line $word"
        val width = calculateStringWidth(candidate, font, fontSize)
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

private fun calculateStringWidth(text: String, font: PDFont, fontSize: Float): Float {
    return try {
        font.getStringWidth(text) / 1000f * fontSize
    } catch (_: Exception) {
        text.length * fontSize * 0.5f
    }
}

private fun validatePdfFile(file: File) {
    if (!file.exists() || file.length() <= 0L) {
        throw IOException("Generated PDF file is empty or missing.")
    }
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
