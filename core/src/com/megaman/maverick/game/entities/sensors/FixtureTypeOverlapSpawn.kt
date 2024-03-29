package com.megaman.maverick.game.entities.sensors

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.badlogic.gdx.utils.OrderedSet
import com.engine.common.enums.ProcessState
import com.engine.common.extensions.toOrderedSet
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.entities.GameEntity
import com.engine.entities.IGameEntity
import com.engine.entities.contracts.IBodyEntity
import com.engine.entities.contracts.IParentEntity
import com.engine.updatables.UpdatablesComponent
import com.engine.world.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.utils.convertObjectPropsToEntities
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType
import com.megaman.maverick.game.world.setConsumer

class FixtureTypeOverlapSpawn(game: MegamanMaverickGame) : GameEntity(game), IBodyEntity, IParentEntity {

    companion object {
        const val TAG = "FixtureTypeOverlapSpawn"
    }

    override var children = Array<IGameEntity>()

    private lateinit var entitiesToSpawn: Array<Pair<IGameEntity, Properties>>
    private lateinit var spawnMask: OrderedSet<FixtureType>

    private val fixturesConsumed = ObjectSet<FixtureType>()

    private var spawned = false

    override fun init() {
        addComponent(defineBodyComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)

        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        body.set(bounds)
        body.fixtures.forEach { ((it.second as Fixture).rawShape as GameRectangle).set(bounds) }

        entitiesToSpawn = convertObjectPropsToEntities(spawnProps)

        val mask = spawnProps.get(ConstKeys.MASK, String::class)!!.split(",")
        spawnMask = (mask.map { FixtureType.valueOf(it.uppercase()) }).toOrderedSet()

        spawned = false
    }

    override fun onDestroy() {
        super<GameEntity>.onDestroy()
        fixturesConsumed.clear()
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)

        val consumerFixture = Fixture(body, FixtureType.CONSUMER, GameRectangle())
        val consumer: (ProcessState, IFixture) -> Unit = { _, it ->
            fixturesConsumed.add(it.getFixtureType() as FixtureType)
        }
        consumerFixture.setConsumer(consumer)
        body.addFixture(consumerFixture)

        return BodyComponentCreator.create(this, body)
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent(this, {
        if (spawned) return@UpdatablesComponent

        for (mask in spawnMask) {
            if (fixturesConsumed.contains(mask)) {
                entitiesToSpawn.forEach { (entity, props) ->
                    game.gameEngine.spawn(entity, props)
                    children.add(entity)
                }
                spawned = true
                break
            }
        }

        fixturesConsumed.clear()
    })

}