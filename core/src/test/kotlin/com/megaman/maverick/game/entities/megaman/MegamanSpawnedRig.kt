package com.megaman.maverick.game.entities.megaman

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.behaviors.BehaviorsComponent
import com.maverick.game.engine.behaviors.BehaviorsSystem
import com.maverick.game.engine.behaviors.IBehavior
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.enums.ProcessState
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.components.require
import com.maverick.game.engine.controller.ControllerComponent
import com.maverick.game.engine.controller.buttons.ButtonStatus
import com.maverick.game.engine.entities.EntityExistenceState
import com.maverick.game.engine.input.InputFrame
import com.maverick.game.engine.input.InputSource
import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.enemies.MetaWheelF
import com.megaman.maverick.game.entities.state.PlayPhase
import com.megaman.maverick.game.entities.state.PlayPhaseComponent
import com.megaman.maverick.game.entities.state.TagsComponent
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.players.updateAButtonTask
import com.megaman.maverick.game.testServices
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.Receivers
import com.megaman.maverick.game.world.body.receiver
import com.megaman.maverick.game.world.body.setBodySense
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

const val SPAWNED_RIG_DELTA = 0.05f

class MegamanSpawnedRig {

    val services: MegaServices = testServices(engine = mockk<GameEngine>(relaxed = true))
    val megaman: Megaman
    val system = BehaviorsSystem()
    var frame: InputFrame = InputFrame.RELEASED
    var overGround = true

    val body: Body
        get() = megaman.body

    val behaviors: BehaviorsComponent
        get() = megaman.require(BehaviorsComponent::class)

    init {
        val atlas = mockk<TextureAtlas>()
        every { atlas.findRegion(any()) } returns AtlasRegion(mockk<Texture>(relaxed = true), 0, 0, 64, 64)
        every { services.assets.get(any<String>(), TextureAtlas::class.java) } returns atlas
        services.levelState.currentLevel = LevelDefinition.DESERT_MAN

        megaman = Megaman(services)
        megaman.init()
        services.players.register(PlayerSlot(0), megaman)
        spawn()
        system.add(megaman)
    }

    fun spawn() {
        megaman.existenceState = EntityExistenceState.LIVE
        megaman.onSpawn(props(ConstKeys.BOUNDS pairTo GameRectangle(0f, 0f, ConstVals.PPM.toFloat(), ConstVals.PPM.toFloat())))
        megaman.weaponsHandler = mockk(relaxed = true)
        megaman.facing = Facing.RIGHT
        megaman.require(PlayPhaseComponent::class).phase = PlayPhase.IN_PLAY
        megaman.require(ControllerComponent::class).input = InputSource { frame }
        body.putProperty(ConstKeys.BODY_OVER_GROUND, { overGround })
        body.setBodySense(BodySense.FEET_ON_GROUND, overGround)
    }

    fun press(vararg pairs: Pair<MegaControllerButton, ButtonStatus>) {
        frame = InputFrame.RELEASED
        pairs.forEach { (button, status) -> frame = frame.with(button, status) }
    }

    fun step(vararg pairs: Pair<MegaControllerButton, ButtonStatus>) {
        press(*pairs)
        system.update(SPAWNED_RIG_DELTA)
        megaman.updateAButtonTask()
    }

    fun ground(value: Boolean) {
        overGround = value
        body.setBodySense(BodySense.FEET_ON_GROUND, value)
    }

    fun water(value: Boolean) = body.setBodySense(BodySense.IN_WATER, value)

    fun behavior(type: BehaviorType): IBehavior = behaviors.getBehavior(type)!!

    fun active(type: BehaviorType) = behaviors.isBehaviorActive(type)

    fun setActive(type: BehaviorType) {
        val behavior = mockk<IBehavior>(relaxed = true)
        every { behavior.isActive() } returns true
        behaviors.addBehavior(type, behavior)
    }

    fun hurt() {
        val damager = MetaWheelF(services).also {
            it.addComponent(TagsComponent(MetaWheelF.TAG))
            it.addComponent(BodyComponent(Body(BodyType.ABSTRACT)))
        }
        megaman.takeDamageFrom(damager) shouldBe true
    }

    private fun fixture(type: FixtureType): IFixture = body.getFixtures(Array<IFixture>()).first { it.getType() == type }

    fun feetBlock(state: ProcessState) {
        val blockBody = Body(BodyType.STATIC)
        val block = mockk<Block>(relaxed = true)
        every { block.body } returns blockBody
        every { block.getOrDefaultProperty(any(), any<Boolean>(), Boolean::class) } answers { secondArg() }
        fixture(FixtureType.FEET).receiver(Receivers.FEET_BLOCK_CONTACT)!!.invoke(state, block)
    }

    fun feetBounce() = fixture(FixtureType.FEET).receiver(Receivers.RUNNABLE)!!.invoke()

    fun waterContact(state: ProcessState) =
        fixture(FixtureType.WATER_LISTENER).receiver(Receivers.WATER_CONTACT)!!.invoke(state, true)
}
