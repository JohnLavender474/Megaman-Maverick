package com.megaman.maverick.game.entities.bosses.glacierman

import com.badlogic.gdx.graphics.Color
import com.maverick.game.engine.common.enums.Facing
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.entities.hazards.SmallIceCube
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.entities.projectiles.Snowhead
import com.megaman.maverick.game.entities.state.BossPhase
import com.megaman.maverick.game.events.EventType
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe

private const val TOLERANCE = 2f * STEP

private val ROWS = listOf(
    TuningRow("normal", hard = false, phase2 = false) to TuningRow("normal phase 2", hard = false, phase2 = true),
    TuningRow("hard", hard = true, phase2 = false) to TuningRow("hard phase 2", hard = true, phase2 = true)
)

private fun farTotal(row: GlacierManTuning) = row.iceBlastWeight * row.farIceBlastScale + row.duckWeight +
    row.sledWeight + row.jumpWeight + row.icicleRainWeight + row.sledRamWeight

class GlacierManPhaseStartTest : DescribeSpec({

    lateinit var rig: GlacierManRig

    afterEach { rig.dispose() }

    fun start(row: TuningRow = ROWS[0].first) {
        rig = GlacierManRig(hard = row.hard)
        rig.startFight()
    }

    fun rollOver() {
        rig.toPhase2()
        rig.tick()
    }

    fun nearX() = (rig.bossBody.getX() - 1f * T) / T + 0.5f

    fun moveCount() = rig.spawnsOf<Snowhead>().size + rig.spawnsOf<SmallIceCube>().size

    describe("the pose") {
        ROWS.forEach { (phase1, phase2) ->
            val duration = phase2.tuning.phaseStartDur
            it("${phase2.name}: fist_flash facing the player, x velocity 0, a cyan tint that blinks every 0.1 s, " +
                "one SHAKE_CAM (0.5 s), CHILL_SHOOT_SOUND, for phaseStartDur = $duration s, then STAND") {
                start(phase1)
                val shakes = rig.eventsOf(EventType.SHAKE_CAM)
                val sounds = rig.sounds(SoundAsset.CHILL_SHOOT_SOUND)
                rollOver()
                rig.animKey shouldBe "fist_flash"
                rig.boss.facing shouldBe Facing.LEFT
                val tints = mutableListOf(rig.sprite().color == Color.CYAN)
                var moving = false
                var ticks = 0
                val seconds = rig.secondsWhile(2f, each = {
                    ticks++
                    if (ticks == 3) rig.placePlayerCenter(rig.bossCenter().x + 3f * T, FLOOR + 0.5f * T)
                }) {
                    if (rig.animKey == "fist_flash") {
                        if (ticks > 0) tints.add(rig.sprite().color == Color.CYAN)
                        if (rig.bossBody.physics.velocity.x != 0f) moving = true
                        if (ticks == 4) rig.boss.facing shouldBe Facing.RIGHT
                    }
                    rig.animKey == "fist_flash"
                }
                (seconds + STEP) shouldBe (duration plusOrMinus TOLERANCE)
                rig.animKey shouldBe "stand"
                moving shouldBe false
                rig.eventsOf(EventType.SHAKE_CAM) shouldBe shakes + 1
                val shake = rig.events.last { it.key == EventType.SHAKE_CAM }
                shake.properties.get(ConstKeys.DURATION, Float::class) shouldBe 0.5f
                rig.sounds(SoundAsset.CHILL_SHOOT_SOUND) shouldBe sounds + 1
                tints.forEachIndexed { k, cyan ->
                    val time = (k + 1) * STEP
                    val phase = time / 0.1f
                    if (phase - phase.toInt() in 0.05f..0.95f) withClue("tick ${k + 1}") {
                        cyan shouldBe (phase.toInt() % 2 == 0)
                    }
                }
                rig.sprite().color shouldBe Color.WHITE
            }
        }
    }

    describe("the start of the pose") {
        it("in STAND, at the next update") {
            start()
            rollOver()
            rig.animKey shouldBe "fist_flash"
        }

        it("a duck ends at once") {
            start()
            val row = GlacierManTuning.NORMAL
            rig.leaveStand((row.iceBlastWeight * row.farIceBlastScale + row.duckWeight / 2f) / farTotal(row)) shouldBe
                "duck"
            rollOver()
            rig.animKey shouldBe "fist_flash"
        }

        it("a sled ends at once, with x velocity 0") {
            start()
            val row = GlacierManTuning.NORMAL
            rig.leaveStand((row.iceBlastWeight * row.farIceBlastScale + row.duckWeight + row.sledWeight / 2f) /
                farTotal(row)) shouldBe "brake_flash"
            rig.untilKey("sled")
            rig.ticks(0.2f)
            (rig.bossBody.physics.velocity.x < 0f) shouldBe true
            rollOver()
            rig.animKey shouldBe "fist_flash"
            rig.bossBody.physics.velocity.x shouldBe 0f
        }

        it("an ice blast ends at once, with no more cubes") {
            start()
            rig.leaveStand(0.01f) shouldBe "throw_flash"
            withClue("the first cube") { rig.ticksUntil(2f) { rig.spawnsOf<SmallIceCube>().isNotEmpty() } shouldBe true }
            val cubes = rig.spawnsOf<SmallIceCube>().size
            rollOver()
            rig.animKey shouldBe "fist_flash"
            rig.ticks(1f)
            rig.spawnsOf<SmallIceCube>().size shouldBe cubes
        }

        it("a jump first lands") {
            start()
            rig.leaveStand(0.999f) shouldBe "fall_flash"
            rig.untilKey("jump", 2f)
            rig.ticks(0.1f)
            rollOver()
            (rig.animKey == "fist_flash") shouldBe false
            var airTicks = 0
            withClue("the pose") {
                rig.ticksUntil(4f, each = {
                    if (rig.bossBody.getY() > FLOOR + 0.01f) airTicks++
                }) {
                    if (rig.animKey == "fist_flash") rig.bossBody.getY() shouldBe (FLOOR plusOrMinus 0.01f)
                    rig.animKey == "fist_flash"
                } shouldBe true
            }
            (airTicks > 10) shouldBe true
        }

        it("not in INIT: a roll-over before the fight waits until INIT ends") {
            rig = GlacierManRig()
            rig.spawnBoss()
            rig.toPhase2()
            var posed = false
            withClue("the entrance") {
                rig.ticksUntil(5f, each = { if (rig.animKey == "fist_flash") posed = true }) {
                    rig.boss.bossComponent.phase == BossPhase.INTRO
                } shouldBe true
            }
            posed shouldBe false
            rig.enterFight(phase2 = true)
        }
    }

    describe("during the pose") {
        it("no move, no snowball and no throw starts; the pose does not count in the repeat rule; the first pick " +
            "uses the phase-2 row") {
            start()
            val row = GlacierManTuning.NORMAL
            rig.place(nearX())
            val near = row.iceBlastWeight + row.duckWeight + row.jumpWeight * row.nearJumpScale
            rig.leaveStand((row.iceBlastWeight + row.duckWeight / 2f) / near) shouldBe "duck"
            rig.untilStand()
            val second = row.iceBlastWeight + row.duckWeight * row.lastMoveScale + row.jumpWeight * row.nearJumpScale
            rig.leaveStand((row.iceBlastWeight + row.duckWeight * row.lastMoveScale / 2f) / second) shouldBe "duck"
            rig.untilStand()
            rollOver()
            rig.animKey shouldBe "fist_flash"
            val picks = rig.random.pickCalls().size
            val moves = moveCount()
            rig.secondsWhile(2f) { rig.animKey == "fist_flash" }
            rig.random.pickCalls().size shouldBe picks
            moveCount() shouldBe moves
            val phase2 = GlacierManTuning.NORMAL_PHASE_2
            rig.leaveStand(0.5f)
            rig.lastPickTotal() shouldBe (phase2.iceBlastWeight + phase2.jumpWeight * phase2.nearJumpScale +
                phase2.icicleRainWeight + phase2.sledRamWeight plusOrMinus 0.001f)
        }

        ROWS.forEach { (phase1, phase2) ->
            it("${phase2.name}: the first pick after the pose uses the ${phase2.name} row") {
                start(phase1)
                rollOver()
                rig.secondsWhile(2f) { rig.animKey == "fist_flash" }
                rig.leaveStand(0.5f)
                rig.lastPickTotal() shouldBe (farTotal(phase2.tuning) plusOrMinus 0.001f)
            }
        }

        it("Glacier Man takes damage; a fire hit starts the FIRED stun") {
            start()
            rig.boss.translateHealth(-30)
            rig.boss.healthPhase shouldBe 2
            rig.tick()
            rig.animKey shouldBe "fist_flash"
            rig.ticks(0.1f)
            rig.animKey shouldBe "fist_flash"
            val bullet = rig.bullet()
            rig.boss.canBeDamagedBy(bullet) shouldBe true
            val health = rig.boss.getCurrentHealth()
            rig.boss.takeDamageFrom(bullet) shouldBe true
            rig.boss.getCurrentHealth() shouldBe health - 2
            rig.ticks(0.2f)
            rig.boss.takeDamageFrom(Fireball(rig.services)) shouldBe true
            rig.tick()
            rig.animKey shouldBe "fired"
        }
    }

    describe("a fire hit that rolls the phase over") {
        it("the FIRED stun first, then the pose") {
            start()
            rig.boss.setHealth(8)
            rig.boss.takeDamageFrom(Fireball(rig.services)) shouldBe true
            rig.boss.healthPhase shouldBe 2
            rig.tick()
            rig.animKey shouldBe "fired"
            val stun = rig.secondsWhile(2f) { rig.animKey == "fired" }
            stun shouldBe (1f plusOrMinus TOLERANCE)
            withClue("the pose after the stun") {
                rig.ticksUntil(2f * STEP) { rig.animKey == "fist_flash" } shouldBe true
            }
            rig.secondsWhile(2f) { rig.animKey == "fist_flash" }
            rig.animKey shouldBe "stand"
        }
    }
})
