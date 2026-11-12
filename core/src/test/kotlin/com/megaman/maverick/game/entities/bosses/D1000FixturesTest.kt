package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.OrderedMap
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.IFixture
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.utils.TickFrameDef
import com.megaman.maverick.game.entities.utils.TickFrames
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.contacts.rules.DamageRule
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import io.mockk.unmockkAll
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import com.megaman.maverick.game.entities.bosses.d1000.D1000Tier
import com.megaman.maverick.game.entities.bosses.d1000.D1000Distance
import com.megaman.maverick.game.entities.bosses.d1000.D1000Action
import com.megaman.maverick.game.entities.bosses.d1000.D1000Director
import com.megaman.maverick.game.entities.bosses.d1000.D1000Frames

private const val BODY_ABOVE_ORIGIN = 0.703125f * RIG_PPM
private const val X_TOLERANCE = 0.01f
private const val Y_TOLERANCE = 0.0625f * RIG_PPM + 0.01f
private const val MIN_COVERED = 0.95f
private const val MIN_FILLED = 0.85f
private const val FRAMES_DIR = "../assets/sprites/frames/Bosses3"
private const val IRIS = 0xF83800

private val TYPES = listOf(FixtureType.BODY, FixtureType.DAMAGEABLE, FixtureType.DAMAGER)

private val NOTCH = intArrayOf(40, -10, 16, 24)
private val NOTCH_LOWER = intArrayOf(40, -6, 16, 24)

class D1000FixturesTest : DescribeSpec({

    lateinit var rig: D1000Rig

    beforeEach { rig = D1000Rig() }

    afterEach {
        rig.dispose()
        MegaEntityFactory.reset()
        unmockkAll()
    }

    val opening = d1000Context(cyclesSinceCombo = D1000Director.comboGap(D1000Tier.NORMAL, false))

    fun fixtures(entity: IGameEntity, type: FixtureType): List<IFixture> {
        val out = Array<IFixture>()
        entity.getComponent(BodyComponent::class)!!.body.getFixtures(out, type)
        return out.toList()
    }

    fun active(entity: IGameEntity, type: FixtureType) = fixtures(entity, type).filter { it.isActive() }

    fun shapeOf(fixture: IFixture) = fixture.getShape() as GameRectangle

    fun drawn(): Pair<String, Int> {
        val sprite = rig.boss.defaultSprite
        val key = D1000Frames.BODY.keys().first {
            val def = D1000Frames.BODY[it]
            def.width == sprite.regionWidth && def.height == sprite.regionHeight
        }
        return key to sprite.regionY / D1000Frames.BODY[key].height
    }

    fun drawnCore(): Pair<String, Int> {
        val animator = rig.core().getComponent(AnimationsComponent::class)!!
            .getAnimator(AnimationsComponent.DEFAULT_KEY) as Animator
        val key = animator.currentKey!!
        return key to rig.core().defaultSprite.regionY / D1000Frames.CORE[key].height
    }

    fun bossOrigin(): Vector2 = rig.bossCenter().sub(0f, BODY_ABOVE_ORIGIN)

    fun worldBox(def: TickFrameDef, box: IntArray, start: Int, origin: Vector2, mirrored: Boolean): GameRectangle {
        val x = box[start]
        val y = box[start + 1]
        val width = box[start + 2]
        val height = box[start + 3]
        val scale = TickFrames.SCALE
        val spriteLeft = if (mirrored) origin.x - (def.width - def.originX) * scale else origin.x - def.originX * scale
        val artLeft = if (mirrored) def.width - x - width else x
        val spriteBottom = origin.y - (def.height - def.originY) * scale
        return GameRectangle(
            spriteLeft + artLeft * scale,
            spriteBottom + (def.height - y - height) * scale,
            width * scale,
            height * scale
        )
    }

    fun shouldMatch(fixture: IFixture, expected: GameRectangle) {
        val shape = shapeOf(fixture)
        shape.getX() shouldBe (expected.getX() plusOrMinus X_TOLERANCE)
        shape.getWidth() shouldBe (expected.getWidth() plusOrMinus X_TOLERANCE)
        shape.getY() shouldBe (expected.getY() plusOrMinus Y_TOLERANCE)
        shape.getHeight() shouldBe (expected.getHeight() plusOrMinus X_TOLERANCE)
    }

    fun checkBodyBoxes(frame: Pair<String, Int>, facingBefore: Facing = rig.boss.facing) {
        val def = D1000Frames.BODY[frame.first]
        val boxes = D1000Frames.BODY_BOXES[frame.first][frame.second]
        val count = boxes.size / 4
        val origin = bossOrigin()
        val placedX = shapeOf(fixtures(rig.boss, FixtureType.BODY)[0]).getX()
        val mirrored = listOf(rig.boss.facing, facingBefore).map { it == Facing.RIGHT }.firstOrNull {
            abs(worldBox(def, boxes, 0, origin, it).getX() - placedX) <= X_TOLERANCE
        } ?: (rig.boss.facing == Facing.RIGHT)
        TYPES.forEach { type ->
            val all = fixtures(rig.boss, type)
            all.drop(count).forEach { it.isActive() shouldBe false }
            if (type != FixtureType.DAMAGER) all.take(count).forEach { it.isActive() shouldBe true }
            (0 until count).forEach { i -> shouldMatch(all[i], worldBox(def, boxes, i * 4, origin, mirrored)) }
        }
    }

    fun followFrames(seconds: Float): Set<Pair<String, Facing>> {
        val seen = HashSet<Pair<String, Facing>>()
        var time = 0f
        while (time < seconds) {
            val frame = drawn()
            val facing = rig.boss.facing
            rig.tick()
            checkBodyBoxes(frame, facing)
            seen.add(frame.first to rig.boss.facing)
            time += RIG_DELTA
        }
        return seen
    }

    fun unionMinY(type: FixtureType) = active(rig.boss, type).minOf { shapeOf(it).getY() }

    fun overlapsDamager(probe: GameRectangle) = active(rig.boss, FixtureType.DAMAGER).any { it.getShape().overlaps(probe) }

    fun xProbe(box: IntArray): GameRectangle {
        val probe = worldBox(D1000Frames.BODY["stand"], box, 0, bossOrigin(), rig.boss.facing == Facing.RIGHT)
        rig.xBody.setCenter(probe.getCenter(Vector2()))
        return rig.xBody.getBounds(GameRectangle())
    }

    fun opaque(image: java.awt.image.BufferedImage, x: Int, y: Int) = (image.getRGB(x, y) ushr 24) != 0

    fun checkArt(folder: String, defs: OrderedMap<String, TickFrameDef>, table: OrderedMap<String, kotlin.Array<IntArray>>) {
        defs.forEach { entry ->
            val def = entry.value
            val image = ImageIO.read(File("$FRAMES_DIR/$folder/${entry.key}.png"))
            table[entry.key].size shouldBe def.count
            (0 until def.count).forEach { frame ->
                val boxes = table[entry.key][frame]
                val covered = kotlin.Array(def.height) { BooleanArray(def.width) }
                for (start in boxes.indices step 4) {
                    val x = boxes[start]
                    val y = boxes[start + 1]
                    withClue("${entry.key} $frame box $start") {
                        (x >= 0 && y >= 0 && x + boxes[start + 2] <= def.width && y + boxes[start + 3] <= def.height) shouldBe true
                    }
                    for (row in y until y + boxes[start + 3]) for (col in x until x + boxes[start + 2]) covered[row][col] = true
                }
                var opaqueCount = 0
                var opaqueCovered = 0
                var coveredCount = 0
                for (row in 0 until def.height) for (col in 0 until def.width) {
                    val solid = opaque(image, col, frame * def.height + row)
                    if (solid) opaqueCount++
                    if (covered[row][col]) coveredCount++
                    if (solid && covered[row][col]) opaqueCovered++
                }
                withClue("${entry.key} $frame") {
                    (opaqueCovered.toFloat() / opaqueCount >= MIN_COVERED) shouldBe true
                    (opaqueCovered.toFloat() / coveredCount >= MIN_FILLED) shouldBe true
                }
            }
        }
    }

    describe("the box tables") {
        it("cover the opaque pixels of each body frame, with little empty space") {
            checkArt("D1000", D1000Frames.BODY, D1000Frames.BODY_BOXES)
        }

        it("cover the opaque pixels of each eyeball frame") {
            checkArt("D1000Core", D1000Frames.CORE, D1000Frames.CORE_BOXES)
        }

        it("put the eye box on the drawn iris of the open eye") {
            val image = ImageIO.read(File("$FRAMES_DIR/D1000Core/core.png"))
            val eye = D1000Frames.EYE_BOXES["core"][0]
            eye.size shouldBe 4
            var iris = 0
            var outside = 0
            val def = D1000Frames.CORE["core"]
            for (row in 0 until def.height) for (col in 0 until def.width) {
                if (image.getRGB(col, row) and 0xFFFFFF != IRIS || !opaque(image, col, row)) continue
                if (col in eye[0] until eye[0] + eye[2] && row in eye[1] until eye[1] + eye[3]) iris++ else outside++
            }
            (iris > 0) shouldBe true
            outside shouldBe 0
        }
    }

    describe("the body boxes") {
        it("follow the frame on screen through a fight, facing left") {
            rig.placeX(1f, 0f)
            rig.startFight()
            val seen = followFrames(10f)
            (seen.map { it.first }.toSet().size >= 3) shouldBe true
            seen.any { it.second == Facing.LEFT } shouldBe true
        }

        it("mirror with the facing: they follow the frame on screen while the body faces right") {
            rig.placeX(14.75f, 0f)
            rig.startFight()
            val seen = followFrames(10f)
            seen.any { it.second == Facing.RIGHT } shouldBe true
        }

        it("move with the throw frames: the arm boxes reach lower from throw frame 2") {
            rig.placeX(8.25f, 0f)
            rig.startFight()
            rig.rollFor(D1000Action.BRACKET, opening.copy(distance = D1000Distance.MID))
            val reach = HashMap<Int, Float>()
            rig.ticksUntil(3f) {
                val frame = drawn()
                rig.tick()
                if (frame.first == "throw") {
                    checkBodyBoxes(frame)
                    reach[frame.second] = unionMinY(FixtureType.DAMAGER) - bossOrigin().y
                }
                reach.keys.containsAll(listOf(0, 2))
            } shouldBe true
            (reach[0]!! - reach[2]!!) shouldBe (4f * TickFrames.SCALE plusOrMinus X_TOLERANCE)
        }

        it("hurt X in the shell, and not in the notch between the head lamp and the top drum") {
            rig.placeX(1f, 0f)
            rig.startFight()
            rig.rollFor(D1000Action.SUMMON, opening)
            var frame = drawn()
            rig.ticksUntil(3f) {
                frame = drawn()
                rig.tick()
                frame == ("stand" to 0)
            } shouldBe true
            checkBodyBoxes(frame)
            active(rig.boss, FixtureType.DAMAGER).isEmpty() shouldBe false

            overlapsDamager(xProbe(NOTCH)) shouldBe false
            overlapsDamager(xProbe(NOTCH_LOWER)) shouldBe true
        }

        it("turn the damager off while stunned, and keep the body and the damageable on") {
            rig.startFight()
            rig.boss.stun()
            rig.tick()
            active(rig.boss, FixtureType.DAMAGER).size shouldBe 0
            (active(rig.boss, FixtureType.BODY).size > 1) shouldBe true
            active(rig.boss, FixtureType.DAMAGEABLE).size shouldBe active(rig.boss, FixtureType.BODY).size
        }

        it("keep the damager off in the entrance") {
            rig.spawnBoss()
            rig.ticks(1f)
            active(rig.boss, FixtureType.DAMAGER).size shouldBe 0
            (active(rig.boss, FixtureType.BODY).size > 1) shouldBe true
        }
    }

    describe("the eyeball boxes") {
        fun coreOrigin() = rig.core().getComponent(BodyComponent::class)!!.body.getCenter(Vector2()).let {
            Vector2(Math.round(it.x / TickFrames.SCALE) * TickFrames.SCALE, Math.round(it.y / TickFrames.SCALE) * TickFrames.SCALE)
        }

        fun checkCore(frame: Pair<String, Int>) {
            val def = D1000Frames.CORE[frame.first]
            listOf(
                FixtureType.DAMAGEABLE to D1000Frames.EYE_BOXES[frame.first][frame.second],
                FixtureType.DAMAGER to D1000Frames.CORE_BOXES[frame.first][frame.second]
            ).forEach { (type, boxes) ->
                val all = fixtures(rig.core(), type)
                val count = boxes.size / 4
                all.drop(count).forEach { it.isActive() shouldBe false }
                (0 until count).forEach { i ->
                    all[i].isActive() shouldBe true
                    shouldMatch(all[i], worldBox(def, boxes, i * 4, coreOrigin(), false))
                }
            }
        }

        it("put the damageable on the eye while the eye shows, and on the whole ball while it flashes") {
            rig.startFight()
            rig.ticks(0.5f)
            var frame = drawnCore()
            rig.tick()
            frame.first shouldBe "core"
            checkCore(frame)
            active(rig.core(), FixtureType.DAMAGEABLE).size shouldBe 1

            rig.ticksUntil(2f) { rig.core().vulnerable } shouldBe true
            rig.core().takeDamageFrom(rig.sabre()) shouldBe true
            rig.tick()
            frame = drawnCore()
            rig.tick()
            frame.first shouldBe "core_vanish"
            checkCore(frame)
            active(rig.core(), FixtureType.DAMAGEABLE).size shouldBe 3
        }

        it("take a full charge through the eye box, and not while the body is stunned") {
            rig.startFight()
            rig.ticksUntil(2f) { rig.core().vulnerable } shouldBe true
            val eye = active(rig.core(), FixtureType.DAMAGEABLE).single()
            val rule = DamageRule { _, _ -> false }

            rig.core().takeDamageFrom(rig.sabre()) shouldBe true
            rig.ticksUntil(3f) { rig.boss.stunned && rig.core().defaultSprite.hidden } shouldBe true
            rig.ticksUntil(6f) { rig.boss.stunned && !rig.core().defaultSprite.hidden } shouldBe true
            rig.ticks(1f)
            rig.boss.stunned shouldBe true
            val health = rig.boss.getCurrentHealth()
            val closedShot = rig.fullCharge()
            rig.core().vulnerable shouldBe false
            rule.onBegin(fixtures(closedShot, FixtureType.DAMAGER).first(), eye, RIG_DELTA)
            rig.destroyed.contains(closedShot) shouldBe false
            rig.boss.getCurrentHealth() shouldBe health

            rig.ticksUntil(8f) { !rig.boss.stunned } shouldBe true
            rig.ticksUntil(2f) { rig.core().vulnerable } shouldBe true
            val before = rig.boss.getCurrentHealth()
            val openShot = rig.fullCharge()
            rule.onBegin(fixtures(openShot, FixtureType.DAMAGER).first(), eye, RIG_DELTA)
            rig.destroyed.contains(openShot) shouldBe true
            rig.boss.getCurrentHealth() shouldBe before - D1000.FULL_CHARGE_DAMAGE
            rig.core().canBeDamagedBy(rig.fullCharge()) shouldBe false
        }
    }
})
