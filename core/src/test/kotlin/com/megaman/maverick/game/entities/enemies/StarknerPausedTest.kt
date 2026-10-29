package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.state.GameClock
import com.megaman.maverick.game.state.GameState
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class StarknerPausedTest : DescribeSpec({

    lateinit var engine: GameEngine
    lateinit var clock: GameClock
    lateinit var starkner: Starkner

    beforeEach {
        engine = GameEngine()
        clock = GameClock()
        val game = mockk<MegamanMaverickGame>(relaxed = true)
        every { game.engine } returns engine
        every { game.state } returns GameState()
        every { game.clock } returns clock
        every { game.getCurrentLevel() } returns LevelDefinition.TEST_1
        val atlas = mockk<TextureAtlas>()
        every { atlas.findRegion(any()) } returns AtlasRegion(mockk<Texture>(relaxed = true), 0, 0, 64, 64)
        every { game.assMan.get(any<String>(), TextureAtlas::class.java) } returns atlas
        val target = mockk<MegaGameEntity>(relaxed = true)
        every { target.getComponent(BodyComponent::class) } returns BodyComponent(Body(BodyType.ABSTRACT))
        every { game.players.targetFor(any()) } returns target

        starkner = Starkner(game)
        starkner.spawn(props(ConstKeys.ID pairTo 201, ConstKeys.POSITION pairTo Vector2())) shouldBe true
        engine.update(0f)
        starkner.direction = Direction.LEFT
    }

    afterEach { engine.reset() }

    fun spriteRotationAfterPreProcess(): Float {
        val sprites = starkner.getComponent(SpritesComponent::class)!!
        sprites.preProcess(0.1f)
        return sprites.sprites[Starkner.TAG].rotation
    }

    describe("the Starkner sprite pre-process") {
        it("keeps the rotation while the game clock is paused") {
            clock.paused = true

            spriteRotationAfterPreProcess() shouldBe 0f
        }

        it("turns the sprite to the body direction when the game clock is not paused") {
            clock.paused = false

            spriteRotationAfterPreProcess() shouldBe Direction.LEFT.rotation
        }

        it("keeps the rotation it reached when the game clock pauses after it turned") {
            spriteRotationAfterPreProcess() shouldBe Direction.LEFT.rotation

            clock.paused = true
            starkner.direction = Direction.DOWN

            spriteRotationAfterPreProcess() shouldBe Direction.LEFT.rotation
        }
    }
})
