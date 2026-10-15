package com.example.oneread

import com.example.oneread.ppt.model.PresentationModel
import com.example.oneread.ppt.model.ShapeElement
import com.example.oneread.ppt.model.ShapeType
import com.example.oneread.ppt.model.TextElement
import com.example.oneread.ppt.parser.PptxParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.min

class PptParserAndRenderingTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun createSamplePptx(
        presentationXmlContent: String,
        slideFiles: Map<String, String>,
        relsMap: Map<String, String> = mapOf("rId1" to "slides/slide1.xml")
    ): File {
        val file = tempFolder.newFile("sample_presentation_${System.currentTimeMillis()}.pptx")
        ZipOutputStream(FileOutputStream(file)).use { zos ->
            // [Content_Types].xml
            zos.putNextEntry(ZipEntry("[Content_Types].xml"))
            zos.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                    <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                    <Default Extension="xml" ContentType="application/xml"/>
                    <Override PartName="/ppt/presentation.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml"/>
                </Types>""".toByteArray())
            zos.closeEntry()

            // _rels/.rels
            zos.putNextEntry(ZipEntry("_rels/.rels"))
            zos.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="ppt/presentation.xml"/>
                </Relationships>""".toByteArray())
            zos.closeEntry()

            // ppt/_rels/presentation.xml.rels
            val relsXml = buildString {
                append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
                append("""<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
                for ((id, target) in relsMap) {
                    append("""<Relationship Id="$id" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide" Target="$target"/>""")
                }
                append("""</Relationships>""")
            }
            zos.putNextEntry(ZipEntry("ppt/_rels/presentation.xml.rels"))
            zos.write(relsXml.toByteArray())
            zos.closeEntry()

            // ppt/presentation.xml
            zos.putNextEntry(ZipEntry("ppt/presentation.xml"))
            zos.write(presentationXmlContent.toByteArray())
            zos.closeEntry()

            // ppt/slides/...
            for ((path, content) in slideFiles) {
                zos.putNextEntry(ZipEntry("ppt/$path"))
                zos.write(content.toByteArray())
                zos.closeEntry()
            }
        }
        return file
    }

    @Test
    fun testPptxParserExtractsSlideDimensionsAndShapes() {
        val presXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <p:presentation xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main"
                            xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">
                <p:sldSz cx="12192000" cy="6858000" type="screen16x9"/>
                <p:sldIdLst>
                    <p:sldId id="256" r:id="rId1" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"/>
                </p:sldIdLst>
            </p:presentation>"""

        val slideXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <p:sld xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main"
                   xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">
                <p:cSld>
                    <p:spTree>
                        <p:nvGrpSpPr>
                            <p:cNvPr id="1" name=""/>
                            <p:cNvGrpSpPr/>
                            <p:nvPr/>
                        </p:nvGrpSpPr>
                        <p:grpSpPr/>
                        
                        <!-- Title Shape -->
                        <p:sp>
                            <p:nvSpPr>
                                <p:cNvPr id="2" name="Title 1"/>
                                <p:cNvSpPr/>
                                <p:nvPr>
                                    <p:ph type="title"/>
                                </p:nvPr>
                            </p:nvSpPr>
                            <p:spPr>
                                <a:xfrm>
                                    <a:off x="1000000" y="500000"/>
                                    <a:ext cx="10000000" cy="1200000"/>
                                </a:xfrm>
                                <a:prstGeom prst="rect"><a:avLst/></a:prstGeom>
                            </p:spPr>
                            <p:txBody>
                                <a:bodyPr/>
                                <a:lstStyle/>
                                <a:p>
                                    <a:r>
                                        <a:rPr b="1" sz="4000"/>
                                        <a:t>Run-Time Environments</a:t>
                                    </a:r>
                                </a:p>
                            </p:txBody>
                        </p:sp>

                        <!-- Bracket Shape -->
                        <p:sp>
                            <p:nvSpPr>
                                <p:cNvPr id="3" name="Left Bracket Shape"/>
                                <p:cNvSpPr/>
                                <p:nvPr/>
                            </p:nvSpPr>
                            <p:spPr>
                                <a:xfrm>
                                    <a:off x="1500000" y="2000000"/>
                                    <a:ext cx="300000" cy="3000000"/>
                                </a:xfrm>
                                <a:prstGeom prst="leftBrace"><a:avLst/></a:prstGeom>
                            </p:spPr>
                        </p:sp>

                        <!-- Connector Line -->
                        <p:cxnSp>
                            <p:nvCxnSpPr>
                                <p:cNvPr id="4" name="Connector Line"/>
                                <p:cNvCxnSpPr/>
                                <p:nvPr/>
                            </p:nvCxnSpPr>
                            <p:spPr>
                                <a:xfrm>
                                    <a:off x="2000000" y="2500000"/>
                                    <a:ext cx="4000000" cy="0"/>
                                </a:xfrm>
                                <a:prstGeom prst="line"><a:avLst/></a:prstGeom>
                            </p:spPr>
                        </p:cxnSp>
                    </p:spTree>
                </p:cSld>
            </p:sld>"""

        val pptxFile = createSamplePptx(presXml, mapOf("slides/slide1.xml" to slideXml))
        val presentation = PptxParser.parse(pptxFile)

        assertNotNull(presentation)
        assertEquals(1, presentation.slides.size)
        assertTrue(presentation.aspectRatio > 1.7f)
        assertEquals(12192000L, presentation.slideWidthEmus)
        assertEquals(6858000L, presentation.slideHeightEmus)

        val slide = presentation.slides[0]
        assertEquals("Run-Time Environments", slide.title)

        val textElements = slide.elements.filterIsInstance<TextElement>()
        assertTrue(textElements.isNotEmpty())
        val allSlideText = textElements.flatMap { it.paragraphs }.flatMap { it.runs }.joinToString { it.text }
        assertTrue(allSlideText.contains("Run-Time Environments"))

        val shapeElements = slide.elements.filterIsInstance<ShapeElement>()
        assertTrue("Expected bracket or connector shape", shapeElements.isNotEmpty())

        val leftBraceShape = shapeElements.find { it.shapeType == ShapeType.LEFT_BRACE }
        assertNotNull("Should parse leftBrace shape geometry", leftBraceShape)
    }

    @Test
    fun testBracketsAndDiamondGeometryTypes() {
        val presXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <p:presentation xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
                <p:sldSz cx="9144000" cy="6858000"/>
                <p:sldIdLst>
                    <p:sldId id="10" r:id="rId1" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"/>
                </p:sldIdLst>
            </p:presentation>"""

        val slideXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <p:sld xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main"
                   xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">
                <p:cSld>
                    <p:spTree>
                        <p:sp>
                            <p:spPr>
                                <a:xfrm><a:off x="0" y="0"/><a:ext cx="100" cy="100"/></a:xfrm>
                                <a:prstGeom prst="rightBrace"><a:avLst/></a:prstGeom>
                            </p:spPr>
                        </p:sp>
                        <p:sp>
                            <p:spPr>
                                <a:xfrm><a:off x="0" y="0"/><a:ext cx="100" cy="100"/></a:xfrm>
                                <a:prstGeom prst="leftBracket"><a:avLst/></a:prstGeom>
                            </p:spPr>
                        </p:sp>
                        <p:sp>
                            <p:spPr>
                                <a:xfrm><a:off x="0" y="0"/><a:ext cx="100" cy="100"/></a:xfrm>
                                <a:prstGeom prst="rightBracket"><a:avLst/></a:prstGeom>
                            </p:spPr>
                        </p:sp>
                        <p:sp>
                            <p:spPr>
                                <a:xfrm><a:off x="0" y="0"/><a:ext cx="100" cy="100"/></a:xfrm>
                                <a:prstGeom prst="diamond"><a:avLst/></a:prstGeom>
                            </p:spPr>
                        </p:sp>
                    </p:spTree>
                </p:cSld>
            </p:sld>"""

        val pptxFile = createSamplePptx(presXml, mapOf("slides/slide1.xml" to slideXml))
        val pres = PptxParser.parse(pptxFile)
        val shapes = pres.slides[0].elements.filterIsInstance<ShapeElement>()
        assertEquals(4, shapes.size)
        assertTrue(shapes.any { it.shapeType == ShapeType.RIGHT_BRACE })
        assertTrue(shapes.any { it.shapeType == ShapeType.LEFT_BRACKET })
        assertTrue(shapes.any { it.shapeType == ShapeType.RIGHT_BRACKET })
        assertTrue(shapes.any { it.shapeType == ShapeType.DIAMOND })
    }

    @Test
    fun testSlideOrderingPreservedFromRelationships() {
        val presXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <p:presentation xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
                <p:sldSz cx="12192000" cy="6858000"/>
                <p:sldIdLst>
                    <p:sldId id="2" r:id="rId2" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"/>
                    <p:sldId id="1" r:id="rId1" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"/>
                </p:sldIdLst>
            </p:presentation>"""

        val slide1Xml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <p:sld xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main"
                   xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">
                <p:cSld><p:spTree><p:sp><p:txBody><a:p><a:r><a:t>First In Order</a:t></a:r></a:p></p:txBody></p:sp></p:spTree></p:cSld>
            </p:sld>"""

        val slide2Xml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <p:sld xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main"
                   xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">
                <p:cSld><p:spTree><p:sp><p:txBody><a:p><a:r><a:t>Second In Order</a:t></a:r></a:p></p:txBody></p:sp></p:spTree></p:cSld>
            </p:sld>"""

        val pptxFile = createSamplePptx(
            presXml,
            mapOf("slides/slideA.xml" to slide1Xml, "slides/slideB.xml" to slide2Xml),
            mapOf("rId2" to "slides/slideA.xml", "rId1" to "slides/slideB.xml")
        )
        val pres = PptxParser.parse(pptxFile)
        assertEquals(2, pres.slides.size)
        // rId2 was first in sldIdLst
        val firstSlideTexts = pres.slides[0].elements.filterIsInstance<TextElement>()
            .flatMap { it.paragraphs }.flatMap { it.runs }.map { it.text }
        assertTrue(firstSlideTexts.contains("First In Order"))
    }

    @Test
    fun testUniformFitScalingFormula() {
        // Test formula: scale = min(availableWidth / slideWidth, availableHeight / slideHeight)
        val slideW = 16f
        val slideH = 9f

        // Case 1: Portrait screen 1080 x 1920 (width constrained)
        val screenW1 = 1080f
        val screenH1 = 1920f
        val scale1 = min(screenW1 / slideW, screenH1 / slideH)
        val renderedW1 = slideW * scale1
        val renderedH1 = slideH * scale1
        assertEquals(1080f, renderedW1, 0.01f)
        assertEquals(607.5f, renderedH1, 0.01f)
        assertTrue(renderedH1 <= screenH1) // Never exceeds vertical bounds!

        // Case 2: Landscape screen 1920 x 1080 (height constrained)
        val screenW2 = 2400f
        val screenH2 = 1080f
        val scale2 = min(screenW2 / slideW, screenH2 / slideH)
        val renderedW2 = slideW * scale2
        val renderedH2 = slideH * scale2
        assertEquals(1080f, renderedH2, 0.01f)
        assertEquals(1920f, renderedW2, 0.01f)
        assertTrue(renderedW2 <= screenW2) // Never exceeds horizontal bounds!
    }

    @Test
    fun testMissingOptionalElementsHandledGracefully() {
        val presXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <p:presentation xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
            </p:presentation>"""

        val slideXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <p:sld xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
                <p:cSld/>
            </p:sld>"""

        val pptxFile = createSamplePptx(presXml, mapOf("slides/slide1.xml" to slideXml))
        val pres = PptxParser.parse(pptxFile)
        assertNotNull(pres)
        // Defaults to 16:9 aspect ratio and 12192000 x 6858000 EMUs
        assertEquals(16f / 9f, pres.aspectRatio, 0.01f)
        assertEquals(1, pres.slides.size)
        assertEquals(0, pres.slides[0].elements.size)
    }
    @Test
    fun textRunsInheritPowerPointParagraphDefaultFontSize() {
        val presXml = """<?xml version="1.0" encoding="UTF-8"?>
            <p:presentation xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main"
                            xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
              <p:sldSz cx="12192000" cy="6858000"/>
              <p:sldIdLst><p:sldId id="256" r:id="rId1"/></p:sldIdLst>
            </p:presentation>"""
        val slideXml = """<?xml version="1.0" encoding="UTF-8"?>
            <p:sld xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main"
                   xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">
              <p:cSld><p:spTree>
                <p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr>
                <p:grpSpPr/>
                <p:sp>
                  <p:nvSpPr><p:cNvPr id="2" name="Title 1"/><p:cNvSpPr/><p:nvPr/></p:nvSpPr>
                  <p:spPr>
                    <a:xfrm><a:off x="1000000" y="500000"/><a:ext cx="10000000" cy="1200000"/></a:xfrm>
                    <a:prstGeom prst="rect"><a:avLst/></a:prstGeom>
                  </p:spPr>
                  <p:txBody>
                    <a:bodyPr/><a:lstStyle/>
                    <a:p>
                      <a:pPr><a:defRPr sz="3600"><a:solidFill><a:srgbClr val="112233"/></a:solidFill></a:defRPr></a:pPr>
                      <a:r><a:t>Large heading</a:t></a:r>
                      <a:endParaRPr sz="3600"/>
                    </a:p>
                  </p:txBody>
                </p:sp>
              </p:spTree></p:cSld>
            </p:sld>"""

        val file = createSamplePptx(presXml, mapOf("slides/slide1.xml" to slideXml))
        val presentation = PptxParser.parse(file)
        val runs = presentation.slides.single().elements
            .filterIsInstance<TextElement>()
            .flatMap { it.paragraphs }
            .flatMap { it.runs }

        assertTrue(runs.any { it.text == "Large heading" })
        assertEquals(36f, runs.first { it.text == "Large heading" }.fontSizePt, 0.1f)
    }

}
