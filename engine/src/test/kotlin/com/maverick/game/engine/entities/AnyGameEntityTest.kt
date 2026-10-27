package com.maverick.game.engine.entities

import com.badlogic.gdx.utils.OrderedMap
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.components.IGameComponent
import com.maverick.game.engine.entities.EntityExistenceState.DESTROY_QUEUED
import com.maverick.game.engine.entities.EntityExistenceState.LIVE
import com.maverick.game.engine.entities.EntityExistenceState.UNSPAWNED
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlin.reflect.KClass
import kotlin.reflect.cast

private class PlainEntity(private val engine: GameEngine) : IGameEntity {

    override val components = OrderedMap<KClass<out IGameComponent>, IGameComponent>()
    override val properties = Properties()
    override var existenceState = UNSPAWNED
    override var initialized = false

    override fun init(vararg params: Any) {}

    override fun canSpawn(spawnProps: Properties) = true

    override fun onSpawn(spawnProps: Properties) {}

    override fun onDestroy() {}

    override fun destroy() = engine.destroy(this)

    override fun <C : IGameComponent> getComponent(key: KClass<C>): C? = components[key]?.let { key.cast(it) }

    override fun addComponent(c: IGameComponent) {
        components.put(c::class, c)
    }

    override fun hasComponent(key: KClass<out IGameComponent>) = components.containsKey(key)

    override fun removeComponent(key: KClass<out IGameComponent>) {
        components.remove(key)
    }

    override fun clearComponents() = components.clear()
}

private class BaseEntity(engine: GameEngine) : GameEntity(engine) {
    override fun onSpawn(spawnProps: Properties) {}
    override fun onDestroy() {}
}

private class OverridingEntity(engine: GameEngine) : GameEntity(engine) {
    var destroyCalls = 0

    override fun onSpawn(spawnProps: Properties) {}
    override fun onDestroy() {}

    override fun destroy(): Boolean {
        destroyCalls++
        return super.destroy()
    }
}

class AnyGameEntityTest : DescribeSpec({

    lateinit var engine: GameEngine

    beforeEach { engine = GameEngine() }

    describe("an IGameEntity that is not a GameEntity") {
        it("goes through spawn, destroy, respawn and reset with the same states as a GameEntity") {
            val plain = PlainEntity(engine)
            val base = BaseEntity(engine)
            val both = listOf<IGameEntity>(plain, base)
            val states = ArrayList<Pair<EntityExistenceState, EntityExistenceState>>()
            fun record() = states.add(plain.existenceState to base.existenceState)

            both.forEach { engine.spawn(it) shouldBe true }
            record()
            engine.update(0f)
            record()
            both.forEach { engine.destroy(it) shouldBe true }
            record()
            both.forEach { engine.spawn(it) shouldBe true }
            record()
            engine.update(0f)
            record()
            both.forEach { it.destroy() shouldBe true }
            record()
            engine.update(0f)
            record()
            both.forEach { engine.spawn(it) shouldBe true }
            engine.update(0f)
            record()
            engine.reset()
            record()

            states.forEach { (plainState, baseState) -> plainState shouldBe baseState }
            states.last().first shouldBe UNSPAWNED
        }
    }

    describe("destroy through an IGameEntity reference") {
        it("returns true for a live entity and false for an unspawned or destroy-queued entity") {
            val entity: IGameEntity = BaseEntity(engine)

            entity.destroy() shouldBe false
            engine.spawn(entity)
            engine.update(0f)
            entity.existenceState shouldBe LIVE
            entity.destroy() shouldBe true
            entity.existenceState shouldBe DESTROY_QUEUED
            entity.destroy() shouldBe false
        }

        it("runs the override of a subclass") {
            val overriding = OverridingEntity(engine)
            val entity: IGameEntity = overriding
            engine.spawn(entity)
            engine.update(0f)

            entity.destroy()

            overriding.destroyCalls shouldBe 1
        }
    }
})
