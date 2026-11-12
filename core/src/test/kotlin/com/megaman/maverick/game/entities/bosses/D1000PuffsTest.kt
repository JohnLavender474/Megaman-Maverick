package com.megaman.maverick.game.entities.bosses

import com.maverick.game.engine.drawables.sprites.GameSprite
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.bosses.d1000.D1000Action
import com.megaman.maverick.game.entities.bosses.d1000.D1000Director
import com.megaman.maverick.game.entities.bosses.d1000.D1000Distance
import com.megaman.maverick.game.entities.bosses.d1000.D1000Frames
import com.megaman.maverick.game.entities.bosses.d1000.D1000Lamp
import com.megaman.maverick.game.entities.bosses.d1000.D1000Tier
import com.megaman.maverick.game.entities.state.BossPhase
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.unmockkAll

private const val STAND_HEIGHT = 92
private const val WALK_HEIGHT = 91
private const val CROUCH_DROP_HEIGHT = 90
private const val CROUCH_SHAKE_HEIGHT = 83
private const val STAGGER_OR_FALL_HEIGHT = 117
private const val LAND_HEIGHT = 83
private const val STAND_UP_HEIGHT = 82
private const val MID_X_TILES = RIG_ARENA_WIDTH / RIG_PPM / 2f - 2.5f
private val HOVER_HEIGHTS = setOf(STAND_HEIGHT, WALK_HEIGHT)
private val GROUND_HEIGHTS = setOf(CROUCH_DROP_HEIGHT, CROUCH_SHAKE_HEIGHT, STAGGER_OR_FALL_HEIGHT, LAND_HEIGHT, STAND_UP_HEIGHT)

private data class Sample(val height: Int, val shown: Boolean, val hidden: Boolean, val y: Float, val frame: Int)

class D1000PuffsTest : DescribeSpec({

    lateinit var rig: D1000Rig

    beforeEach { rig = D1000Rig() }

    afterEach {
        rig.dispose()
        MegaEntityFactory.reset()
        unmockkAll()
    }

    val opening = d1000Context(cyclesSinceCombo = D1000Director.comboGap(D1000Tier.NORMAL, false))

    fun nozzle() = rig.boss.sprites[D1000.PUFF]

    fun puffs() = listOf(nozzle())

    fun bodyHeight() = rig.boss.defaultSprite.regionHeight

    fun puffFrame(sprite: GameSprite) =
        if (sprite.texture == null) -1
        else Math.round(minOf(sprite.v, sprite.v2) * RIG_TEXTURE_SIZE) / D1000Frames.PUFF_HEIGHT

    fun sample() = Sample(
        bodyHeight(), puffs().none { it.hidden }, puffs().all { it.hidden }, rig.bossCenter().y, puffFrame(nozzle())
    )

    fun record(seconds: Float): List<Sample> {
        val samples = ArrayList<Sample>()
        rig.ticksUntil(seconds) {
            samples.add(sample())
            false
        }
        return samples
    }

    fun List<Sample>.checkFloorRule() {
        forEachIndexed { i, s ->
            withClue("tick $i: height ${s.height}") {
                if (s.height in HOVER_HEIGHTS) s.shown shouldBe true
                if (s.height in GROUND_HEIGHTS) s.hidden shouldBe true
            }
        }
    }

    fun List<Sample>.firstIndexOf(height: Int, from: Int = 0) = (from until size).first { this[it].height == height }

    describe("off the floor") {
        it("puffs from the fan while the body hovers in the stand pose") {
            rig.startFight()
            rig.tick()
            bodyHeight() shouldBe STAND_HEIGHT
            puffs().none { it.hidden } shouldBe true
        }

        it("puffs while the body walks in the air") {
            rig.startFight()
            rig.rollFor(D1000Action.WALK, opening)
            rig.ticksUntil(2f) { bodyHeight() == WALK_HEIGHT } shouldBe true
            val walk = record(1f).takeWhile { it.height == WALK_HEIGHT }
            (walk.size >= 10) shouldBe true
            walk.all { it.shown } shouldBe true
        }

        it("puffs through the ceiling rise and the track at the top, not in the drop and the landing, and again in the rise back") {
            rig.placeX(MID_X_TILES, 0f)
            rig.startFight()
            rig.rollFor(D1000Action.CEILING, opening.copy(distance = D1000Distance.MID))
            val samples = record(7f)
            samples.checkFloorRule()
            val rise = samples.firstIndexOf(WALK_HEIGHT)
            val top = samples.firstIndexOf(STAND_HEIGHT, rise)
            val drop = samples.firstIndexOf(CROUCH_DROP_HEIGHT, top)
            val landing = samples.firstIndexOf(CROUCH_SHAKE_HEIGHT, drop)
            val back = samples.firstIndexOf(WALK_HEIGHT, landing)
            (samples[top].y > samples[rise].y) shouldBe true
            (samples[back].y < samples[top].y) shouldBe true
            samples[rise].shown shouldBe true
            samples[top].shown shouldBe true
            samples[drop].hidden shouldBe true
            samples[landing].hidden shouldBe true
            samples[back].shown shouldBe true
        }
    }

    describe("on the floor") {
        it("stays off in the entry fall, the landing and the stand-up, and starts with the rise") {
            rig.spawnBoss()
            val samples = record(ENTRY_SECONDS)
            samples.checkFloorRule()
            val fall = samples.firstIndexOf(STAGGER_OR_FALL_HEIGHT)
            val land = samples.firstIndexOf(LAND_HEIGHT, fall)
            val standUp = samples.firstIndexOf(STAND_UP_HEIGHT, land)
            val rise = samples.firstIndexOf(WALK_HEIGHT, standUp)
            samples[fall].hidden shouldBe true
            samples[land].hidden shouldBe true
            samples[standUp].hidden shouldBe true
            samples[rise].shown shouldBe true
            (samples.last().height == STAND_HEIGHT && samples.last().shown) shouldBe true
        }

        it("stays off through the stun: the stagger, the fall, the down window and the stand-up") {
            rig.startFight()
            rig.rollFor(D1000Action.SUMMON, opening)
            rig.ticksUntil(2f) { rig.core().vulnerable } shouldBe true
            rig.core().takeDamageFrom(rig.sabre()) shouldBe true
            val samples = record(6f)
            samples.checkFloorRule()
            val stagger = samples.firstIndexOf(STAGGER_OR_FALL_HEIGHT)
            val land = samples.firstIndexOf(LAND_HEIGHT, stagger)
            val standUp = samples.firstIndexOf(STAND_UP_HEIGHT, land)
            val rise = samples.firstIndexOf(WALK_HEIGHT, standUp)
            samples[stagger].hidden shouldBe true
            samples[land].hidden shouldBe true
            samples[rise].shown shouldBe true
        }
    }

    describe("the loop") {
        it("steps one puff frame every ${D1000Lamp.PUFF_STEP} s across body frames and across the start of a tell") {
            rig.placeX(1f, 0f)
            rig.startFight()
            rig.rollFor(D1000Action.RAM, opening)
            val samples = record(1.5f)
            samples.all { it.shown } shouldBe true
            (samples.map { it.height }.distinct().size >= 2) shouldBe true
            val runs = ArrayList<Pair<Int, Int>>()
            samples.forEach { s ->
                if (runs.isNotEmpty() && runs.last().first == s.frame) runs[runs.size - 1] = s.frame to runs.last().second + 1
                else runs.add(s.frame to 1)
            }
            val step = (D1000Lamp.PUFF_STEP * 60f).toInt()
            withClue("runs $runs") {
                (runs.size > 10) shouldBe true
                runs.drop(1).dropLast(1).all { it.second in (step - 1)..(step + 1) } shouldBe true
                runs.zipWithNext().all { (a, b) -> b.first == (a.first + 1) % D1000Frames.PUFF_FRAMES } shouldBe true
            }
        }

        it("puffs only from the fan, upward: no claw jets") {
            rig.startFight()
            rig.tick()
            nozzle().hidden shouldBe false
            nozzle().isFlipY shouldBe false
            listOf("puff_jet_front", "puff_jet_back").none { rig.boss.spritesComponent.containsSprite(it) } shouldBe true
        }
    }

    describe("the place") {
        it("faces with the body") {
            rig.placeX(RIG_ARENA_WIDTH / RIG_PPM - 1f, 0f)
            rig.startFight()
            rig.tick()
            rig.boss.defaultSprite.isFlipX shouldBe true
            puffs().forEach { it.isFlipX shouldBe true }
            rig.placeX(1f, 0f)
            rig.ticks(0.1f)
            rig.boss.defaultSprite.isFlipX shouldBe false
            puffs().forEach { it.isFlipX shouldBe false }
        }

        it("moves with the shake of the rage") {
            rig.spawnBoss()
            rig.hitEye(3)
            rig.ticks(ENTRY_SECONDS)
            rig.boss.bossComponent.phase = BossPhase.FIGHT
            rig.ticks(0.5f)
            rig.boss.enraging shouldBe true
            val body = rig.boss.defaultSprite
            val samples = ArrayList<Triple<Int, Float, Float>>()
            rig.ticksUntil(0.3f) {
                nozzle().hidden shouldBe false
                samples.add(Triple(body.regionY, body.y, nozzle().y - body.y))
                false
            }
            val frame = samples.groupBy { it.first }.maxBy { it.value.size }.value
            (frame.size >= 6) shouldBe true
            frame.map { it.third }.distinct().size shouldBe 1
            (frame.map { it.second }.distinct().size >= 2) shouldBe true
        }
    }
})
