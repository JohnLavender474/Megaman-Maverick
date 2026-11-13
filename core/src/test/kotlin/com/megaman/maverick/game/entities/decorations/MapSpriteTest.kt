package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.systems.GameSystem
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.TEXTURE_ASSET_PREFIX
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.testServices
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk

private const val ATLAS = "Enemies2.txt"
private const val REGION_A = "Wreck/wreck_a"
private const val REGION_B = "Wreck/wreck_b"

class MapSpriteTest : DescribeSpec({

    lateinit var engine: GameEngine
    lateinit var services: MegaServices
    lateinit var regionA: AtlasRegion
    lateinit var regionB: AtlasRegion

    fun spawnProps(region: String?, bounds: GameRectangle): Properties {
        val props = props(ConstKeys.ID pairTo 218, ConstKeys.BOUNDS pairTo bounds, ConstKeys.ATLAS pairTo ATLAS)
        if (region != null) props.put(ConstKeys.REGION, region)
        return props
    }

    fun spawn(region: String?, bounds: GameRectangle): MapSprite {
        val sprite = MegaEntityFactory.fetch(MapSprite::class)!!
        sprite.spawn(spawnProps(region, bounds)) shouldBe true
        engine.update(0f)
        return sprite
    }

    beforeEach {
        engine = GameEngine(Array<GameSystem>())
        services = testServices(engine = engine)
        val texture = mockk<Texture>(relaxed = true)
        every { texture.width } returns 1024
        every { texture.height } returns 1024
        regionA = AtlasRegion(texture, 758, 104, 48, 36)
        regionB = AtlasRegion(texture, 173, 2, 18, 14)
        val atlas = mockk<TextureAtlas>()
        every { atlas.findRegion(any()) } returns AtlasRegion(texture, 0, 0, 64, 64)
        every { atlas.findRegion("Wreck/missing") } returns null
        every { atlas.findRegion(REGION_A) } returns regionA
        every { atlas.findRegion(REGION_B) } returns regionB
        val source = TEXTURE_ASSET_PREFIX + ATLAS
        every { services.assets.isLoaded(source, TextureAtlas::class.java) } returns true
        every { services.assets.get(any<String>(), TextureAtlas::class.java) } returns atlas
        MegaEntityFactory.init(services)
    }

    afterEach {
        engine.reset()
        MegaEntityFactory.reset()
    }

    describe("MapSprite") {
        it("draws the map region in the object bounds, not flipped, at BACKGROUND 1") {
            val entity = spawn(REGION_A, GameRectangle(0f, 383f, 96f, 72f))
            val sprite = entity.defaultSprite
            sprite.texture shouldBe regionA.texture
            sprite.regionX shouldBe 758
            sprite.regionY shouldBe 104
            sprite.regionWidth shouldBe 48
            sprite.regionHeight shouldBe 36
            sprite.x shouldBe 0f
            sprite.y shouldBe 383f
            sprite.width shouldBe 96f
            sprite.height shouldBe 72f
            sprite.isFlipX shouldBe false
            sprite.isFlipY shouldBe false
            sprite.priority shouldBe DrawingPriority(DrawingSection.BACKGROUND, 1)
        }

        it("takes the new region and bounds on a second spawn of the pooled instance") {
            val first = spawn(REGION_A, GameRectangle(0f, 383f, 96f, 72f))
            first.destroy()
            engine.update(0f)
            val second = spawn(REGION_B, GameRectangle(1868f, 494f, 36f, 28f))
            second shouldBe first
            val sprite = second.defaultSprite
            sprite.regionX shouldBe 173
            sprite.regionY shouldBe 2
            sprite.regionWidth shouldBe 18
            sprite.regionHeight shouldBe 14
            sprite.x shouldBe 1868f
            sprite.y shouldBe 494f
            sprite.width shouldBe 36f
            sprite.height shouldBe 28f
            sprite.isFlipX shouldBe false
        }

        it("throws at spawn when the region is not in the atlas, and names the region and the object") {
            val error = shouldThrow<IllegalArgumentException> { spawn("Wreck/missing", GameRectangle(0f, 0f, 32f, 32f)) }
            error.message!! shouldContain "Wreck/missing"
            error.message!! shouldContain ATLAS
            error.message!! shouldContain "id=218"
        }

        it("throws at spawn when the map object has no region") {
            val error = shouldThrow<IllegalArgumentException> { spawn(null, GameRectangle(0f, 0f, 32f, 32f)) }
            error.message!! shouldContain ConstKeys.REGION
            error.message!! shouldContain "id=218"
        }

        it("throws at spawn when the map object has no atlas") {
            val props = props(ConstKeys.ID pairTo 218, ConstKeys.BOUNDS pairTo GameRectangle(0f, 0f, 32f, 32f))
            props.put(ConstKeys.REGION, REGION_A)
            val error = shouldThrow<IllegalArgumentException> {
                MegaEntityFactory.fetch(MapSprite::class)!!.spawn(props)
                engine.update(0f)
            }
            error.message!! shouldContain "has no ${ConstKeys.ATLAS} property"
            error.message!! shouldContain "id=218"
        }

        it("throws at spawn when the atlas is not loaded") {
            every { services.assets.isLoaded(TEXTURE_ASSET_PREFIX + ATLAS, TextureAtlas::class.java) } returns false
            val error = shouldThrow<IllegalArgumentException> { spawn(REGION_A, GameRectangle(0f, 0f, 32f, 32f)) }
            error.message!! shouldContain "is loaded"
            error.message!! shouldContain "id=218"
        }
    }
})
