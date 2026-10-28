package com.megaman.maverick.game.entities.contracts

import com.maverick.game.engine.damage.IDamageableEntity
import com.maverick.game.engine.damage.IDamagerEntity
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

        listOf(IDamageableEntity::class, IDamagerEntity::class).forEach { contract ->
            it("${contract.simpleName} is an IGameEntity") {
                IGameEntity::class.java.isAssignableFrom(contract.java) shouldBe true
            }
        }
    }
})
