package com.megaman.maverick.game.entities.sensors


import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.badlogic.gdx.utils.OrderedSet
import com.mega.game.engine.common.enums.ProcessState
import com.mega.game.engine.common.extensions.toOrderedSet
import com.mega.game.engine.common.objects.GamePair
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.entities.GameEntity
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.IParentEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.utils.convertObjectPropsToEntitySuppliers
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.setConsumer

class FixtureTypeOverlapSpawn(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, IParentEntity {

    companion object {
        const val TAG = "FixtureTypeOverlapSpawn"
    }

    override var children = Array<IGameEntity>()

    private lateinit var entitySuppliers: Array<GamePair<() -> GameEntity, Properties>>
    private lateinit var spawnMask: OrderedSet<FixtureType>
    private val fixturesConsumed = ObjectSet<FixtureType>()
    private var objectSpawned = false

    override fun getEntityType() = EntityType.SENSOR

    override fun init() {
        addComponent(defineBodyComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        body.set(bounds)
        body.fixtures.forEach { ((it.second as Fixture).rawShape as GameRectangle).set(bounds) }

        entitySuppliers = convertObjectPropsToEntitySuppliers(spawnProps)

        val mask = spawnProps.get(ConstKeys.MASK, String::class)!!.split(",")
        spawnMask = (mask.map { FixtureType.valueOf(it.uppercase()) }).toOrderedSet()

        objectSpawned = false
    }

    override fun onDestroy() {
        super.onDestroy()
        fixturesConsumed.clear()
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)

        val consumerFixture = Fixture(body, FixtureType.CONSUMER, GameRectangle())
        val consumer: (ProcessState, IFixture) -> Unit = { _, it ->
            fixturesConsumed.add(it.getType() as FixtureType)
        }
        consumerFixture.setConsumer(consumer)
        body.addFixture(consumerFixture)

        return BodyComponentCreator.create(this, body)
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({
        if (objectSpawned) return@UpdatablesComponent

        for (mask in spawnMask) {
            if (fixturesConsumed.contains(mask)) {
                entitySuppliers.forEach { (entitySupplier, props) ->
                    val entity = entitySupplier.invoke()
                    entity.spawn(props)
                    children.add(entity)
                }
                objectSpawned = true
                break
            }
        }

        fixturesConsumed.clear()
    })

}
