package com.megaman.maverick.game.entities.bosses.elecdevil

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.damage.IDamagerEntity
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.bosses.ElecDevilBodyPiece
import com.megaman.maverick.game.entities.bosses.ElecDevilBodyPieces
import com.megaman.maverick.game.entities.bosses.ElecDevilClaw
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.explosions.ElecExplosion
import com.megaman.maverick.game.entities.explosions.IceBombExplosion
import com.megaman.maverick.game.entities.explosions.MagmaExplosion
import com.megaman.maverick.game.entities.explosions.ReactorExplosion
import com.megaman.maverick.game.entities.hazards.MagmaFlame
import com.megaman.maverick.game.entities.hazards.SmallIceCube
import com.megaman.maverick.game.entities.players.PlayerSabreSlash
import com.megaman.maverick.game.entities.projectiles.Axe
import com.megaman.maverick.game.entities.projectiles.ElecBall
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

class ElecDevilDamageTest : DescribeSpec({

    lateinit var fight: ElecDevilRig

    beforeEach {
        fight = ElecDevilRig()
        fight.spawnBoss()
    }

    afterEach { fight.dispose() }

    fun damageOf(damager: IDamagerEntity): Int {
        fight.boss.setHealthToMax()
        fight.boss.takeDamageFrom(damager)
        return fight.boss.getMaxHealth() - fight.boss.getCurrentHealth()
    }

    describe("the health phases") {
        it("has 2 phases, and is defeated only at 0 in phase 2: 15 bullets per phase, 2 x the master bar") {
            fight.boss.healthPhases shouldBe 2
            fight.toPhase2()
            fight.rig.checkPoints(fight.boss)
            fight.boss.defeated shouldBe false
            repeat(14) { fight.boss.takeDamageFrom(fight.rig.bullet()) }
            fight.rig.checkPoints(fight.boss)
            fight.boss.defeated shouldBe false
            fight.boss.takeDamageFrom(fight.rig.bullet())
            fight.rig.checkPoints(fight.boss)
            fight.boss.getCurrentHealth() shouldBe 0
            fight.boss.defeated shouldBe true
        }
    }

    describe("the damage on Elec Devil: 2 x the master values") {
        it("the buster does 2, 2 and 4, and the charge explosion 2 and 4") {
            damageOf(fight.rig.bullet()) shouldBe 2
            damageOf(fight.rig.chargedShot(full = false)) shouldBe 2
            damageOf(fight.rig.chargedShot(full = true)) shouldBe 4
            damageOf(fight.rig.chargedExplosion(full = false)) shouldBe 2
            damageOf(fight.rig.chargedExplosion(full = true)) shouldBe 4
        }

        it("a full-strength sabre slash does 4, and the Proto Man buster shot 2") {
            damageOf(PlayerSabreSlash(fight.rig.services)) shouldBe 4
            val shot = MegaEntityFactory.fetch(ProtoBuster::class)!!
            shot.spawn(
                props(
                    ConstKeys.OWNER pairTo fight.rig.player,
                    ConstKeys.POSITION pairTo Vector2(),
                    ConstKeys.TRAJECTORY pairTo Vector2()
                )
            )
            fight.rig.processSpawns()
            damageOf(shot) shouldBe 2
        }

        val services = { fight.rig.services }
        listOf<Triple<String, () -> IDamagerEntity, Int>>(
            Triple("Axe", { Axe(services()) }, 4),
            Triple("MoonScythe", { MoonScythe(services()) }, 2),
            Triple("small ReactorManProjectile", { ReactorManProjectile(services()) }, 2),
            Triple("big ReactorManProjectile", { ReactorManProjectile(services()).also { it.big = true } }, 2),
            Triple("SmallIceCube", { SmallIceCube(services()) }, 2),
            Triple("MagmaWave", { MagmaWave(services()) }, 2),
            Triple("Fireball", { Fireball(services()) }, 2),
            Triple("PreciousGem", { PreciousGem(services()) }, 2),
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
        val services = { fight.rig.services }
        listOf<Pair<String, () -> MegaGameEntity>>(
            "ElecDevil hand" to { fight.boss },
            "ElecDevilClaw" to { ElecDevilClaw(services()) },
            "ElecDevilBodyPieces" to { ElecDevilBodyPieces(services()) },
            "ElecDevilBodyPiece" to { ElecDevilBodyPiece(services()) },
            "ElecBall" to { ElecBall(services()) },
            "ElecExplosion" to { ElecExplosion(services()) }
        ).forEach { (name, damager) ->
            it("$name does 3") {
                val entity = damager()
                if (!entity.initialized) entity.init()
                PlayerDamageNegotiator(fight.rig.player).get(entity as IDamagerEntity) shouldBe 3
            }
        }
    }
})
