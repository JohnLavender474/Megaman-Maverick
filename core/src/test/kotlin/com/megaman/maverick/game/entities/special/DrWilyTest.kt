package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.entities.EntityExistenceState
import com.maverick.game.engine.events.Event
import com.maverick.game.engine.events.EventsManager
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.state.GameState
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class DrWilyTest : DescribeSpec({

    lateinit var engine: GameEngine
    lateinit var game: MegamanMaverickGame
    lateinit var submitted: MutableList<Int>
    var step = 0
    var alive = true

    beforeEach {
        engine = GameEngine()
        submitted = mutableListOf()
        step = 0
        alive = true
        game = mockk(relaxed = true)
        every { game.engine } returns engine
        every { game.state } returns GameState()
        every { game.getCurrentLevel() } returns LevelDefinition.TEST_1
        val atlas = mockk<TextureAtlas>()
        every { atlas.findRegion(any()) } returns AtlasRegion(mockk<Texture>(relaxed = true), 0, 0, 64, 64)
        every { game.assMan.get(any<String>(), TextureAtlas::class.java) } returns atlas
        val target = mockk<MegaGameEntity>(relaxed = true)
        every { target.getComponent(BodyComponent::class) } returns BodyComponent(Body(BodyType.ABSTRACT))
        every { target.existenceState } answers {
            if (alive) EntityExistenceState.LIVE else EntityExistenceState.DESTROY_QUEUED
        }
        every { game.players.targetFor(any()) } returns target
        val eventsMan = mockk<EventsManager>(relaxed = true)
        every { eventsMan.submitEvent(any()) } answers {
            if (firstArg<Event>().key == EventType.GAME_COMPLETE) submitted.add(step)
        }
        every { game.eventsMan } returns eventsMan
    }

    afterEach { engine.reset() }

    fun runDrWily(deadFromStep: Int = Int.MAX_VALUE) {
        val wily = DrWily(game)
        wily.spawn(
            props(
                ConstKeys.POSITION pairTo Vector2(0f, 0f),
                ConstKeys.TARGET pairTo Vector2(64f, 32f),
                ConstKeys.MAX_Y pairTo 320f,
                ConstKeys.FACING pairTo Facing.LEFT
            )
        ) shouldBe true
        engine.update(0f)
        for (i in 0 until 400) {
            step = i
            alive = i < deadFromStep
            wily.update(0.05f)
        }
    }

    describe("DrWily") {
        it("submits GAME_COMPLETE once, when the still timer finishes and the target player lives") {
            runDrWily()

            submitted.size shouldBe 1
            submitted.first() shouldBeGreaterThan 7.0.div(0.05).toInt() - 1
            verify(exactly = 0) { game.setCurrentScreen(any(), any()) }
        }

        it("never submits GAME_COMPLETE when the target player is dead when the still timer finishes") {
            runDrWily()
            val switchStep = submitted.firstOrNull().shouldNotBeNull()
            submitted.clear()

            runDrWily(deadFromStep = switchStep)

            submitted.size shouldBe 0
        }
    }
})
