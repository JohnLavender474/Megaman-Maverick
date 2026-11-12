package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.world.body.BodyComponent
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.decorations.D1000BombMarker
import com.megaman.maverick.game.entities.projectiles.D1000Bomb
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.unmockkAll
import kotlin.math.abs
import com.megaman.maverick.game.entities.bosses.d1000.D1000Tell
import com.megaman.maverick.game.entities.bosses.d1000.D1000Moves
import com.megaman.maverick.game.entities.bosses.d1000.D1000Tier
import com.megaman.maverick.game.entities.bosses.d1000.D1000Action
import com.megaman.maverick.game.entities.bosses.d1000.D1000Director

private const val TOLERANCE = 0.01f
private const val WALK_HEIGHT = 91
private const val LEFT_WALL_ORIGIN = RIG_ARENA_X + 2.1875f * RIG_PPM

class D1000CornerTest : DescribeSpec({

    var rig: D1000Rig? = null

    afterEach {
        rig?.dispose()
        rig = null
        MegaEntityFactory.reset()
        unmockkAll()
    }

    fun newRig(arenaTiles: Float = RIG_ARENA_WIDTH / RIG_PPM) =
        D1000Rig(arenaWidth = arenaTiles * RIG_PPM).also { rig = it }

    fun shownTell(sprite: GameSprite) = D1000Tell.entries.first {
        abs(it.red - sprite.color.r) < TOLERANCE && abs(it.green - sprite.color.g) < TOLERANCE &&
            abs(it.blue - sprite.color.b) < TOLERANCE
    }

    val opening = d1000Context(cyclesSinceCombo = D1000Director.comboGap(D1000Tier.NORMAL, false))

    describe("the retreat") {
        it("steps the body back from a cornered X at the start of THINK: walk pose, lamp off, no orb, until the pocket is fair") {
            val rig = newRig()
            rig.placeX(1f, 0f)
            rig.startFight()
            rig.rollFor(D1000Action.WALK, opening)
            rig.ticksUntil(4f) { rig.boss.retreating } shouldBe true
            (rig.boss.pocket() < D1000.FAIR_POCKET) shouldBe true
            val lamp = rig.boss.sprites[D1000.LAMP]
            val start = rig.bossCenter().x
            var walkPose = false
            var lampOff = true
            rig.ticksUntil(2f) {
                walkPose = walkPose || rig.boss.defaultSprite.regionHeight == WALK_HEIGHT
                lampOff = lampOff && shownTell(lamp) == D1000Tell.OFF
                !rig.boss.retreating
            } shouldBe true
            walkPose shouldBe true
            lampOff shouldBe true
            (rig.bossCenter().x > start + RIG_PPM) shouldBe true
            withClue("pocket ${rig.boss.pocket() / RIG_PPM} tiles") {
                (rig.boss.pocket() >= D1000.FAIR_POCKET - TOLERANCE * RIG_PPM) shouldBe true
            }
            rig.spawnedOf<D1000Bomb>().isEmpty() shouldBe true
            rig.spawnedOf<D1000BombMarker>().isEmpty() shouldBe true
            rig.boss.cornered() shouldBe false
        }

        it("does not run when X has room: the body stands still in THINK after a walk to an X 7 tiles from the wall") {
            val rig = newRig()
            rig.placeX(7f, 0f)
            rig.startFight()
            rig.rollFor(D1000Action.WALK, opening)
            rig.ticksUntil(4f) { !rig.boss.retreating && rig.boss.pocket() < 10f * RIG_PPM } shouldBe true
            rig.boss.cornered() shouldBe false
            rig.ticksUntil(1.5f) { rig.boss.retreating } shouldBe false
        }
    }

    describe("the pocket") {
        it("is measured on the widest hover reach, so it does not change with the stand frame") {
            val rig = newRig()
            rig.placeX(1f, 0f)
            rig.startFight()
            val center = rig.bossCenter().x
            val frames = HashSet<Int>()
            val pockets = HashSet<Float>()
            rig.ticksUntil(0.6f) {
                if (rig.bossCenter().x == center) {
                    frames.add(rig.boss.defaultSprite.regionY)
                    pockets.add(rig.boss.pocket())
                }
                false
            }
            (frames.size >= 2) shouldBe true
            pockets.size shouldBe 1
        }
    }

    describe("no orb into an unfair pocket") {
        it("refuses the Bracket when the retreat ends at the far wall with the pocket still unfair") {
            val rig = newRig(9.5f)
            rig.placeX(1f, 0f)
            rig.startFight()
            rig.rollFor(D1000Action.BRACKET, opening)
            rig.ticksUntil(4f) { rig.boss.retreating } shouldBe true
            rig.ticksUntil(2f) { !rig.boss.retreating } shouldBe true
            (rig.bossCenter().x > LEFT_WALL_ORIGIN + 4f * RIG_PPM) shouldBe true
            rig.boss.cornered() shouldBe true
            rig.ticksUntil(2f) { rig.boss.guard.refused.contains(D1000Action.BRACKET) } shouldBe true
            rig.spawnedOf<D1000Bomb>().isEmpty() shouldBe true
        }
    }

    describe("the eye in X's band") {
        it("is a danger in the trap model while its ball is inside X's height band, and not when X is on the floor") {
            val rig = newRig()
            rig.placeX(3f, 0f)
            rig.startFight()
            val eye = rig.core().getComponent(BodyComponent::class)!!.body.getCenter(Vector2())
            val eyeTiles = (eye.x - RIG_ARENA_X) / RIG_PPM
            fun eyeDangers(): Int {
                rig.boss.guard.begin(rig.arena, rig.xBody, D1000Moves.NORMAL)
                val escape = rig.boss.guard.escape
                return (0 until escape.count).count { i ->
                    val danger = escape.danger(i)
                    !danger.stun && danger.from == 0f && danger.left <= eye.x - 0.3f * RIG_PPM &&
                        danger.right >= eye.x + 0.3f * RIG_PPM
                }
            }
            eyeDangers() shouldBe 0
            rig.placeX(eyeTiles - 2f, (eye.y - RIG_FLOOR) / RIG_PPM - 0.75f)
            (eyeDangers() > 0) shouldBe true
        }
    }
})
