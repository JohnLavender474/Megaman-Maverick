package com.megaman.maverick.game.entities.special

import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.extensions.objectMapOf
import com.maverick.game.engine.common.extensions.objectSetOf
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.cullables.CullablesComponent
import com.maverick.game.engine.entities.contracts.ICullableEntity
import com.maverick.game.engine.events.Event
import com.maverick.game.engine.events.IEventListener
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.targetPlayer
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.players.body
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.isSensing

class SwimBooster(game: MegamanMaverickGame) : MegaGameEntity(game), ICullableEntity, IEventListener {

    companion object {
        const val TAG = "SwimBooster"
    }

    override val eventKeyMask = objectSetOf<Any>(EventType.END_ROOM_TRANS)

    private val bounds = GameRectangle()

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        super.init()
        addComponent(defineCullablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)
        game.eventsMan.addListener(this)
        bounds.set(spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        game.eventsMan.removeListener(this)
    }

    override fun onEvent(event: Event) {
        GameLogger.debug(TAG, "onEvent(): event=$event")
        if (event.key == EventType.END_ROOM_TRANS) {
            // TODO: the boost uses Mega Man's swim velocity for the target player; a value for each character is for
            //  the second playable character (Proto Man item)
            if (targetPlayer.body.isSensing(BodySense.IN_WATER))
                targetPlayer.body.physics.velocity.y = MegamanValues.SWIM_VEL_Y * ConstVals.PPM
            destroy()
        }
    }

    private fun defineCullablesComponent() = CullablesComponent(
        objectMapOf(ConstKeys.CULL_OUT_OF_BOUNDS pairTo getGameCameraCullingLogic(game.getGameCamera(), { bounds }))
    )

    override fun getType() = EntityType.SPECIAL

    override fun getTag() = TAG
}
