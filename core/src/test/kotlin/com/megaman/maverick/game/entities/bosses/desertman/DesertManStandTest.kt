package com.megaman.maverick.game.entities.bosses.desertman

import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.entities.bosses.DesertMan.DesertManState
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.state.BossPhase
import com.megaman.maverick.game.events.EventType
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

private val INTRO_CHANGES = listOf(0.2f, 0.32f, 0.44f, 0.69f, 0.77f, 0.85f)

class DesertManStandTest : DescribeSpec({

    lateinit var rig: DesertManRig

    afterEach { rig.dispose() }

    describe("the intro") {
        it("shows jump in the air, then plays intro once on the sand and holds the fist-up frame until STAND") {
            rig = DesertManRig()
            rig.spawnBoss()
            val keys = mutableListOf<String?>()
            rig.ticksUntil(3f, each = { keys.add(rig.animKey) }) { rig.animKey == "intro" } shouldBe true
            keys.dropLast(1).filter { it != null }.forEach { it shouldBe "jump" }
            val changes = mutableListOf<Float>()
            var last = rig.animIndex
            var time = 0f
            while (time < 3f) {
                rig.tick()
                time += STEP
                withClue("at $time") {
                    rig.animKey shouldBe "intro"
                    rig.state shouldBe DesertManState.INIT
                }
                if (rig.animIndex != last) {
                    changes.add(time)
                    last = rig.animIndex
                }
            }
            last shouldBe 6
            changes.size shouldBe 6
            changes.zip(INTRO_CHANGES).forEach { (seen, expected) -> seen shouldBe (expected plusOrMinus 2f * STEP) }
            rig.boss.bossComponent.phase shouldBe BossPhase.INTRO
            rig.boss.bossComponent.phase = BossPhase.FIGHT
            val after = mutableListOf<String?>()
            rig.ticksUntil(1f, each = { after.add(rig.animKey) }) { rig.state == DesertManState.STAND } shouldBe true
            (keys + after) shouldNotContain "dance"
            after.dropLast(1).forEach { it shouldBe "intro" }
        }
    }

    describe("the guards of STAND") {
        data class Row(val name: String, val hard: Boolean, val phase2: Boolean)

        val rows = listOf(
            Row("normal", hard = false, phase2 = false),
            Row("normal phase 2", hard = false, phase2 = true),
            Row("hard", hard = true, phase2 = false),
            Row("hard phase 2", hard = true, phase2 = true)
        )

        fun start(row: Row) {
            rig = DesertManRig(hard = row.hard)
            rig.startFight(phase2 = row.phase2)
        }

        fun chooses(row: Row, roll: Float, playerX: Float, playerY: Float, before: Int): DesertManState {
            start(row)
            rig.place(playerX, playerY)
            val rolls = FloatArray(before + 1) { FAIL }
            rolls[before] = roll
            rig.rolls(*rolls)
            rig.ticksUntil(3f) { rig.state != DesertManState.STAND } shouldBe true
            return rig.state
        }

        rows.forEach { row ->
            val tuning = DesertManTuning.of(row.hard, if (row.phase2) 2 else 1)

            it("${row.name}: the punch roll is under ${tuning.punchChance}") {
                chooses(row, tuning.punchChance - 0.001f, 18f, 0f, 1) shouldBe DesertManState.PUNCH
                rig.dispose()
                chooses(row, tuning.punchChance, 18f, 0f, 1) shouldNotBe DesertManState.PUNCH
            }

            it("${row.name}: the tornado roll is under ${tuning.tornadoChance}") {
                chooses(row, tuning.tornadoChance - 0.001f, 21.5f, 0f, 1) shouldBe DesertManState.TORNADO
                rig.dispose()
                chooses(row, tuning.tornadoChance, 21.5f, 0f, 1) shouldNotBe DesertManState.TORNADO
            }

            it("${row.name}: the needle spin roll is under ${tuning.needleSpinChance}") {
                chooses(row, tuning.needleSpinChance - 0.001f, 21.5f, 0f, 2) shouldBe DesertManState.NEEDLE_SPIN
                rig.dispose()
                chooses(row, tuning.needleSpinChance, 21.5f, 0f, 2) shouldNotBe DesertManState.NEEDLE_SPIN
            }

            it("${row.name}: the throw roll is under ${tuning.throwChance}") {
                chooses(row, tuning.throwChance - 0.001f, 21.5f, 0f, 3) shouldBe DesertManState.THROW
                rig.dispose()
                chooses(row, tuning.throwChance, 21.5f, 0f, 3) shouldNotBe DesertManState.THROW
            }

            it("${row.name}: the dance roll is under ${tuning.danceChance}") {
                chooses(row, tuning.danceChance - 0.001f, 21.5f, 0f, 4) shouldBe DesertManState.DANCE
                rig.dispose()
                chooses(row, tuning.danceChance, 21.5f, 0f, 4) shouldNotBe DesertManState.DANCE
            }

            if (row.phase2) it("${row.name}: the burrow roll is under ${tuning.burrowChance}") {
                chooses(row, tuning.burrowChance - 0.001f, 18f, 0f, 0) shouldBe DesertManState.BURROW
                rig.dispose()
                chooses(row, tuning.burrowChance, 18f, 0f, 0) shouldNotBe DesertManState.BURROW
            } else it("${row.name}: never burrows, even with a roll of 0") {
                start(row)
                rig.place(18f, 0f)
                rig.attack(DesertManState.PUNCH, PASS, PASS)
            }
        }

        it("jumps when no guard passes") {
            rig = DesertManRig()
            rig.startFight()
            rig.place(18f, 0f)
            rig.attack(DesertManState.JUMP)
        }
    }

    describe("the dance") {
        it("never comes twice in a row") {
            rig = DesertManRig()
            EntityFactories.initialize(rig.services)
            EntityFactories.init()
            try {
                rig.startFight()
                rig.place(21.5f, 0f)
                rig.attack(DesertManState.DANCE, FAIL, FAIL, FAIL, FAIL, PASS)
                rig.ticksUntil(4f) { rig.state == DesertManState.STAND } shouldBe true
                rig.attack(DesertManState.JUMP, FAIL, FAIL, FAIL, FAIL, PASS, PASS)
            } finally {
                EntityFactories.clear()
            }
        }
    }

    describe("the tuning rows") {
        it("follow the difficulty and the phase") {
            DesertManTuning.of(hard = false, phase = 1) shouldBe DesertManTuning.NORMAL
            DesertManTuning.of(hard = false, phase = 2) shouldBe DesertManTuning.NORMAL_PHASE_2
            DesertManTuning.of(hard = true, phase = 1) shouldBe DesertManTuning.HARD
            DesertManTuning.of(hard = true, phase = 2) shouldBe DesertManTuning.HARD_PHASE_2
        }

        it("give the chances of the spec") {
            val rows = listOf(
                DesertManTuning.NORMAL, DesertManTuning.NORMAL_PHASE_2, DesertManTuning.HARD,
                DesertManTuning.HARD_PHASE_2
            )
            rows.map { it.burrowChance } shouldBe listOf(0f, 0.35f, 0f, 0.4f)
            rows.map { it.punchChance } shouldBe listOf(0.75f, 0.75f, 0.75f, 0.75f)
            rows.map { it.tornadoChance } shouldBe listOf(0.6f, 0.6f, 0.7f, 0.7f)
            rows.map { it.danceChance } shouldBe listOf(0.5f, 0.5f, 0.5f, 0.5f)
            rows.map { it.throwChance } shouldBe listOf(0.5f, 0.6f, 0.6f, 0.7f)
            rows.map { it.needleSpinChance } shouldBe listOf(0.45f, 0.5f, 0.5f, 0.55f)
        }

        it("a punch that starts in phase 1 keeps the phase-1 hold time after the phase 2 starts") {
            val widths = mutableListOf<Float>()
            rig = DesertManRig()
            rig.startFight()
            rig.place(18f, 0f)
            rig.attack(DesertManState.PUNCH, FAIL, PASS)
            rig.toPhase2()
            rig.secondsWhile(each = {
                if (rig.armDamager.isActive()) widths.add(rig.rect(rig.armDamager).getWidth())
            }) { rig.state == DesertManState.PUNCH }
            val reach = widths.max()
            val held = widths.count { it >= reach - 0.01f } * STEP
            held shouldBe (DesertManTuning.PUNCH_HOLD_DUR + STEP plusOrMinus 1.5f * STEP)
        }
    }

    describe("the phase-2 start") {
        it("shakes and quakes at once, and shows dance_flash at the next STAND for 0.6 s before the stand timer runs") {
            rig = DesertManRig()
            rig.startFight()
            rig.place(18f, 0f)
            rig.attack(DesertManState.PUNCH, FAIL, PASS)
            val shakes = rig.eventsOf(EventType.SHAKE_CAM)
            val quakes = rig.sounds(SoundAsset.QUAKE_SOUND)
            rig.toPhase2()
            rig.tick()
            rig.eventsOf(EventType.SHAKE_CAM) shouldBe shakes + 1
            rig.sounds(SoundAsset.QUAKE_SOUND) shouldBe quakes + 1
            val punchKeys = mutableListOf<String?>()
            rig.secondsWhile(each = { punchKeys.add(rig.animKey) }) { rig.state == DesertManState.PUNCH }
            punchKeys.dropLast(1).forEach { it shouldBe "punch" }
            rig.state shouldBe DesertManState.STAND
            val keys = mutableListOf<String?>()
            val standing = rig.secondsWhile(each = { keys.add(rig.animKey) }) { rig.state == DesertManState.STAND }
            val flash = keys.count { it == "dance_flash" } * STEP
            flash shouldBe (0.6f plusOrMinus 2f * STEP)
            keys.takeWhile { it == "dance_flash" }.size shouldBe keys.count { it == "dance_flash" }
            keys.dropLast(1).filter { it != "dance_flash" }.forEach { it shouldBe "stand" }
            standing shouldBe (0.6f + DesertManTuning.STAND_DUR_PHASE_2 plusOrMinus 3f * STEP)
            keys.shouldContainInOrder("dance_flash", "stand")
        }
    }

    describe("a respawn") {
        it("forgets the last attack, so the punch can come first again") {
            rig = DesertManRig()
            rig.startFight()
            rig.place(18f, 0f)
            rig.attack(DesertManState.PUNCH, FAIL, PASS)
            rig.ticksUntil(4f) { rig.state == DesertManState.STAND } shouldBe true
            rig.respawn()
            rig.place(18f, 0f)
            rig.attack(DesertManState.PUNCH, FAIL, PASS)
        }

        it("turns the X friction back on, also after a death in the erupt jump") {
            rig = DesertManRig()
            rig.startFight()
            rig.bossBody.physics.applyFrictionX = false
            rig.respawn()
            rig.bossBody.physics.applyFrictionX shouldBe true
        }
    }
})
