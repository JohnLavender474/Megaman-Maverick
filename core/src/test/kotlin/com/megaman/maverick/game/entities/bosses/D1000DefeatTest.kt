package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.IFixture
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.decorations.DebrisPiece
import com.maverick.game.engine.common.UtilMethods
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.entities.decorations.D1000RubblePile
import com.megaman.maverick.game.entities.explosions.Explosion
import com.megaman.maverick.game.entities.explosions.SmokePuff
import com.megaman.maverick.game.entities.projectiles.D1000Bomb
import com.megaman.maverick.game.entities.hazards.D1000CeilingDebris
import com.megaman.maverick.game.entities.projectiles.JunkShot
import com.megaman.maverick.game.entities.state.BossPhase
import com.megaman.maverick.game.entities.utils.TickFrames
import com.megaman.maverick.game.world.body.FixtureType
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import io.mockk.unmockkAll
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import com.megaman.maverick.game.entities.bosses.d1000.D1000Tier
import com.megaman.maverick.game.entities.bosses.d1000.D1000Action
import com.megaman.maverick.game.entities.bosses.d1000.D1000Director
import com.megaman.maverick.game.entities.bosses.d1000.D1000Frames
import com.megaman.maverick.game.entities.bosses.d1000.D1000Defeat

private const val FRAMES_DIR = "../assets/sprites/frames/Bosses3"
private const val IRIS = 0xF83800
private const val TOLERANCE = 0.01f
private const val HOME_TILES_X = 10.75f + 3.25f
private const val HOME_TILES_Y = 8f
private val PPM = ConstVals.PPM.toFloat()

class D1000DefeatTest : DescribeSpec({

    lateinit var rig: D1000Rig

    beforeEach { rig = D1000Rig() }

    afterEach {
        rig.dispose()
        MegaEntityFactory.reset()
        unmockkAll()
    }

    val opening = d1000Context(cyclesSinceCombo = D1000Director.comboGap(D1000Tier.NORMAL, false))

    fun coreCenter(): Vector2 = rig.core().getComponent(BodyComponent::class)!!.body.getCenter(Vector2())

    fun animator() = rig.core().getComponent(AnimationsComponent::class)!!
        .getAnimator(AnimationsComponent.DEFAULT_KEY) as Animator

    fun drawn(): Pair<String, Int> {
        val key = animator().currentKey!!
        return TickFrames.baseKey(key) to rig.core().defaultSprite.regionY / D1000Frames.CORE[TickFrames.baseKey(key)].height
    }

    fun pupil() = rig.core().getComponent(SpritesComponent::class)!!.sprites[D1000Core.PUPIL]

    fun active(entity: IGameEntity, type: FixtureType): List<IFixture> {
        val out = Array<IFixture>()
        entity.getComponent(BodyComponent::class)!!.body.getFixtures(out, type)
        return out.filter { it.isActive() }
    }

    fun idleFight(xTiles: Float = 1f) {
        rig.placeX(xTiles, 0f)
        rig.startFight()
        rig.rollFor(D1000Action.WALK, opening)
        rig.ticks(0.3f)
    }

    fun defeat() {
        rig.boss.takeEyeDamage(rig.fullCharge(), ConstVals.MAX_HEALTH)
        rig.tick()
        rig.boss.defeated shouldBe true
    }

    fun fragments() = rig.spawnedOf<DebrisPiece>()

    fun pieceBody(piece: DebrisPiece) = piece.getComponent(BodyComponent::class)!!.body

    fun explosions() = rig.spawnedOf<Explosion>()

    fun secondsUntil(maxSeconds: Float, done: () -> Boolean): Float {
        var time = 0f
        while (time < maxSeconds && !done()) {
            rig.tick()
            time += RIG_DELTA
        }
        return time
    }

    describe("the defeat") {
        it("stops the fight at once: no damager on the body or the eye, no damage to X, no new attack") {
            idleFight()
            val eye = rig.core()
            defeat()
            rig.boss.fighting shouldBe false
            rig.boss.canDamage(rig.x) shouldBe false
            eye.canDamage(rig.x) shouldBe false
            eye.canBeDamagedBy(rig.sabre()) shouldBe false
            eye.vulnerable shouldBe false
            val shots = rig.spawnedOf<JunkShot>().size
            val debris = rig.spawnedOf<D1000CeilingDebris>().size
            val bombs = rig.spawnedOf<D1000Bomb>().size
            rig.ticksUntil(D1000Defeat.DEFEAT_DUR + 1f) {
                active(rig.boss, FixtureType.DAMAGER).size shouldBe 0
                if (!rig.destroyed.contains(eye)) active(eye, FixtureType.DAMAGER).size shouldBe 0
                rig.destroyed.contains(rig.boss)
            } shouldBe true
            rig.spawnedOf<JunkShot>().size shouldBe shots
            rig.spawnedOf<D1000CeilingDebris>().size shouldBe debris
            rig.spawnedOf<D1000Bomb>().size shouldBe bombs
            explosions().isNotEmpty() shouldBe true
            explosions().forEach { it.canDamage(rig.x) shouldBe false }
        }

        it("sends the eye over the body, where it spins faster and faster (5 ticks a frame down to 2) and then explodes") {
            idleFight()
            val eye = rig.core()
            defeat()
            val flight = secondsUntil(2f) { eye.spinning }
            (flight <= 1.5f) shouldBe true
            val body = rig.bossBody()
            coreCenter().x shouldBe (body.getCenter(Vector2()).x plusOrMinus TOLERANCE)
            coreCenter().y shouldBe (body.getMaxY() + D1000Core.DEFEAT_OVER plusOrMinus TOLERANCE)
            val runs = ArrayList<Pair<Int, Int>>()
            val spin = secondsUntil(D1000Core.SPIN_DUR + 0.5f) {
                if (!rig.destroyed.contains(eye)) {
                    val (key, frame) = drawn()
                    key shouldBe "core_spin"
                    if (runs.isNotEmpty() && runs.last().first == frame) runs[runs.size - 1] = frame to runs.last().second + 1
                    else runs.add(frame to 1)
                }
                rig.destroyed.contains(eye)
            }
            spin shouldBe (D1000Core.SPIN_DUR plusOrMinus 2 * RIG_DELTA)
            (runs.size > D1000Frames.SPIN_FRAMES * 3) shouldBe true
            (abs(runs[1].second - D1000Frames.SPIN_RAMP_TICKS[0]) <= 1) shouldBe true
            (abs(runs[runs.size - 2].second - D1000Frames.SPIN_LOOP_TICKS) <= 1) shouldBe true
            runs.zipWithNext().all { (a, b) -> b.first == (a.first + 1) % D1000Frames.SPIN_FRAMES } shouldBe true
            (explosions().size >= 3) shouldBe true
            (rig.spawnedOf<SmokePuff>().size >= 2) shouldBe true
            rig.tick()
            rig.boss.broken shouldBe true
        }

        fun breakNow() {
            idleFight()
            val eye = rig.core()
            defeat()
            rig.ticks(D1000Defeat.DEFEAT_SHAKE_MIN)
            rig.boss.broken shouldBe false
            rig.boss.defaultSprite.hidden shouldBe false
            (explosions().size >= 4) shouldBe true
            fragments().size shouldBe 0
            rig.ticksUntil(D1000Defeat.DEFEAT_SHAKE_MAX) { rig.destroyed.contains(eye) } shouldBe true
            rig.boss.broken shouldBe false
        }

        fun inBodyBox(point: Vector2, margin: Float = 0f): Boolean {
            val box = rig.bossBody().getBounds()
            return point.x >= box.getX() - margin && point.x <= box.getMaxX() + margin &&
                point.y >= box.getY() - margin && point.y <= box.getMaxY() + margin
        }

        it("shakes the body with sparks until the eye explodes, then hides it under a burst of ${D1000Defeat.BREAK_BURST_COUNT} explosions and a chain of ${D1000Defeat.BREAK_CHAIN_COUNT} over 1 s with smoke, all harmless") {
            breakNow()
            val before = explosions().size
            val puffsBefore = rig.spawnedOf<SmokePuff>().size
            rig.tick()
            rig.boss.broken shouldBe true
            rig.tick()
            rig.boss.defaultSprite.hidden shouldBe true
            explosions().size shouldBe before + D1000Defeat.BREAK_BURST_COUNT
            explosions().drop(before).forEach { inBodyBox(it.getComponent(BodyComponent::class)!!.body.getCenter(Vector2())) shouldBe true }
            val chainStart = explosions().size
            var seenAt = ArrayList<Float>()
            var time = 0f
            while (time < D1000Defeat.BREAK_CHAIN_COUNT * D1000Defeat.BREAK_CHAIN_STEP + 0.1f) {
                rig.tick()
                time += RIG_DELTA
                while (seenAt.size < explosions().size - chainStart) seenAt.add(time)
            }
            explosions().size shouldBe chainStart + D1000Defeat.BREAK_CHAIN_COUNT
            seenAt.forEachIndexed { i, at -> at shouldBe ((i + 1) * D1000Defeat.BREAK_CHAIN_STEP plusOrMinus 2 * RIG_DELTA) }
            rig.spawnedOf<SmokePuff>().size shouldBe puffsBefore + D1000Defeat.BREAK_CHAIN_COUNT / D1000Defeat.BREAK_PUFF_EVERY
            explosions().forEach { it.canDamage(rig.x) shouldBe false }
            rig.spawnedOf<SmokePuff>().drop(puffsBefore).forEach { it.canDamage(rig.x) shouldBe false }
        }

        it("throws ${D1000Defeat.DEBRIS.size} + ${D1000Defeat.DEBRIS_EXTRA_MIN} to ${D1000Defeat.DEBRIS_EXTRA_MAX} body fragments from the body box, up and to both sides, which fall and are gone after 2 s") {
            breakNow()
            UtilMethods.random.setSeed(7L)
            rig.tick()
            val pieces = fragments()
            (pieces.size in D1000Defeat.DEBRIS.size + D1000Defeat.DEBRIS_EXTRA_MIN..D1000Defeat.DEBRIS.size + D1000Defeat.DEBRIS_EXTRA_MAX) shouldBe true
            D1000Defeat.DEBRIS.forEach { key -> pieces.any { it.key == key } shouldBe true }
            pieces.forEach { piece ->
                withClue("fragment ${piece.key}") {
                    (piece.key in D1000Defeat.DEBRIS) shouldBe true
                    inBodyBox(pieceBody(piece).getCenter(Vector2()), 0.25f * PPM) shouldBe true
                    (pieceBody(piece).physics.velocity.y > 0f) shouldBe true
                    val speed = pieceBody(piece).physics.velocity.len()
                    (speed >= D1000Defeat.DEBRIS_SPEED_MIN - TOLERANCE && speed <= D1000Defeat.DEBRIS_SPEED_MAX + TOLERANCE) shouldBe true
                }
            }
            (pieces.count { pieceBody(it).physics.velocity.x < 0f } >= 2) shouldBe true
            (pieces.count { pieceBody(it).physics.velocity.x > 0f } >= 2) shouldBe true
            val tops = pieces.map { pieceBody(it).getCenter(Vector2()).y }
            rig.ticks(1f)
            pieces.forEach { rig.destroyed.contains(it) shouldBe false }
            pieces.zip(tops).forEach { (piece, top) -> (pieceBody(piece).physics.velocity.y < 0f || pieceBody(piece).getCenter(Vector2()).y > top) shouldBe true }
            rig.ticks(1.1f)
            pieces.forEach { rig.destroyed.contains(it) shouldBe true }
        }

        it("shows the pile on the floor at the body's place ${D1000Defeat.PILE_SHOW_AT} s after the break, under the last chain explosions, and ends the boss ${D1000Defeat.PILE_HOLD} s later inside defeatDur") {
            idleFight()
            defeat()
            val toBreak = secondsUntil(D1000Defeat.DEFEAT_DUR) { rig.boss.broken }
            rig.spawnedOf<D1000RubblePile>().size shouldBe 0
            val toPile = secondsUntil(2f) { rig.spawnedOf<D1000RubblePile>().isNotEmpty() }
            toPile shouldBe (D1000Defeat.PILE_SHOW_AT plusOrMinus 2 * RIG_DELTA)
            val pile = rig.spawnedOf<D1000RubblePile>().single()
            rig.boss.pile shouldBe pile
            pile.bottomCenter.x shouldBe (rig.bossCenter().x plusOrMinus TOLERANCE)
            pile.bottomCenter.y shouldBe (RIG_FLOOR plusOrMinus TOLERANCE)
            val atPile = explosions().size
            rig.destroyed.contains(rig.boss) shouldBe false
            val toEnd = secondsUntil(D1000Defeat.PILE_HOLD + 0.1f) { rig.destroyed.contains(rig.boss) }
            toEnd shouldBe (D1000Defeat.PILE_HOLD plusOrMinus 2 * RIG_DELTA)
            (explosions().size > atPile) shouldBe true
            rig.destroyed.contains(pile) shouldBe false
            (toBreak + toPile + toEnd < D1000Defeat.DEFEAT_DUR) shouldBe true
            (D1000Defeat.DEFEAT_DUR >= D1000Defeat.DEFEAT_SHAKE_MAX + D1000Defeat.PILE_SHOW_AT + D1000Defeat.PILE_HOLD) shouldBe true
        }
    }

    describe("the eye facing") {
        fun eyeBoxLeft() = (active(rig.core(), FixtureType.DAMAGEABLE).single().getShape() as GameRectangle).getX()

        it("flips to face X when X is more than 0.5 tile to the right of it, and back when X is 0.5 tile to the left") {
            idleFight()
            rig.core().facingRight shouldBe false
            rig.core().defaultSprite.isFlipX shouldBe false
            val leftBox = eyeBoxLeft() - rig.core().defaultSprite.x
            rig.placeX(HOME_TILES_X + 0.1f, 0f)
            rig.ticks(0.2f)
            rig.core().facingRight shouldBe false
            rig.placeX(HOME_TILES_X + 0.9f, 0f)
            rig.ticks(0.2f)
            rig.core().facingRight shouldBe true
            rig.core().defaultSprite.isFlipX shouldBe true
            val rightBox = eyeBoxLeft() - rig.core().defaultSprite.x
            val eye = D1000Frames.EYE_BOXES["core"][0]
            (rightBox - leftBox) shouldBe ((D1000Frames.CORE["core"].width - 2 * eye[0] - eye[2]) * TickFrames.SCALE plusOrMinus TOLERANCE)
            rig.placeX(HOME_TILES_X - 0.1f, 0f)
            rig.ticks(0.2f)
            rig.core().facingRight shouldBe true
            rig.placeX(HOME_TILES_X - 0.9f, 0f)
            rig.ticks(0.2f)
            rig.core().facingRight shouldBe false
        }

        it("holds the facing through a Flank pause and its shots, and turns again when idle") {
            rig.placeX(5f, 0f)
            rig.startFight()
            rig.rollFor(D1000Action.FLANK, opening)
            rig.ticksUntil(4f) { abs(coreCenter().x - (RIG_ARENA_X + 7.5f * PPM)) < 0.001f * PPM } shouldBe true
            rig.core().facingRight shouldBe false
            rig.placeX(7.5f + 3f, 0f)
            val shots = rig.spawnedOf<JunkShot>().size
            rig.ticksUntil(3f) {
                rig.core().facingRight shouldBe false
                rig.spawnedOf<JunkShot>().size > shots && drawn().first == "core"
            } shouldBe true
            rig.ticksUntil(1f) { rig.core().facingRight } shouldBe false
            rig.placeX(HOME_TILES_X + 3f, 0f)
            rig.ticksUntil(1f) { rig.core().facingRight } shouldBe true
        }

        it("holds the facing through the defeat spin") {
            idleFight()
            defeat()
            rig.ticksUntil(2f) { rig.core().spinning } shouldBe true
            rig.placeX(20f, 0f)
            rig.ticks(0.5f)
            rig.core().facingRight shouldBe false
        }
    }

    describe("the pupil") {
        fun eyeSprite() = rig.core().defaultSprite

        fun pupilArt(): Pair<Int, Int> {
            val left = Math.round((pupil().x - eyeSprite().x) / TickFrames.SCALE)
            val top = D1000Frames.CORE["core"].height - Math.round((pupil().y - eyeSprite().y) / TickFrames.SCALE) - D1000Frames.PUPIL_SIZE
            return left to top
        }

        it("sits at its rest place moved toward X: 2 px toward an X to the left, 1 px down toward an X below") {
            idleFight()
            rig.placeX(1f, HOME_TILES_Y - 0.5f)
            rig.tick()
            rig.ticksUntil(2f) { drawn() == ("core" to 0) && !pupil().hidden } shouldBe true
            pupilArt() shouldBe (D1000Frames.PUPIL_REST_X - D1000Frames.PUPIL_REACH_X to D1000Frames.PUPIL_REST_Y)
            pupil().regionY shouldBe D1000Frames.PUPIL_OPEN * D1000Frames.PUPIL_SIZE
            rig.placeX(HOME_TILES_X, 0f)
            rig.tick()
            rig.ticksUntil(2f) { drawn() == ("core" to 0) && !pupil().hidden } shouldBe true
            pupilArt().second shouldBe D1000Frames.PUPIL_REST_Y + D1000Frames.PUPIL_REACH_Y
            (abs(pupilArt().first - D1000Frames.PUPIL_REST_X) <= 1) shouldBe true
        }

        it("moves in the mirrored frame when the eye faces right") {
            idleFight()
            rig.placeX(HOME_TILES_X + 4f, HOME_TILES_Y - 0.5f)
            rig.tick()
            rig.ticksUntil(2f) { rig.core().facingRight && drawn() == ("core" to 0) && !pupil().hidden } shouldBe true
            val width = D1000Frames.CORE["core"].width
            pupilArt().first shouldBe width - (D1000Frames.PUPIL_REST_X - D1000Frames.PUPIL_REACH_X) - D1000Frames.PUPIL_SIZE
        }

        it("is hidden in the blink-shut frame, while the eye appears, while it vanishes and in the spin") {
            rig.placeX(1f, 0f)
            rig.spawnBoss()
            rig.ticksUntil(4f) { rig.spawnedOf<D1000Core>().isNotEmpty() && animator().currentKey == "core_appear" } shouldBe true
            pupil().hidden shouldBe true
            rig.ticks(ENTRY_SECONDS)
            rig.boss.bossComponent.phase = BossPhase.FIGHT
            rig.rollFor(D1000Action.WALK, opening)
            rig.ticksUntil(4f) { drawn() == ("core" to D1000Frames.CORE_SHUT_FRAME) } shouldBe true
            pupil().hidden shouldBe true
            rig.ticksUntil(2f) { rig.core().vulnerable && drawn() == ("core" to 0) } shouldBe true
            pupil().hidden shouldBe false
            rig.core().takeDamageFrom(rig.sabre()) shouldBe true
            rig.ticksUntil(1f) { drawn().first == "core_vanish" } shouldBe true
            pupil().hidden shouldBe true
            rig.ticksUntil(8f) { !rig.boss.stunned && rig.core().vulnerable } shouldBe true
            defeat()
            rig.ticksUntil(2f) { drawn().first == "core_spin" } shouldBe true
            pupil().hidden shouldBe true
        }

        it("turns grey in the hurt flicker and orange in the second angry frame") {
            idleFight()
            rig.ticksUntil(2f) { rig.core().vulnerable } shouldBe true
            rig.core().takeDamageFrom(rig.bullet()) shouldBe true
            rig.ticksUntil(0.2f) { !eyeSprite().hidden && drawn().first == "core_hit" } shouldBe true
            pupil().hidden shouldBe false
            pupil().regionY shouldBe D1000Frames.PUPIL_HIT * D1000Frames.PUPIL_SIZE
            rig.ticksUntil(12f) { drawn() == ("core_angry" to 1) } shouldBe true
            pupil().regionY shouldBe D1000Frames.PUPIL_ANGRY * D1000Frames.PUPIL_SIZE
        }

        it("stays inside the iris at every offset, and the iris rows match the art") {
            val image = ImageIO.read(File("$FRAMES_DIR/D1000Core/core.png"))
            val eye = D1000Frames.EYE_BOXES["core"][0]
            val rows = HashMap<Int, Pair<Int, Int>>()
            for (row in eye[1] until eye[1] + eye[3]) for (col in eye[0] until eye[0] + eye[2]) {
                if (image.getRGB(col, row) and 0xFFFFFF != IRIS || (image.getRGB(col, row) ushr 24) == 0) continue
                val range = rows[row]
                rows[row] = if (range == null) col to col else minOf(range.first, col) to maxOf(range.second, col)
            }
            D1000Frames.IRIS_ROWS.map { it[0] to (it[1] to it[2]) }.toMap() shouldBe rows
            val out = IntArray(2)
            for (dx in -D1000Frames.PUPIL_REACH_X..D1000Frames.PUPIL_REACH_X) for (dy in -D1000Frames.PUPIL_REACH_Y..D1000Frames.PUPIL_REACH_Y) {
                D1000Frames.pupilOffset(dx.toFloat() / D1000Frames.PUPIL_REACH_X, dy.toFloat() / D1000Frames.PUPIL_REACH_Y, out)
                withClue("toward $dx, $dy gave ${out.toList()}") {
                    D1000Frames.inIris(D1000Frames.PUPIL_REST_X + out[0], D1000Frames.PUPIL_REST_Y + out[1]) shouldBe true
                    (abs(out[0]) <= abs(dx) && abs(out[1]) <= abs(dy)) shouldBe true
                }
            }
            D1000Frames.pupilOffset(1f, 1f, out).toList() shouldBe listOf(1, 1)
        }
    }
})
