package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.common.objects.Pool
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.factories.GameEntityPoolCreator
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.state.GameState
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.every
import io.mockk.mockk

class SpiderWebDestroyTest : DescribeSpec({

    lateinit var engine: GameEngine
    lateinit var game: MegamanMaverickGame
    lateinit var pool: Pool<MegaGameEntity>

    beforeEach {
        engine = GameEngine()
        game = mockk(relaxed = true)
        every { game.engine } returns engine
        every { game.state } returns GameState()
        every { game.getCurrentLevel() } returns LevelDefinition.TEST_1
        val atlas = mockk<TextureAtlas>()
        every { atlas.findRegion(any()) } returns AtlasRegion(mockk<Texture>(relaxed = true), 0, 0, 64, 64)
        every { game.assMan.get(any<String>(), TextureAtlas::class.java) } returns atlas
        val target = mockk<MegaGameEntity>(relaxed = true)
        every { target.getComponent(BodyComponent::class) } returns BodyComponent(Body(BodyType.ABSTRACT))
        every { game.players.targetFor(any()) } returns target
        pool = GameEntityPoolCreator.create { SpiderWeb(game) }
    }

    afterEach { engine.reset() }

    fun spawnLive(id: Int): SpiderWeb {
        val web = pool.fetch() as SpiderWeb
        val owner = mockk<MegaGameEntity>(relaxed = true)
        web.spawn(props(ConstKeys.ID pairTo id, ConstKeys.POSITION pairTo Vector2(), ConstKeys.OWNER pairTo owner))
            .shouldBe(true)
        engine.update(0f)
        MegaGameEntities.getOfId(id).contains(web) shouldBe true
        web.owner shouldBeSameInstanceAs owner
        return web
    }

    describe("a destroyed SpiderWeb") {
        it("leaves the entity registry") {
            val web = spawnLive(201)

            web.destroy()
            engine.update(0f)

            MegaGameEntities.getOfId(201).contains(web) shouldBe false
            MegaGameEntities.getOfTag(SpiderWeb.TAG).contains(web) shouldBe false
        }

        it("runs its runnablesOnDestroy and goes back to its pool") {
            val web = spawnLive(202)
            var ran = false
            web.runnablesOnDestroy.put("test") { ran = true }

            web.destroy()
            engine.update(0f)

            ran shouldBe true
            pool.fetch() shouldBeSameInstanceAs web
        }

        it("clears its owner") {
            val web = spawnLive(203)

            web.destroy()
            engine.update(0f)

            web.owner shouldBe null
        }
    }
})
