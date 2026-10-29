package com.megaman.maverick.game.entities

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.maverick.game.engine.GameEngine
import com.megaman.maverick.game.entities.enemies.Met
import com.megaman.maverick.game.testServices
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.every
import io.mockk.mockk

class MegaEntityFactoryTest : DescribeSpec({

    afterEach { MegaEntityFactory.reset() }

    describe("MegaEntityFactory") {
        it("builds each fetched entity with the holder that init got") {
            val services = testServices(engine = GameEngine())
            val atlas = mockk<TextureAtlas>()
            every { atlas.findRegion(any()) } returns AtlasRegion(mockk<Texture>(relaxed = true), 0, 0, 64, 64)
            every { services.assMan.get(any<String>(), TextureAtlas::class.java) } returns atlas
            MegaEntityFactory.reset()
            MegaEntityFactory.init(services)

            MegaEntityFactory.fetch(Met::class)!!.game shouldBeSameInstanceAs services
        }
    }
})
