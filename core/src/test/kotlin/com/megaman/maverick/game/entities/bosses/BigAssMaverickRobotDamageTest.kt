package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.damage.IDamagerEntity
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.explosions.IceBombExplosion
import com.megaman.maverick.game.entities.explosions.MagmaExplosion
import com.megaman.maverick.game.entities.explosions.ReactorExplosion
import com.megaman.maverick.game.entities.hazards.MagmaFlame
import com.megaman.maverick.game.entities.hazards.SmallIceCube
import com.megaman.maverick.game.entities.megaman.constants.MegaEnhancement
import com.megaman.maverick.game.entities.projectiles.Axe
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.entities.projectiles.MagmaWave
import com.megaman.maverick.game.entities.projectiles.MoonScythe
import com.megaman.maverick.game.entities.projectiles.Needle
import com.megaman.maverick.game.entities.projectiles.PreciousGem
import com.megaman.maverick.game.entities.projectiles.PreciousShard
import com.megaman.maverick.game.entities.projectiles.ProtoBuster
import com.megaman.maverick.game.entities.projectiles.ReactorManProjectile
import com.megaman.maverick.game.entities.projectiles.SlashWave
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class BigAssMaverickRobotDamageTest : DescribeSpec({

    lateinit var rig: BossRig
    lateinit var boss: BigAssMaverickRobot

    beforeEach {
        rig = BossRig()
        boss = BigAssMaverickRobot(rig.services)
        boss.init()
    }

    afterEach { rig.dispose() }

    fun damageOf(damager: IDamagerEntity): Int {
        boss.setHealthToMax()
        boss.takeDamageFrom(damager)
        return 30 - boss.getCurrentHealth()
    }

    describe("the buster on BigAssMaverickRobot") {
        it("does 3, 4 and 6") {
            damageOf(rig.bullet()) shouldBe 3
            damageOf(rig.chargedShot(full = false)) shouldBe 4
            damageOf(rig.chargedShot(full = true)) shouldBe 6
        }

        it("does 3 and 4 with the explosion of a half and a full charge") {
            damageOf(rig.chargedExplosion(full = false)) shouldBe 3
            damageOf(rig.chargedExplosion(full = true)) shouldBe 4
        }

        it("scales with DAMAGE_INCREASE") {
            rig.services.gameState.addEnhancement(MegaEnhancement.DAMAGE_INCREASE)
            damageOf(rig.bullet()) shouldBe 5
            damageOf(rig.chargedShot(full = false)) shouldBe 6
            damageOf(rig.chargedShot(full = true)) shouldBe 9
        }

        it("does 4 with the Proto Man buster shot, the damage of a half charge") {
            val shot = MegaEntityFactory.fetch(ProtoBuster::class)!!
            shot.spawn(
                props(
                    ConstKeys.OWNER pairTo rig.player,
                    ConstKeys.POSITION pairTo Vector2(),
                    ConstKeys.TRAJECTORY pairTo Vector2()
                )
            )
            rig.processSpawns()
            damageOf(shot) shouldBe 4
        }
    }

    describe("the special weapons on BigAssMaverickRobot") {
        val services = { rig.services }
        listOf<Triple<String, () -> IDamagerEntity, Int>>(
            Triple("SmallIceCube", { SmallIceCube(services()) }, 3),
            Triple("IceBombExplosion", { IceBombExplosion(services()) }, 2),
            Triple("MagmaWave", { MagmaWave(services()) }, 3),
            Triple("Fireball", { Fireball(services()) }, 3),
            Triple("MagmaExplosion", { MagmaExplosion(services()) }, 2),
            Triple("MagmaFlame", { MagmaFlame(services()) }, 2),
            Triple("MoonScythe", { MoonScythe(services()) }, 3),
            Triple("PreciousGem", { PreciousGem(services()) }, 3),
            Triple("PreciousShard", { PreciousShard(services()) }, 2),
            Triple("Axe", { Axe(services()) }, 4),
            Triple("Needle", { Needle(services()) }, 3),
            Triple("SlashWave", { SlashWave(services()) }, 3),
            Triple("small ReactorManProjectile", { ReactorManProjectile(services()) }, 3),
            Triple("big ReactorManProjectile", { ReactorManProjectile(services()).also { it.big = true } }, 6),
            Triple("ReactorExplosion", { ReactorExplosion(services()) }, 3)
        ).forEach { (name, damager, expected) ->
            it("$name does $expected") {
                withClue(name) { damageOf(damager()) shouldBe expected }
            }
        }
    }
})
