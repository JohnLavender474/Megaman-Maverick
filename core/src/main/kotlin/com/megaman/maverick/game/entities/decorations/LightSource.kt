package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.extensions.toInt
import com.mega.game.engine.common.extensions.toObjectSet
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.ILightSourceEntity
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.sendLightEvent
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.entities.utils.getStandardEventCullingLogic
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.extensions.getCenter

open class LightSource(game: MegamanMaverickGame) : MegaGameEntity(game), ILightSourceEntity, ICullableEntity {

    companion object {
        const val TAG = "LightSource"
        private const val DEFAULT_RADIUS = 5
        private const val DEFAULT_RADIANCE = 1f
    }

    override val keys = ObjectSet<Int>()
    override val center: Vector2
        get() = bounds.getCenter()
    override var radiance = 0f
    override var radius = 0

    protected lateinit var type: String
    protected lateinit var bounds: GameRectangle
    protected lateinit var spritePos: Position

    override fun init() {
        super.init()
        addComponent(CullablesComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        keys.addAll(
            spawnProps.get(ConstKeys.KEYS, String::class)!!
            .replace("\\s+", "")
            .split(",")
            .map { it.toInt() }
            .toObjectSet()
        )
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
        keys.clear()
    }

    open fun sendLightEvent() {
        GameLogger.debug(TAG, "sendLightEvent()")
        sendLightEvent(game, this)
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ sendLightEvent() })

    override fun getType() = EntityType.DECORATION

    override fun getTag() = TAG
}
