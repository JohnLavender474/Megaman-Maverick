package com.megaman.maverick.game.entities.bosses.desertman

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.damage.IDamagerEntity
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.explosions.IceBombExplosion
import com.megaman.maverick.game.entities.explosions.MagmaExplosion
import com.megaman.maverick.game.entities.explosions.ReactorExplosion
import com.megaman.maverick.game.entities.hazards.MagmaFlame
import com.megaman.maverick.game.entities.hazards.SmallIceCube
import com.megaman.maverick.game.entities.players.PlayerSabreSlash
import com.megaman.maverick.game.entities.projectiles.Axe
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.entities.projectiles.MagmaWave
import com.megaman.maverick.game.entities.projectiles.MoonScythe
import com.megaman.maverick.game.entities.projectiles.Needle
import com.megaman.maverick.game.entities.projectiles.PreciousGem
import com.megaman.maverick.game.entities.projectiles.PreciousShard
import com.megaman.maverick.game.entities.projectiles.ProtoBuster
import com.megaman.maverick.game.entities.projectiles.ReactorManProjectile
import com.megaman.maverick.game.entities.projectiles.SlashWave
import com.megaman.maverick.game.players.PlayerDamageNegotiator
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class DesertManDamageTest : DescribeSpec({

    lateinit var rig: DesertManRig

    beforeEach {
        rig = DesertManRig()
        rig.spawnBoss()
    }

    afterEach { rig.dispose() }

    fun damageOf(damager: IDamagerEntity): Int {
        rig.boss.setHealthToMax()
        rig.boss.takeDamageFrom(damager)
        return 30 - rig.boss.getCurrentHealth()
    }

    fun charged(full: Boolean): ChargedShot {
        val shot = MegaEntityFactory.fetch(ChargedShot::class)!!
        shot.spawn(
            props(
                ConstKeys.OWNER pairTo rig.player,
                ConstKeys.BOOLEAN pairTo full,
                ConstKeys.POSITION pairTo Vector2(),
                ConstKeys.TRAJECTORY pairTo Vector2(1f, 0f)
            )
        )
        rig.processSpawns()
        return shot
    }

    fun explosion(full: Boolean): ChargedShotExplosion {
        val explosion = MegaEntityFactory.fetch(ChargedShotExplosion::class)!!
        explosion.spawn(
            props(ConstKeys.OWNER pairTo rig.player, ConstKeys.BOOLEAN pairTo full, ConstKeys.POSITION pairTo Vector2())
        )
        rig.processSpawns()
        return explosion
    }

    describe("the health phases") {
        it("rolls the bar over at 0 in phase 1, and is defeated only at 0 in phase 2") {
            repeat(14) { rig.boss.takeDamageFrom(rig.bullet()) }
            rig.boss.healthPhase shouldBe 1
            rig.boss.getCurrentHealth() shouldBe 2
            rig.boss.takeDamageFrom(rig.bullet())
            rig.checkPoints()
            rig.boss.healthPhase shouldBe 2
            rig.boss.getCurrentHealth() shouldBe 30
            rig.boss.defeated shouldBe false
            repeat(14) { rig.boss.takeDamageFrom(rig.bullet()) }
            rig.checkPoints()
            rig.boss.defeated shouldBe false
            rig.boss.takeDamageFrom(rig.bullet())
            rig.checkPoints()
            rig.boss.getCurrentHealth() shouldBe 0
            rig.boss.defeated shouldBe true
        }

        it("has 2 phases") {
            rig.boss.healthPhases shouldBe 2
        }
    }

    describe("the damage on Desert Man: 2 x the master table") {
        it("the buster does 2, 2 and 4, and the charge explosion 2 and 4") {
            damageOf(rig.bullet()) shouldBe 2
            damageOf(charged(full = false)) shouldBe 2
            damageOf(charged(full = true)) shouldBe 4
            damageOf(explosion(full = false)) shouldBe 2
            damageOf(explosion(full = true)) shouldBe 4
        }

        it("a full-strength sabre slash does 4, and the Proto Man buster shot 2") {
            damageOf(PlayerSabreSlash(rig.services)) shouldBe 4
            val shot = MegaEntityFactory.fetch(ProtoBuster::class)!!
            shot.spawn(
                props(
                    ConstKeys.OWNER pairTo rig.player,
                    ConstKeys.POSITION pairTo Vector2(),
                    ConstKeys.TRAJECTORY pairTo Vector2()
                )
            )
            rig.processSpawns()
            damageOf(shot) shouldBe 2
        }

        val services = { rig.services }
        listOf<Triple<String, () -> IDamagerEntity, Int>>(
            Triple("MoonScythe", { MoonScythe(services()) }, 8),
            Triple("small ReactorManProjectile", { ReactorManProjectile(services()) }, 6),
            Triple("big ReactorManProjectile", { ReactorManProjectile(services()).also { it.big = true } }, 6),
            Triple("SmallIceCube", { SmallIceCube(services()) }, 2),
            Triple("MagmaWave", { MagmaWave(services()) }, 2),
            Triple("Fireball", { Fireball(services()) }, 2),
            Triple("PreciousGem", { PreciousGem(services()) }, 2),
            Triple("Axe", { Axe(services()) }, 2),
            Triple("Needle", { Needle(services()) }, 2),
            Triple("SlashWave", { SlashWave(services()) }, 2),
            Triple("IceBombExplosion", { IceBombExplosion(services()) }, 2),
            Triple("MagmaFlame", { MagmaFlame(services()) }, 2),
            Triple("ReactorExplosion", { ReactorExplosion(services()) }, 2),
            Triple("MagmaExplosion", { MagmaExplosion(services()) }, 0),
            Triple("PreciousShard", { PreciousShard(services()) }, 0)
        ).forEach { (name, damager, expected) ->
            it("$name does $expected") {
                withClue(name) { damageOf(damager()) shouldBe expected }
            }
        }
    }

    describe("the damage on the player") {
        it("every Desert Man damager does 3") {
            PlayerDamageNegotiator(rig.player).get(rig.boss) shouldBe 3
        }

        it("a Needle of Desert Man does 3, and a Needle of another owner also 3") {
            val own = MegaEntityFactory.fetch(Needle::class)!!
            own.spawn(props(ConstKeys.OWNER pairTo rig.boss, ConstKeys.POSITION pairTo Vector2()))
            rig.processSpawns()
            own.owner shouldBe rig.boss
            PlayerDamageNegotiator(rig.player).get(own) shouldBe 3
            val other = MegaEntityFactory.fetch(Needle::class)!!
            other.spawn(props(ConstKeys.POSITION pairTo Vector2()))
            rig.processSpawns()
            (other.owner == rig.boss) shouldBe false
            PlayerDamageNegotiator(rig.player).get(other) shouldBe 3
        }
    }
})
