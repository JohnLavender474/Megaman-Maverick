package com.megaman.maverick.game.locks

import com.maverick.game.engine.behaviors.BehaviorsComponent
import com.maverick.game.engine.behaviors.IBehavior
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.entities.GameEntity
import com.maverick.game.engine.locks.LocksComponent
import com.maverick.game.engine.locks.lock
import com.maverick.game.engine.locks.unlock
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

private class LockedEntity : GameEntity(mockk(relaxed = true)) {
    override fun onSpawn(spawnProps: Properties) {}
    override fun onDestroy() {}
}

class MegaLocksTest : DescribeSpec({

    describe("MegaLockGroup and MegaLockReason") {
        it("have unique indexes in the locks component's range") {
            MegaLockGroup.entries.map { it.index } shouldBe MegaLockGroup.entries.indices.toList()
            MegaLockReason.entries.map { it.index } shouldBe MegaLockReason.entries.indices.toList()
            (MegaLockReason.entries.size <= LocksComponent.MAX_REASONS) shouldBe true
        }
    }

    describe("behaviorsLockHook") {
        it("disallows all behaviors on each behaviors lock, and allows them again only when the group is free") {
            val behavior = mockk<IBehavior>(relaxed = true) { every { isActive() } returns true }
            val behaviors = BehaviorsComponent().also { it.addBehavior("jump", behavior) }
            val entity = LockedEntity()
            entity.addComponent(LocksComponent(MegaLockGroup.entries.size, behaviorsLockHook(entity)))
            entity.addComponent(behaviors)

            entity.lock(MegaLockGroup.BEHAVIORS, MegaLockReason.SPAWN)
            entity.lock(MegaLockGroup.BEHAVIORS, MegaLockReason.SPIDER_WEB)

            behaviors.isBehaviorAllowed("jump") shouldBe false
            verify(exactly = 2) { behavior.end() }

            entity.unlock(MegaLockGroup.BEHAVIORS, MegaLockReason.SPAWN)
            behaviors.isBehaviorAllowed("jump") shouldBe false

            entity.unlock(MegaLockGroup.BEHAVIORS, MegaLockReason.SPIDER_WEB)
            behaviors.isBehaviorAllowed("jump") shouldBe true
        }

        it("does not touch behaviors for another group") {
            val behaviors = BehaviorsComponent().also { it.addBehavior("jump", mockk(relaxed = true)) }
            val entity = LockedEntity()
            entity.addComponent(LocksComponent(MegaLockGroup.entries.size, behaviorsLockHook(entity)))
            entity.addComponent(behaviors)

            entity.lock(MegaLockGroup.DAMAGE, MegaLockReason.SPAWN)

            behaviors.isBehaviorAllowed("jump") shouldBe true
        }
    }
})
