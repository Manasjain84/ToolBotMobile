package com.toolbot.mobile.feature.pdfdocuments

import com.toolbot.mobile.feature.pdfdocuments.word.ExactPageData
import com.toolbot.mobile.feature.pdfdocuments.word.buildExactLayoutDocumentXml
import com.toolbot.mobile.feature.pdfdocuments.word.validateDocxPackage
import com.toolbot.mobile.feature.pdfdocuments.word.writeExactLayoutDocxDocument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory

class PdfToWordTest {

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

    @Test
    fun exactLayoutMultiPageDocxPackageContainsAllEmbeddedImages() {
        val tempDocx = File.createTempFile("test_multipage_exact_docx_", ".docx")
        val dummyImg1 = File.createTempFile("dummy_p1_", ".jpg").apply { writeBytes(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0x01.toByte())) }
        val dummyImg2 = File.createTempFile("dummy_p2_", ".jpg").apply { writeBytes(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0x02.toByte())) }
        val dummyImg3 = File.createTempFile("dummy_p3_", ".jpg").apply { writeBytes(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0x03.toByte())) }
        try {
            val exactPages = listOf(
                ExactPageData(
                    imageFile = dummyImg1,
                    widthPt = 612f,
                    heightPt = 792f,
                    widthTwips = 12240,
                    heightTwips = 15840,
                    cxEmu = 7772400L,
                    cyEmu = 10058400L
                ),
                ExactPageData(
                    imageFile = dummyImg2,
                    widthPt = 612f,
                    heightPt = 792f,
                    widthTwips = 12240,
                    heightTwips = 15840,
                    cxEmu = 7772400L,
                    cyEmu = 10058400L
                ),
                ExactPageData(
                    imageFile = dummyImg3,
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

            // Validate zip package and XML well-formedness for multi-page document
            validateDocxPackage(tempDocx)

            // Verify embedded page image entries in zip file
            ZipFile(tempDocx).use { zip ->
                assertNotNull(zip.getEntry("word/media/exact_page_1.jpg"))
                assertNotNull(zip.getEntry("word/media/exact_page_2.jpg"))
                assertNotNull(zip.getEntry("word/media/exact_page_3.jpg"))

                // Verify relationships file references all page images
                val relsEntry = zip.getEntry("word/_rels/document.xml.rels")
                assertNotNull(relsEntry)
                val relsXml = zip.getInputStream(relsEntry).bufferedReader().readText()
                assertTrue(relsXml.contains("media/exact_page_1.jpg"))
                assertTrue(relsXml.contains("media/exact_page_2.jpg"))
                assertTrue(relsXml.contains("media/exact_page_3.jpg"))
            }
        } finally {
            tempDocx.delete()
            dummyImg1.delete()
            dummyImg2.delete()
            dummyImg3.delete()
        }
    }

    @Test
    fun verifyOnlyExactLayoutFunctionalityIsExposed() {
        // Reflection check to ensure convertPdfToWord only takes inputUri, outputUri, context
        val convertMethod = Class.forName("com.toolbot.mobile.feature.pdfdocuments.word.PdfToWordLogicKt")
            .declaredMethods.firstOrNull { it.name == "convertPdfToWord" }

        assertNotNull(convertMethod)
        assertEquals(3, convertMethod!!.parameterCount)
        assertEquals(android.net.Uri::class.java, convertMethod.parameterTypes[0])
        assertEquals(android.net.Uri::class.java, convertMethod.parameterTypes[1])
        assertEquals(android.content.Context::class.java, convertMethod.parameterTypes[2])
    }
}

