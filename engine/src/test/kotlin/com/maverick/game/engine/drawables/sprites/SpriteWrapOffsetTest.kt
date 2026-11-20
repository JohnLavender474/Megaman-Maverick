package com.maverick.game.engine.drawables.sprites

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.OrderedMap
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.MockGameEntity
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.world.IDrawOffsets
import com.maverick.game.engine.world.WorldWrap
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.IBody
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify

class SpriteWrapOffsetTest : DescribeSpec({

    describe("GameSprite far half") {
        lateinit var draws: ArrayList<FloatArray>
        lateinit var batch: Batch
        lateinit var sprite: GameSprite

        beforeEach {
            draws = ArrayList()
            batch = mockk {
                every { draw(any<Texture>(), any<FloatArray>(), any(), any()) } answers {
                    draws.add(secondArg<FloatArray>().copyOf())
                }
            }
            sprite = spyk(GameSprite())
            every { sprite.texture } returns mockk {
                every { width } returns 1
                every { height } returns 1
            }
            sprite.setBounds(10f, 20f, 4f, 4f)
        }

        it("draws once when the field is 0") {
            sprite.draw(batch)
            verify(exactly = 1) { batch.draw(any<Texture>(), any<FloatArray>(), any(), any()) }
        }

        it("draws a second time at the far-half offset on top of the draw offset") {
            sprite.drawOffsetX = 1f
            sprite.drawOffsetY = -2f
            sprite.wrapOffsetX = 100f
            sprite.draw(batch)
            draws.size shouldBe 2
            draws[0][0] shouldBe 11f
            draws[0][1] shouldBe 18f
            draws[1][0] shouldBe 111f
            draws[1][1] shouldBe 18f
            draws[1][10] shouldBe 115f
            sprite.x shouldBe 10f
        }

        it("keeps the vertex colors in the second draw") {
            sprite.setColor(1f, 0f, 0f, 0.5f)
            sprite.wrapOffsetX = -100f
            sprite.draw(batch)
            draws.size shouldBe 2
            draws[1][2] shouldBe draws[0][2]
            draws[1][2] shouldBe sprite.vertices[2]
        }

        it("draws nothing when hidden") {
            sprite.wrapOffsetX = 100f
            sprite.hidden = true
            sprite.draw(batch)
            verify(exactly = 0) { batch.draw(any<Texture>(), any<FloatArray>(), any(), any()) }
        }
    }

    describe("SpritesSystem far half") {
        val region = GameRectangle(100f, 0f, 200f, 200f)
        var regionOn = true
        val accepted = HashSet<IGameEntity>()
        var cap = WorldWrap.UNLIMITED_WRAPS
        var regionCalls = 0
        var wrapsCalls = 0

        val wrap = WorldWrap(
            region = { out ->
                regionCalls++
                if (regionOn) out.set(region) else null
            },
            wraps = { entity ->
                wrapsCalls++
                accepted.contains(entity)
            },
            maxWraps = { _ -> cap }
        )

        beforeEach {
            regionOn = true
            accepted.clear()
            cap = WorldWrap.UNLIMITED_WRAPS
            regionCalls = 0
            wrapsCalls = 0
        }

        fun entityWith(vararg sprites: GameSprite, body: Body? = null): MockGameEntity {
            val map = OrderedMap<Any, GameSprite>()
            sprites.forEachIndexed { i, sprite -> map.put(i, sprite) }
            val entity = MockGameEntity()
            entity.addComponent(SpritesComponent(map))
            if (body != null) entity.addComponent(BodyComponent(body))
            return entity
        }

        fun spriteAt(x: Float) = GameSprite().apply {
            setBounds(x, 10f, 20f, 20f)
            wrapOffsetX = 7f
        }

        fun run(
            vararg entities: IGameEntity,
            worldWrap: WorldWrap? = wrap,
            drawOffsets: IDrawOffsets? = null,
            frames: Int = 1
        ): GameEngine {
            val engine = GameEngine()
            engine.systems.add(SpritesSystem({}, drawOffsets = drawOffsets, wrap = worldWrap).apply { on = true })
            entities.forEach { engine.spawn(it) }
            repeat(frames) { engine.update(1f) }
            return engine
        }

        it("gives +W to a sprite past the left edge") {
            val sprite = spriteAt(90f)
            val entity = entityWith(sprite)
            accepted.add(entity)
            run(entity)
            sprite.wrapOffsetX shouldBe 200f
        }

        it("gives -W to a sprite past the right edge") {
            val sprite = spriteAt(290f)
            val entity = entityWith(sprite)
            accepted.add(entity)
            run(entity)
            sprite.wrapOffsetX shouldBe -200f
        }

        it("gives 0 to a sprite inside the region") {
            val sprite = spriteAt(150f)
            val entity = entityWith(sprite)
            accepted.add(entity)
            run(entity)
            sprite.wrapOffsetX shouldBe 0f
        }

        it("reads the box with its draw offset") {
            val sprite = spriteAt(105f)
            val entity = entityWith(sprite, body = Body(BodyType.DYNAMIC))
            accepted.add(entity)
            val offsets = object : IDrawOffsets {
                override fun getDrawOffset(body: IBody, out: Vector2) = out.set(-10f, 0f)
            }
            run(entity, drawOffsets = offsets)
            sprite.wrapOffsetX shouldBe 200f
        }

        it("sets each sprite of the entity on its own") {
            val left = spriteAt(90f)
            val inside = spriteAt(150f)
            val entity = entityWith(left, inside)
            accepted.add(entity)
            run(entity)
            left.wrapOffsetX shouldBe 200f
            inside.wrapOffsetX shouldBe 0f
        }

        it("gives 0 with no region") {
            regionOn = false
            val sprite = spriteAt(90f)
            val entity = entityWith(sprite)
            accepted.add(entity)
            run(entity)
            sprite.wrapOffsetX shouldBe 0f
        }

        it("gives 0 with no wrap") {
            val sprite = spriteAt(90f)
            val entity = entityWith(sprite)
            accepted.add(entity)
            run(entity, worldWrap = null)
            sprite.wrapOffsetX shouldBe 0f
        }

        it("gives 0 to an entity that is not accepted") {
            val sprite = spriteAt(90f)
            run(entityWith(sprite))
            sprite.wrapOffsetX shouldBe 0f
        }

        it("gives 0 to an entity whose body is at the cap") {
            cap = 1
            val body = Body(BodyType.DYNAMIC).apply { wrapBy(0f) }
            val sprite = spriteAt(90f)
            val entity = entityWith(sprite, body = body)
            accepted.add(entity)
            run(entity)
            sprite.wrapOffsetX shouldBe 0f
        }

        it("gives the offset at the cap on the edge that the last wrap came in through") {
            cap = 1
            val body = Body(BodyType.DYNAMIC).apply { wrapBy(200f) }
            val sprite = spriteAt(290f)
            val entity = entityWith(sprite, body = body)
            accepted.add(entity)
            run(entity)
            sprite.wrapOffsetX shouldBe -200f
        }

        it("gives the offset to an entity whose body is under the cap") {
            cap = 2
            val body = Body(BodyType.DYNAMIC).apply { wrapBy(0f) }
            val sprite = spriteAt(90f)
            val entity = entityWith(sprite, body = body)
            accepted.add(entity)
            run(entity)
            sprite.wrapOffsetX shouldBe 200f
        }

        it("counts 0 wraps for an entity with no body") {
            cap = 1
            val sprite = spriteAt(90f)
            val entity = entityWith(sprite)
            accepted.add(entity)
            run(entity)
            sprite.wrapOffsetX shouldBe 200f
        }

        it("sets the field again each frame") {
            val sprite = spriteAt(90f)
            val entity = entityWith(sprite)
            accepted.add(entity)
            val engine = run(entity)
            sprite.wrapOffsetX shouldBe 200f
            sprite.setX(150f)
            engine.update(1f)
            sprite.wrapOffsetX shouldBe 0f
        }

        it("reads the region once per frame and the filter once per entity per frame") {
            val first = entityWith(spriteAt(90f), spriteAt(150f))
            val second = entityWith(spriteAt(290f))
            accepted.add(first)
            run(first, second, frames = 3)
            regionCalls shouldBe 3
            wrapsCalls shouldBe 6
        }
    }
})
