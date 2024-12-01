package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.cullables.CullableOnEvent
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
import com.megaman.maverick.game.events.EventType
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

    override fun getEntityType() = EntityType.HAZARD

    override fun init() {
        addComponent(defineUpdatablesComponent())
        addComponent(defineCullablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        var speed = spawnProps.getOrDefault(ConstKeys.SPEED, DEFAULT_SPEED, Float::class)

        val flip = spawnProps.getOrDefault(ConstKeys.FLIP, false, Boolean::class)
        if (flip) speed *= -1f

        rotatingLine = RotatingLine(spawn, RADIUS * ConstVals.PPM, speed * ConstVals.PPM)

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
    }

    override fun onDestroy() {
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

    private fun defineCullablesComponent(): CullablesComponent {
        val cullEvents = objectSetOf<Any>(
            EventType.PLAYER_SPAWN, EventType.BEGIN_ROOM_TRANS, EventType.GATE_INIT_OPENING
        )
        val cullOnEvents = CullableOnEvent({ cullEvents.contains(it.key) }, cullEvents)
        runnablesOnSpawn.put(ConstKeys.CULL_EVENTS) { game.eventsMan.addListener(cullOnEvents) }
        runnablesOnDestroy.put(ConstKeys.CULL_EVENTS) { game.eventsMan.removeListener(cullOnEvents) }
        return CullablesComponent(objectMapOf(ConstKeys.CULL_EVENTS pairTo cullOnEvents))
    }
}
