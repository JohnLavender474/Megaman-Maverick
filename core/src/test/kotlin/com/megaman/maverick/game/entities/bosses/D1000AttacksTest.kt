package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.world.body.BodyComponent
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.world.body.IFixture
import com.megaman.maverick.game.world.body.FixtureType
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.difficulty.DifficultyMode
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.megamanx.MegamanXSabreSlash
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.hazards.D1000CeilingDebris
import com.megaman.maverick.game.entities.hazards.D1000Shockwave
import com.megaman.maverick.game.players.PlayerDamageNegotiator
import com.megaman.maverick.game.entities.state.BossPhase
import com.megaman.maverick.game.entities.projectiles.D1000Bomb
import com.megaman.maverick.game.entities.hazards.D1000Pillar
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.entities.projectiles.EnemyShot
import com.megaman.maverick.game.entities.decorations.D1000BombMarker
import com.megaman.maverick.game.entities.decorations.D1000DebrisWarning
import com.megaman.maverick.game.entities.megamanx.MegamanXValues
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlin.math.abs
import io.mockk.unmockkAll
import com.megaman.maverick.game.entities.bosses.d1000.D1000Orbs
import com.megaman.maverick.game.entities.bosses.d1000.D1000Tier
import com.megaman.maverick.game.entities.bosses.d1000.D1000Distance
import com.megaman.maverick.game.entities.bosses.d1000.D1000Action
import com.megaman.maverick.game.entities.bosses.d1000.D1000Director
import com.megaman.maverick.game.entities.bosses.d1000.D1000Tuning

private const val TOLERANCE = 0.01f
private const val ENTRY_TILES = 10.75f
private const val ENTRY_X = RIG_ARENA_X + ENTRY_TILES * RIG_PPM
private const val LEFT_RAM_STOP = RIG_ARENA_X + 2.1875f * RIG_PPM
private const val RIGHT_RAM_STOP = RIG_ARENA_X + RIG_ARENA_WIDTH - 2.1875f * RIG_PPM
private const val FAR_EYE_TILES = ENTRY_TILES + 3.25f
private const val MID_X_TILES = ENTRY_TILES - 2.5f
private const val NEAR_X_TILES = ENTRY_TILES - 1f
private const val EYE_DRIFT_TILES = 0.41f

class D1000AttacksTest : DescribeSpec({

    lateinit var rig: D1000Rig

    beforeEach { rig = D1000Rig().also { it.flyBombs = false } }

    afterEach {
        rig.dispose()
        MegaEntityFactory.reset()
        unmockkAll()
    }

    val opening = d1000Context(cyclesSinceCombo = D1000Director.comboGap(D1000Tier.NORMAL, false))

    fun tiles(px: Float) = (px - RIG_ARENA_X) / RIG_PPM

    fun bombLanes() = rig.spawnedOf<D1000Bomb>().map { tiles(rig.landingX(it)) }

    fun warnings() = rig.spawnedOf<D1000DebrisWarning>()

    fun debris() = rig.spawnedOf<D1000CeilingDebris>()

    fun warningTiles() = warnings().map { tiles(it.position.x) }

    fun warningsUnderCeiling() = warnings().all {
        val ceiling = RIG_FLOOR + RIG_ARENA_HEIGHT
        it.position.y <= ceiling && it.position.y >= ceiling - RIG_PPM
    }

    fun coreTiles() = rig.core().getComponent(BodyComponent::class)!!.body.getCenter(Vector2()).let {
        Vector2(tiles(it.x), (it.y - RIG_FLOOR) / RIG_PPM)
    }

    fun overdriveFight() {
        rig.spawnBoss()
        rig.hitEye(3)
        rig.ticks(ENTRY_SECONDS)
        rig.boss.bossComponent.phase = BossPhase.FIGHT
        rig.ticks(1.15f)
    }

    describe("the director in the fight") {
        it("starts the combo follow-up at the end of the first action, with no walk between") {
            rig.startFight()
            rig.placeX(RIG_ARENA_WIDTH / RIG_PPM - 1f, 0f)
            rig.rollFor(D1000Action.SUMMON, opening)
            rig.ticksUntil(3f) { rig.spawnedOf<EnemyShot>().size == 4 } shouldBe true
            val start = rig.bossCenter().x
            rig.ticks(1.8f)
            rig.bossCenter().x shouldBe (start plusOrMinus TOLERANCE)
            rig.ticksUntil(2f) { rig.bossCenter().x >= RIGHT_RAM_STOP - TOLERANCE } shouldBe true
        }

        it("drops the follow-up when a core hit stuns the body") {
            rig.startFight()
            rig.rollFor(D1000Action.SUMMON, opening)
            rig.ticksUntil(3f) { rig.spawnedOf<EnemyShot>().isNotEmpty() && rig.core().vulnerable } shouldBe true
            rig.core().takeDamageFrom(rig.sabre()) shouldBe true
            rig.rollFor(D1000Action.WALK, d1000Context(last = D1000Action.SUMMON))
            rig.ticksUntil(10f) { rig.bossCenter().x < ENTRY_X - TOLERANCE } shouldBe true
            val start = rig.bossCenter().x
            rig.ticks(0.5f)
            (start - rig.bossCenter().x) shouldBe (1.75f * RIG_PPM plusOrMinus 0.1f * RIG_PPM)
        }
    }

    describe("the bomb patterns") {
        data class Release(val tick: Int, val targetTiles: Float, val xTiles: Float)

        fun releases(count: Int, maxSeconds: Float, moveX: (Int) -> Float?): List<Release> {
            val out = mutableListOf<Release>()
            var tick = 0
            var xTiles = tiles(rig.xBody.getCenter(Vector2()).x)
            while (out.size < count && tick < maxSeconds * 60f) {
                rig.tick()
                tick++
                val bombs = rig.spawnedOf<D1000Bomb>()
                if (bombs.size > out.size) {
                    out.add(Release(tick, tiles(bombs.last().targetX), xTiles))
                    moveX(out.size)?.let {
                        xTiles = it
                        rig.placeX(it, 0f)
                    }
                }
            }
            return out
        }

        data class Lock(val start: Int, val end: Int, val eyeTiles: Float, val bodyTiles: Float)

        fun locks(maxSeconds: Float, count: Int): List<Lock> {
            val out = mutableListOf<Lock>()
            var start = -1
            var tick = 0
            while (out.size < count && tick < maxSeconds * 60f) {
                rig.tick()
                tick++
                val charging = rig.boss.chargeProgress() != null
                if (charging && start < 0) start = tick
                if (!charging && start >= 0) {
                    out.add(Lock(start, tick, coreTiles().x, tiles(rig.bossCenter().x)))
                    start = -1
                }
            }
            return out
        }

        fun settle(maxSeconds: Float): Boolean {
            var quiet = 0f
            var count = rig.spawnedOf<D1000Bomb>().size
            return rig.ticksUntil(maxSeconds) {
                val now = rig.spawnedOf<D1000Bomb>().size
                if (now != count) {
                    count = now
                    quiet = 0f
                } else quiet += RIG_DELTA
                count > 0 && quiet >= 2.5f
            }
        }

        listOf(
            Triple(D1000Action.BRACKET, D1000Tier.NORMAL, D1000Tuning.BRACKET_COUNT),
            Triple(D1000Action.SWEEP, D1000Tier.OVERDRIVE, D1000Tuning.SWEEP_COUNT)
        ).forEach { (action, tier, count) ->
            it("$action ($tier): $count orbs, each after an eye signal, off-X marks near X and never under the body, a flight and a ${D1000Orbs.MARK_DUR} s lock over the orb's lane") {
                rig.placeX(MID_X_TILES, 0f)
                if (tier == D1000Tier.OVERDRIVE) overdriveFight() else rig.startFight()
                rig.rollFor(action, opening.copy(tier = tier, distance = D1000Distance.MID))
                val signal = if (tier == D1000Tier.OVERDRIVE) D1000Tuning.GUIDE_SIGNAL_DUR_OVERDRIVE else D1000Tuning.GUIDE_SIGNAL_DUR
                val found = locks(12f, count)
                found.size shouldBe count
                val lockTicks = (D1000Orbs.MARK_DUR * 60f).toInt()
                found.forEach { ((it.end - it.start) in lockTicks - 2..lockTicks + 2) shouldBe true }
                found.zipWithNext { a, b -> (b.start - a.end) >= (signal * 60f).toInt() - 1 }.forEach { it shouldBe true }
                rig.spawnedOf<D1000Bomb>().size shouldBe count
                bombLanes().zip(found).forEach { (lane, lock) -> lane shouldBe (lock.eyeTiles plusOrMinus 0.1f) }
                val reach = (D1000.HALF_WIDTH + D1000Orbs.BODY_LANE_MARGIN) / RIG_PPM
                found.filter { abs(it.eyeTiles - MID_X_TILES) > 0.2f }.forEach {
                    withClue("an off-X mark at ${it.eyeTiles} tiles, body at ${it.bodyTiles}") {
                        (abs(it.eyeTiles - MID_X_TILES) <= D1000Orbs.FEINT_MAX / RIG_PPM + 0.01f) shouldBe true
                        (abs(it.eyeTiles - it.bodyTiles) >= reach - 0.01f) shouldBe true
                    }
                }
            }
        }

        it("off-X marks measure from where a running X will be: the lead point, not his place now") {
            rig.placeX(3f, 0f)
            overdriveFight()
            rig.rollFor(D1000Action.SWEEP, opening.copy(tier = D1000Tier.OVERDRIVE, distance = D1000Distance.FAR))
            val speed = -3f * RIG_PPM
            rig.xBody.physics.velocity.x = speed
            val ahead = 3f + speed * D1000Tuning.LEAD_TIME / RIG_PPM
            val found = locks(14f, D1000Tuning.SWEEP_COUNT)
            found.size shouldBe D1000Tuning.SWEEP_COUNT
            withClue("marks ${found.map { it.eyeTiles }}") {
                found.forEach {
                    val hunt = abs(it.eyeTiles - ahead) < 0.1f
                    val cut = abs(it.eyeTiles - (ahead + D1000Orbs.CUT_GAP / RIG_PPM)) < 0.1f
                    (hunt || cut) shouldBe true
                }
                found.any { abs(it.eyeTiles - ahead) >= 0.1f } shouldBe true
            }
        }

        it("Overdrive Sweep with a Ram follow-up: the last mark cuts 3 tiles past X, on the side away from the body") {
            rig.placeX(MID_X_TILES, 0f)
            overdriveFight()
            rig.rollFor(D1000Action.SWEEP, opening.copy(tier = D1000Tier.OVERDRIVE, distance = D1000Distance.MID))
            val found = locks(12f, D1000Tuning.SWEEP_COUNT)
            found.size shouldBe D1000Tuning.SWEEP_COUNT
            val side = if (MID_X_TILES < tiles(rig.bossCenter().x)) -1f else 1f
            found.last().eyeTiles shouldBe (MID_X_TILES + side * D1000Orbs.CUT_GAP / RIG_PPM plusOrMinus 0.1f)
        }

        it("aimed Bomb on Hard: the first orb hunts a moving X with a lead of ${D1000Tuning.LEAD_TIME_HARD} s of its speed") {
            rig.services.gameState.setDifficultyMode(DifficultyMode.HARD)
            rig.flyBombs = true
            rig.placeX(MID_X_TILES, 0f)
            overdriveFight()
            rig.rollFor(D1000Action.BOMB, opening.copy(tier = D1000Tier.OVERDRIVE, hard = true, distance = D1000Distance.MID))
            val speed = 3f * RIG_PPM
            rig.xBody.physics.velocity.x = speed
            val out = releases(1, 5f) { null }
            out.size shouldBe 1
            out[0].targetTiles shouldBe (MID_X_TILES + speed * D1000Tuning.LEAD_TIME_HARD / RIG_PPM plusOrMinus TOLERANCE)
        }

        it("the eye flies over a walking X, the marker shows under it at the lock start and holds ${D1000Orbs.MARK_DUR} s, and the orb flies to the marker") {
            rig.flyBombs = true
            rig.placeX(MID_X_TILES, 0f)
            overdriveFight()
            rig.rollFor(D1000Action.BOMB, opening.copy(tier = D1000Tier.OVERDRIVE, distance = D1000Distance.MID))
            val speed = MegamanXValues.WALK_SPEED * RIG_PPM
            rig.xBody.physics.velocity.x = speed
            rig.ticksUntil(4f) {
                rig.xBody.translate(speed * RIG_DELTA, 0f)
                rig.spawnedOf<D1000BombMarker>().isNotEmpty()
            } shouldBe true
            rig.boss.chargeProgress()!! shouldBe (0f plusOrMinus 0.05f)
            val marker = rig.spawnedOf<D1000BombMarker>().single()
            val marks = ArrayList<Float>()
            val eyes = ArrayList<Float>()
            while (rig.spawnedOf<D1000Bomb>().isEmpty() && marks.size < 120) {
                rig.xBody.translate(speed * RIG_DELTA, 0f)
                rig.tick()
                marks.add(tiles(marker.position.x))
                eyes.add(coreTiles().x)
            }
            val held = (D1000Orbs.MARK_DUR * 60f).toInt()
            withClue("ticks ${marks.size}") { (marks.size in held - 2..held + 2) shouldBe true }
            marks.distinct().size shouldBe 1
            eyes.dropLast(1).forEach { it shouldBe (marks[0] plusOrMinus 0.1f) }
            rig.spawnedOf<D1000Bomb>().single().targetX shouldBe (marker.position.x plusOrMinus TOLERANCE)
        }

        it("Wall volley: after X spends time in the air, the body rises, the eye beams sideways at the far wall, and each orb makes a wall-to-wall pillar above X's standing band and below the body") {
            rig.flyBombs = true
            rig.placeX(5f, 4f)
            rig.startFight()
            rig.rollFor(D1000Action.WALL, opening.copy(distance = D1000Distance.FAR, playerAirborne = true))
            val beam = rig.core().sprites[D1000Core.BEAM]
            val rotations = mutableSetOf<Float>()
            rig.ticksUntil(12f) {
                if (!beam.hidden) rotations.add(beam.rotation)
                rig.spawnedOf<D1000Pillar>().size == D1000Orbs.WALL_COUNT
            } shouldBe true
            rig.bossBody().getMaxY() shouldBe (RIG_FLOOR + RIG_ARENA_HEIGHT - 0.5f * RIG_PPM plusOrMinus 0.5f * RIG_PPM)
            rotations shouldBe setOf(90f)
            val pillars = rig.spawnedOf<D1000Pillar>()
            val bodyBottom = (rig.bossBody().getY() - RIG_FLOOR) / RIG_PPM
            withClue("pillar heights ${pillars.map { (it.baseY - RIG_FLOOR) / RIG_PPM }}, body bottom $bodyBottom") {
                pillars.forEach {
                    it.horizontal shouldBe true
                    tiles(it.baseX) shouldBe (RIG_ARENA_WIDTH / RIG_PPM plusOrMinus TOLERANCE)
                    val height = (it.baseY - RIG_FLOOR) / RIG_PPM
                    (height >= D1000Orbs.WALL_FLOOR_CLEAR / RIG_PPM - TOLERANCE) shouldBe true
                    val box = it.getComponent(BodyComponent::class)!!.body.getBounds()
                    box.getWidth() shouldBe (RIG_ARENA_WIDTH plusOrMinus TOLERANCE)
                    box.getX() shouldBe (RIG_ARENA_X plusOrMinus TOLERANCE)
                }
                pillars.any { abs((it.baseY - RIG_FLOOR) / RIG_PPM - 4.75f) < 0.1f } shouldBe true
            }
        }

        it("Pillar Fence: rises to the middle top, then sweeps the 4.1-tile grid from X's lane to the far wall, with a hunt mark between lanes") {
            rig.placeX(5f, 0f)
            rig.startFight()
            rig.rollFor(D1000Action.FENCE, opening)
            rig.ticksUntil(5f) { rig.spawnedOf<D1000Bomb>().isNotEmpty() } shouldBe true
            rig.bossCenter().x shouldBe (RIG_ARENA_X + RIG_ARENA_WIDTH / 2f plusOrMinus TOLERANCE)
            rig.bossBody().getMaxY() shouldBe (RIG_FLOOR + RIG_ARENA_HEIGHT - 0.5f * RIG_PPM plusOrMinus TOLERANCE)
            tiles(rig.spawnedOf<D1000Bomb>().first().targetX) shouldBe (5f plusOrMinus TOLERANCE)
            settle(16f) shouldBe true
            val lanes = rig.spawnedOf<D1000Bomb>().map { tiles(it.targetX) }
            val sweeps = lanes.filter { abs(it - 5f) > 0.05f }
            val expected = listOf(9.1f, 13.2f, 17.3f, 0.9f)
            withClue("lanes $lanes") {
                sweeps.size shouldBe expected.size
                sweeps.zip(expected).forEach { (lane, want) -> lane shouldBe (want plusOrMinus 0.05f) }
            }
        }

        it("Pillar Fence: a lane that leaves a wall pocket from 1 to 2 tiles is not swept") {
            rig.placeX(2f, 0f)
            rig.startFight()
            rig.rollFor(D1000Action.FENCE, opening)
            settle(16f) shouldBe true
            val sweeps = rig.spawnedOf<D1000Bomb>().map { tiles(it.targetX) }.filter { abs(it - 2f) > 0.05f }
            sweeps.none { it in 1.5f..2.49f || it in (21.5f - 2.49f)..(21.5f - 1.5f) } shouldBe true
            sweeps.minOrNull()!! shouldBe (6.1f plusOrMinus 0.05f)
        }
    }

    describe("the ram") {
        it("Wall Crash and Dazed Eye: 5 junk warnings show at the ceiling past the body, the junk falls, and the eye drops beside it, then goes home") {
            rig.startFight()
            rig.rollFor(D1000Action.RAM, opening)
            rig.ticksUntil(4f) { rig.bossCenter().x <= LEFT_RAM_STOP + TOLERANCE } shouldBe true
            rig.tick()
            warningTiles() shouldBe listOf(4.875f, 6.625f, 8.375f, 10.125f, 11.875f)
            warningsUnderCeiling() shouldBe true
            rig.ticks(D1000DebrisWarning.LEAD_DUR + 0.05f)
            (debris().size >= 5) shouldBe true
            debris().all { it.getComponent(BodyComponent::class)!!.body.physics.velocity.y <= 0f } shouldBe true
            rig.ticks(1.05f)
            coreTiles().x shouldBe (5.125f plusOrMinus 0.01f)
            coreTiles().y shouldBe (3f plusOrMinus 0.01f)
            rig.ticks(0.5f)
            coreTiles().x shouldBe (5.125f plusOrMinus 0.01f)
            val home = Vector2(FAR_EYE_TILES, 8f)
            rig.ticks(1.1f)
            val before = coreTiles().dst(home)
            rig.ticks(0.4f)
            (before - coreTiles().dst(home)) shouldBe (D1000Core.HOME_SPEED / RIG_PPM * 0.4f plusOrMinus 0.05f)
        }

        it("Wall Crash on Hard: 6 junk pieces, all from the ceiling") {
            rig.services.gameState.setDifficultyMode(DifficultyMode.HARD)
            rig.startFight()
            rig.rollFor(D1000Action.RAM, opening.copy(hard = true))
            rig.ticksUntil(4f) { rig.bossCenter().x <= LEFT_RAM_STOP + TOLERANCE } shouldBe true
            rig.tick()
            warningTiles() shouldBe listOf(4.875f, 6.625f, 8.375f, 10.125f, 11.875f, 13.625f)
            warningsUnderCeiling() shouldBe true
        }

        it("Ricochet Ram (Overdrive): hits the near wall, re-aims, then rams the far wall") {
            overdriveFight()
            rig.rollFor(D1000Action.RAM, opening.copy(tier = D1000Tier.OVERDRIVE))
            rig.ticksUntil(4f) { rig.bossCenter().x <= LEFT_RAM_STOP + TOLERANCE } shouldBe true
            warnings().size shouldBe 5
            rig.ticks(0.5f)
            rig.bossCenter().x shouldBe (LEFT_RAM_STOP plusOrMinus TOLERANCE)
            rig.ticksUntil(3f) { rig.bossCenter().x >= RIGHT_RAM_STOP - TOLERANCE } shouldBe true
            warnings().size shouldBe 10
            rig.ticks(1f)
            coreTiles().x shouldBe (tiles(RIGHT_RAM_STOP) - 2.9375f plusOrMinus 0.01f)
        }
    }

    describe("the Ceiling Crash") {
        it("rises to the top, drops junk on lanes X +- 1.25, X +- 3.75 and X +- 6.25 (none on X), tracks X, then drops to the crouch height") {
            rig.placeX(MID_X_TILES, 0f)
            rig.startFight()
            rig.rollFor(D1000Action.CEILING, opening.copy(distance = D1000Distance.MID))
            rig.ticksUntil(3f) { warnings().isNotEmpty() } shouldBe true
            rig.bossBody().getMaxY() shouldBe (RIG_FLOOR + RIG_ARENA_HEIGHT - 0.5f * RIG_PPM plusOrMinus TOLERANCE)
            warningTiles() shouldBe listOf(-6.25f, -3.75f, -1.25f, 1.25f, 3.75f, 6.25f).map { MID_X_TILES + it }
            warningsUnderCeiling() shouldBe true
            rig.ticks(1f)
            tiles(rig.bossCenter().x) shouldBe (MID_X_TILES plusOrMinus 0.01f)
            (rig.bossBody().getMaxY() > RIG_FLOOR + RIG_ARENA_HEIGHT - 0.6f * RIG_PPM) shouldBe true
            rig.ticks(0.4f)
            (rig.bossBody().getMaxY() > RIG_FLOOR + RIG_ARENA_HEIGHT - 0.6f * RIG_PPM) shouldBe true
            rig.ticks(0.8f)
            rig.bossCenter().y shouldBe (RIG_FLOOR + (1.5f + 0.703125f) * RIG_PPM plusOrMinus TOLERANCE)
        }
    }

    describe("the shockwave") {
        it("runs along the floor at 7 tiles/s, then up the wall, and ends at the ceiling") {
            val wave = MegaEntityFactory.fetch(D1000Shockwave::class)!!
            wave.spawn(
                props(
                    ConstKeys.POSITION pairTo Vector2(RIG_ARENA_X + RIG_ARENA_WIDTH - 5.5f * RIG_PPM, RIG_FLOOR),
                    ConstKeys.FACING pairTo Facing.RIGHT,
                    ConstKeys.BOUNDS pairTo rig.arena
                )
            )
            val body = wave.getComponent(BodyComponent::class)!!.body
            fun step(seconds: Float) {
                var time = 0f
                while (time < seconds - 0.0001f) {
                    wave.getComponent(UpdatablesComponent::class)!!.updatables.values().forEach { it.update(RIG_DELTA) }
                    time += RIG_DELTA
                }
            }
            step(0.5f)
            tiles(body.getCenter(Vector2()).x) shouldBe (RIG_ARENA_WIDTH / RIG_PPM - 2f plusOrMinus 0.05f)
            body.getY() shouldBe (RIG_FLOOR plusOrMinus TOLERANCE)
            body.getHeight() shouldBe (0.75f * RIG_PPM plusOrMinus TOLERANCE)
            step(0.5f)
            body.getMaxX() shouldBe (RIG_ARENA_X + RIG_ARENA_WIDTH plusOrMinus TOLERANCE)
            body.getWidth() shouldBe (0.75f * RIG_PPM plusOrMinus TOLERANCE)
            (body.getY() > RIG_FLOOR) shouldBe true
            rig.destroyed.contains(wave, true) shouldBe false
            step(1.5f)
            rig.destroyed.contains(wave, true) shouldBe true
            (body.getMaxY() >= RIG_FLOOR + RIG_ARENA_HEIGHT - TOLERANCE) shouldBe true
            PlayerDamageNegotiator(rig.x).get(wave) shouldBe 2
        }

        it("no wave leaves a slam before Overdrive") {
            rig.placeX(NEAR_X_TILES, 0f)
            rig.startFight()
            rig.ticks(1.5f)
            rig.spawnedOf<D1000Shockwave>().size shouldBe 0
        }

        it("2 waves leave the feet of a slam in Overdrive") {
            rig.placeX(NEAR_X_TILES, 0f)
            overdriveFight()
            rig.ticksUntil(2.5f) { rig.spawnedOf<D1000Shockwave>().isNotEmpty() } shouldBe true
            rig.spawnedOf<D1000Shockwave>().map { tiles(it.getComponent(BodyComponent::class)!!.body.getCenter(Vector2()).x) }
                .sorted() shouldBe listOf(ENTRY_TILES - 2.1875f, ENTRY_TILES + 2.1875f)
        }
    }

    fun coreShots() = rig.spawnedOf<EnemyShot>().filter { it.kind == EnemyShot.Kind.CORE }

    describe("the eyeball") {
        it("damages X on contact: 2, as its shot") {
            rig.startFight()
            PlayerDamageNegotiator(rig.x).get(rig.core()) shouldBe 2
            val fixtures = Array<IFixture>()
            rig.core().getComponent(BodyComponent::class)!!.body.getFixtures(fixtures, FixtureType.DAMAGER)
            fixtures.size shouldBe 3
            fixtures.all { it.isActive() } shouldBe true
        }

        it("is open while the body thinks and while it walks; shut only while hurt or while the body is stunned") {
            rig.startFight()
            rig.rollFor(D1000Action.WALK, opening)
            rig.ticks(0.2f)
            rig.core().vulnerable shouldBe true
            rig.core().canBeDamagedBy(rig.fullCharge()) shouldBe true
            rig.ticks(0.75f)
            (rig.bossCenter().x < ENTRY_X - TOLERANCE) shouldBe true
            rig.core().vulnerable shouldBe true
            rig.core().canBeDamagedBy(rig.bullet()) shouldBe true
            rig.core().takeDamageFrom(rig.sabre()) shouldBe true
            rig.tick()
            rig.boss.stunned shouldBe true
            rig.core().vulnerable shouldBe false
            rig.ticksUntil(8f) { !rig.boss.stunned } shouldBe true
            rig.ticksUntil(2f) { rig.core().vulnerable } shouldBe true
        }

        it("takes the sabre slash and every buster shot while it is open") {
            rig.startFight()
            rig.ticks(0.35f)
            rig.core().canBeDamagedBy(MegamanXSabreSlash(rig.services)) shouldBe true
            rig.core().canBeDamagedBy(Bullet(rig.services)) shouldBe true
            rig.core().canBeDamagedBy(rig.halfCharge()) shouldBe true
        }

        it("Volley: the eye rises to the rail, is over each lane at its throw, stays open, and the body faces the lane") {
            rig.placeX(MID_X_TILES, 0f)
            rig.startFight()
            rig.rollFor(D1000Action.BRACKET, opening.copy(distance = D1000Distance.MID))
            rig.ticksUntil(1.5f) { coreTiles().y >= 8.5f - 0.001f } shouldBe true
            val marks = mutableListOf<Float>()
            var bombs = 0
            rig.ticksUntil(10f) {
                val now = rig.spawnedOf<D1000Bomb>().size
                if (now > bombs) {
                    bombs = now
                    marks.add(coreTiles().x)
                    rig.core().vulnerable shouldBe true
                    val lane = tiles(rig.spawnedOf<D1000Bomb>().last().targetX)
                    rig.boss.isFacing(if (lane < tiles(rig.bossCenter().x)) Facing.LEFT else Facing.RIGHT) shouldBe true
                }
                bombs == D1000Tuning.BRACKET_COUNT
            } shouldBe true
            rig.spawnedOf<D1000Bomb>().map { tiles(it.targetX) }.forEachIndexed { i, lane -> lane shouldBe (marks[i] plusOrMinus 0.1f) }
            coreTiles().y shouldBe (8.5f plusOrMinus 0.01f)
        }

        it("Spread: a 0.6 s glow, then a fan of 5 shots; no second fan before Overdrive") {
            rig.startFight()
            rig.rollFor(D1000Action.SPREAD, opening)
            rig.ticks(0.5f + 0.5f)
            coreShots().size shouldBe 0
            rig.ticks(0.5f)
            coreShots().size shouldBe 5
            rig.ticks(1f)
            coreShots().size shouldBe 5
        }

        it("Spread in Overdrive: a second fan of 4 shots 0.35 s after the first") {
            overdriveFight()
            rig.rollFor(D1000Action.SPREAD, opening.copy(tier = D1000Tier.OVERDRIVE))
            rig.ticksUntil(2f) { coreShots().isNotEmpty() } shouldBe true
            rig.ticks(0.5f)
            coreShots().size shouldBe 9
        }

        it("Dive: when X is level with the eye and near it, the eye shakes, then dives at X, then goes home") {
            rig.startFight()
            rig.placeX(FAR_EYE_TILES + 2f, 7.5f)
            val near = opening.copy(distance = D1000Distance.FAR, playerOnWall = false, playerAbove = true, playerNearEye = true)
            rig.rollFor(D1000Action.DIVE, near)
            rig.ticks(0.5f + 0.3f)
            coreTiles().x shouldBe (FAR_EYE_TILES plusOrMinus EYE_DRIFT_TILES)
            rig.ticks(0.9f)
            val xCenter = rig.xBody.getCenter(Vector2())
            coreTiles().x shouldBe (tiles(xCenter.x) plusOrMinus 0.01f)
            coreTiles().y shouldBe ((xCenter.y - RIG_FLOOR) / RIG_PPM plusOrMinus 0.01f)
            rig.ticksUntil(2f) { abs(coreTiles().x - FAR_EYE_TILES) <= EYE_DRIFT_TILES } shouldBe true
        }
    }

    describe("Overdrive") {
        it("starts with a 1.2 s rage at the first THINK at half HP: still, open eye; then faster values") {
            rig.spawnBoss()
            rig.hitEye(3)
            rig.ticks(ENTRY_SECONDS)
            rig.boss.bossComponent.phase = BossPhase.FIGHT
            rig.boss.overdrive shouldBe false
            rig.ticks(1f)
            rig.bossCenter().x shouldBe (ENTRY_X plusOrMinus TOLERANCE)
            rig.core().vulnerable shouldBe true
            rig.boss.overdrive shouldBe false
            rig.rollFor(D1000Action.WALK, opening.copy(tier = D1000Tier.OVERDRIVE))
            rig.ticks(0.25f)
            rig.boss.overdrive shouldBe true
            rig.ticks(0.65f)
            (rig.bossCenter().x < ENTRY_X - TOLERANCE) shouldBe true
            val start = rig.bossCenter().x
            rig.ticks(0.5f)
            (start - rig.bossCenter().x) shouldBe (2.125f * RIG_PPM plusOrMinus 0.1f * RIG_PPM)
        }

        it("never starts above half HP") {
            rig.startFight()
            rig.ticks(3f)
            rig.boss.overdrive shouldBe false
        }
    }
})
