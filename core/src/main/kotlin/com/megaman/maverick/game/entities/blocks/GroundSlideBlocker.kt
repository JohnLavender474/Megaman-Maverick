package com.megaman.maverick.game.entities.blocks

import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.world.body.BodyComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.entities.contracts.megaman

class GroundSlideBlocker(game: MegamanMaverickGame): AbstractBlock(game) {

    companion object {
        const val TAG = "GroundSlideBlock"
    }

    override fun init() {
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

    private fun isActive() = !megaman.isAnyBehaviorActive(BehaviorType.GROUND_SLIDING, BehaviorType.CROUCHING) &&
        !game.controllerPoller.isPressed(MegaControllerButton.DOWN)
}
