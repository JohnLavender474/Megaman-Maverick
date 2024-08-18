package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.utils.Array
import com.engine.common.extensions.objectMapOf
import com.engine.common.extensions.objectSetOf
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.cullables.CullableOnEvent
import com.engine.cullables.CullablesComponent
import com.engine.entities.IGameEntity
import com.engine.entities.contracts.IParentEntity
import com.engine.motion.RotatingLine
import com.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaGameEntity
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.events.EventType

class FireballBar(game: MegamanMaverickGame) : MegaGameEntity(game), IParentEntity {

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

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        var speed = spawnProps.getOrDefault(ConstKeys.SPEED, DEFAULT_SPEED, Float::class)
        val flip = spawnProps.getOrDefault(ConstKeys.FLIP, false, Boolean::class)
        if (flip) speed *= -1f
        rotatingLine = RotatingLine(spawn, RADIUS * ConstVals.PPM, speed * ConstVals.PPM)
        for (i in 0 until BALLS) {
            val fireball = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.FIREBALL)!!
            game.engine.spawn(
                fireball, props(
                    ConstKeys.OWNER to this,
                    ConstKeys.CULL_OUT_OF_BOUNDS to false,
                    ConstKeys.CULL_EVENTS to false,
                    Fireball.BURST_ON_HIT_BLOCK to false
                )
            )
            children.add(fireball)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        children.forEach { it.kill() }
        children.clear()
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        rotatingLine.update(delta)
        for (i in 0 until BALLS) {
            val child = children.get(i) as Fireball
            val position = rotatingLine.getScaledPosition(i.toFloat() / BALLS.toFloat())
            child.body.setCenter(position)
        }
    })

    private fun defineCullablesComponent(): CullablesComponent {
        val cullEvents = objectSetOf<Any>(
            EventType.PLAYER_SPAWN, EventType.BEGIN_ROOM_TRANS, EventType.GATE_INIT_OPENING
        )
        val cullOnEvents = CullableOnEvent({ cullEvents.contains(it.key) }, cullEvents)
        return CullablesComponent(objectMapOf(ConstKeys.CULL_EVENTS to cullOnEvents))
    }
}