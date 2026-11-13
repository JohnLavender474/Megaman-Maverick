package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.world.body.BodyComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.difficulty.DifficultyMode
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.hazards.D1000Pillar
import com.megaman.maverick.game.entities.projectiles.EnemyShot
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import io.mockk.unmockkAll
import com.megaman.maverick.game.entities.bosses.d1000.D1000Tell
import com.megaman.maverick.game.entities.bosses.d1000.D1000Tier
import com.megaman.maverick.game.entities.bosses.d1000.D1000Action
import com.megaman.maverick.game.entities.bosses.d1000.D1000Director
import com.megaman.maverick.game.entities.bosses.d1000.D1000Tuning

private const val TOLERANCE = 0.01f
private const val HOME_X_TILES = 10.75f + 3.25f
private const val HOME_Y_TILES = 8f
private const val X_CENTER_TILES = 0.75f
private const val SIDE_TILES = 2.5f
private const val UP_TILES = 1f
private const val EYE_DRIFT_TILES = 0.41f

class D1000FlankTest : DescribeSpec({

    lateinit var rig: D1000Rig

    beforeEach { rig = D1000Rig() }

    afterEach {
        rig.dispose()
        MegaEntityFactory.reset()
        unmockkAll()
    }

    val opening = d1000Context(cyclesSinceCombo = D1000Director.comboGap(D1000Tier.NORMAL, false))

    fun tiles(px: Float) = (px - RIG_ARENA_X) / RIG_PPM

    fun coreCenter(): Vector2 = rig.core().getComponent(BodyComponent::class)!!.body.getCenter(Vector2())

    fun coreTiles() = coreCenter().let { Vector2(tiles(it.x), (it.y - RIG_FLOOR) / RIG_PPM) }

    fun coreShots() = rig.spawnedOf<EnemyShot>().filter { it.kind == EnemyShot.Kind.CORE }

    fun animKey() = (rig.core().getComponent(AnimationsComponent::class)!!
        .getAnimator(AnimationsComponent.DEFAULT_KEY) as Animator).currentKey!!

    fun startFlank(xTiles: Float, hard: Boolean = false) {
        if (hard) rig.services.gameState.setDifficultyMode(DifficultyMode.HARD)
        rig.placeX(xTiles, 0f)
        rig.startFight()
        rig.rollFor(D1000Action.FLANK, opening.copy(hard = hard))
    }

    fun arrivesAt(xTiles: Float, yTiles: Float, maxSeconds: Float = 4f) = rig.ticksUntil(maxSeconds) {
        val eye = coreTiles()
        eye.x in (xTiles - TOLERANCE)..(xTiles + TOLERANCE) && eye.y in (yTiles - TOLERANCE)..(yTiles + TOLERANCE)
    }

    fun secondsUntil(maxSeconds: Float, done: () -> Boolean): Float {
        var time = 0f
        while (time < maxSeconds && !done()) {
            rig.tick()
            time += RIG_DELTA
        }
        return time
    }

    describe("the approach") {
        it("stops beside X on the side away from the nearest wall, 2.5 tiles out and 1 tile up: X near the left wall") {
            startFlank(5f)
            arrivesAt(5f + SIDE_TILES, X_CENTER_TILES + UP_TILES) shouldBe true
        }

        it("takes the left side when X is nearer the right wall") {
            startFlank(16.5f)
            arrivesAt(16.5f - SIDE_TILES, X_CENTER_TILES + UP_TILES) shouldBe true
        }

        it("moves at 7.5 tiles per second") {
            startFlank(5f)
            rig.ticksUntil(2f) { coreTiles().dst(HOME_X_TILES, HOME_Y_TILES) > EYE_DRIFT_TILES + 0.1f } shouldBe true
            val point = Vector2(5f + SIDE_TILES, X_CENTER_TILES + UP_TILES)
            val before = coreTiles().dst(point)
            rig.ticks(0.4f)
            (before - coreTiles().dst(point)) shouldBe (D1000Core.FLANK_SPEED / RIG_PPM * 0.4f plusOrMinus 0.1f)
        }

        it("runs a Spread from home in place of the approach when both sides are blocked (a wall and a pillar)") {
            rig.placeX(3f, 0f)
            rig.startFight()
            rig.ticks(0.1f)
            MegaEntityFactory.fetch(D1000Pillar::class)!!.spawn(
                props(
                    ConstKeys.POSITION pairTo Vector2(RIG_ARENA_X + 5f * RIG_PPM, RIG_FLOOR),
                    ConstKeys.MAX_Y pairTo RIG_FLOOR + 4f * RIG_PPM
                )
            )
            rig.core().summon(D1000EyeAttack.FLANK)
            rig.ticksUntil(1.5f) { coreShots().isNotEmpty() } shouldBe true
            coreShots().size shouldBe 5
            coreTiles().x shouldBe (HOME_X_TILES plusOrMinus EYE_DRIFT_TILES)
            coreTiles().y shouldBe (HOME_Y_TILES plusOrMinus EYE_DRIFT_TILES)
        }
    }

    describe("the pause") {
        it("holds still for 0.8 s, open to damage, with the stare and then the angry glow in the second half; no shot before it ends") {
            startFlank(5f)
            arrivesAt(5f + SIDE_TILES, X_CENTER_TILES + UP_TILES) shouldBe true
            val point = coreCenter()
            var stillFor = 0f
            var glowFrom = -1f
            while (coreShots().isEmpty() && stillFor < 2f) {
                rig.core().vulnerable shouldBe true
                coreCenter() shouldBe point
                if (stillFor < D1000Tuning.FLANK_PAUSE / 2f - 0.02f) animKey() shouldBe "core_stare"
                if (glowFrom < 0f && animKey() == "core_angry") glowFrom = stillFor
                rig.tick()
                stillFor += RIG_DELTA
            }
            stillFor shouldBe (D1000Tuning.FLANK_PAUSE plusOrMinus 0.04f)
            glowFrom shouldBe (D1000Tuning.FLANK_PAUSE / 2f plusOrMinus 0.04f)
        }

        it("lasts 0.6 s on Hard") {
            startFlank(5f, hard = true)
            arrivesAt(5f + SIDE_TILES, X_CENTER_TILES + UP_TILES) shouldBe true
            secondsUntil(2f) { coreShots().isNotEmpty() } shouldBe (D1000Tuning.FLANK_PAUSE_HARD plusOrMinus 0.04f)
        }

        it("a sabre slash in the pause stuns the body and ends the attack with no shot") {
            startFlank(5f)
            arrivesAt(5f + SIDE_TILES, X_CENTER_TILES + UP_TILES) shouldBe true
            rig.ticks(0.3f)
            rig.core().takeDamageFrom(rig.sabre()) shouldBe true
            rig.tick()
            rig.boss.stunned shouldBe true
            rig.core().vulnerable shouldBe false
            rig.ticks(3f)
            coreShots().size shouldBe 0
        }
    }

    describe("the shots") {
        fun aimedAtX(shot: EnemyShot, from: Vector2) {
            val want = rig.xBody.getCenter(Vector2()).sub(from).nor()
            val velocity = shot.getComponent(BodyComponent::class)!!.body.physics.velocity
            velocity.len() shouldBe (D1000Core.SHOT_SPEED plusOrMinus 0.5f)
            withClue("shot direction $velocity, want $want") { (velocity.cpy().nor().dot(want) > 0.999f) shouldBe true }
        }

        it("Normal: 2 core orbs 0.2 s apart, each aimed at X's center from the flank point") {
            startFlank(5f)
            arrivesAt(5f + SIDE_TILES, X_CENTER_TILES + UP_TILES) shouldBe true
            val point = coreCenter()
            rig.ticksUntil(1.5f) { coreShots().size == 1 } shouldBe true
            aimedAtX(coreShots()[0], point)
            secondsUntil(1f) { coreShots().size == 2 } shouldBe (D1000Core.FLANK_SHOT_GAP plusOrMinus 0.02f)
            aimedAtX(coreShots()[1], point)
            rig.ticks(1f)
            coreShots().size shouldBe 2
        }

        it("Hard: 3 core orbs") {
            startFlank(5f, hard = true)
            arrivesAt(5f + SIDE_TILES, X_CENTER_TILES + UP_TILES) shouldBe true
            rig.ticksUntil(1.5f) { coreShots().size == 1 } shouldBe true
            rig.ticks(2f * D1000Core.FLANK_SHOT_GAP + 0.05f)
            coreShots().size shouldBe 3
            rig.ticks(1f)
            coreShots().size shouldBe 3
        }
    }

    describe("the escape") {
        it("dashes home at 10 tiles per second right after the last shot, then idles there") {
            startFlank(5f)
            arrivesAt(5f + SIDE_TILES, X_CENTER_TILES + UP_TILES) shouldBe true
            rig.ticksUntil(1.5f) { coreShots().size == 2 } shouldBe true
            val home = Vector2(HOME_X_TILES, HOME_Y_TILES)
            val before = coreTiles().dst(home)
            rig.ticks(0.3f)
            (before - coreTiles().dst(home)) shouldBe (10f * 0.3f plusOrMinus 0.1f)
            arrivesAt(HOME_X_TILES, HOME_Y_TILES, 1f) shouldBe true
            rig.core().vulnerable shouldBe true
            rig.ticks(1f)
            coreTiles().x shouldBe (HOME_X_TILES plusOrMinus EYE_DRIFT_TILES)
            coreTiles().y shouldBe (HOME_Y_TILES plusOrMinus EYE_DRIFT_TILES)
        }
    }

    describe("the director") {
        it("weighs the Flank as the Spread (8, Overdrive 10), not while X is on a wall, and gives it no combo") {
            D1000Director.weight(D1000Action.FLANK, d1000Context()) shouldBe 8
            D1000Director.weight(D1000Action.FLANK, d1000Context(tier = D1000Tier.OVERDRIVE)) shouldBe 10
            D1000Director.weight(D1000Action.FLANK, d1000Context(playerOnWall = true)) shouldBe 0
            D1000Director.comboOf(D1000Action.FLANK) shouldBe null
            D1000Action.FLANK.family shouldBe D1000Tell.EYE
        }
    }
})
