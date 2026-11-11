package com.megaman.maverick.game.players

import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.behaviors.BehaviorsComponent
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.controller.ControllerComponent
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.world.body.BodyComponent
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.entities.state.KnockbackComponent
import com.megaman.maverick.game.entities.state.PostActionMomentumComponent
import com.megaman.maverick.game.entities.state.StunComponent

abstract class TestMegaPlayer(services: MegaServices) : MegaPlayer(services) {

    override val cameraFocusHeight = 1.5f

    override var direction: Direction
        get() = body.direction
        set(value) {
            body.direction = value
        }

    override fun defineBodyComponent(): BodyComponent = notUsed()

    override fun defineMomentumComponent(): PostActionMomentumComponent = notUsed()

    override fun defineKnockbackComponent(): KnockbackComponent = notUsed()

    override fun defineStunComponent(): StunComponent = notUsed()

    override fun defineBehaviorsComponent(): BehaviorsComponent = notUsed()

    override fun defineControllerComponent(): ControllerComponent = notUsed()

    override fun defineSpritesComponent(): SpritesComponent = notUsed()

    override fun defineAnimationsComponent(): AnimationsComponent = notUsed()

    private fun notUsed(): Nothing =
        throw UnsupportedOperationException("a test player adds its components in its own init block")
}
