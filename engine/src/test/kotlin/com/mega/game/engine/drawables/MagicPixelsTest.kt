package com.mega.game.engine.drawables

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.TextureData
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.objects.IntPair
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk

class MagicPixelsTest : DescribeSpec({

    describe("MagicPixels.get() method") {

        it("should return true and add magic color pixel to output map when magic color is found") {
            val frameMock = mockk<TextureRegion>()
            val textureDataMock = mockk<TextureData>()
            val pixmapMock = mockk<Pixmap>()

            every { frameMock.regionWidth } returns 2
            every { frameMock.regionHeight } returns 1
            every { frameMock.texture.textureData } returns textureDataMock

            every { textureDataMock.isPrepared } returns true
            every { textureDataMock.consumePixmap() } returns pixmapMock

            justRun { pixmapMock.dispose() }

            val magicColor = Color(1f, 0f, 0f, 1f)
            every { pixmapMock.getPixel(0, 0) } returns magicColor.toIntBits()
            every { pixmapMock.getPixel(1, 0) } returns 0

            val colors = objectSetOf(magicColor)
            val out = OrderedMap<IntPair, Color>()

            val result = MagicPixels.get(frameMock, colors, out)

            result shouldBe true
            out.size shouldBe 1
            out.containsKey(IntPair(0, 0)) shouldBe true
            out[IntPair(0, 0)] shouldBe magicColor
        }

        it("should return false and leave output map empty when no magic pixel is found") {
            val frameMock = mockk<TextureRegion>()
            val textureDataMock = mockk<TextureData>()
            val pixmapMock = mockk<Pixmap>()

            every { frameMock.regionWidth } returns 2
            every { frameMock.regionHeight } returns 1
            every { frameMock.texture.textureData } returns textureDataMock

            every { textureDataMock.isPrepared } returns true
            every { textureDataMock.consumePixmap() } returns pixmapMock

            justRun { pixmapMock.dispose() }
            every { pixmapMock.getPixel(0, 0) } returns 0
            every { pixmapMock.getPixel(1, 0) } returns 0

            val magicColor = Color(1f, 0f, 0f, 1f)
            val colors = objectSetOf(magicColor)
            val out = OrderedMap<IntPair, Color>()

            val result = MagicPixels.get(frameMock, colors, out)

            result shouldBe false
            out.isEmpty shouldBe true
        }

        it("should correctly handle multiple magic pixels and add them to the output map") {
            val frameMock = mockk<TextureRegion>()
            val textureDataMock = mockk<TextureData>()
            val pixmapMock = mockk<Pixmap>()

            every { frameMock.regionWidth } returns 2
            every { frameMock.regionHeight } returns 1
            every { frameMock.texture.textureData } returns textureDataMock

            every { textureDataMock.isPrepared } returns true
            every { textureDataMock.consumePixmap() } returns pixmapMock

            justRun { pixmapMock.dispose() }

            val magicColor = Color(1f, 0f, 0f, 1f)
            every { pixmapMock.getPixel(0, 0) } returns magicColor.toIntBits()
            every { pixmapMock.getPixel(1, 0) } returns magicColor.toIntBits()

            val colors = objectSetOf(magicColor)
            val out = OrderedMap<IntPair, Color>()

            val result = MagicPixels.get(frameMock, colors, out)

            result shouldBe true
            out.size shouldBe 2
            out.containsKey(IntPair(0, 0)) shouldBe true
            out.containsKey(IntPair(1, 0)) shouldBe true
        }
    }
})
