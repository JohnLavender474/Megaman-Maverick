package com.mega.game.engine.world

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.OrderedSet
import com.mega.game.engine.common.objects.ImmutableCollection
import com.mega.game.engine.common.objects.MutableOrderedSet
import com.mega.game.engine.common.objects.Pool
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.shapes.IGameShape2D
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.systems.GameSystem
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.IBody
import com.mega.game.engine.world.body.IFixture
import com.mega.game.engine.world.collisions.ICollisionHandler
import com.mega.game.engine.world.contacts.Contact
import com.mega.game.engine.world.contacts.IContactFilter
import com.mega.game.engine.world.contacts.IContactListener
import com.mega.game.engine.world.container.IWorldContainer

class WorldSystem(
    private val ppm: Int,
    private val fixedStep: Float,
    private val worldContainerSupplier: () -> IWorldContainer?,
    private val contactListener: IContactListener,
    private val collisionHandler: ICollisionHandler,
    private val contactFilter: IContactFilter,
    var fixedStepScalar: Float = 1f
) : GameSystem(BodyComponent::class) {

    companion object {
        const val TAG = "WorldSystem"
    }

    init {
        if (fixedStepScalar <= 0f) throw IllegalArgumentException("Value of fixedStepScalar must be greater than 0")
    }

    internal class DummyFixture : IFixture {
        override fun getShape() =
            throw IllegalStateException("The `getType` method should never be called on a DummyFixture instance")

        override fun setShape(shape: IGameShape2D) =
            throw IllegalStateException("The `setShape` method should never be called on a DummyFixture instance")

        override fun setActive(active: Boolean) =
            throw IllegalStateException("The `setShape` method should never be called on a DummyFixture instance")

        override fun isActive() =
            throw IllegalStateException("The `getType` method should never be called on a DummyFixture instance")

        override fun getType() =
            throw IllegalStateException("The `getType` method should never be called on a DummyFixture instance")

        override val properties: Properties
            get() = throw IllegalStateException("The `getType` method should never be called on a DummyFixture instance")
    }

    private val worldContainer: IWorldContainer
        get() = worldContainerSupplier()
            ?: throw IllegalStateException("World container supplier must supply a non-null value for WorldSystem")

    private val contactPool = Pool(supplier = { Contact(DummyFixture(), DummyFixture()) })
    private var priorContactSet = OrderedSet<Contact>()
    private var currentContactSet = OrderedSet<Contact>()

    private var accumulator = 0f

    private val reusableBodyArray = Array<IBody>()
    private val reusableGameRect = GameRectangle()
    private val reusableBodySet = MutableOrderedSet<IBody>()
    private val reusableFixtureSet = MutableOrderedSet<IFixture>()
    private val out1 = GameRectangle()
    private val out2 = GameRectangle()

    override fun process(on: Boolean, entities: ImmutableCollection<IGameEntity>, delta: Float) {
        if (!on) return

        accumulator += delta
        if (accumulator >= fixedStep) {
            entities.forEach {
                val component = it.getComponent(BodyComponent::class)!!
                if (component.doUpdate()) reusableBodyArray.add(component.body)
            }

            while (accumulator >= fixedStep) {
                accumulator -= fixedStep / fixedStepScalar
                cycle(reusableBodyArray, fixedStep)
            }

            worldContainer.clear()
            reusableBodyArray.forEach { body ->
                if (body.physics.collisionOn) worldContainer.addBody(body)
                body.forEachFixture { if (it.isActive()) worldContainer.addFixture(it) }
            }

            reusableBodyArray.clear()
        }
    }

    override fun reset() {
        super.reset()
        accumulator = 0f

        priorContactSet.forEach { contactPool.free(it) }
        currentContactSet.forEach { contactPool.free(it) }
        priorContactSet.clear()
        currentContactSet.clear()

        worldContainerSupplier()?.clear()
    }

    internal fun filterContact(fixture1: IFixture, fixture2: IFixture) = contactFilter.filter(fixture1, fixture2)

    internal fun cycle(bodies: Array<IBody>, delta: Float) {
        bodies.forEach { body -> body.preProcess() }

        worldContainer.clear()

        bodies.forEach { body ->
            body.process(delta)
            worldContainer.addBody(body)
            body.forEachFixture { worldContainer.addFixture(it) }
        }

        bodies.forEach { body -> collectContacts(body) }

        processContacts()

        bodies.forEach { body -> resolveCollisions(body) }

        bodies.forEach { body -> body.postProcess() }
    }

    internal fun processContacts() {
        currentContactSet.forEach {
            if (priorContactSet.contains(it)) contactListener.continueContact(it, fixedStep)
            else contactListener.beginContact(it, fixedStep)
        }

        priorContactSet.forEach {
            if (!currentContactSet.contains(it)) {
                contactListener.endContact(it, fixedStep)
                contactPool.free(it)
            }
        }

        priorContactSet.clear()
        priorContactSet.addAll(currentContactSet)
        currentContactSet.clear()
    }

    internal fun collectContacts(body: IBody) = body.forEachFixture { fixture ->
        if (fixture.isActive() && contactFilter.shouldProceedFiltering(fixture)) {
            fixture.getShape().getBoundingRectangle(reusableGameRect)

            worldContainer.getFixtures(
                MathUtils.floor(reusableGameRect.getX() / ppm),
                MathUtils.floor(reusableGameRect.getY() / ppm),
                MathUtils.floor(reusableGameRect.getMaxX() / ppm),
                MathUtils.floor(reusableGameRect.getMaxY() / ppm),
                reusableFixtureSet
            )

            reusableFixtureSet.forEach {
                if (it.isActive() && filterContact(fixture, it) && fixture.getShape().overlaps(it.getShape())) {
                    val contact = contactPool.fetch()
                    contact.set(fixture, it)
                    currentContactSet.add(contact)
                }
            }

            reusableFixtureSet.clear()
        }
    }

    internal fun resolveCollisions(body: IBody) {
        val bounds = body.getBounds(out1)

        worldContainer.getBodies(
            MathUtils.floor(bounds.getX() / ppm),
            MathUtils.floor(bounds.getY() / ppm),
            MathUtils.floor(bounds.getMaxX() / ppm),
            MathUtils.floor(bounds.getMaxY() / ppm),
            reusableBodySet
        )

        reusableBodySet.forEach {
            if (it != body && it.getBounds(out2).overlaps(bounds))
                collisionHandler.handleCollision(body, it)
        }

        reusableBodySet.clear()
    }
}
