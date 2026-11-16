package com.megaman.maverick.game.damage

import com.maverick.game.engine.common.enums.Size
import com.maverick.game.engine.damage.IDamagerEntity
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.entities.megamanx.MegamanXSabreMoves
import com.megaman.maverick.game.entities.players.PlayerSabreSlash
import com.megaman.maverick.game.entities.protoman.ProtomanSabreMoves
import com.megaman.maverick.game.entities.projectiles.ProtoBuster
import com.megaman.maverick.game.testServices
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.unmockkAll

class ProtoShotDamageTest : DescribeSpec({

    afterEach { unmockkAll() }

    fun halfChargeDamage(damager: IDamagerEntity) {
        StandardDamageNegotiator.get(Size.LARGE, damager) shouldBe 3
        StandardDamageNegotiator.get(Size.MEDIUM, damager) shouldBe 10
        StandardDamageNegotiator.get(Size.SMALL, damager) shouldBe 15
    }

    describe("a standard target") {
        it("takes the half-charge damage of its size from a ProtoBuster") {
            halfChargeDamage(ProtoBuster(testServices()))
        }

        it("takes the full sabre damage of its size from a sabre slash that has not placed a move") {
            val slash = PlayerSabreSlash(testServices())
            StandardDamageNegotiator.get(Size.LARGE, slash) shouldBe 12
            StandardDamageNegotiator.get(Size.MEDIUM, slash) shouldBe 15
            StandardDamageNegotiator.get(Size.SMALL, slash) shouldBe ConstVals.MAX_HEALTH
        }
    }

    describe("the sabre move strength") {
        it("is full on Proto Man's third floor slash only") {
            ProtomanSabreMoves.ALL.filter { it.fullStrength }.toList() shouldBe listOf(ProtomanSabreMoves.SLASH_3)
        }

        it("is full on every X move") {
            MegamanXSabreMoves.ALL.all { it.fullStrength } shouldBe true
        }
    }
})
