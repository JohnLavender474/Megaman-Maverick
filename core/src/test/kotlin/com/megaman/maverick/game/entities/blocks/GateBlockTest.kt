package com.megaman.maverick.game.entities.blocks

import com.maverick.game.engine.common.objects.Properties
import com.megaman.maverick.game.state.GameMode
import com.megaman.maverick.game.testServices
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class GateBlockTest : DescribeSpec({

    describe("GateBlock spawn rule") {
        it("spawns in co-op") {
            GateBlock(testServices(rules = GameMode.COOP.rules)).canSpawn(Properties()) shouldBe true
        }

        it("does not spawn in single-player") {
            GateBlock(testServices(rules = GameMode.SINGLE.rules)).canSpawn(Properties()) shouldBe false
        }

        it("does not spawn in pvp") {
            GateBlock(testServices(rules = GameMode.PVP.rules)).canSpawn(Properties()) shouldBe false
        }
    }
})
