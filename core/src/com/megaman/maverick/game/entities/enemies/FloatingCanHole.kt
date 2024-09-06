package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.entities.GameEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.entities.contracts.IParentEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.EnemiesFactory
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.world.body.BodyComponentCreator

class FloatingCanHole(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, IParentEntity, ICullableEntity,
    IHazard {

    companion object {
        const val TAG = "FloatingCanHole"
        private const val SPAWN_DELAY = 2.5f
        private const val DEFAULT_MAX_SPAWNED = 3
    }

    override var children = Array<GameEntity>()

    private val spawnDelayTimer = Timer(SPAWN_DELAY)
    private var maxToSpawn = DEFAULT_MAX_SPAWNED

    override fun getEntityType() = EntityType.HAZARD

    override fun init() {
        addComponent(defineBodyComponent())
        addComponent(defineUpdatablesComponent())
        addComponent(defineCullablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)
        maxToSpawn = spawnProps.getOrDefault(ConstKeys.MAX, DEFAULT_MAX_SPAWNED, Int::class)
        spawnDelayTimer.reset()
    }

    override fun onDestroy() {
        super.onDestroy()
        children.clear()
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        val iter = children.iterator()
        while (iter.hasNext()) {
            val child = iter.next()
            if (!child.state.spawned) iter.remove()
        }
        if (children.size < maxToSpawn) {
            spawnDelayTimer.update(delta)
            if (spawnDelayTimer.isFinished()) {
                val floatingCan = EntityFactories.fetch(EntityType.ENEMY, EnemiesFactory.FLOATING_CAN)!!
                floatingCan.spawn(props(ConstKeys.POSITION to body.getCenter()))
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