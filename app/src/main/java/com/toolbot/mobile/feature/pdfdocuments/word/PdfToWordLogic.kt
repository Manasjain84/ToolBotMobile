package com.toolbot.mobile.feature.pdfdocuments.word

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.rendering.PDFRenderer
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.roundToInt

data class ExactPageData(
    val imageFile: File,
    val widthPt: Float,
    val heightPt: Float,
    val widthTwips: Int,
    val heightTwips: Int,
    val cxEmu: Long,
    val cyEmu: Long,
)

fun convertPdfToWord(
    inputUri: Uri,
    outputUri: Uri,
    context: Context,
): Result<Unit> {
    return runCatching {
        convertPdfToWordExactLayout(inputUri, outputUri, context)
    }
}

private fun convertPdfToWordExactLayout(
    inputUri: Uri,
    outputUri: Uri,
    context: Context,
) {
    val contentResolver = context.contentResolver
    val inputStream = contentResolver.openInputStream(inputUri)
        ?: throw IOException("Unable to open selected PDF.")

    val tempDocxFile = File.createTempFile("toolbot_exact_pdf_to_word_", ".docx", context.cacheDir)
    val tempImageFiles = mutableListOf<File>()

    try {
        val exactPages = mutableListOf<ExactPageData>()

        PDDocument.load(inputStream).use { document ->
            if (document.numberOfPages == 0) {
                throw IOException("Selected PDF file has no pages.")
            }

            val renderer = PDFRenderer(document)

            for (pageIndex in 0 until document.numberOfPages) {
                val page = document.getPage(pageIndex)
                val cropBox = page.cropBox ?: page.mediaBox
                val widthPt = cropBox.width.coerceAtLeast(1f)
                val heightPt = cropBox.height.coerceAtLeast(1f)

                val widthTwips = (widthPt * 20f).roundToInt().coerceAtLeast(1)
                val heightTwips = (heightPt * 20f).roundToInt().coerceAtLeast(1)

                val cxEmu = (widthPt * 12700f).toLong().coerceAtLeast(1L)
                val cyEmu = (heightPt * 12700f).toLong().coerceAtLeast(1L)

                // Render page at 150 DPI for crisp readability and reasonable file size
                val bitmap = renderer.renderImageWithDPI(pageIndex, 150f)
                val pageImageFile = File.createTempFile("exact_page_${pageIndex}_", ".jpg", context.cacheDir)
                tempImageFiles.add(pageImageFile)

                try {
                    pageImageFile.outputStream().use { os ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, os)
                    }
                } finally {
                    bitmap.recycle()
                }

                exactPages.add(
                    ExactPageData(
                        imageFile = pageImageFile,
                        widthPt = widthPt,
                        heightPt = heightPt,
                        widthTwips = widthTwips,
                        heightTwips = heightTwips,
                        cxEmu = cxEmu,
                        cyEmu = cyEmu,
                    )
                )
            }

            tempDocxFile.outputStream().use { outputStream ->
                writeExactLayoutDocxDocument(outputStream, exactPages)
            }
        }

        if (!tempDocxFile.exists() || tempDocxFile.length() <= 0L) {
            throw IOException("Generated Exact Layout document is empty.")
        }

        validateDocxPackage(tempDocxFile)

        contentResolver.openOutputStream(outputUri)?.use { outputStream ->
            tempDocxFile.inputStream().use { input -> input.copyTo(outputStream) }
        } ?: throw IOException("Unable to create output DOCX file.")

    } finally {
        tempDocxFile.delete()
        for (file in tempImageFiles) {
            file.delete()
        }
    }
}

fun writeExactLayoutDocxDocument(outputStream: OutputStream, pages: List<ExactPageData>) {
    val bodyXml = buildExactLayoutDocumentXml(pages)

    val stylesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:docDefaults>
    <w:rPrDefault>
      <w:rPr>
        <w:rFonts w:ascii="Calibri" w:hAnsi="Calibri" w:cs="Calibri"/>
        <w:sz w:val="22"/>
        <w:szCs w:val="22"/>
        <w:lang w:val="en-US" w:fareast="en-US" w:bidi="ar-SA"/>
      </w:rPr>
    </w:rPrDefault>
    <w:pPrDefault>
      <w:pPr/>
    </w:pPrDefault>
  </w:docDefaults>
  <w:style w:type="paragraph" w:default="1" w:styleId="Normal">
    <w:name w:val="Normal"/>
    <w:qFormat/>
  </w:style>
</w:styles>"""

    val settingsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:settings xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:zoom w:pct="100"/>
</w:settings>"""

    val fontTableXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:fonts xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:font w:name="Calibri">
    <w:panose1 w:val="020F0502020204030204"/>
    <w:charset w:val="00"/>
    <w:family w:val="swiss"/>
    <w:pitch w:val="variable"/>
    <w:sig w:val="E0002AFF"/>
  </w:font>
</w:fonts>"""

    val contentTypesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Default Extension="jpeg" ContentType="image/jpeg"/>
  <Default Extension="jpg" ContentType="image/jpeg"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
  <Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
  <Override PartName="/word/settings.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.settings+xml"/>
  <Override PartName="/word/fontTable.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.fontTable+xml"/>
  <Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>
  <Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>
</Types>"""

    val rootRelationsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/>
</Relationships>"""

    val rels = mutableListOf<String>()
    rels.add("<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>")
    rels.add("<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/settings\" Target=\"settings.xml\"/>")
    rels.add("<Relationship Id=\"rId3\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/fontTable\" Target=\"fontTable.xml\"/>")

    for ((index, _) in pages.withIndex()) {
        val relId = "rId${index + 4}"
        rels.add("<Relationship Id=\"$relId\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/image\" Target=\"media/exact_page_${index + 1}.jpg\"/>")
    }

    val documentRelationsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  ${rels.joinToString(separator = "\n  ")}
</Relationships>"""

    val coreXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:dcmitype="http://purl.org/dc/dcmitype/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <dc:title>Converted PDF Document (Exact Layout)</dc:title>
  <dc:creator>ToolBot Mobile</dc:creator>
  <cp:lastModifiedBy>ToolBot Mobile</cp:lastModifiedBy>
  <dcterms:created xsi:type="dcterms:W3CDTF">2026-09-08T00:00:00Z</dcterms:created>
  <dcterms:modified xsi:type="dcterms:W3CDTF">2026-09-08T00:00:00Z</dcterms:modified>
</cp:coreProperties>"""

    val appXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties" xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes">
  <Template>Normal.dotm</Template>
  <TotalTime>0</TotalTime>
  <Pages>${pages.size}</Pages>
  <Words>0</Words>
  <Characters>0</Characters>
  <Lines>0</Lines>
  <Paragraphs>${pages.size}</Paragraphs>
  <ScaleCrop>false</ScaleCrop>
  <Company>ToolBot</Company>
  <LinksUpToDate>false</LinksUpToDate>
  <CharactersWithSpaces>0</CharactersWithSpaces>
  <SharedDoc>false</SharedDoc>
  <HyperlinksChanged>false</HyperlinksChanged>
  <AppVersion>16.0000</AppVersion>
</Properties>"""

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

        zip.putNextEntry(ZipEntry("word/settings.xml"))
        zip.write(settingsXml.toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()

        zip.putNextEntry(ZipEntry("word/fontTable.xml"))
        zip.write(fontTableXml.toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()

        zip.putNextEntry(ZipEntry("word/_rels/document.xml.rels"))
        zip.write(documentRelationsXml.toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()

        zip.putNextEntry(ZipEntry("word/document.xml"))
        zip.write(bodyXml.toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()

        for ((index, page) in pages.withIndex()) {
            zip.putNextEntry(ZipEntry("word/media/exact_page_${index + 1}.jpg"))
            if (page.imageFile.exists()) {
                page.imageFile.inputStream().use { input -> input.copyTo(zip) }
            }
            zip.closeEntry()
        }
    }
}

fun buildExactLayoutDocumentXml(pages: List<ExactPageData>): String {
    val bodyXmlBuilder = StringBuilder()

    if (pages.isEmpty()) {
        bodyXmlBuilder.append("<w:p><w:r><w:t xml:space=\"preserve\">No pages found in document.</w:t></w:r></w:p>")
    } else {
        for ((index, page) in pages.withIndex()) {
            val relId = "rId${index + 4}"
            val pageNum = index + 1

            bodyXmlBuilder.append("""
                <w:p>
                  <w:pPr>
                    <w:spacing w:before="0" w:after="0" w:line="240" w:lineRule="auto"/>
                    ${if (index < pages.size - 1) """
                    <w:sectPr>
                      <w:pgSz w:w="${page.widthTwips}" w:h="${page.heightTwips}"/>
                      <w:pgMar w:top="0" w:right="0" w:bottom="0" w:left="0" w:header="0" w:footer="0" w:gutter="0"/>
                    </w:sectPr>
                    """.trimIndent() else ""}
                  </w:pPr>
                  <w:r>
                    <w:drawing>
                      <wp:inline distT="0" distB="0" distL="0" distR="0">
                        <wp:extent cx="${page.cxEmu}" cy="${page.cyEmu}"/>
                        <wp:effectExtent l="0" t="0" r="0" b="0"/>
                        <wp:docPr id="$pageNum" name="Page $pageNum"/>
                        <wp:cNvGraphicFramePr/>
                        <a:graphic xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">
                          <a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture">
                            <pic:pic xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture">
                              <pic:nvPr>
                                <pic:cNvPr id="$pageNum" name="exact_page_$pageNum.jpg"/>
                                <pic:cNvPicPr/>
                              </pic:nvPr>
                              <pic:blipFill>
                                <a:blip r:embed="$relId"/>
                                <a:stretch><a:fillRect/></a:stretch>
                              </pic:blipFill>
                              <pic:spPr>
                                <a:xfrm>
                                  <a:off x="0" y="0"/>
                                  <a:ext cx="${page.cxEmu}" cy="${page.cyEmu}"/>
                                </a:xfrm>
                                <a:prstGeom prst="rect"><a:avLst/></a:prstGeom>
                              </pic:spPr>
                            </pic:pic>
                          </a:graphicData>
                        </a:graphic>
                      </wp:inline>
                    </w:drawing>
                  </w:r>
                </w:p>
            """.trimIndent())
        }
    }

    val lastPage = pages.lastOrNull()
    val finalW = lastPage?.widthTwips ?: 12240
    val finalH = lastPage?.heightTwips ?: 15840

    return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main" 
            xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
            xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing"
            xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
            xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture">
  <w:body>
    $bodyXmlBuilder
    <w:sectPr>
      <w:pgSz w:w="$finalW" w:h="$finalH"/>
      <w:pgMar w:top="0" w:right="0" w:bottom="0" w:left="0" w:header="0" w:footer="0" w:gutter="0"/>
    </w:sectPr>
  </w:body>
</w:document>"""
}

fun validateDocxPackage(file: File) {
    val requiredEntries = setOf(
        "[Content_Types].xml",
        "_rels/.rels",
        "docProps/core.xml",
        "docProps/app.xml",
        "word/styles.xml",
        "word/settings.xml",
        "word/fontTable.xml",
        "word/_rels/document.xml.rels",
        "word/document.xml"
    )

    val foundEntries = mutableSetOf<String>()
    val factory = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
        isIgnoringComments = true
        isIgnoringElementContentWhitespace = true
    }
    val builder = factory.newDocumentBuilder()

    ZipFile(file).use { zip ->
        val entries = zip.entries()
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            foundEntries.add(entry.name)

            if (entry.name.endsWith(".xml", ignoreCase = true) || entry.name.endsWith(".rels", ignoreCase = true)) {
                zip.getInputStream(entry).use { stream ->
                    try {
                        builder.parse(stream)
                    } catch (e: Exception) {
                        throw IOException("XML parsing validation failed for entry '${entry.name}': ${e.message}", e)
                    }
                }
            }
        }
    }

    val missing = requiredEntries - foundEntries
    if (missing.isNotEmpty()) {
        throw IOException("Generated DOCX package is missing required entries: $missing")
    }
}

