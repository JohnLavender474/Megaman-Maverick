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
    var diagnostics: RuntimeDiagnostics? = null,
    var batchQueryCellAreaThreshold: Int? = null
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

        diagnostics?.beginEntry("WorldSystem")

        accumulator += delta

        if (accumulator >= fixedStep) {
            diagnostics?.beginEntry("buildBodyArray")
            entities.forEach { entity ->
                try {
                    val component = entity.getComponent(BodyComponent::class)!!
                    if (component.doUpdate()) reusableBodyArray.add(component.body)
                } catch (e: Exception) {
                    throw Exception("Exception occured while processing world for entity: $entity", e)
                }
            }
            diagnostics?.endEntry()

            var iterations = 0
            while (accumulator >= fixedStep) {
                accumulator -= fixedStep / fixedStepScalar
                iterations++

                diagnostics?.beginEntry("cycle[$iterations]")
                cycle(reusableBodyArray, fixedStep)
                diagnostics?.endEntry()

                if (iterations >= maxIterations) {
                    accumulator = 0f  // drop leftover time — physics lags, but no spiral
                    break
                }
            }

            diagnostics?.beginEntry("updateWorldContainer")
            worldContainer.clear()
            reusableBodyArray.forEach { body ->
                if (body.physics.collisionOn) worldContainer.addBody(body)
                body.forEachFixture { if (it.isActive()) worldContainer.addFixture(it) }
            }
            diagnostics?.endEntry()

            reusableBodyArray.clear()
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

    internal fun cycle(bodies: Array<IBody>, delta: Float) {
        diagnostics?.beginEntry("preProcess")
        bodies.forEach { body -> body.preProcess() }
        diagnostics?.endEntry()

        worldContainer.clear()

        diagnostics?.beginEntry("bodyProcess")
        bodies.forEach { body ->
            body.process(delta)
            worldContainer.addBody(body)
            body.forEachFixture { worldContainer.addFixture(it) }
        }
        diagnostics?.endEntry()

        diagnostics?.beginEntry("collectContacts")
        bodies.forEach { body -> collectContacts(body) }
        diagnostics?.endEntry()

        diagnostics?.beginEntry("processContacts")
        processContacts(delta)
        diagnostics?.endEntry()

        diagnostics?.beginEntry("resolveCollisions")
        bodies.forEach { body -> resolveCollisions(body) }
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
            if (!currentContactSet.contains(it)) {
                contactListener.endContact(it, delta)
                contactPool.free(it)
            }
        }
        diagnostics?.endEntry()

        diagnostics?.beginEntry("reset contact sets")
        priorContactSet.clear()
        priorContactSet.addAll(currentContactSet)
        currentContactSet.clear()
        diagnostics?.endEntry()
    }

    internal fun collectContacts(body: IBody) =
        collectContactsUnionFixturesBBox(body) || collectContactsPerFixtureBasis(body)

    internal fun collectContactsUnionFixturesBBox(body: IBody): Boolean {
        val batchThreshold = batchQueryCellAreaThreshold ?: return false

        var unionMinX = Int.MAX_VALUE
        var unionMinY = Int.MAX_VALUE
        var unionMaxX = Int.MIN_VALUE
        var unionMaxY = Int.MIN_VALUE

        var qualifyingCount = 0

        body.forEachFixture { fixture ->
            if (fixture.isActive()) {
                fixture.getShape().getBoundingRectangle(reusableGameRect)

                val x1 = MathUtils.floor(reusableGameRect.getX() / ppm)
                val y1 = MathUtils.floor(reusableGameRect.getY() / ppm)
                val x2 = MathUtils.ceil(reusableGameRect.getMaxX() / ppm)
                val y2 = MathUtils.ceil(reusableGameRect.getMaxY() / ppm)

                if (x1 < unionMinX) unionMinX = x1
                if (y1 < unionMinY) unionMinY = y1
                if (x2 > unionMaxX) unionMaxX = x2
                if (y2 > unionMaxY) unionMaxY = y2

                qualifyingCount++
            }
        }

        if (qualifyingCount == 0) return false

        val unionCellArea = (unionMaxX - unionMinX + 1) * (unionMaxY - unionMinY + 1)
        if (qualifyingCount > 1 && unionCellArea <= batchThreshold) {
            diagnostics?.beginEntry("collect contacts via union bounding box")

            worldContainer.getFixtures(unionMinX, unionMinY, unionMaxX, unionMaxY, reusableFixtureSet)

            body.forEachFixture { fixture ->
                if (fixture.isActive() && contactFilter.shouldProceedFiltering(fixture)) {
                    reusableFixtureSet.forEach { candidate ->
                        if (candidate.isActive() && filterContact(fixture, candidate) &&
                            fixture.getShape().overlaps(candidate.getShape())
                        ) {
                            val contact = contactPool.fetch()
                            contact.set(fixture, candidate)
                            currentContactSet.add(contact)
                        }
                    }
                }
            }

            reusableFixtureSet.clear()

            diagnostics?.endEntry()
        }

        return true
    }

    internal fun collectContactsPerFixtureBasis(body: IBody): Boolean {
        diagnostics?.beginEntry("collect contacts on per-fixture basis")

        body.forEachFixture { fixture ->
            if (fixture.isActive() && contactFilter.shouldProceedFiltering(fixture)) {
                fixture.getShape().getBoundingRectangle(reusableGameRect)
                worldContainer.getFixtures(
                    MathUtils.floor(reusableGameRect.getX() / ppm),
                    MathUtils.floor(reusableGameRect.getY() / ppm),
                    MathUtils.ceil(reusableGameRect.getMaxX() / ppm),
                    MathUtils.ceil(reusableGameRect.getMaxY() / ppm),
                    reusableFixtureSet
                )

                reusableFixtureSet.forEach { candidate ->
                    if (candidate.isActive() && filterContact(fixture, candidate) &&
                        fixture.getShape().overlaps(candidate.getShape())
                    ) {
                        val contact = contactPool.fetch()
                        contact.set(fixture, candidate)
                        currentContactSet.add(contact)
                    }
                }

                reusableFixtureSet.clear()
            }
        }

        diagnostics?.endEntry()

        return true
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
