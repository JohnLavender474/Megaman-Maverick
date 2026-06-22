package com.mega.game.engine.world

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.OrderedSet
import com.mega.game.engine.common.objects.ImmutableCollection
import com.mega.game.engine.common.objects.Pool
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.shapes.IGameShape2D
import com.mega.game.engine.diagnostics.RuntimeDiagnostics
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
    var ppm: Int,
    var fixedStep: Float,
    var worldContainerSupplier: () -> IWorldContainer?,
    var contactListener: IContactListener,
    var collisionHandler: ICollisionHandler,
    var contactFilter: IContactFilter,
    var fixedStepScalar: Float = 1f,
    var maxIterations: Int = Int.MAX_VALUE, // max iters cap does not account for fixed step scalar
    var diagnostics: RuntimeDiagnostics? = null
) : GameSystem(BodyComponent::class) {

    companion object {
        const val TAG = "WorldSystem"
    }

    init {
        if (fixedStepScalar <= 0f) throw IllegalArgumentException("Value of fixedStepScalar must be greater than 0")
    }

    private class ContactPool {

        private class DummyFixture : IFixture {
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

        private val dummyFixture1 = DummyFixture()
        private val dummyFixture2 = DummyFixture()

        private val pool = Pool(
            supplier = { Contact(dummyFixture1, dummyFixture2) }
        )

        fun fetch() = pool.fetch()

        fun free(contact: Contact) {
            contact.set(dummyFixture1, dummyFixture2)
            pool.free(contact)
        }
    }

    private val contactPool = ContactPool()
    private var priorContactSet = OrderedSet<Contact>()
    private var currentContactSet = OrderedSet<Contact>()

    private var accumulator = 0f

    private val bodyArray = Array<IBody>()
    private val out1 = GameRectangle()
    private val out2 = GameRectangle()

    override fun process(on: Boolean, entities: ImmutableCollection<IGameEntity>, delta: Float) {
        if (!on) return

        diagnostics?.beginEntry("WorldSystem")

        accumulator += delta

        if (accumulator >= fixedStep) {
            val worldContainer = worldContainerSupplier()!!

            diagnostics?.beginEntry("buildBodyArray")
            entities.forEach { entity ->
                try {
                    val component = entity.getComponent(BodyComponent::class)!!
                    if (component.doUpdate()) bodyArray.add(component.body)
                } catch (e: Exception) {
                    throw Exception("Exception occurred while processing world for entity: $entity", e)
                }
            }
            diagnostics?.endEntry()

            val fixedStepScaled = fixedStep / fixedStepScalar
            var iterations = 0
            while (accumulator >= fixedStep) {
                accumulator -= fixedStepScaled
                iterations++

                diagnostics?.beginEntry("cycle[$iterations]")
                cycle(bodyArray, fixedStep, worldContainer)
                diagnostics?.endEntry()

                if (iterations >= maxIterations) {
                    accumulator = 0f  // drop leftover time — physics lags, but no spiral
                    break
                }
            }

            diagnostics?.beginEntry("updateWorldContainer")
            worldContainer.clear()
            bodyArray.forEach { body ->
                if (body.physics.collisionOn) worldContainer.addBody(body)
                body.forEachFixture { if (it.isActive()) worldContainer.addFixture(it) }
            }
            diagnostics?.endEntry()

            bodyArray.clear()
        }

        diagnostics?.endEntry()
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

    internal fun cycle(bodies: Array<IBody>, delta: Float, worldContainer: IWorldContainer) {
        diagnostics?.beginEntry("preProcess")
        bodies.forEach { body -> body.preProcess() }
        diagnostics?.endEntry()

        worldContainer.clear()

        diagnostics?.beginEntry("bodyProcess")
        bodies.forEach { body ->
            body.process(delta)
            worldContainer.addBody(body)
            body.forEachFixture { fixture ->
                if (fixture.isActive()) worldContainer.addFixture(fixture)
            }
        }
        diagnostics?.endEntry()

        diagnostics?.beginEntry("collectContacts")
        bodies.forEach { body -> collectContacts(body, worldContainer) }
        diagnostics?.endEntry()

        diagnostics?.beginEntry("processContacts")
        processContacts(delta)
        diagnostics?.endEntry()

        diagnostics?.beginEntry("resolveCollisions")
        bodies.forEach { body -> resolveCollisions(body, worldContainer) }
        diagnostics?.endEntry()

        diagnostics?.beginEntry("postProcess")
        bodies.forEach { body -> body.postProcess() }
        diagnostics?.endEntry()
    }

    internal fun processContacts(delta: Float) {
        diagnostics?.beginEntry("begin/continue contacts")
        currentContactSet.forEach {
            if (priorContactSet.contains(it)) contactListener.continueContact(it, delta)
            else contactListener.beginContact(it, delta)
        }
        diagnostics?.endEntry()

        diagnostics?.beginEntry("end contacts")
        priorContactSet.forEach {
            if (!currentContactSet.contains(it)) contactListener.endContact(it, delta)
            contactPool.free(it)
        }
        diagnostics?.endEntry()

        diagnostics?.beginEntry("reset contact sets")
        priorContactSet.clear()
        priorContactSet.addAll(currentContactSet)
        currentContactSet.clear()
        diagnostics?.endEntry()
    }

    internal fun collectContacts(body: IBody, worldContainer: IWorldContainer) = body.forEachFixture { fixture ->
        if (!fixture.isActive() || !contactFilter.shouldProceedFiltering(fixture)) return@forEachFixture

        val bounds = fixture.getShape().getBoundingRectangle(out1)
        worldContainer.forEachFixture(
            MathUtils.floor(bounds.getX() / ppm),
            MathUtils.floor(bounds.getY() / ppm),
            MathUtils.ceil(bounds.getMaxX() / ppm),
            MathUtils.ceil(bounds.getMaxY() / ppm)
        ) { candidate, _ ->
            if (candidate.isActive() &&
                filterContact(fixture, candidate) &&
                fixture.getShape().overlaps(candidate.getShape())
            ) {
                val contact = contactPool.fetch()
                contact.set(fixture, candidate)
                if (!currentContactSet.add(contact))
                    contactPool.free(contact)
            }
            return@forEachFixture true
        }
    }

    internal fun resolveCollisions(body: IBody, worldContainer: IWorldContainer) {
        val bodyBounds = body.getBounds(out1)
        worldContainer.forEachBody(
            MathUtils.floor(bodyBounds.getX() / ppm),
            MathUtils.floor(bodyBounds.getY() / ppm),
            MathUtils.ceil(bodyBounds.getMaxX() / ppm),
            MathUtils.ceil(bodyBounds.getMaxY() / ppm),
        ) { candidate, _ ->
            if (candidate == body) return@forEachBody true
            val candidateBounds = candidate.getBounds(out2)
            if (candidateBounds.overlaps(bodyBounds))
                collisionHandler.handleCollision(body, candidate)
            return@forEachBody true
        }
    }
}
