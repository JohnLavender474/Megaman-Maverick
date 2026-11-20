package com.megaman.maverick.game.world.wrap

import com.maverick.game.engine.world.WorldWrap
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.players.nonPlayer
import com.megaman.maverick.game.players.registeredPlayer
import com.megaman.maverick.game.testServices
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class WrapPolicyTest : DescribeSpec({

    describe("WrapPolicy") {
        it("a registered player wraps with no cap") {
            val player = registeredPlayer()
            WrapPolicy.wraps(player) shouldBe true
            WrapPolicy.maxWraps(player) shouldBe WorldWrap.UNLIMITED_WRAPS
        }

        it("a shot wraps 1 time") {
            val shot = Bullet(testServices())
            WrapPolicy.wraps(shot) shouldBe true
            WrapPolicy.maxWraps(shot) shouldBe 1
        }

        it("a player type that is not in a slot does not wrap") {
            val entity = nonPlayer()
            WrapPolicy.wraps(entity) shouldBe false
            WrapPolicy.maxWraps(entity) shouldBe 0
        }
    }
})
