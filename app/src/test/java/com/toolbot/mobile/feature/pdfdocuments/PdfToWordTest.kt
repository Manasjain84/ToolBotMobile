package com.toolbot.mobile.feature.pdfdocuments

import com.toolbot.mobile.feature.pdfdocuments.word.ExactPageData
import com.toolbot.mobile.feature.pdfdocuments.word.PageContentData
import com.toolbot.mobile.feature.pdfdocuments.word.buildExactLayoutDocumentXml
import com.toolbot.mobile.feature.pdfdocuments.word.validateDocxPackage
import com.toolbot.mobile.feature.pdfdocuments.word.writeExactLayoutDocxDocument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import javax.xml.parsers.DocumentBuilderFactory

class PdfToWordTest {

    @Test
    fun generatedDocxXmlIsValid() {
        val pageDataList = listOf(
            PageContentData(
                paragraphs = listOf("Hello world & welcome to ToolBot PDF to Word generator <test>."),
                images = emptyList()
            )
        )

        val method = Class.forName("com.toolbot.mobile.feature.pdfdocuments.word.PdfToWordLogicKt")
            .getDeclaredMethod("buildDocumentXml", List::class.java, Boolean::class.java)
        method.isAccessible = true
        val xmlResult = method.invoke(null, pageDataList, false) as String

        assertFalse(xmlResult.isBlank())

        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            isIgnoringComments = true
            isIgnoringElementContentWhitespace = true
        }
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(xmlResult.byteInputStream(StandardCharsets.UTF_8))
        val paragraphs = doc.getElementsByTagNameNS("*", "p")
        assertTrue(paragraphs.length > 0)
        assertTrue(paragraphs.item(0).textContent.contains("Hello world"))
    }

    @Test
    fun docxPackageGenerationAndValidationPasses() {
        val tempFile = File.createTempFile("test_docx_", ".docx")
        try {
            val pageDataList = listOf(
                PageContentData(
                    paragraphs = listOf("Testing complete DOCX package generation and validation."),
                    images = emptyList()
                )
            )

            val writeMethod = Class.forName("com.toolbot.mobile.feature.pdfdocuments.word.PdfToWordLogicKt")
                .getDeclaredMethod("writeDocxDocument", java.io.OutputStream::class.java, List::class.java, Boolean::class.java)
            writeMethod.isAccessible = true

            FileOutputStream(tempFile).use { fos ->
                writeMethod.invoke(null, fos, pageDataList, false)
            }

            assertTrue(tempFile.exists())
            assertTrue(tempFile.length() > 0L)

            // Validate package entries and XML well-formedness
            validateDocxPackage(tempFile)
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun exactLayoutXmlIsWellFormedAndStructured() {
        val dummyImgFile = File.createTempFile("dummy_page_", ".jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        try {
            val exactPages = listOf(
                ExactPageData(
                    imageFile = dummyImgFile,
                    widthPt = 612f,
                    heightPt = 792f,
                    widthTwips = 12240,
                    heightTwips = 15840,
                    cxEmu = 7772400L,
                    cyEmu = 10058400L
                ),
                ExactPageData(
                    imageFile = dummyImgFile,
                    widthPt = 612f,
                    heightPt = 792f,
                    widthTwips = 12240,
                    heightTwips = 15840,
                    cxEmu = 7772400L,
                    cyEmu = 10058400L
                )
            )

            val xmlResult = buildExactLayoutDocumentXml(exactPages)
            assertFalse(xmlResult.isBlank())

            val factory = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = true
                isIgnoringComments = true
                isIgnoringElementContentWhitespace = true
            }
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(xmlResult.byteInputStream(StandardCharsets.UTF_8))

            val paragraphs = doc.getElementsByTagNameNS("*", "p")
            assertEquals(2, paragraphs.length)

            val drawings = doc.getElementsByTagNameNS("*", "drawing")
            assertEquals(2, drawings.length)
        } finally {
            dummyImgFile.delete()
        }
    }

    @Test
    fun exactLayoutDocxPackageGenerationAndValidationPasses() {
        val tempDocx = File.createTempFile("test_exact_docx_", ".docx")
        val dummyImg = File.createTempFile("dummy_exact_", ".jpg").apply { writeBytes(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())) }
        try {
            val exactPages = listOf(
                ExactPageData(
                    imageFile = dummyImg,
                    widthPt = 612f,
                    heightPt = 792f,
                    widthTwips = 12240,
                    heightTwips = 15840,
                    cxEmu = 7772400L,
                    cyEmu = 10058400L
                )
            )

            FileOutputStream(tempDocx).use { fos ->
                writeExactLayoutDocxDocument(fos, exactPages)
            }

            assertTrue(tempDocx.exists())
            assertTrue(tempDocx.length() > 0L)

            // Validate zip package and XML well-formedness
            validateDocxPackage(tempDocx)
        } finally {
            tempDocx.delete()
            dummyImg.delete()
        }
    }
}
