package com.megaman.maverick.game.entities

import com.megaman.maverick.game.testServices
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.groups.GroupsComponent
import com.maverick.game.engine.groups.GroupsSystem
import com.maverick.game.engine.systems.GameSystem
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.levels.LevelState
import com.megaman.maverick.game.state.GameState
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

private class GroupsTestEntity(game: MegaServices, private val callsSuperOnDestroy: Boolean = true) :
    MegaGameEntity(game) {

    override fun getType() = EntityType.OTHER

    override fun onDestroy() {
        if (callsSuperOnDestroy) super.onDestroy()
    }
}

class EntityGroupsTest : DescribeSpec({

    lateinit var engine: GameEngine
    lateinit var groups: GroupsSystem
    lateinit var game: MegaServices

    beforeEach {
        groups = GroupsSystem()
        engine = GameEngine(systems = Array<GameSystem>().apply { add(groups) })
        game = testServices(engine = engine)
    }

    fun members(group: String) = groups.getEntitiesInGroup(group).toList()

    describe("MegaGameEntity groups") {

        it("puts the group spawn property into a GroupsComponent") {
            val entity = GroupsTestEntity(game)

            entity.spawn(props(ConstKeys.GROUP pairTo "entity_groups_a"))
            engine.update(0f)

            entity.getComponent(GroupsComponent::class)!!.groups.toList() shouldBe listOf("entity_groups_a")
            members("entity_groups_a") shouldBe listOf(entity)
        }

        it("adds no GroupsComponent when the spawn has no group") {
            val entity = GroupsTestEntity(game)

            entity.spawn(Properties())
            engine.update(0f)

            entity.hasComponent(GroupsComponent::class) shouldBe false
        }

        it("has an empty GroupsComponent when it spawns again with no group") {
            val entity = GroupsTestEntity(game)
            entity.spawn(props(ConstKeys.GROUP pairTo "entity_groups_b"))
            engine.update(0f)
            entity.destroy()
            engine.update(0f)

            entity.spawn(Properties())
            engine.update(0f)

            entity.getComponent(GroupsComponent::class)!!.groups.isEmpty shouldBe true
            members("entity_groups_b") shouldBe emptyList()
        }

        it("takes an entity that skips super.onDestroy() out of its group at destroy") {
            val entity = GroupsTestEntity(game, callsSuperOnDestroy = false)
            entity.spawn(props(ConstKeys.GROUP pairTo "entity_groups_c"))
            engine.update(0f)

            entity.destroy()
            engine.update(0f)

            members("entity_groups_c") shouldBe emptyList()
        }
    }
})
