package com.megaman.maverick.game.entities.contracts

import com.maverick.game.engine.damage.IDamageable
import com.maverick.game.engine.damage.IDamager
import com.maverick.game.engine.entities.IGameEntity
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class EntityContractsTest : DescribeSpec({

    describe("the entity contracts") {
        listOf(
            IFreezableEntity::class,
            IFreezerEntity::class,
            IFireEntity::class,
            IFireableEntity::class,
            IScalableGravityEntity::class,
            ILightSource::class,
            IBossListener::class,
            IHazard::class,
            IOwnable::class,
            ITeleporterEntity::class,
            ItemEntity::class,
            IGateTrigger::class
        ).forEach { contract ->
            it("${contract.simpleName} is an IMegaGameEntity") {
                IMegaGameEntity::class.java.isAssignableFrom(contract.java) shouldBe true
            }
        }

        listOf(IDamageable::class, IDamager::class).forEach { contract ->
            it("${contract.simpleName} is an IGameEntity") {
                IGameEntity::class.java.isAssignableFrom(contract.java) shouldBe true
            }
        }
    }
})
