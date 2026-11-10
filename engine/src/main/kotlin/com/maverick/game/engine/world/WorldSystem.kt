package com.maverick.game.engine.world

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.OrderedSet
import com.maverick.game.engine.common.objects.ImmutableCollection
import com.maverick.game.engine.common.objects.Pool
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.common.shapes.IGameShape2D
import com.maverick.game.engine.diagnostics.RuntimeDiagnostics
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.systems.GameSystem
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.IBody
import com.maverick.game.engine.world.body.IFixture
import com.maverick.game.engine.world.collisions.ICollisionHandler
import com.maverick.game.engine.world.contacts.Contact
import com.maverick.game.engine.world.contacts.IContactFilter
import com.maverick.game.engine.world.contacts.IContactListener
import com.maverick.game.engine.world.container.IWorldContainer
import kotlin.math.abs

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
    var interpolation: Boolean = false
) : GameSystem(BodyComponent::class), IDrawOffsets, IWorldBodies {

    companion object {
        const val TAG = "WorldSystem"

        // pre-built so that diagnostics entry names cost no string interpolation per cycle
        private val CYCLE_NAMES = Array(50) { "cycle[$it]" }

        private fun cycleName(iteration: Int) =
            if (iteration < CYCLE_NAMES.size) CYCLE_NAMES[iteration] else "cycle[$iteration]"
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

    var iteration = 0
        private set

    private val contactPool = ContactPool()
    private var priorContactSet = OrderedSet<Contact>()
    private var currentContactSet = OrderedSet<Contact>()

    private var accumulator = 0f

    var stepSerial = 0
        private set

    private val containerBodies = Array<IBody>()

    override val bodyCount: Int
        get() = containerBodies.size

    override fun getBody(index: Int): IBody = containerBodies[index]

    private var passContainer: IWorldContainer? = null
    private val addActiveFixture: (IFixture) -> Unit = { fixture ->
        if (fixture.isActive()) passContainer!!.addFixture(fixture)
    }

    private lateinit var collectFixture: IFixture
    private lateinit var collectShape: IGameShape2D
    private val collectFixtureContacts: (IFixture) -> Unit = { fixture -> collectContacts(fixture) }
    private val collectCandidate: (IFixture, IWorldContainer) -> Boolean = { candidate, _ ->
        if (candidate.isActive() &&
            filterContact(collectFixture, candidate) &&
            collectShape.overlaps(candidate.getShape())
        ) {
            val contact = contactPool.fetch()
            contact.set(collectFixture, candidate)
            if (!currentContactSet.add(contact))
                contactPool.free(contact)
        }
        true
    }

    private lateinit var resolveBody: IBody
    private lateinit var resolveBounds: GameRectangle
    private val resolveCandidate: (IBody, IWorldContainer) -> Boolean = { candidate, _ ->
        if (candidate != resolveBody) {
            val candidateBounds = candidate.getBounds(out2)
            if (candidateBounds.overlaps(resolveBounds))
                collisionHandler.handleCollision(resolveBody, candidate)
        }
        true
    }
    var alpha = 1f
        private set

    private val bodyArray = Array<IBody>()
    private val out1 = GameRectangle()
    private val out2 = GameRectangle()

    override fun process(on: Boolean, entities: ImmutableCollection<IGameEntity>, delta: Float) {
        if (!on) return

        diagnostics?.beginEntry("WorldSystem")

        iteration = 0
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
            while (accumulator >= fixedStep) {
                accumulator -= fixedStepScaled
                iteration++

                stepSerial++

                diagnostics?.beginEntry(cycleName(iteration))
                cycle(bodyArray, fixedStep, worldContainer)
                diagnostics?.endEntry()

                if (iteration >= maxIterations) {
                    accumulator = 0f  // drop leftover time — physics lags, but no spiral
                    break
                }
            }

            diagnostics?.beginEntry("updateWorldContainer")
            worldContainer.clear()
            containerBodies.clear()
            passContainer = worldContainer
            bodyArray.forEach { body ->
                if (body.physics.collisionOn) {
                    worldContainer.addBody(body)
                    containerBodies.add(body)
                }
                body.forEachFixture(addActiveFixture)
            }
            diagnostics?.endEntry()

            bodyArray.clear()
        }

        val fixedStepScaled = fixedStep / fixedStepScalar
        alpha = if (iteration >= maxIterations) 1f
        else ((accumulator - (fixedStep - fixedStepScaled)) / fixedStepScaled).coerceIn(0f, 1f)

        diagnostics?.endEntry()
    }

    override fun getDrawOffset(body: IBody, out: Vector2): Vector2 {
        if (!interpolation || body.stepSerial != stepSerial) return out.setZero()
        body.getStepMove(out)
        // TODO: a teleport inside a step that moves less than 1 tile and does not call restartStep is drawn as a
        //  slide for 1 frame; PipePortal calls it, other teleports are outside the steps (item 134, 134b audit)
        if (abs(out.x) > ppm || abs(out.y) > ppm) return out.setZero()
        return out.scl(alpha - 1f)
    }

    override fun reset() {
        super.reset()
        accumulator = 0f
        alpha = 1f
        containerBodies.clear()

        priorContactSet.forEach { contactPool.free(it) }
        currentContactSet.forEach { contactPool.free(it) }
        priorContactSet.clear()
        currentContactSet.clear()

        worldContainerSupplier()?.clear()
    }

    internal fun filterContact(fixture1: IFixture, fixture2: IFixture) = contactFilter.filter(fixture1, fixture2)

    internal fun cycle(bodies: Array<IBody>, delta: Float, worldContainer: IWorldContainer) {
        diagnostics?.beginEntry("preProcess")
        bodies.forEach { body ->
            body.beginStep(stepSerial)
            body.preProcess()
        }
        diagnostics?.endEntry()

        worldContainer.clear()

        diagnostics?.beginEntry("bodyProcess")

        diagnostics?.beginEntry("process")
        bodies.forEach { body -> body.process(delta) }
        diagnostics?.endEntry()

        diagnostics?.beginEntry("addBody")
        bodies.forEach { body -> worldContainer.addBody(body) }
        diagnostics?.endEntry()

        diagnostics?.beginEntry("addFixtures")
        passContainer = worldContainer
        bodies.forEach { body -> body.forEachFixture(addActiveFixture) }
        diagnostics?.endEntry()

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
        bodies.forEach { body ->
            body.postProcess()
            body.endStep()
        }
        diagnostics?.endEntry()
    }

    internal fun processContacts(delta: Float) {
        diagnostics?.beginEntry("begin/continue contacts")
        currentContactSet.forEach {
            if (priorContactSet.contains(it)) contactListener.continueContact(it.fixture1, it.fixture2, delta)
            else contactListener.beginContact(it.fixture1, it.fixture2, delta)
        }
        diagnostics?.endEntry()

        diagnostics?.beginEntry("end contacts")
        priorContactSet.forEach {
            if (!currentContactSet.contains(it)) contactListener.endContact(it.fixture1, it.fixture2, delta)
            contactPool.free(it)
        }
        diagnostics?.endEntry()

        diagnostics?.beginEntry("reset contact sets")
        priorContactSet.clear()
        priorContactSet.addAll(currentContactSet)
        currentContactSet.clear()
        diagnostics?.endEntry()
    }

    internal fun collectContacts(body: IBody, worldContainer: IWorldContainer) {
        passContainer = worldContainer
        body.forEachFixture(collectFixtureContacts)
    }

    private fun collectContacts(fixture: IFixture) {
        if (!fixture.isActive() || !contactFilter.shouldProceedFiltering(fixture)) return

        val shape = fixture.getShape()
        collectFixture = fixture
        collectShape = shape

        val bounds = shape.getBoundingRectangle(out1)
        passContainer!!.forEachFixture(
            MathUtils.floor(bounds.getX() / ppm),
            MathUtils.floor(bounds.getY() / ppm),
            MathUtils.ceil(bounds.getMaxX() / ppm),
            MathUtils.ceil(bounds.getMaxY() / ppm),
            collectCandidate
        )
    }

    internal fun resolveCollisions(body: IBody, worldContainer: IWorldContainer) {
        // Static bodies never move, so they never need to be resolved OUT of anything. Any static-vs-dynamic overlap is
        // already handled from the dynamic body's side (dynamic bodies query the container, which includes static
        // bodies), and static-vs-static is meaningless. Skipping statics here removes a broad-phase grid query per
        // active static body every cycle -- a large saving in block-dense rooms.
        if (body.type == BodyType.STATIC) return

        resolveBody = body
        resolveBounds = body.getBounds(out1)
        worldContainer.forEachBody(
            MathUtils.floor(resolveBounds.getX() / ppm),
            MathUtils.floor(resolveBounds.getY() / ppm),
            MathUtils.ceil(resolveBounds.getMaxX() / ppm),
            MathUtils.ceil(resolveBounds.getMaxY() / ppm),
            resolveCandidate
        )
    }
}
