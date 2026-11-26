package com.megaman.maverick.game.entities.megaman

import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.behaviors.BehaviorsComponent
import com.maverick.game.engine.behaviors.IBehavior
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.controller.ControllerComponent
import com.maverick.game.engine.controller.buttons.ButtonStatus
import com.maverick.game.engine.entities.EntityExistenceState
import com.maverick.game.engine.input.InputFrame
import com.maverick.game.engine.input.InputSource
import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.megaman.maverick.game.entities.state.WeaponWindupComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.players.AButtonTask
import com.megaman.maverick.game.players.aButtonTask
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.megaman.weapons.MegamanWeapons
import com.megaman.maverick.game.entities.state.PostActionMomentumComponent
import com.megaman.maverick.game.entities.state.RunComponent
import com.megaman.maverick.game.entities.state.TeleportableComponent
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.testServices
import io.mockk.every
import io.mockk.mockk

class MegamanMovesRig {

    val services: MegaServices = testServices(engine = mockk<GameEngine>(relaxed = true))
    val megaman: Megaman
    val body = Body(BodyType.DYNAMIC)
    val behaviors = BehaviorsComponent()
    var frame: InputFrame = InputFrame.RELEASED
    var overGround = false

    init {
        services.levelState.currentLevel = LevelDefinition.DESERT_MAN

        megaman = Megaman(services)
        megaman.existenceState = EntityExistenceState.LIVE
        megaman.weaponsHandler = mockk(relaxed = true)
        megaman.jetpackHandler = mockk(relaxed = true)
        megaman.currentWeapon = MegamanWeapons.MEGA_BUSTER
        megaman.facing = Facing.RIGHT
        megaman.jumpVel = MegamanValues.JUMP_VEL
        megaman.wallJumpVel = MegamanValues.WALL_JUMP_VEL
        megaman.wallSlideNotAllowedTimer.setToEnd()
        megaman.wallJumpTimer.setToEnd()

        body.putProperty(ConstKeys.BODY_OVER_GROUND, { overGround })
        megaman.addComponent(BodyComponent(body))
        megaman.addComponent(TeleportableComponent())
        megaman.addComponent(WeaponWindupComponent())
        megaman.addComponent(RunComponent())
        megaman.addComponent(PostActionMomentumComponent(1f, 1f, { 1f }, { overGround }, { false }, { false }))
        megaman.addComponent(behaviors)
        megaman.addRuleComponents()
        megaman.aButtonTask = AButtonTask.AIR_DASH

        services.players.register(PlayerSlot(0), megaman)
        megaman.addComponent(ControllerComponent().also { it.input = InputSource { frame } })
    }

    fun press(vararg pairs: Pair<MegaControllerButton, ButtonStatus>) {
        frame = InputFrame.RELEASED
        pairs.forEach { (button, status) -> frame = frame.with(button, status) }
    }

    fun setActive(type: BehaviorType) {
        val behavior = mockk<IBehavior>(relaxed = true)
        every { behavior.isActive() } returns true
        behaviors.addBehavior(type, behavior)
    }
}
