package com.toolbot.mobile.feature.pdfdocuments.word

import android.content.ContentResolver
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.io.StringReader
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.xml.sax.InputSource

fun convertPdfToWord(
    inputUri: Uri,
    outputUri: Uri,
    contentResolver: ContentResolver,
): Result<Unit> {
    return runCatching {
        val inputStream = contentResolver.openInputStream(inputUri)
            ?: throw IOException("Unable to open selected PDF.")

        PDDocument.load(inputStream).use { document ->
            val stripper = PDFTextStripper().apply {
                setSortByPosition(true)
                startPage = 1
                endPage = document.numberOfPages
            }

            val rawText = stripper.getText(document)
            val normalizedText = rawText
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .trim()

            val paragraphs = if (normalizedText.isBlank()) {
                listOf(
                    "No selectable text was found in this PDF.",
                    "This document was generated from a PDF that did not contain extractable text.",
                )
            } else {
                buildParagraphs(normalizedText)
            }

            val tempFile = createTempFile("toolbot_pdf_to_word_", ".docx")
            try {
                tempFile.outputStream().use { outputStream ->
                    writeDocxDocument(outputStream, paragraphs)
                }
                validateDocxFile(tempFile)
                contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                    tempFile.inputStream().use { input -> input.copyTo(outputStream) }
                } ?: throw IOException("Unable to create output DOCX file.")
            } finally {
                tempFile.delete()
            }
        }
    }
}

private fun buildParagraphs(rawText: String): List<String> {
    val pages = rawText.split("\u000C")
    return pages.flatMap { page ->
        page
            .split("\n\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }
}

private fun writeDocxDocument(outputStream: OutputStream, paragraphs: List<String>) {
    val bodyXml = buildDocumentXml(paragraphs)
    if (!isWellFormedXml(bodyXml)) {
        throw IOException("Generated Word XML is invalid or contains unsupported characters.")
    }

    val stylesXml = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
          <w:style w:type="paragraph" w:default="1" w:styleId="Normal">
            <w:name w:val="Normal"/>
            <w:qFormat/>
          </w:style>
        </w:styles>
    """.trimIndent()

    val contentTypesXml = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
          <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
          <Default Extension="xml" ContentType="application/xml"/>
          <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
          <Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
          <Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>
          <Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>
        </Types>
    """.trimIndent()

    val rootRelationsXml = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
          <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/>
          <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/>
        </Relationships>
    """.trimIndent()

    val documentRelationsXml = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
        </Relationships>
    """.trimIndent()

    val coreXml = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:dcmitype="http://purl.org/dc/dcmitype/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
          <dc:title>Converted PDF</dc:title>
          <dc:creator>ToolBot Mobile</dc:creator>
          <cp:lastModifiedBy>ToolBot Mobile</cp:lastModifiedBy>
          <dcterms:created xsi:type="dcterms:W3CDTF">2026-09-08T00:00:00Z</dcterms:created>
          <dcterms:modified xsi:type="dcterms:W3CDTF">2026-09-08T00:00:00Z</dcterms:modified>
        </cp:coreProperties>
    """.trimIndent()

    val appXml = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties" xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes">
          <Application>ToolBot Mobile</Application>
        </Properties>
    """.trimIndent()

    ZipOutputStream(outputStream).use { zip ->
        zip.putNextEntry(ZipEntry("[Content_Types].xml"))
        zip.write(contentTypesXml.toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()

        zip.putNextEntry(ZipEntry("_rels/.rels"))
        zip.write(rootRelationsXml.toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()

        zip.putNextEntry(ZipEntry("docProps/core.xml"))
        zip.write(coreXml.toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()

        zip.putNextEntry(ZipEntry("docProps/app.xml"))
        zip.write(appXml.toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()

        zip.putNextEntry(ZipEntry("word/styles.xml"))
        zip.write(stylesXml.toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()

        zip.putNextEntry(ZipEntry("word/_rels/document.xml.rels"))
        zip.write(documentRelationsXml.toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()

        zip.putNextEntry(ZipEntry("word/document.xml"))
        zip.write(bodyXml.toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()
    }
}

private fun buildDocumentXml(paragraphs: List<String>): String {
    val paragraphXml = paragraphs.ifEmpty { listOf("No selectable text was found in this file.") }
        .joinToString(separator = "") { paragraph ->
            val lines = paragraph.split('\n')
            val runs = lines.joinToString(separator = "") { line ->
                val safeText = escapeXml(sanitizeXmlText(line))
                if (line == lines.last()) {
                    "<w:t xml:space=\"preserve\">$safeText</w:t>"
                } else {
                    "<w:t xml:space=\"preserve\">$safeText</w:t><w:br/>"
                }
            }
            "<w:p><w:r>$runs</w:r></w:p>"
        }

    return """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
          <w:body>
            $paragraphXml
            <w:sectPr>
              <w:pgSz w:w="12240" w:h="15840"/>
              <w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440" w:header="720" w:footer="720" w:gutter="0"/>
            </w:sectPr>
          </w:body>
        </w:document>
    """.trimIndent()
}

private fun sanitizeXmlText(value: String): String {
    return value.filter { ch ->
        val code = ch.code
        code == 0x9 || code == 0xA || code == 0xD ||
            code in 0x20..0xD7FF ||
            code in 0xE000..0xFFFD ||
            code in 0x10000..0x10FFFF
    }
}

private fun isWellFormedXml(xml: String): Boolean {
    return try {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            isCoalescing = true
            isIgnoringComments = true
        }
        val builder = factory.newDocumentBuilder()
        builder.parse(InputSource(StringReader(xml)))
        true
    } catch (_: Exception) {
        false
    }
}

private fun escapeXml(value: String): String {
    return buildString {
        value.forEach { ch ->
            when (ch) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                else -> append(ch)
            }
        }
    }
}

private fun validateDocxFile(file: File) {
    val zipFile = ZipFile(file)
    val names = zipFile.entries().asSequence().map { it.name }.toSet()
    val required = setOf(
        "[Content_Types].xml",
        "_rels/.rels",
        "docProps/core.xml",
        "docProps/app.xml",
        "word/document.xml",
        "word/styles.xml",
        "word/_rels/document.xml.rels",
    )
    if (!required.all { it in names }) {
        throw IOException("Generated DOCX package is missing required OOXML entries.")
    }

    val documentXml = zipFile.getInputStream(zipFile.getEntry("word/document.xml")).bufferedReader().use { it.readText() }
    if (!isWellFormedXml(documentXml)) {
        throw IOException("Generated DOCX XML is malformed.")
    }
}
