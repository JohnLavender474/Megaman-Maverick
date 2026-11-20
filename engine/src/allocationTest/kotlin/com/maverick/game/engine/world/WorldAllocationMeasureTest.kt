package com.maverick.game.engine.world

import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.entities.GameEntity
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.maverick.game.engine.world.body.IBody
import com.maverick.game.engine.world.body.IFixture
import com.maverick.game.engine.world.body.IFixtureType
import com.maverick.game.engine.world.collisions.ICollisionHandler
import com.maverick.game.engine.world.container.SimpleGridWorldContainer
import com.maverick.game.engine.world.pathfinding.BodyCellsSnapshot
import com.maverick.game.engine.world.contacts.IContactFilter
import com.maverick.game.engine.world.contacts.IContactListener
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.longs.shouldBeLessThan
import java.lang.management.ManagementFactory

private enum class MeasureFixtureType : IFixtureType {
    TYPE;

    override val index: Int
        get() = ordinal
}

private class MeasureEntity(engine: GameEngine) : GameEntity(engine) {
    override fun init(vararg params: Any) {}
    override fun onSpawn(spawnProps: com.maverick.game.engine.common.objects.Properties) {}
    override fun onDestroy() {}
}

class WorldAllocationMeasureTest : DescribeSpec({

    val threads = ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean

    fun allocatedBytes() = threads.getThreadAllocatedBytes(Thread.currentThread().id)

    fun worldBytesPerFrame(wrap: WorldWrap?, speedX: Float, wrapping: HashSet<IGameEntity>): Long {
        val ppm = 32
        val container = SimpleGridWorldContainer(ppm)
        val world = WorldSystem(
            ppm = ppm,
            fixedStep = 1f / 150f,
            worldContainerSupplier = { container },
            contactListener = object : IContactListener {
                override fun beginContact(fixture1: IFixture, fixture2: IFixture, delta: Float) {}
                override fun continueContact(fixture1: IFixture, fixture2: IFixture, delta: Float) {}
                override fun endContact(fixture1: IFixture, fixture2: IFixture, delta: Float) {}
            },
            collisionHandler = object : ICollisionHandler {
                override fun handleCollision(body1: IBody, body2: IBody) = false
            },
            contactFilter = object : IContactFilter {
                override fun shouldProceedFiltering(fixture: IFixture) = true
                override fun filter(fixture1: IFixture, fixture2: IFixture) = true
            },
            wrap = wrap
        )
        val engine = GameEngine()
        val entities = ArrayList<IGameEntity>()
        for (i in 0 until 200) {
            val body = Body(if (i % 2 == 0) BodyType.STATIC else BodyType.DYNAMIC)
            body.setSize(ppm.toFloat(), ppm.toFloat())
            body.setPosition((i % 20) * ppm * 0.8f, (i / 20) * ppm * 0.8f)
            body.physics.gravityOn = false
            if (body.type == BodyType.DYNAMIC) body.physics.velocity.set(speedX, 0.5f)
            body.addFixture(Fixture(body, MeasureFixtureType.TYPE, GameRectangle().setSize(ppm.toFloat())))
            body.addFixture(Fixture(body, MeasureFixtureType.TYPE, GameRectangle().setSize(ppm * 0.5f)))
            val entity = MeasureEntity(engine).apply { addComponent(BodyComponent(body)) }
            if (body.type == BodyType.DYNAMIC) wrapping.add(entity)
            entities.add(entity)
        }
        world.on = true
        entities.forEach { world.add(it) }
        repeat(3000) { world.update(1f / 60f) }
        val frames = 1000
        val before = allocatedBytes()
        repeat(frames) { world.update(1f / 60f) }
        return (allocatedBytes() - before) / frames
    }

    it("world steps allocate almost nothing per frame after warm-up") {
        val perFrame = worldBytesPerFrame(wrap = null, speedX = 1f, wrapping = HashSet())
        println("WORLD_ALLOC bytes per frame: $perFrame")
        perFrame shouldBeLessThan 1024L
    }

    it("world steps with a wrap region and bodies that cross its edges allocate almost nothing per frame") {
        val wrapping = HashSet<IGameEntity>()
        val region = GameRectangle(64f, 0f, 384f, 448f)
        val wrap = WorldWrap(
            region = { out -> out.set(region) },
            wraps = { entity -> wrapping.contains(entity) },
            maxWraps = { _ -> WorldWrap.UNLIMITED_WRAPS }
        )
        val perFrame = worldBytesPerFrame(wrap = wrap, speedX = 300f, wrapping = wrapping)
        println("WORLD_WRAP_ALLOC bytes per frame: $perFrame")
        perFrame shouldBeLessThan 1024L
    }

    it("a pathfinding snapshot fill allocates almost nothing after the first fill") {
        val ppm = 32
        val bodies = ArrayList<IBody>()
        for (i in 0 until 2000) bodies.add(Body(BodyType.STATIC, (i % 100) * ppm.toFloat(), (i / 100) * ppm.toFloat(), 30f, 30f))
        val source = object : IWorldBodies {
            override val bodyCount get() = bodies.size
            override fun getBody(index: Int) = bodies[index]
        }
        val snapshot = BodyCellsSnapshot(ppm)

        repeat(100) { snapshot.fill(source) }
        val before = allocatedBytes()
        repeat(100) { snapshot.fill(source) }
        val perFill = (allocatedBytes() - before) / 100

        println("SNAPSHOT_ALLOC bytes per fill: $perFill")
        perFill shouldBeLessThan 1024L
    }
})
