package com.megaman.maverick.game.entities.megaman.sprites

import com.megaman.maverick.game.entities.megaman.weapons.MegamanWeapons
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

class MegamanAnimationsKeyTest : DescribeSpec({

    describe("MegamanAnimations.buildFullKey") {

        it("gives the region key with the weapon suffix, for every weapon") {
            MegamanAnimations.buildFullKey("stand", MegamanWeapons.MEGA_BUSTER) shouldBe "stand_megabuster"
            MegamanWeapons.all.forEach { weapon ->
                MegamanAnimations.buildFullKey("jump_shoot", weapon) shouldBe
                    "jump_shoot_${weapon.id.replace("_", "")}"
            }
        }

        it("gives the same string instance on the next call, so a frame builds no string") {
            val first = MegamanAnimations.buildFullKey("run_shoot", MegamanWeapons.RODENT_CLAWS)
            MegamanAnimations.buildFullKey("run_shoot", MegamanWeapons.RODENT_CLAWS) shouldBeSameInstanceAs first
        }
    }
})
