package com.megaman.maverick.game.levels

import com.megaman.maverick.game.entities.enemies.Met
import com.megaman.maverick.game.entities.items.HealthBulb
import com.megaman.maverick.game.entities.items.Life
import com.megaman.maverick.game.entities.items.WeaponEnergyBulb
import com.megaman.maverick.game.testServices
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class LevelOverrideCanSpawnTest : DescribeSpec({

    val services = testServices()

    describe("the GAUNTLET_1 spawn filter") {
        val canSpawn = LevelDefinition.GAUNTLET_1.levelOverride!!.canSpawn!!

        it("rejects lives and weapon energy before the entity runs init()") {
            canSpawn(Life(services)) shouldBe false
            canSpawn(WeaponEnergyBulb(services)) shouldBe false
        }

        it("lets other entities spawn before they run init()") {
            canSpawn(Met(services)) shouldBe true
            canSpawn(HealthBulb(services)) shouldBe true
        }
    }
})
