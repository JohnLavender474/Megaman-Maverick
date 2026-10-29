package com.megaman.maverick.game.entities.blocks

import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.entities.input
import com.maverick.game.engine.entities.isAnyBehaviorActive
import com.maverick.game.engine.world.body.BodyComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.entities.contracts.targetPlayer

class GroundSlideBlocker(game: MegaServices): AbstractBlock(game) {

    companion object {
        const val TAG = "GroundSlideBlock"
    }

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        super.init()
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
    }

    override fun defineBodyComponent(): BodyComponent {
        val component = super.defineBodyComponent()
        component.body.preProcess.put(ConstKeys.ACTIVE) {
            val active = isActive()
            body.physics.collisionOn = active
            body.forEachFixture { it.setActive(active) }
        }
        return component
    }

    private fun isActive() = !targetPlayer.isAnyBehaviorActive(BehaviorType.GROUND_SLIDING, BehaviorType.CROUCHING) &&
        !targetPlayer.input.isPressed(MegaControllerButton.DOWN)
}
