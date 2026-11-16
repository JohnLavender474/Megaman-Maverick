package com.megaman.maverick.game.entities.bosses.desertman

import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.shapes.GameRectangle
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.entities.bosses.DesertMan.DesertManState
import com.megaman.maverick.game.entities.decorations.Splash
import com.megaman.maverick.game.events.EventType
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.floats.shouldBeGreaterThanOrEqual
import io.kotest.matchers.floats.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.math.abs
import kotlin.math.max

private const val TOL = 0.01f
private const val WALL_TOL = TOL
private const val SHOULDER = 12f * ART_PX
private const val REST = 10f * ART_PX
private const val MIN_LENGTH = 6f * ART_PX
private const val REACH = 9f * T
private const val MAX_STEP = 0.5f * T
private const val FIST_SPRITE_WIDTH = 12f * ART_PX
private const val FIST_HIT_WIDTH = 11f * ART_PX
private const val FIST_HIT_HEIGHT = 9f * ART_PX
private const val ARM_HEIGHT = 5f * ART_PX
private const val SPRITE_BOTTOM = -0.5f * T

private class Frame(
    val state: DesertManState,
    val facing: Facing,
    val centerX: Float,
    val bodyY: Float,
    val tremble: Float,
    val armActive: Boolean,
    val fistActive: Boolean,
    val armDamageableActive: Boolean,
    val fistDamageableActive: Boolean,
    val arm: GameRectangle,
    val fist: GameRectangle,
    val armDamageable: GameRectangle,
    val fistDamageable: GameRectangle,
    val armSpriteHidden: Boolean,
    val armSpriteX: Float,
    val armSpriteMaxX: Float,
    val fistSpriteHidden: Boolean,
    val fistSpriteX: Float,
    val fistSpriteMaxX: Float,
    val fistRegionY: Int,
    val speedHidden: Boolean
) {
    val length: Float
        get() = arm.getWidth()
    val shoulder: Float
        get() = centerX + tremble + facing.value * SHOULDER
    val tip: Float
        get() = if (facing == Facing.RIGHT) arm.getMaxX() else arm.getX()
}

private fun moveDur(distance: Float, averageSpeed: Float) =
    max(distance / (averageSpeed * T), distance * MathUtils.HALF_PI / (30f * T))

class DesertManPunchTest : DescribeSpec({

    lateinit var rig: DesertManRig

    afterEach { rig.dispose() }

    fun frame() = Frame(
        rig.state, rig.boss.facing, rig.bossCenter().x, rig.bossBody.getY(),
        rig.sprite(ConstKeys.MAIN).x - (rig.bossCenter().x - HALF_SPRITE),
        rig.armDamager.isActive(), rig.fistDamager.isActive(), rig.armDamageable.isActive(),
        rig.fistDamageable.isActive(), GameRectangle(rig.rect(rig.armDamager)), GameRectangle(rig.rect(rig.fistDamager)),
        GameRectangle(rig.rect(rig.armDamageable)), GameRectangle(rig.rect(rig.fistDamageable)),
        rig.sprite(ConstKeys.ARM).hidden, rig.sprite(ConstKeys.ARM).x,
        rig.sprite(ConstKeys.ARM).x + rig.sprite(ConstKeys.ARM).width, rig.sprite(ConstKeys.HAND).hidden,
        rig.sprite(ConstKeys.HAND).x, rig.sprite(ConstKeys.HAND).x + rig.sprite(ConstKeys.HAND).width,
        rig.sprite(ConstKeys.HAND).regionY, rig.sprite(ConstKeys.SPEED).hidden
    )

    fun start(hard: Boolean = false, phase2: Boolean = false, playerX: Float = 18f, bossX: Float? = null) {
        rig = DesertManRig(hard)
        rig.startFight(phase2 = phase2)
        bossX?.let { rig.placeBoss(it) }
        rig.place(playerX, 0f)
    }

    fun punch(each: () -> Unit = {}): List<Frame> {
        val frames = mutableListOf<Frame>()
        rig.rolls(FAIL, PASS)
        rig.ticksUntil(3f, each = {
            if (rig.state == DesertManState.PUNCH) {
                frames.add(frame())
                each()
            }
        }) { rig.state != DesertManState.STAND } shouldBe true
        rig.random.rolls.clear()
        rig.state shouldBe DesertManState.PUNCH
        rig.ticksUntil(5f, each = {
            if (rig.state == DesertManState.PUNCH) {
                frames.add(frame())
                each()
            }
        }) { rig.state != DesertManState.PUNCH } shouldBe true
        rig.state shouldBe DesertManState.STAND
        return frames
    }

    class Phases(frames: List<Frame>) {
        val windUp = frames.takeWhile { !it.armActive }
        private val rest = frames.drop(windUp.size)
        val reach = rest.maxOf { it.length }
        private val reachedAt = rest.indexOfFirst { it.length >= reach - TOL }
        val extend = rest.take(reachedAt + 1)
        val hold = rest.drop(reachedAt).takeWhile { it.length >= reach - TOL }
        val retract = rest.drop(reachedAt + hold.size - 1)
    }

    describe("the guard") {
        it("needs the target center within 8 tiles in x") {
            start(playerX = 20f)
            rig.attack(DesertManState.PUNCH, FAIL, PASS)
            rig.dispose()
            start(playerX = 20.1f)
            rig.rolls(FAIL, PASS)
            rig.ticksUntil(3f) { rig.state != DesertManState.STAND } shouldBe true
            rig.state shouldNotBe DesertManState.PUNCH
        }

        it("needs the target bottom within 1.5 tiles in y") {
            start()
            rig.place(18f, 1.5f)
            rig.attack(DesertManState.PUNCH, FAIL, PASS)
            rig.dispose()
            start()
            rig.place(18f, 1.6f)
            rig.rolls(FAIL, PASS)
            rig.ticksUntil(3f) { rig.state != DesertManState.STAND } shouldBe true
            rig.state shouldNotBe DesertManState.PUNCH
        }
    }

    describe("the facing and the position") {
        it("is set at the start of the wind-up and does not change until the punch ends; no move in x") {
            start()
            val frames = punch { rig.place(6f, 0f) }
            frames.forEach { it.facing shouldBe Facing.RIGHT }
            frames.forEach { it.centerX shouldBe (frames.first().centerX plusOrMinus TOL) }
            rig.tick()
            rig.boss.facing shouldBe Facing.LEFT
        }
    }

    describe("the wind-up") {
        listOf(
            Triple("normal", false to false, 0.5f),
            Triple("normal phase 2", false to true, 0.4f),
            Triple("hard", true to false, 0.35f),
            Triple("hard phase 2", true to true, 0.35f)
        ).forEach { (name, row, dur) ->
            it("$name: lasts $dur s with the arm and fist fixtures off") {
                start(hard = row.first, phase2 = row.second)
                val windUp = Phases(punch()).windUp
                windUp.size * STEP shouldBe (dur plusOrMinus 1.5f * STEP)
                windUp.forEach {
                    it.armActive shouldBe false
                    it.fistActive shouldBe false
                    it.armDamageableActive shouldBe false
                    it.fistDamageableActive shouldBe false
                }
            }
        }

        it("pulls the fist back 4 art px, flashes it every 0.06 s and trembles the body 1 art px") {
            start()
            val frames = punch()
            val windUp = Phases(frames).windUp
            val fistLengths = windUp.map { it.fistSpriteMaxX - it.shoulder }
            fistLengths.first() shouldBe (REST plusOrMinus 0.5f)
            fistLengths.last() shouldBe (MIN_LENGTH plusOrMinus TOL)
            fistLengths.zipWithNext().forEach { (a, b) -> b shouldBeLessThanOrEqual a + TOL }
            windUp.forEach { abs(it.tremble) shouldBe (ART_PX plusOrMinus TOL) }
            windUp.map { it.tremble }.toSet().size shouldBe 2
            windUp.forEachIndexed { k, it ->
                val cycles = k * STEP / 0.06f
                if (abs(cycles - Math.round(cycles)) > 0.05f) withClue("tick $k") {
                    val flashing = cycles.toInt() % 2 == 0
                    it.fistRegionY shouldBe if (flashing) 10 else 0
                }
            }
            frames.drop(windUp.size).forEach { it.tremble shouldBe (0f plusOrMinus TOL) }
        }
    }

    describe("the extend") {
        it("reaches 9 tiles from the shoulder when no wall is nearer, with no wall effects") {
            start()
            val pounds = rig.sounds(SoundAsset.POUND_SOUND)
            val phases = Phases(punch())
            phases.reach shouldBe (REACH plusOrMinus TOL)
            rig.sounds(SoundAsset.POUND_SOUND) shouldBe pounds
            rig.spawnPositionsOf<Splash>().none { abs(it.y - FLOOR) > 1f } shouldBe true
        }

        listOf(Facing.LEFT to (6f to 2.5f), Facing.RIGHT to (18f to 21.5f)).forEach { (side, places) ->
            it("facing $side stops at the inner edge of the wall, splashes at the tip, shakes 0.2 s and pounds once") {
                start(bossX = places.first, playerX = places.second)
                val shakes = rig.eventsOf(EventType.SHAKE_CAM)
                val pounds = rig.sounds(SoundAsset.POUND_SOUND)
                val frames = punch()
                val phases = Phases(frames)
                val wall = if (side == Facing.LEFT) LEFT_WALL else RIGHT_WALL
                phases.reach shouldBe (abs(wall - phases.extend.last().shoulder) plusOrMinus WALL_TOL)
                frames.filter { it.armActive }.forEach {
                    if (side == Facing.LEFT) it.tip shouldBeGreaterThanOrEqual wall - WALL_TOL
                    else it.tip shouldBeLessThanOrEqual wall + WALL_TOL
                }
                rig.sounds(SoundAsset.POUND_SOUND) shouldBe pounds + 1
                rig.eventsOf(EventType.SHAKE_CAM) shouldBe shakes + 1
                (rig.events.last { it.key == EventType.SHAKE_CAM }.properties.get(ConstKeys.DURATION) as Float) shouldBe 0.2f
                val splashes = rig.spawnPositionsOf<Splash>().filter { abs(it.y - FLOOR) > 1f }
                splashes.size shouldBe 1
                splashes.first().x shouldBe (wall plusOrMinus WALL_TOL)
                splashes.first().y shouldBe (frames.first().bodyY + SPRITE_BOTTOM + 13f * ART_PX plusOrMinus TOL)
            }
        }

        listOf(
            Triple("normal", false to false, 24f),
            Triple("hard phase 2", true to true, 32f)
        ).forEach { (name, row, speed) ->
            it("$name: eases out from the wind-up length to the reach, with the peak at 30 tiles/s or less") {
                start(hard = row.first, phase2 = row.second)
                val extend = Phases(punch()).extend
                val dur = moveDur(REACH - MIN_LENGTH, speed)
                extend.forEachIndexed { k, it ->
                    val ratio = (k * STEP / dur).coerceAtMost(1f)
                    withClue("tick $k") {
                        it.length shouldBe
                            (MIN_LENGTH + (REACH - MIN_LENGTH) * Interpolation.sineOut.apply(ratio) plusOrMinus 0.05f)
                    }
                }
                (extend.size - 1) * STEP shouldBe (dur plusOrMinus STEP)
                extend.zipWithNext().forEach { (a, b) -> b.length - a.length shouldBeLessThanOrEqual MAX_STEP + TOL }
            }
        }

        it("shows the speed lines only in the first 75 % of the extend") {
            start()
            val frames = punch()
            val phases = Phases(frames)
            val dur = moveDur(REACH - MIN_LENGTH, 24f)
            phases.extend.forEachIndexed { k, it ->
                val ratio = k * STEP / dur
                if (abs(ratio - 0.75f) > 0.02f) withClue("tick $k") { it.speedHidden shouldBe (ratio >= 0.75f) }
            }
            phases.windUp.forEach { it.speedHidden shouldBe true }
            phases.hold.drop(1).forEach { it.speedHidden shouldBe true }
            phases.retract.forEach { it.speedHidden shouldBe true }
        }
    }

    describe("the hold") {
        listOf(
            Triple("normal", false to false, 0.35f),
            Triple("normal phase 2", false to true, 0.3f),
            Triple("hard", true to false, 0.3f),
            Triple("hard phase 2", true to true, 0.25f)
        ).forEach { (name, row, dur) ->
            it("$name: keeps the arm at full reach for $dur s") {
                start(hard = row.first, phase2 = row.second)
                Phases(punch()).hold.size * STEP shouldBe (dur + STEP plusOrMinus 1.5f * STEP)
            }
        }
    }

    describe("the retract") {
        it("eases in back to the wind-up length at 16 tiles/s on average, then STAND") {
            start()
            val retract = Phases(punch()).retract
            val dur = moveDur(REACH - MIN_LENGTH, 16f)
            retract.forEachIndexed { k, it ->
                val ratio = (k * STEP / dur).coerceAtMost(1f)
                withClue("tick $k") {
                    it.length shouldBe (REACH - (REACH - MIN_LENGTH) * Interpolation.sineIn.apply(ratio) plusOrMinus 0.05f)
                }
            }
            retract.size * STEP shouldBe (dur plusOrMinus STEP)
            retract.zipWithNext().forEach { (a, b) -> a.length - b.length shouldBeLessThanOrEqual MAX_STEP + TOL }
            rig.state shouldBe DesertManState.STAND
        }
    }

    describe("the fixtures and the sprites") {
        listOf(Facing.RIGHT to 18f, Facing.LEFT to 6f).forEach { (side, playerX) ->
            it("facing $side: the arm spans shoulder to tip 5 art px high, the fist hit box covers the fist, on each tick") {
                start(playerX = playerX)
                val frames = punch().filter { it.armActive }
                frames.forEach { f ->
                    f.facing shouldBe side
                    f.fistActive shouldBe true
                    f.armDamageableActive shouldBe true
                    f.fistDamageableActive shouldBe true
                    val shoulder = f.shoulder
                    val tip = shoulder + side.value * f.length
                    if (side == Facing.RIGHT) f.arm.getX() shouldBe (shoulder plusOrMinus TOL)
                    else f.arm.getMaxX() shouldBe (shoulder plusOrMinus TOL)
                    f.arm.getY() shouldBe (f.bodyY + SPRITE_BOTTOM + 14f * ART_PX plusOrMinus TOL)
                    f.arm.getHeight() shouldBe (ARM_HEIGHT plusOrMinus TOL)
                    f.fist.getWidth() shouldBe (FIST_HIT_WIDTH plusOrMinus TOL)
                    f.fist.getHeight() shouldBe (FIST_HIT_HEIGHT plusOrMinus TOL)
                    f.fist.getY() shouldBe (f.bodyY + SPRITE_BOTTOM + 13f * ART_PX plusOrMinus TOL)
                    if (side == Facing.RIGHT) f.fist.getMaxX() shouldBe (tip plusOrMinus TOL)
                    else f.fist.getX() shouldBe (tip plusOrMinus TOL)
                    f.armDamageable shouldBe f.arm
                    f.fistDamageable shouldBe f.fist
                }
            }

            it("facing $side: the arm sprite joins the shoulder and the fist sprite is at the tip, at every length") {
                start(playerX = playerX)
                val frames = punch()
                frames.forEach { f ->
                    f.fistSpriteHidden shouldBe false
                    val length = if (f.armActive) f.length else abs(
                        (if (side == Facing.RIGHT) f.fistSpriteMaxX else f.fistSpriteX) - f.shoulder
                    )
                    val tip = f.shoulder + side.value * length
                    if (side == Facing.RIGHT) f.fistSpriteMaxX shouldBe (tip plusOrMinus TOL)
                    else f.fistSpriteX shouldBe (tip plusOrMinus TOL)
                    (f.fistSpriteMaxX - f.fistSpriteX) shouldBe (FIST_SPRITE_WIDTH plusOrMinus TOL)
                    if (f.armSpriteHidden) return@forEach
                    if (side == Facing.RIGHT) {
                        f.armSpriteX shouldBe (f.shoulder plusOrMinus TOL)
                        f.armSpriteMaxX shouldBeGreaterThanOrEqual f.fistSpriteX - TOL
                    } else {
                        f.armSpriteMaxX shouldBe (f.shoulder plusOrMinus TOL)
                        f.armSpriteX shouldBeLessThanOrEqual f.fistSpriteMaxX + TOL
                    }
                }
                frames.count { !it.armSpriteHidden } shouldNotBe 0
            }
        }
    }
})

private const val HALF_SPRITE = 1.75f * T
