package com.megaman.maverick.game.entities.megaman

import com.megaman.maverick.game.testServices
import com.maverick.game.engine.controller.ControllerComponent
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.behaviors.BehaviorsComponent
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.events.Event
import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.events.PlayerEventKeys
import com.megaman.maverick.game.players.MegaPlayers
import com.megaman.maverick.game.utils.misc.StunType
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll

class MegamanStunTargetTest : DescribeSpec({

    lateinit var megaman: Megaman

    beforeEach {
        val players = MegaPlayers(2)
        val game = testServices(engine = mockk<GameEngine>(relaxed = true), players = players)

        megaman = Megaman(game)
        megaman.addComponent(BodyComponent(Body(BodyType.DYNAMIC)))
        megaman.addComponent(BehaviorsComponent())
        megaman.addComponent(ControllerComponent())

        players.register(PlayerSlot(0), megaman)

        megaman.stunTimer.setToEnd()
    }

    afterEach { unmockkAll() }

    fun stunEvent(targets: List<PlayerSlot>) = Event(
        EventType.STUN_PLAYER,
        props(
            ConstKeys.TYPE pairTo StunType.STUN_BOUNCE_ALWAYS,
            PlayerEventKeys.SLOTS pairTo Array<PlayerSlot>().apply { targets.forEach { add(it) } }
        )
    )

    describe("shooting") {
        it("reads the shoot animation timer that its setter drives") {
            megaman.shooting = true
            megaman.shooting shouldBe true

            megaman.shooting = false
            megaman.shooting shouldBe false
        }
    }

    describe("STUN_PLAYER") {
        it("stuns Megaman when the targets hold his slot") {
            megaman.facing = Facing.RIGHT

            megaman.onEvent(stunEvent(listOf(PlayerSlot(1), PlayerSlot(0))))

            megaman.stunned shouldBe true
        }

        it("is ignored when the targets do not hold Megaman's slot") {
            megaman.onEvent(stunEvent(listOf(PlayerSlot(1))))

            megaman.stunned shouldBe false
        }

        it("is ignored when the event has no targets") {
            megaman.onEvent(Event(EventType.STUN_PLAYER, props(ConstKeys.TYPE pairTo StunType.STUN_BOUNCE_ALWAYS)))

            megaman.stunned shouldBe false
        }
    }
})
