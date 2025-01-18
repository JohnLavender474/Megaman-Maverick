package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.extensions.toInt
import com.mega.game.engine.common.extensions.toObjectSet
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.events.Event
import com.mega.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.entities.utils.getStandardEventCullingLogic
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.extensions.getCenter

open class LightSource(game: MegamanMaverickGame) : MegaGameEntity(game), ICullableEntity {

    companion object {
        const val TAG = "LightSource"
        private const val DEFAULT_RADIUS = 5
        private const val DEFAULT_RADIANCE = 1f
    }

    protected lateinit var type: String
    protected lateinit var bounds: GameRectangle
    protected lateinit var keys: ObjectSet<Int>
    protected lateinit var spritePos: Position
    protected var radiance = 0f
    protected var radius = 0

    override fun init() {
        super.init()
        addComponent(CullablesComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        keys = spawnProps.get(ConstKeys.KEYS, String::class)!!
            .replace("\\s+", "")
            .split(",")
            .map { it.toInt() }
            .toObjectSet()
        radiance = spawnProps.getOrDefault(ConstKeys.RADIANCE, DEFAULT_RADIANCE, Float::class)
        radius = spawnProps.getOrDefault(ConstKeys.RADIUS, DEFAULT_RADIUS, Int::class)

        val rawSpritePos = spawnProps.get("${ConstKeys.SPRITE}_${ConstKeys.POSITION}")
        spritePos = when (rawSpritePos) {
            is Position -> rawSpritePos
            is String -> Position.valueOf(rawSpritePos.uppercase())
            else -> Position.BOTTOM_CENTER
        }

        val cullOutOfBounds = spawnProps.getOrDefault(ConstKeys.CULL_OUT_OF_BOUNDS, true, Boolean::class)
        if (cullOutOfBounds) putCullable(
            ConstKeys.CULL_OUT_OF_BOUNDS, getGameCameraCullingLogic(game.getGameCamera(), { bounds })
        ) else removeCullable(ConstKeys.CULL_OUT_OF_BOUNDS)

        val cullEvents = spawnProps.getOrDefault(ConstKeys.CULL_EVENTS, true, Boolean::class)
        if (cullEvents) putCullable(
            ConstKeys.CULL_EVENTS, getStandardEventCullingLogic(this, objectSetOf(EventType.BEGIN_ROOM_TRANS))
        ) else removeCullable(ConstKeys.CULL_EVENTS)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
    }

    open fun sendAddLightSourceEvent() {
        GameLogger.debug(TAG, "sendAddLightSourceEvent()")
        game.eventsMan.submitEvent(
            Event(
                EventType.ADD_LIGHT_SOURCE, props(
                    ConstKeys.KEYS pairTo keys,
                    ConstKeys.CENTER pairTo bounds.getCenter(),
                    ConstKeys.RADIUS pairTo radius * ConstVals.PPM,
                    ConstKeys.RADIANCE pairTo radiance
                )
            )
        )
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ sendAddLightSourceEvent() })

    override fun getType() = EntityType.DECORATION

    override fun getTag() = TAG
}
