package com.megaman.maverick.game.entities.state

import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.components.MissingComponentException
import com.maverick.game.engine.entities.GameEntity
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.contracts.ITeleporterEntity
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.mockk

private class TeleportedEntity : GameEntity(mockk(relaxed = true)) {
    override fun onSpawn(spawnProps: Properties) {}
    override fun onDestroy() {}
}

class TeleportsTest : DescribeSpec({

    describe("teleportable") {
        it("is not teleporting without the component") {
            val entity = TeleportedEntity()

            entity.teleporter shouldBe null
            entity.teleporting shouldBe false
        }

        it("calls the start hook before it sets the teleporter, and the end hook before it clears it") {
            val entity = TeleportedEntity().also { it.addComponent(TeleportableComponent()) }
            val teleporter = mockk<ITeleporterEntity>()
            val seen = ArrayList<String>()
            entity.putProperty(ConstKeys.ON_TELEPORT_START, { t: ITeleporterEntity ->
                t shouldBeSameInstanceAs teleporter
                seen.add("start teleporting=${entity.teleporting}")
            })
            entity.putProperty(ConstKeys.ON_TELEPORT_END, { seen.add("end teleporting=${entity.teleporting}") })

            entity.beginTeleport(teleporter)

            entity.teleporting shouldBe true
            entity.teleporter shouldBeSameInstanceAs teleporter

            entity.endTeleport()

            entity.teleporting shouldBe false
            seen shouldBe listOf("start teleporting=false", "end teleporting=true")
        }

        it("marks an entity that has no hooks") {
            val entity = TeleportedEntity().also { it.addComponent(TeleportableComponent()) }

            entity.beginTeleport(mockk())

            entity.teleporting shouldBe true
        }

        it("fails to begin or end a teleport for an entity without the component") {
            shouldThrow<MissingComponentException> { TeleportedEntity().beginTeleport(mockk()) }
            shouldThrow<MissingComponentException> { TeleportedEntity().endTeleport() }
        }

        it("clears the teleporter on a reset of the component") {
            val component = TeleportableComponent(mockk())

            component.reset()

            component.teleporter shouldBe null
        }
    }
})
