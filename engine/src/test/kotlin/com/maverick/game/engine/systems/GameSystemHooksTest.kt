package com.maverick.game.engine.systems

import com.maverick.game.engine.MockGameEntity
import com.maverick.game.engine.common.objects.ImmutableCollection
import com.maverick.game.engine.components.IGameComponent
import com.maverick.game.engine.entities.IGameEntity
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

private class HookComponent : IGameComponent

private class HookSystem : GameSystem(HookComponent::class) {

    val calls = ArrayList<String>()
    val members = ArrayList<IGameEntity>()
    var duringProcess: (() -> Unit)? = null

    override fun process(on: Boolean, entities: ImmutableCollection<IGameEntity>, delta: Float) {
        members.clear()
        members.addAll(entities)
        duringProcess?.invoke()
        duringProcess = null
    }

    override fun onEntityJoined(entity: IGameEntity) {
        calls.add("join:${(entity as MockGameEntity).name}")
    }

    override fun onEntityLeft(entity: IGameEntity) {
        calls.add("leave:${(entity as MockGameEntity).name}")
    }
}

private val MockGameEntity.name: String
    get() = getProperty("name") as String

private fun entity(name: String, qualifies: Boolean = true) = MockGameEntity().also {
    it.putProperty("name", name)
    if (qualifies) it.addComponent(HookComponent())
}

class GameSystemHooksTest : DescribeSpec({

    describe("GameSystem join and leave hooks") {

        it("fires the join hook once for an add when the system is not updating") {
            val system = HookSystem()
            val a = entity("a")

            system.add(a) shouldBe 1
            system.add(a) shouldBe 1

            system.calls shouldBe listOf("join:a")
            system.contains(a) shouldBe true
        }

        it("fires nothing for an entity that does not qualify") {
            val system = HookSystem()
            val a = entity("a", qualifies = false)

            system.add(a) shouldBe 0
            system.remove(a) shouldBe 0

            system.calls shouldBe emptyList()
        }

        it("fires the leave hook once for a remove of a member when the system is not updating") {
            val system = HookSystem()
            val a = entity("a")
            system.add(a)

            system.remove(a) shouldBe 1
            system.remove(a) shouldBe 0

            system.calls shouldBe listOf("join:a", "leave:a")
            system.contains(a) shouldBe false
        }

        it("fires the hooks of deferred adds and removes at the next update, in call order") {
            val system = HookSystem()
            val a = entity("a")
            val b = entity("b")
            val c = entity("c")
            system.add(a)
            system.add(b)
            system.duringProcess = {
                system.add(c) shouldBe 2
                system.add(a) shouldBe 2
                system.remove(a) shouldBe 2
                system.remove(c) shouldBe 0
            }

            system.update(0f)
            system.calls shouldBe listOf("join:a", "join:b")

            system.update(0f)
            system.calls shouldBe listOf("join:a", "join:b", "join:c", "leave:a")
            system.members shouldBe listOf(b, c)
        }

        it("fires the leave hook for a member that no longer qualifies") {
            val system = HookSystem()
            val a = entity("a")
            val b = entity("b")
            system.add(a)
            system.add(b)

            a.removeComponent(HookComponent::class)
            system.update(0f)

            system.calls shouldBe listOf("join:a", "join:b", "leave:a")
            system.members shouldBe listOf(b)
            system.contains(a) shouldBe false
        }

        it("fires the leave hook for each member in member order at reset") {
            val system = HookSystem()
            val a = entity("a")
            val b = entity("b")
            val c = entity("c")
            system.add(b)
            system.add(a)
            system.add(c)

            system.reset()

            system.calls shouldBe listOf("join:b", "join:a", "join:c", "leave:b", "leave:a", "leave:c")
            system.contains(a) shouldBe false
            system.contains(b) shouldBe false
            system.contains(c) shouldBe false
        }

        it("keeps the members in join order") {
            val system = HookSystem()
            val a = entity("a")
            val b = entity("b")
            val c = entity("c")
            system.add(c)
            system.add(a)
            system.add(b)
            system.remove(a)
            system.add(a)

            system.update(0f)

            system.members shouldBe listOf(c, b, a)
        }
    }
})
