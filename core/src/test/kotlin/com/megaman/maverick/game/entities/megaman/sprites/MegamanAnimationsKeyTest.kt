package com.megaman.maverick.game.entities.megaman.sprites

import com.megaman.maverick.game.entities.megaman.constants.MegamanWeapon
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

class MegamanAnimationsKeyTest : DescribeSpec({

    describe("MegamanAnimations.buildFullKey") {

        it("gives the region key with the weapon suffix, for every weapon") {
            MegamanAnimations.buildFullKey("stand", MegamanWeapon.MEGA_BUSTER) shouldBe "stand_megabuster"
            MegamanWeapon.entries.forEach { weapon ->
                MegamanAnimations.buildFullKey("jump_shoot", weapon) shouldBe
                    "jump_shoot_${weapon.name.lowercase().replace("_", "")}"
            }
        }

        it("gives the same string instance on the next call, so a frame builds no string") {
            val first = MegamanAnimations.buildFullKey("run_shoot", MegamanWeapon.RODENT_CLAWS)
            MegamanAnimations.buildFullKey("run_shoot", MegamanWeapon.RODENT_CLAWS) shouldBeSameInstanceAs first
        }
    }
})
