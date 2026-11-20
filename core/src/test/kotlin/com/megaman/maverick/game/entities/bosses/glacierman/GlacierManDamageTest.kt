package com.megaman.maverick.game.entities.bosses.glacierman

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.damage.IDamagerEntity
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.drawables.ui.BossBitsBar
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.bosses.DesertMan
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.explosions.IceBombExplosion
import com.megaman.maverick.game.entities.explosions.MagmaExplosion
import com.megaman.maverick.game.entities.explosions.ReactorExplosion
import com.megaman.maverick.game.entities.hazards.MagmaFlame
import com.megaman.maverick.game.entities.hazards.SmallIceCube
import com.megaman.maverick.game.entities.players.PlayerSabreSlash
import com.megaman.maverick.game.entities.projectiles.Axe
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.FallingIcicle
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.entities.projectiles.MagmaWave
import com.megaman.maverick.game.entities.projectiles.MoonScythe
import com.megaman.maverick.game.entities.projectiles.Needle
import com.megaman.maverick.game.entities.projectiles.PreciousGem
import com.megaman.maverick.game.entities.projectiles.PreciousShard
import com.megaman.maverick.game.entities.projectiles.ProtoBuster
import com.megaman.maverick.game.entities.projectiles.ReactorManProjectile
import com.megaman.maverick.game.entities.projectiles.SlashWave
import com.megaman.maverick.game.entities.projectiles.Snowhead
import com.megaman.maverick.game.entities.projectiles.SpitFireball
import com.megaman.maverick.game.entities.state.frozen
import com.megaman.maverick.game.entities.state.freezeFrom
import com.megaman.maverick.game.players.PlayerDamageNegotiator
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe

class GlacierManDamageTest : DescribeSpec({

    lateinit var rig: GlacierManRig

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
        beforeEach {
            rig = GlacierManRig()
            rig.spawnBoss()
        }

        it("has 2 phases, and the second bar region is PHASE_2_BITS") {
            rig.boss.healthPhases shouldBe 2
            rig.boss.healthBarRegions.size shouldBe 2
            rig.boss.healthBarRegions[1] shouldBe BossBitsBar.PHASE_2_BITS
        }

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
    }

    describe("the damage on Glacier Man: 2 x the master table") {
        beforeEach {
            rig = GlacierManRig()
            rig.spawnBoss()
        }

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
            Triple("Fireball", { Fireball(services()) }, 8),
            Triple("MagmaWave", { MagmaWave(services()) }, 8),
            Triple("MagmaFlame", { MagmaFlame(services()) }, 8),
            Triple("small ReactorManProjectile", { ReactorManProjectile(services()) }, 6),
            Triple("big ReactorManProjectile", { ReactorManProjectile(services()).also { it.big = true } }, 6),
            Triple("MoonScythe", { MoonScythe(services()) }, 2),
            Triple("SmallIceCube", { SmallIceCube(services()) }, 2),
            Triple("Axe", { Axe(services()) }, 2),
            Triple("Needle", { Needle(services()) }, 2),
            Triple("SlashWave", { SlashWave(services()) }, 2),
            Triple("PreciousGem", { PreciousGem(services()) }, 2),
            Triple("IceBombExplosion", { IceBombExplosion(services()) }, 2),
            Triple("ReactorExplosion", { ReactorExplosion(services()) }, 2),
            Triple("MagmaExplosion (not in the table: the BOSS_DMG_NEG value)", { MagmaExplosion(services()) }, 0),
            Triple("PreciousShard (not in the table: the BOSS_DMG_NEG value)", { PreciousShard(services()) }, 0)
        ).forEach { (name, damager, expected) ->
            it("$name does $expected") {
                withClue(name) { damageOf(damager()) shouldBe expected }
            }
        }
    }

    describe("a fire hit") {
        beforeEach {
            rig = GlacierManRig()
            rig.startFight()
        }

        it("that does damage starts the FIRED stun for 1 s and plays SOLAR_BLAZE_SOUND") {
            rig.boss.takeDamageFrom(Fireball(rig.services)) shouldBe true
            rig.animKey shouldBe "fired"
            rig.sounds(SoundAsset.SOLAR_BLAZE_SOUND) shouldBe 1
            val stun = rig.secondsWhile(3f) { rig.animKey == "fired" }
            stun shouldBe (1f plusOrMinus 2f * STEP)
            rig.animKey shouldBe "stand"
        }

        it("that does no damage starts no stun and plays no sound") {
            rig.boss.takeDamageFrom(SpitFireball(rig.services)) shouldBe false
            rig.tick()
            (rig.animKey == "fired") shouldBe false
            rig.sounds(SoundAsset.SOLAR_BLAZE_SOUND) shouldBe 0
        }

        it("a hit that is not fire starts no stun") {
            rig.boss.takeDamageFrom(rig.bullet()) shouldBe true
            rig.tick()
            (rig.animKey == "fired") shouldBe false
            rig.sounds(SoundAsset.SOLAR_BLAZE_SOUND) shouldBe 0
        }
    }

    describe("the damage on the player") {
        beforeEach {
            rig = GlacierManRig()
            rig.spawnBoss()
        }

        fun snowhead(owner: Any?): Snowhead {
            val ball = MegaEntityFactory.fetch(Snowhead::class)!!
            val spawn = props(ConstKeys.POSITION pairTo Vector2(), ConstKeys.TYPE pairTo ConstKeys.BLANK)
            if (owner != null) spawn.put(ConstKeys.OWNER, owner)
            ball.spawn(spawn)
            rig.processSpawns()
            return ball
        }

        fun cube(owner: Any?): SmallIceCube {
            val cube = MegaEntityFactory.fetch(SmallIceCube::class)!!
            val spawn = props(ConstKeys.POSITION pairTo Vector2())
            if (owner != null) spawn.put(ConstKeys.OWNER, owner)
            cube.spawn(spawn)
            rig.processSpawns()
            return cube
        }

        fun icicle(owner: Any?): FallingIcicle {
            val icicle = MegaEntityFactory.fetch(FallingIcicle::class)!!
            val spawn = props(ConstKeys.POSITION pairTo Vector2())
            if (owner != null) spawn.put(ConstKeys.OWNER, owner)
            icicle.spawn(spawn)
            rig.processSpawns()
            return icicle
        }

        it("Glacier Man does 3") {
            PlayerDamageNegotiator(rig.player).get(rig.boss) shouldBe 3
        }

        it("a Snowhead, a SmallIceCube and a FallingIcicle of Glacier Man do 3") {
            val ball = snowhead(rig.boss)
            ball.owner shouldBe rig.boss
            PlayerDamageNegotiator(rig.player).get(ball) shouldBe 3
            val cube = cube(rig.boss)
            cube.owner shouldBe rig.boss
            PlayerDamageNegotiator(rig.player).get(cube) shouldBe 3
            val icicle = icicle(rig.boss)
            icicle.owner shouldBe rig.boss
            PlayerDamageNegotiator(rig.player).get(icicle) shouldBe 3
        }

        it("a Snowhead, a SmallIceCube and a FallingIcicle of another owner or of no owner do 3 (the master fallback)") {
            val other = DesertMan(rig.services)
            listOf<Any?>(other, null).forEach { owner ->
                withClue("owner $owner") {
                    val ball = snowhead(owner)
                    ball.owner shouldBe owner
                    PlayerDamageNegotiator(rig.player).get(ball) shouldBe 3
                    val cube = cube(owner)
                    cube.owner shouldBe owner
                    PlayerDamageNegotiator(rig.player).get(cube) shouldBe 3
                    val icicle = icicle(owner)
                    icicle.owner shouldBe owner
                    PlayerDamageNegotiator(rig.player).get(icicle) shouldBe 3
                }
            }
        }

        it("a SmallIceCube of Glacier Man still freezes the player") {
            rig.player.frozen shouldBe false
            rig.player.freezeFrom(cube(rig.boss))
            rig.player.frozen shouldBe true
        }
    }

    describe("the tuning row") {
        it("follows the difficulty and the phase") {
            GlacierManTuning.of(hard = false, phase = 1) shouldBe GlacierManTuning.NORMAL
            GlacierManTuning.of(hard = false, phase = 2) shouldBe GlacierManTuning.NORMAL_PHASE_2
            GlacierManTuning.of(hard = true, phase = 1) shouldBe GlacierManTuning.HARD
            GlacierManTuning.of(hard = true, phase = 2) shouldBe GlacierManTuning.HARD_PHASE_2
            rig = GlacierManRig()
        }

        val allRows = listOf(
            GlacierManTuning.NORMAL, GlacierManTuning.NORMAL_PHASE_2, GlacierManTuning.HARD, GlacierManTuning.HARD_PHASE_2
        )
        listOf(
            TuningRow("normal", hard = false, phase2 = false),
            TuningRow("normal phase 2", hard = false, phase2 = true),
            TuningRow("hard", hard = true, phase2 = false),
            TuningRow("hard phase 2", hard = true, phase2 = true)
        ).forEach { row ->
            it("${row.name}: the boss reads the stand ranges of its row only") {
                rig = GlacierManRig(hard = row.hard)
                rig.startFight(phase2 = row.phase2)
                rig.ticks(0.1f)
                val tuning = row.tuning
                rig.random.floatCalls(tuning.standDurMin, tuning.standDurMax).isNotEmpty() shouldBe true
                rig.random.floatCalls(tuning.standShotMin, tuning.standShotMax).isNotEmpty() shouldBe true
                allRows.filter { it !== tuning }.forEach { other ->
                    rig.random.floatCalls(other.standDurMin, other.standDurMax).size shouldBe 0
                }
            }
        }
    }
})
