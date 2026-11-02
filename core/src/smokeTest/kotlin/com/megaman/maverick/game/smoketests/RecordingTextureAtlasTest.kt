package com.megaman.maverick.game.smoketests

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.maverick.game.engine.drawables.sprites.containsRegion
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.mockk

class RecordingTextureAtlasTest : DescribeSpec({

    fun atlasWith(misses: MutableList<String>) =
        RecordingTextureAtlas("Test.txt", TextureAtlas.TextureAtlasData(), misses).apply {
            addRegion("present", mockk<Texture>(relaxed = true), 0, 0, 1, 1)
        }

    describe("findRegion") {
        it("records a miss with the atlas and the region") {
            val misses = mutableListOf<String>()
            atlasWith(misses).findRegion("absent").shouldBeNull()
            misses.shouldContainExactly("Test.txt:absent")
        }

        it("records a miss of the indexed form") {
            val misses = mutableListOf<String>()
            atlasWith(misses).findRegion("absent", 2).shouldBeNull()
            misses.shouldContainExactly("Test.txt:absent#2")
        }

        it("does not record a hit") {
            val misses = mutableListOf<String>()
            atlasWith(misses).findRegion("present").shouldNotBeNull()
            misses.shouldBeEmpty()
        }
    }

    describe("containsRegion") {
        it("gives the answer and does not record a miss") {
            val misses = mutableListOf<String>()
            val atlas = atlasWith(misses)
            atlas.containsRegion("absent") shouldBe false
            atlas.containsRegion("present") shouldBe true
            misses.shouldBeEmpty()
        }
    }
})
