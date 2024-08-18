package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.utils.Array
import com.engine.common.extensions.objectMapOf
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.cullables.CullablesComponent
import com.engine.entities.IGameEntity
import com.engine.entities.contracts.IBodyEntity
import com.engine.entities.contracts.ICullableEntity
import com.engine.entities.contracts.IParentEntity
import com.engine.updatables.UpdatablesComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.EnemiesFactory
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.world.BodyComponentCreator

class FloatingCanHole(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, IParentEntity, ICullableEntity,
    IHazard {

    companion object {
        const val TAG = "FloatingCanHole"
        private const val SPAWN_DELAY = 2.5f
        private const val DEFAULT_MAX_SPAWNED = 3
    }

    override var children = Array<IGameEntity>()

    private val spawnDelayTimer = Timer(SPAWN_DELAY)
    private var maxToSpawn = DEFAULT_MAX_SPAWNED

    override fun getEntityType() = EntityType.HAZARD

    override fun init() {
        super<MegaGameEntity>.init()
        addComponent(defineBodyComponent())
        addComponent(defineUpdatablesComponent())
        addComponent(defineCullablesComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)
        maxToSpawn = spawnProps.getOrDefault(ConstKeys.MAX, DEFAULT_MAX_SPAWNED, Int::class)
        spawnDelayTimer.reset()
    }

    override fun onDestroy() {
        super<MegaGameEntity>.onDestroy()
        // children.forEach { it.kill() }
        children.clear()
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        val iter = children.iterator()
        while (iter.hasNext()) {
            val child = iter.next()
            if (child.dead) iter.remove()
        }
        if (children.size < maxToSpawn) {
            spawnDelayTimer.update(delta)
            if (spawnDelayTimer.isFinished()) {
                val floatingCan = EntityFactories.fetch(EntityType.ENEMY, EnemiesFactory.FLOATING_CAN)!!
                game.engine.spawn(floatingCan, props(ConstKeys.POSITION to body.getCenter()))
                children.add(floatingCan)
                spawnDelayTimer.reset()
            }
        }
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ConstVals.PPM.toFloat())
        return BodyComponentCreator.create(this, body)
    }

    private fun defineCullablesComponent(): CullablesComponent {
        val camCullable = getGameCameraCullingLogic(this)
        return CullablesComponent(objectMapOf(ConstKeys.CULL_OUT_OF_BOUNDS to camCullable))
    }
}