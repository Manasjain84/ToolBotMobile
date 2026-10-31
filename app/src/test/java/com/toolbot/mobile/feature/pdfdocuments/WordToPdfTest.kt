package com.toolbot.mobile.feature.pdfdocuments

import android.content.ContentResolver
import android.content.Context
import android.content.res.AssetManager
import com.toolbot.mobile.feature.pdfdocuments.word.DocxContentItem
import com.toolbot.mobile.feature.pdfdocuments.word.createPdfFromDocxContent
import com.toolbot.mobile.feature.pdfdocuments.word.createPdfFromText
import com.toolbot.mobile.feature.pdfdocuments.word.extractDocxContent
import com.toolbot.mobile.feature.pdfdocuments.word.extractDocxText
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class WordToPdfTest {

    private lateinit var mockContext: Context

    @Before
    fun setUp() {
        val identityHCmapBytes = """
            /CIDInit /ProcSet findresource begin
            12 dict begin
            begincmap
            /CIDSystemInfo << /Registry (Adobe) /Ordering (Identity) /Supplement 0 >> def
            /CMapName /Identity-H def
            /CMapType 1 def
            1 begincodespacerange
            <0000> <FFFF>
            endcodespacerange
            1 beginnotdefrange
            <0000> <FFFF> 0
            endnotdefrange
            endcmap
            CMapName currentdict /CMap defineresource pop
            end
            end
        """.trimIndent().toByteArray(StandardCharsets.UTF_8)

        val mockAssetManager = mock(AssetManager::class.java)
        `when`(mockAssetManager.open(anyString())).thenAnswer { invocation ->
            val path = invocation.getArgument<String>(0)
            val candidateResourcePaths = listOf(
                path,
                "assets/$path",
                "com/tom_roush/fontbox/resources/cmap/${path.substringAfterLast('/')}",
                "com/tom_roush/pdfbox/resources/cmap/${path.substringAfterLast('/')}",
            )

            var stream: InputStream? = null
            for (resPath in candidateResourcePaths) {
                stream = WordToPdfTest::class.java.classLoader?.getResourceAsStream(resPath)
                    ?: Thread.currentThread().contextClassLoader?.getResourceAsStream(resPath)
                    ?: ClassLoader.getSystemResourceAsStream(resPath)
                if (stream != null) break
            }

            if (stream == null && path.contains("fonts/")) {
                stream = File("app/src/main/assets/$path").takeIf { it.exists() }?.inputStream()
                    ?: File("src/main/assets/$path").takeIf { it.exists() }?.inputStream()
                    ?: File("C:/Users/Manas/AndroidStudioProjects/ToolBotMobile/app/src/main/assets/$path").takeIf { it.exists() }?.inputStream()
            }

            if (stream == null && path.endsWith("Identity-H")) {
                stream = ByteArrayInputStream(identityHCmapBytes)
            }

            stream ?: throw IOException("Asset not found: $path")
        }

        mockContext = mock(Context::class.java)
        `when`(mockContext.applicationContext).thenReturn(mockContext)
        `when`(mockContext.assets).thenReturn(mockAssetManager)

        if (!PDFBoxResourceLoader.isReady()) {
            PDFBoxResourceLoader.init(mockContext)
        }
    }

    private fun createSampleJpegImageBytes(): ByteArray {
        return intArrayOf(
            255, 216, 255, 224, 0, 10, 74, 70, 73, 70, 0, 1, 1, 1, 0, 72, 0, 72, 0, 0,
            255, 219, 0, 67, 0, 13, 9, 10, 11, 10, 8, 13, 11, 10, 11, 14, 14, 13, 15, 19,
            32, 21, 19, 18, 18, 19, 39, 28, 30, 23, 32, 47, 41, 49, 49, 46, 41, 45, 44,
            52, 58, 74, 62, 52, 55, 70, 55, 44, 45, 64, 87, 65, 70, 76, 78, 83, 83, 83,
            50, 62, 91, 97, 90, 81, 96, 74, 81, 83, 80, 255, 219, 0, 67, 1, 14, 14, 14,
            19, 17, 19, 38, 21, 21, 38, 80, 54, 46, 54, 80, 80, 80, 80, 80, 80, 80, 80,
            80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80,
            80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80,
            80, 80, 80, 255, 192, 0, 17, 8, 0, 1, 0, 1, 3, 1, 34, 0, 2, 17, 1, 3, 17, 1,
            255, 196, 0, 21, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 7,
            255, 196, 0, 22, 10, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            255, 218, 0, 12, 3, 1, 0, 2, 17, 3, 17, 0, 63, 0, 149, 7, 255, 217
        ).map { it.toByte() }.toByteArray()
    }

    private fun createSamplePngImageBytes(): ByteArray {
        return intArrayOf(
            137, 80, 78, 71, 13, 10, 26, 10, 0, 0, 0, 13, 73, 72, 68, 82, 0, 0, 0, 1,
            0, 0, 0, 1, 8, 2, 0, 0, 0, 108, 141, 83, 137, 0, 0, 0, 12, 73, 68, 65, 84,
            8, 215, 99, 248, 255, 255, 63, 0, 5, 2, 1, 2, 38, 16, 108, 226, 0, 0, 0, 0, 73,
            69, 78, 68, 174, 66, 96, 130
        ).map { it.toByte() }.toByteArray()
    }

    private fun createMockDocxWithImages(
        documentXml: String,
        relsXml: String,
        imagesMap: Map<String, ByteArray>
    ): File {
        val tempFile = File.createTempFile("mock_word_img_", ".docx")
        ZipOutputStream(FileOutputStream(tempFile)).use { zip ->
            zip.putNextEntry(ZipEntry("word/document.xml"))
            zip.write(documentXml.toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()

            if (relsXml.isNotBlank()) {
                zip.putNextEntry(ZipEntry("word/_rels/document.xml.rels"))
                zip.write(relsXml.toByteArray(StandardCharsets.UTF_8))
                zip.closeEntry()
            }

            for ((path, bytes) in imagesMap) {
                zip.putNextEntry(ZipEntry(path))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return tempFile
    }

    private fun dummyContentResolver(): ContentResolver {
        return object : ContentResolver(null) {}
    }

    @Test
    fun testSimpleEnglishDocxExtractionAndPdfGeneration() {
        val xmlContent = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
              <w:body>
                <w:p><w:r><w:t>Hello ToolBot Word to PDF conversion test.</w:t></w:r></w:p>
              </w:body>
            </w:document>
        """.trimIndent()

        val docxFile = createMockDocxWithImages(xmlContent, "", emptyMap())
        val pdfFile = File.createTempFile("test_out_", ".pdf")

        try {
            val resolver = dummyContentResolver()
            val extractedTextResult = extractDocxText(null, resolver, docxFile.inputStream())
            assertTrue(extractedTextResult.isSuccess)

            val extractedText = extractedTextResult.getOrThrow()
            assertTrue(extractedText.contains("Hello ToolBot Word to PDF conversion test."))

            createPdfFromText(mockContext, extractedText, pdfFile)

            assertTrue(pdfFile.exists())
            assertTrue(pdfFile.length() > 0L)

            val header = pdfFile.readBytes().copyOfRange(0, 5).toString(StandardCharsets.UTF_8)
            assertEquals("%PDF-", header)

            PDDocument.load(pdfFile.inputStream()).use { doc ->
                assertTrue(doc.numberOfPages > 0)
            }
        } finally {
            docxFile.delete()
            pdfFile.delete()
        }
    }

    @Test
    fun testDocxRelationshipExtractionAndNormalisation() {
        val documentXml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
                        xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
                        xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">
              <w:body>
                <w:p><w:r><w:t>Header Text</w:t></w:r></w:p>
                <w:p>
                  <w:r>
                    <w:drawing>
                      <wp:inline xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing">
                        <a:graphic>
                          <a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture">
                            <pic:pic xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture">
                              <pic:blipFill><a:blip r:embed="rIdPng1"/></pic:blipFill>
                            </pic:pic>
                          </a:graphicData>
                        </a:graphic>
                      </wp:inline>
                    </w:drawing>
                  </w:r>
                </w:p>
                <w:p><w:r><w:t>Footer Text</w:t></w:r></w:p>
              </w:body>
            </w:document>
        """.trimIndent()

        val relsXml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
              <Relationship Id="rIdPng1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="../media/sample.png"/>
            </Relationships>
        """.trimIndent()

        val pngBytes = createSamplePngImageBytes()
        val docxFile = createMockDocxWithImages(documentXml, relsXml, mapOf("word/media/sample.png" to pngBytes))

        try {
            val resolver = dummyContentResolver()
            val contentResult = extractDocxContent(null, resolver, docxFile.inputStream())
            assertTrue(contentResult.isSuccess)

            val content = contentResult.getOrThrow()
            assertEquals(3, content.items.size)
            assertTrue(content.items[0] is DocxContentItem.Paragraph)
            assertEquals("Header Text", (content.items[0] as DocxContentItem.Paragraph).text)
            assertTrue(content.items[1] is DocxContentItem.Image)
            assertEquals("word/media/sample.png", (content.items[1] as DocxContentItem.Image).name)
            assertTrue(content.items[2] is DocxContentItem.Paragraph)
            assertEquals("Footer Text", (content.items[2] as DocxContentItem.Paragraph).text)
        } finally {
            docxFile.delete()
        }
    }

    @Test
    fun testDocxWithJpegImageExtractionAndPdfEmbedding() {
        val documentXml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
                        xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
                        xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">
              <w:body>
                <w:p><w:r><w:t>Document with JPEG Image.</w:t></w:r></w:p>
                <w:p>
                  <w:r>
                    <w:drawing>
                      <wp:inline xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing">
                        <a:graphic>
                          <a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture">
                            <pic:pic xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture">
                              <pic:blipFill>
                                <a:blip r:embed="rIdJpeg1"/>
                              </pic:blipFill>
                            </pic:pic>
                          </a:graphicData>
                        </a:graphic>
                      </wp:inline>
                    </w:drawing>
                  </w:r>
                </w:p>
              </w:body>
            </w:document>
        """.trimIndent()

        val relsXml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
              <Relationship Id="rIdJpeg1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="media/image1.jpg"/>
            </Relationships>
        """.trimIndent()

        val jpegBytes = createSampleJpegImageBytes()
        val docxFile = createMockDocxWithImages(documentXml, relsXml, mapOf("word/media/image1.jpg" to jpegBytes))
        val pdfFile = File.createTempFile("test_jpeg_pdf_", ".pdf")

        try {
            val resolver = dummyContentResolver()
            val contentResult = extractDocxContent(null, resolver, docxFile.inputStream())
            assertTrue(contentResult.isSuccess)

            val content = contentResult.getOrThrow()
            assertTrue(content.items.any { it is DocxContentItem.Image })

            createPdfFromDocxContent(mockContext, content, pdfFile)

            assertTrue(pdfFile.exists())
            assertTrue(pdfFile.length() > 0L)

            PDDocument.load(pdfFile.inputStream()).use { doc ->
                val page = doc.getPage(0)
                val resources = page.resources
                val hasJpegImage = resources.xObjectNames.any { name ->
                    resources.getXObject(name) is PDImageXObject
                }
                assertTrue("Generated PDF must contain an embedded PDImageXObject", hasJpegImage)
            }
        } finally {
            docxFile.delete()
            pdfFile.delete()
        }
    }

    @Test
    fun testMissingRelationshipOrCorruptImageDataHandling() {
        val documentXml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
                        xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
                        xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">
              <w:body>
                <w:p><w:r><w:t>Document with broken image relationship.</w:t></w:r></w:p>
                <w:p>
                  <w:r>
                    <w:drawing>
                      <wp:inline xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing">
                        <a:graphic>
                          <a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture">
                            <pic:pic xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture">
                              <pic:blipFill><a:blip r:embed="rIdMissing"/></pic:blipFill>
                            </pic:pic>
                          </a:graphicData>
                        </a:graphic>
                      </wp:inline>
                    </w:drawing>
                  </w:r>
                </w:p>
                <w:p><w:r><w:t>Document continues successfully.</w:t></w:r></w:p>
              </w:body>
            </w:document>
        """.trimIndent()

        val docxFile = createMockDocxWithImages(documentXml, "", emptyMap())
        val pdfFile = File.createTempFile("test_broken_img_", ".pdf")

        try {
            val resolver = dummyContentResolver()
            val contentResult = extractDocxContent(null, resolver, docxFile.inputStream())
            assertTrue(contentResult.isSuccess)

            val content = contentResult.getOrThrow()
            assertTrue(content.hasAnyContent())

            createPdfFromDocxContent(mockContext, content, pdfFile)

            assertTrue(pdfFile.exists())
            assertTrue(pdfFile.length() > 0L)

            PDDocument.load(pdfFile.inputStream()).use { doc ->
                assertTrue(doc.numberOfPages > 0)
            }
        } finally {
            docxFile.delete()
            pdfFile.delete()
        }
    }

    @Test
    fun testDocxWithSmartQuotesBulletsAndSpecialCharacters() {
        val xmlContent = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
              <w:body>
                <w:p><w:r><w:t>“Smart Quotes” and ‘Single Quotes’ test.</w:t></w:r></w:p>
                <w:p><w:r><w:t>• Bullet point list item 1</w:t></w:r></w:p>
                <w:p><w:r><w:t>Em—dash and En–dash test.</w:t></w:r></w:p>
                <w:p><w:r><w:t>Café, résumé &amp; naïve accented text.</w:t></w:r></w:p>
              </w:body>
            </w:document>
        """.trimIndent()

        val docxFile = createMockDocxWithImages(xmlContent, "", emptyMap())
        val pdfFile = File.createTempFile("test_special_chars_", ".pdf")

        try {
            val resolver = dummyContentResolver()
            val textResult = extractDocxText(null, resolver, docxFile.inputStream())
            assertTrue(textResult.isSuccess)

            val text = textResult.getOrThrow()
            assertTrue(text.contains("Smart Quotes"))

            createPdfFromText(mockContext, text, pdfFile)

            assertTrue(pdfFile.exists())
            assertTrue(pdfFile.length() > 0L)

            PDDocument.load(pdfFile.inputStream()).use { doc ->
                assertTrue(doc.numberOfPages > 0)
            }
        } finally {
            docxFile.delete()
            pdfFile.delete()
        }
    }

    @Test
    fun testDocxWithHindiAndUnicodeText() {
        val xmlContent = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
              <w:body>
                <w:p><w:r><w:t>नमस्ते दुनिया - Hindi Text &amp; Unicode Test</w:t></w:r></w:p>
              </w:body>
            </w:document>
        """.trimIndent()

        val docxFile = createMockDocxWithImages(xmlContent, "", emptyMap())
        val pdfFile = File.createTempFile("test_hindi_", ".pdf")

        try {
            val resolver = dummyContentResolver()
            val textResult = extractDocxText(null, resolver, docxFile.inputStream())
            assertTrue(textResult.isSuccess)

            val text = textResult.getOrThrow()
            assertTrue(text.contains("नमस्ते"))

            createPdfFromText(mockContext, text, pdfFile)

            assertTrue(pdfFile.exists())
            assertTrue(pdfFile.length() > 0L)

            PDDocument.load(pdfFile.inputStream()).use { doc ->
                assertTrue(doc.numberOfPages > 0)
            }
        } finally {
            docxFile.delete()
            pdfFile.delete()
        }
    }

    @Test
    fun testEmptyOrInvalidDocxHandling() {
        val invalidFile = File.createTempFile("invalid_", ".docx").apply {
            writeText("Not a zip package")
        }

        try {
            val resolver = dummyContentResolver()
            val textResult = extractDocxText(null, resolver, invalidFile.inputStream())

            assertTrue(textResult.isFailure)
            val exception = textResult.exceptionOrNull()
            assertFalse(exception?.message.isNullOrEmpty())
        } finally {
            invalidFile.delete()
        }
    }
}
