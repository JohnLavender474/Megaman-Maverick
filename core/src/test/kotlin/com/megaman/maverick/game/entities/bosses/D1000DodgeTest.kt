package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.IFixture
import com.megaman.maverick.game.difficulty.DifficultyMode
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.decorations.D1000BombMarker
import com.megaman.maverick.game.entities.hazards.D1000Pillar
import com.megaman.maverick.game.entities.megamanx.MegamanXValues
import com.megaman.maverick.game.entities.projectiles.D1000Bomb
import com.megaman.maverick.game.entities.projectiles.JunkShot
import com.megaman.maverick.game.entities.state.BossPhase
import com.megaman.maverick.game.world.body.FixtureType
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.unmockkAll
import kotlin.math.abs
import kotlin.math.min
import com.megaman.maverick.game.entities.bosses.d1000.D1000Tier
import com.megaman.maverick.game.entities.bosses.d1000.D1000Distance
import com.megaman.maverick.game.entities.bosses.d1000.D1000Action
import com.megaman.maverick.game.entities.bosses.d1000.D1000Director
import com.megaman.maverick.game.entities.bosses.d1000.D1000Tuning

private const val REACTION_TICKS = 12
private const val SAFETY = 0.125f * RIG_PPM
private const val SEARCH_STEP = RIG_PPM / 32f
private const val MIN_GAP = 3f * RIG_PPM
private const val MIN_WALL_POCKET = 2f * RIG_PPM
private const val GAP_TOLERANCE = 0.01f * RIG_PPM
private const val MAX_PATTERN_SECONDS = 20f
private const val QUIET_END = 2.5f

class D1000DodgeTest : DescribeSpec({

    var rig: D1000Rig? = null

    afterEach {
        rig?.dispose()
        rig = null
        MegaEntityFactory.reset()
        unmockkAll()
    }

    data class Result(
        val bombs: Int, val pillars: Int, val hitTick: Int, val minGap: Float, val minPocket: Float, val x: Float
    ) {
        override fun toString() = "bombs=$bombs pillars=$pillars hitTick=$hitTick " +
            "gapAroundX=${minGap / RIG_PPM} wallPocketAroundX=${minPocket / RIG_PPM} tiles endX=$x tiles"
    }

    fun damager(pillar: D1000Pillar): IFixture {
        val out = Array<IFixture>()
        pillar.getComponent(BodyComponent::class)!!.body.getFixtures(out, FixtureType.DAMAGER)
        return out.first()
    }

    fun live(rig: D1000Rig, entity: IGameEntity) = !rig.destroyed.contains(entity, true)

    fun pillarX(pillar: D1000Pillar) = pillar.getComponent(BodyComponent::class)!!.body.getCenter(Vector2()).x

    fun startPattern(
        action: D1000Action, tier: D1000Tier, hard: Boolean, arenaTiles: Float, xTiles: Float
    ): D1000Rig {
        val made = D1000Rig(arenaWidth = arenaTiles * RIG_PPM).also { rig = it }
        if (hard) made.services.gameState.setDifficultyMode(DifficultyMode.HARD)
        made.flyBombs = true
        made.placeX(xTiles, 0f)
        made.spawnBoss()
        if (tier == D1000Tier.OVERDRIVE) made.hitEye(3)
        made.ticks(ENTRY_SECONDS)
        made.boss.bossComponent.phase = BossPhase.FIGHT
        if (tier == D1000Tier.OVERDRIVE) made.ticks(1.15f)
        fun roll(gap: Float) = made.rollFor(
            action,
            d1000Context(
                tier = tier, distance = if (gap <= 5f) D1000Distance.MID else D1000Distance.FAR, hard = hard,
                cyclesSinceCombo = D1000Director.comboGap(tier, hard)
            )
        )
        roll(abs(xTiles - arenaTiles / 2f))
        made.tick()
        if (made.boss.retreating) {
            made.ticksUntil(D1000.RETREAT_MAX_DUR + 0.5f) { !made.boss.retreating } shouldBe true
            roll(abs(RIG_ARENA_X + xTiles * RIG_PPM - made.boss.origin.x) / RIG_PPM)
        }
        return made
    }

    fun dodge(rig: D1000Rig, noEyeShots: Boolean = false): Result {
        val arena = rig.arena
        val half = rig.xBody.getWidth() / 2f
        val minX = arena.getX() + half
        val maxX = arena.getMaxX() - half
        val step = MegamanXValues.WALK_SPEED * RIG_PPM * RIG_DELTA
        val seenAt = HashMap<IGameEntity, Int>()
        var tick = 0
        var started = false
        var quiet = 0f
        var hitTick = -1
        var minGap = Float.POSITIVE_INFINITY
        var minPocket = Float.POSITIVE_INFINITY
        var xCenter = rig.xBody.getCenter(Vector2()).x

        while (tick < MAX_PATTERN_SECONDS / RIG_DELTA) {
            val markers = rig.spawnedOf<D1000BombMarker>().filter { live(rig, it) }
            markers.forEach { seenAt.getOrPut(it) { tick } }
            val pillars = rig.spawnedOf<D1000Pillar>().filter { live(rig, it) }
            val bombs = rig.spawnedOf<D1000Bomb>().filter { live(rig, it) }
            if (bombs.isNotEmpty() || pillars.isNotEmpty()) started = true
            quiet = if (pillars.isEmpty() && bombs.isEmpty() && markers.isEmpty()) quiet + RIG_DELTA else 0f
            if (started && quiet >= QUIET_END) break

            val hazards = markers.filter { tick - seenAt[it]!! >= REACTION_TICKS }.map { it.position.x } +
                pillars.map { pillarX(it) }
            fun danger(c: Float) = hazards.any { abs(c - it) < 2f * half + SAFETY }
            fun blocked(c: Float) = c < minX || c > maxX || pillars.any { abs(c - pillarX(it)) < 2f * half }

            if (danger(xCenter)) {
                var target: Float? = null
                var offset = SEARCH_STEP
                var leftOpen = true
                var rightOpen = true
                while (target == null && (leftOpen || rightOpen)) {
                    if (leftOpen) {
                        val c = xCenter - offset
                        if (blocked(c)) leftOpen = false else if (!danger(c)) target = c
                    }
                    if (target == null && rightOpen) {
                        val c = xCenter + offset
                        if (blocked(c)) rightOpen = false else if (!danger(c)) target = c
                    }
                    offset += SEARCH_STEP
                }
                if (target != null) xCenter += (target - xCenter).coerceIn(-step, step)
            }
            rig.placeX((xCenter - arena.getX()) / RIG_PPM, 0f)
            if (noEyeShots) rig.spawnedOf<JunkShot>().forEach { if (live(rig, it)) it.destroy() }

            rig.tick()
            tick++

            val active = rig.spawnedOf<D1000Pillar>().filter { live(rig, it) && damager(it).isActive() }
                .map { damager(it).getShape() as GameRectangle }.sortedBy { it.getX() }
            if (hitTick < 0 && active.any { it.getX() < rig.xBody.getMaxX() && it.getMaxX() > rig.xBody.getX() })
                hitTick = tick
            if (active.isNotEmpty()) {
                val center = rig.xBody.getCenter(Vector2()).x
                val left = active.filter { it.getMaxX() <= center }.maxOfOrNull { it.getMaxX() }
                val right = active.filter { it.getX() >= center }.minOfOrNull { it.getX() }
                val span = (right ?: arena.getMaxX()) - (left ?: arena.getX())
                if (left != null && right != null) minGap = min(minGap, span) else minPocket = min(minPocket, span)
            }
        }
        return Result(
            rig.spawnedOf<D1000Bomb>().size, rig.spawnedOf<D1000Pillar>().size, hitTick, minGap, minPocket,
            (xCenter - arena.getX()) / RIG_PPM
        )
    }

    data class Pattern(val action: D1000Action, val tier: D1000Tier)

    val patterns = listOf(
        Pattern(D1000Action.BRACKET, D1000Tier.NORMAL),
        Pattern(D1000Action.BRACKET, D1000Tier.OVERDRIVE),
        Pattern(D1000Action.FENCE, D1000Tier.NORMAL),
        Pattern(D1000Action.BOMB, D1000Tier.OVERDRIVE),
        Pattern(D1000Action.SWEEP, D1000Tier.OVERDRIVE)
    )

    describe("a walking X that reads only the markers (0.2 s late) dodges every orb; the Fence leaves a gap of 3 tiles or more between pillars (2 tiles against a wall)") {
        patterns.forEach { pattern ->
            listOf(false, true).forEach { hard ->
                listOf(18.5f, 21.5f).forEach { arenaTiles ->
                    listOf(1f, arenaTiles / 2f - 3.5f, arenaTiles - 1f).forEach { xTiles ->
                        val mode = if (hard) "Hard" else "Normal"
                        it("${pattern.action} ($mode, ${pattern.tier}), $arenaTiles-tile arena, X at $xTiles tiles") {
                            val result = dodge(startPattern(pattern.action, pattern.tier, hard, arenaTiles, xTiles))
                            withClue("$result") {
                                (result.bombs > 0) shouldBe true
                                result.pillars shouldBe result.bombs
                                result.hitTick shouldBe -1
                                if (pattern.action == D1000Action.FENCE) {
                                    (result.minGap >= MIN_GAP - GAP_TOLERANCE) shouldBe true
                                    (result.minPocket >= MIN_WALL_POCKET - GAP_TOLERANCE) shouldBe true
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    describe("the orb counts") {
        data class Count(val action: D1000Action, val tier: D1000Tier, val hard: Boolean, val bombs: Int)
        listOf(
            Count(D1000Action.BRACKET, D1000Tier.NORMAL, false, D1000Tuning.BRACKET_COUNT),
            Count(D1000Action.BRACKET, D1000Tier.NORMAL, true, D1000Tuning.BRACKET_COUNT_HARD),
            Count(D1000Action.FENCE, D1000Tier.NORMAL, false, 7),
            Count(D1000Action.FENCE, D1000Tier.NORMAL, true, 7),
            Count(D1000Action.SWEEP, D1000Tier.OVERDRIVE, false, D1000Tuning.SWEEP_COUNT),
            Count(D1000Action.SWEEP, D1000Tier.OVERDRIVE, true, D1000Tuning.SWEEP_COUNT_HARD),
            Count(D1000Action.BOMB, D1000Tier.OVERDRIVE, false, D1000Tuning.BOMB_COUNT),
            Count(D1000Action.BOMB, D1000Tier.OVERDRIVE, true, D1000Tuning.BOMB_COUNT_HARD)
        ).forEach { count ->
            val mode = if (count.hard) "Hard" else "Normal"
            it("${count.action} ($mode) throws ${count.bombs} orbs in the 18.5-tile arena with X 3.5 tiles from the middle and no eye shot in the air") {
                val made = startPattern(count.action, count.tier, count.hard, 18.5f, 18.5f / 2f - 3.5f)
                val result = dodge(made, noEyeShots = true)
                withClue("$result") { result.bombs shouldBe count.bombs }
            }
        }
    }
})
