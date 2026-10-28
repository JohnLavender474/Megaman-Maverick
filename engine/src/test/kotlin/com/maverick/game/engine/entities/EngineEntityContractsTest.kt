package com.maverick.game.engine.entities

import com.maverick.game.engine.entities.contracts.IChildEntity
import com.maverick.game.engine.entities.contracts.IParentEntity
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class EngineEntityContractsTest : DescribeSpec({

    describe("engine entity contracts") {
        listOf(IChildEntity::class, IParentEntity::class).forEach { contract ->
            it("${contract.simpleName} is an IGameEntity") {
                IGameEntity::class.java.isAssignableFrom(contract.java) shouldBe true
            }
        }
    }
})
