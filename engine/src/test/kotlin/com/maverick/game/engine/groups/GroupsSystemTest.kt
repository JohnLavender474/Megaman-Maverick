package com.maverick.game.engine.groups

import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.MockGameEntity
import com.maverick.game.engine.common.objects.ImmutableCollection
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.components.IGameComponent
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.systems.GameSystem
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

private const val GROUP = "group"

private class ProbeSystem(private val probe: () -> Unit) : GameSystem() {
    override fun process(on: Boolean, entities: ImmutableCollection<IGameEntity>, delta: Float) = probe()
}

private class ResetProbeComponent(private val onReset: () -> Unit) : IGameComponent {
    override fun reset() = onReset()
}

private fun groupedEntity(engine: GameEngine, onSpawn: (MockGameEntity) -> Unit = {}) =
    MockGameEntity(engine, onSpawn = { entity, spawnProps ->
        val group = spawnProps.get(GROUP) as String?
        if (group != null) {
            val component = entity.getComponent(GroupsComponent::class)
                ?: GroupsComponent().also { entity.addComponent(it) }
            component.groups.add(group)
        }
        onSpawn(entity)
    })

private fun withGroup(group: String) = props(GROUP pairTo group)

class GroupsSystemTest : DescribeSpec({

    lateinit var groups: GroupsSystem
    lateinit var engine: GameEngine

    beforeEach {
        groups = GroupsSystem()
        engine = GameEngine(systems = Array<GameSystem>().apply { add(groups) })
    }

    fun members(group: String) = groups.getEntitiesInGroup(group).toList()

    describe("GroupsSystem") {

        it("adds an entity to its group after onSpawn, not during it") {
            var duringOnSpawn: List<IGameEntity>? = null
            val a = groupedEntity(engine) { duringOnSpawn = members("g") }

            a.spawn(withGroup("g"))
            members("g") shouldBe emptyList()

            engine.update(0f)

            duringOnSpawn shouldBe emptyList()
            members("g") shouldBe listOf(a)
        }

        it("keeps a destroyed entity in its group until the next update, and removes it before the component reset") {
            var duringReset: List<IGameEntity>? = null
            val a = groupedEntity(engine)
            a.addComponent(ResetProbeComponent { duringReset = members("g") })
            a.spawn(withGroup("g"))
            engine.update(0f)

            a.destroy()
            members("g") shouldBe listOf(a)

            engine.update(0f)

            duringReset shouldBe emptyList()
            members("g") shouldBe emptyList()
            a.getComponent(GroupsComponent::class)!!.groups.isEmpty shouldBe true
        }

        it("never shows an entity that is spawned and destroyed before one update") {
            val a = groupedEntity(engine)

            a.spawn(withGroup("g"))
            a.destroy()
            engine.update(0f)

            members("g") shouldBe emptyList()
        }

        it("moves an entity that is destroyed and spawned again in one frame to its new group") {
            val a = groupedEntity(engine)
            a.spawn(withGroup("g1"))
            engine.update(0f)

            a.destroy()
            a.spawn(withGroup("g2"))
            engine.update(0f)

            members("g1") shouldBe emptyList()
            members("g2") shouldBe listOf(a)
        }

        it("puts an entity that spawns again with no group in no group") {
            val a = groupedEntity(engine)
            a.spawn(withGroup("g"))
            engine.update(0f)
            a.destroy()
            engine.update(0f)

            a.spawn(Properties())
            engine.update(0f)

            members("g") shouldBe emptyList()
            groups.contains(a) shouldBe true
        }

        it("keeps the members of a group in join order") {
            val a = groupedEntity(engine)
            val b = groupedEntity(engine)
            val c = groupedEntity(engine)
            c.spawn(withGroup("g"))
            a.spawn(withGroup("g"))
            engine.update(0f)
            b.spawn(withGroup("g"))
            c.destroy()
            engine.update(0f)
            c.spawn(withGroup("g"))
            engine.update(0f)

            members("g") shouldBe listOf(a, b, c)
        }

        it("gives the same empty result for an unknown group and for a group whose last member left") {
            val empty = groups.getEntitiesInGroup("unknown")
            empty.isEmpty() shouldBe true

            val a = groupedEntity(engine)
            a.spawn(withGroup("g"))
            engine.update(0f)
            a.destroy()
            engine.update(0f)

            groups.getEntitiesInGroup("g") shouldBeSameInstanceAs empty
        }

        it("returns the same collection on each call for a group") {
            val a = groupedEntity(engine)
            a.spawn(withGroup("g"))
            engine.update(0f)

            groups.getEntitiesInGroup("g") shouldBeSameInstanceAs groups.getEntitiesInGroup("g")
        }

        it("leaves the index empty after an engine reset") {
            val a = groupedEntity(engine)
            val b = groupedEntity(engine)
            a.spawn(withGroup("g"))
            b.spawn(withGroup("h"))
            engine.update(0f)
            a.destroy()

            engine.reset()

            members("g") shouldBe emptyList()
            members("h") shouldBe emptyList()
        }

        it("leaves the index empty after an engine dispose") {
            val a = groupedEntity(engine)
            a.spawn(withGroup("g"))
            engine.update(0f)

            engine.dispose()

            members("g") shouldBe emptyList()
        }

        it("answers a lookup from another system's update with the spawns of the same frame") {
            val seen = ArrayList<List<IGameEntity>>()
            val probe = ProbeSystem { seen.add(members("g")) }
            engine = GameEngine(systems = Array<GameSystem>().apply {
                add(probe)
                add(groups)
            })
            val a = groupedEntity(engine)
            a.spawn(withGroup("g"))

            engine.update(0f)
            a.destroy()
            engine.update(0f)

            seen shouldBe listOf(listOf(a), emptyList())
        }

        it("updates the index when the system is off") {
            groups.on = false
            val a = groupedEntity(engine)
            a.spawn(withGroup("g"))

            engine.update(0f)

            members("g") shouldBe listOf(a)
        }

        it("indexes an entity in each of its groups") {
            val a = MockGameEntity(engine, onSpawn = { entity, _ ->
                entity.addComponent(GroupsComponent().also { it.groups.add("g"); it.groups.add("h") })
            })
            a.spawn(Properties())
            engine.update(0f)

            members("g") shouldBe listOf(a)
            members("h") shouldBe listOf(a)

            a.destroy()
            engine.update(0f)

            members("g") shouldBe emptyList()
            members("h") shouldBe emptyList()
        }
    }
})
