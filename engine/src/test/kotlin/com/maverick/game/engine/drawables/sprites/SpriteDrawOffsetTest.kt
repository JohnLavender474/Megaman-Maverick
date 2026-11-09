package com.maverick.game.engine.drawables.sprites

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.OrderedMap
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.MockGameEntity
import com.maverick.game.engine.world.IDrawOffsets
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk

class SpriteDrawOffsetTest : DescribeSpec({

    lateinit var drawn: FloatArray
    lateinit var batch: Batch
    lateinit var sprite: GameSprite

    beforeEach {
        drawn = FloatArray(20)
        batch = mockk {
            every { draw(any<Texture>(), any<FloatArray>(), any(), any()) } answers {
                secondArg<FloatArray>().copyInto(drawn)
            }
        }
        sprite = spyk(GameSprite())
        every { sprite.texture } returns mockk {
            every { width } returns 1
            every { height } returns 1
        }
        sprite.setBounds(10f, 20f, 4f, 4f)
    }

    describe("GameSprite draw offset") {
        it("draws the sprite moved by the offset") {
            sprite.drawOffsetX = 1.5f
            sprite.drawOffsetY = -2f
            sprite.draw(batch)
            drawn[0] shouldBe 11.5f
            drawn[1] shouldBe 18f
            drawn[10] shouldBe 15.5f
            drawn[11] shouldBe 22f
            sprite.x shouldBe 10f
            sprite.y shouldBe 20f
        }

        it("draws the sprite at its position when the offset is 0") {
            sprite.draw(batch)
            drawn[0] shouldBe 10f
            drawn[1] shouldBe 20f
        }

        it("does not move a sprite that is placed once, over 100 frames") {
            val before = sprite.vertices.copyOf()
            repeat(100) {
                sprite.drawOffsetX = 0.1f * (it % 7) - 0.3f
                sprite.drawOffsetY = 0.037f * it
                sprite.draw(batch)
            }
            sprite.vertices.contentEquals(before) shouldBe true
        }
    }

    describe("SpritesSystem draw offset") {
        fun collect(drawOffsets: IDrawOffsets?, withBody: Boolean): GameSprite {
            val gameSprite = GameSprite().apply {
                drawOffsetX = 9f
                drawOffsetY = 9f
            }
            val entity = MockGameEntity()
            entity.addComponent(SpritesComponent(OrderedMap<Any, GameSprite>().apply { put("s", gameSprite) }))
            if (withBody) entity.addComponent(BodyComponent(Body(BodyType.DYNAMIC)))
            val system = SpritesSystem({}, drawOffsets = drawOffsets).apply { on = true }
            GameEngine().apply {
                systems.add(system)
                spawn(entity)
                update(1f)
            }
            return gameSprite
        }

        val offsets = object : IDrawOffsets {
            override fun getDrawOffset(body: com.maverick.game.engine.world.body.IBody, out: Vector2) = out.set(1f, 2f)
        }

        it("gives each sprite the offset of the entity's body") {
            val gameSprite = collect(offsets, withBody = true)
            gameSprite.drawOffsetX shouldBe 1f
            gameSprite.drawOffsetY shouldBe 2f
        }

        it("gives 0 to an entity without a body") {
            val gameSprite = collect(offsets, withBody = false)
            gameSprite.drawOffsetX shouldBe 0f
            gameSprite.drawOffsetY shouldBe 0f
        }

        it("gives 0 with no draw offsets") {
            val gameSprite = collect(null, withBody = true)
            gameSprite.drawOffsetX shouldBe 0f
            gameSprite.drawOffsetY shouldBe 0f
        }
    }
})
