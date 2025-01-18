package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.entities.GameEntity
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.entities.contracts.IParentEntity
import com.mega.game.engine.motion.RotatingLine
import com.mega.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.entities.utils.getStandardEventCullingLogic
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.screens.levels.spawns.SpawnType
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter

class FireballBar(game: MegamanMaverickGame) : MegaGameEntity(game), IParentEntity, ICullableEntity {

    companion object {
        const val TAG = "FireballBar"
        private const val RADIUS = 2.5f
        private const val BALLS = 4
        private const val DEFAULT_SPEED = 4f
    }

    override var children = Array<IGameEntity>()

    private lateinit var rotatingLine: RotatingLine
    private lateinit var spawnRoom: String

    override fun init() {
        GameLogger.debug(TAG, "init()")
        super.init()
        addComponent(defineUpdatablesComponent())
        addComponent(defineCullablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")

        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        var speed = spawnProps.getOrDefault(ConstKeys.SPEED, DEFAULT_SPEED, Float::class)

        val flip = spawnProps.getOrDefault(ConstKeys.FLIP, false, Boolean::class)
        if (flip) speed *= -1f

        val angle = spawnProps.getOrDefault(ConstKeys.ANGLE, 0f, Float::class)
        rotatingLine = RotatingLine(spawn, RADIUS * ConstVals.PPM, speed * ConstVals.PPM, angle)

        (0 until BALLS).forEach {
            val fireball = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.FIREBALL)!!
            fireball.spawn(
                props(
                    ConstKeys.OWNER pairTo this,
                    ConstKeys.CULL_OUT_OF_BOUNDS pairTo false,
                    ConstKeys.CULL_EVENTS pairTo false,
                    Fireball.BURST_ON_HIT_BLOCK pairTo false
                )
            )
            children.add(fireball)
        }

        spawnRoom = spawnProps.get(SpawnType.SPAWN_ROOM, String::class)!!
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        children.forEach { (it as GameEntity).destroy() }
        children.clear()
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        rotatingLine.update(delta)
        for (i in 0 until BALLS) {
            val child = children.get(i) as Fireball
            val position =
                rotatingLine.getScaledPosition(i.toFloat() / BALLS.toFloat(), GameObjectPools.fetch(Vector2::class))
            child.body.setCenter(position)
        }
    })

    private fun defineCullablesComponent() = CullablesComponent(
        objectMapOf(
            ConstKeys.CULL_EVENTS pairTo getStandardEventCullingLogic(
                this, objectSetOf(EventType.END_ROOM_TRANS), { event ->
                    val room = event.getProperty(ConstKeys.ROOM, RectangleMapObject::class)!!.name
                    val cull = room != spawnRoom
                    GameLogger.debug(
                        TAG,
                        "defineCullablesComponent(): currentRoom=$room, spawnRoom=$spawnRoom, cull=$cull"
                    )
                    cull
                }
            )
        )
    )

    override fun getType() = EntityType.HAZARD

    override fun getTag() = TAG
}
