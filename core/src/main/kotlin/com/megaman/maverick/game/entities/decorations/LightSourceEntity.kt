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
import com.megaman.maverick.game.entities.contracts.ILightSource
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.entities.utils.getStandardEventCullingLogic
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.misc.LightSourceUtils

open class LightSourceEntity(game: MegamanMaverickGame) : MegaGameEntity(game), ILightSource, ICullableEntity {

    companion object {
        const val TAG = "LightSource"
        private const val DEFAULT_RADIUS = 5
        private const val DEFAULT_RADIANCE = 1f
    }

    override val lightSourceKeys = ObjectSet<Int>()
    override val lightSourceCenter: Vector2
        get() = bounds.getCenter()
    override var lightSourceRadius = 0
    override var lightSourceRadiance = 0f

    protected lateinit var type: String

    protected lateinit var spritePos: Position

    protected val bounds = GameRectangle()

    override fun init() {
        GameLogger.debug(TAG, "init()")
        super.init()
        addComponent(CullablesComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        bounds.set(spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!)
        lightSourceKeys.addAll(
            spawnProps.get(ConstKeys.KEYS, String::class)!!
                .replace("\\s+", "")
                .split(",")
                .map { it.toInt() }
                .toObjectSet()
        )
        lightSourceRadius = spawnProps.getOrDefault(ConstKeys.RADIUS, DEFAULT_RADIUS, Int::class)
        lightSourceRadiance = spawnProps.getOrDefault(ConstKeys.RADIANCE, DEFAULT_RADIANCE, Float::class)

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
        lightSourceKeys.clear()
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ LightSourceUtils.sendLightSourceEvent(game, this) })

    override fun getType() = EntityType.DECORATION

    override fun getTag() = TAG
}
