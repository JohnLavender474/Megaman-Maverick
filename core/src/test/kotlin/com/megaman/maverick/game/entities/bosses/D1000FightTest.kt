package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.hazards.D1000Pillar
import com.megaman.maverick.game.entities.projectiles.D1000Bomb
import com.megaman.maverick.game.entities.projectiles.JunkShot
import com.megaman.maverick.game.entities.state.BossPhase
import com.megaman.maverick.game.entities.utils.TickFrames
import com.megaman.maverick.game.players.PlayerDamageNegotiator
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import io.mockk.unmockkAll
import com.megaman.maverick.game.entities.bosses.d1000.D1000Frames

private const val TOLERANCE = 0.01f

private const val BODY_HEIGHT = 3.59375f * RIG_PPM
private const val BODY_ABOVE_ORIGIN = 0.703125f * RIG_PPM

class D1000FightTest : DescribeSpec({

    lateinit var rig: D1000Rig

    fun landThenRise() {
        rig.ticks(3f)
        rig.boss.onCoreSignal()
        rig.ticks(4f)
    }

    beforeEach { rig = D1000Rig() }

    afterEach {
        rig.dispose()
        MegaEntityFactory.reset()
        unmockkAll()
    }

    describe("the body") {
        it("is at the boss from the spawn, before any world update") {
            rig.spawnBoss()

            rig.bossCenter().x shouldBe (RIG_ARENA_X + RIG_ARENA_WIDTH / 2f plusOrMinus TOLERANCE)
            rig.bossCenter().y shouldBe (RIG_FLOOR + 8f * RIG_PPM + BODY_ABOVE_ORIGIN plusOrMinus TOLERANCE)
        }
    }

    describe("the landing") {
        it("falls until the landing pose's feet are a quarter tile into the floor, so the hull rests on it") {
            rig.spawnBoss()
            rig.ticks(3f)

            val origin = rig.bossCenter().y - BODY_ABOVE_ORIGIN
            val land = D1000Frames.BODY["land"]!!
            val feetBelowOrigin = (land.height - land.originY) * TickFrames.SCALE
            origin - feetBelowOrigin shouldBe (RIG_FLOOR - 0.25f * RIG_PPM plusOrMinus TOLERANCE)
        }
    }

    describe("the rise") {
        it("stays as low as its hover art allows when X is on the floor") {
            rig.spawnBoss()
            landThenRise()

            val artBelowOrigin = listOf("stand", "walk", "throw", "ram").maxOf {
                D1000Frames.BODY[it]!!.let { def -> (def.height - def.originY) * TickFrames.SCALE }
            }
            val origin = rig.bossCenter().y - BODY_ABOVE_ORIGIN
            origin - artBelowOrigin shouldBe (RIG_FLOOR plusOrMinus TOLERANCE)
            (rig.bossBody().getY() >= RIG_FLOOR + 0.5f * RIG_PPM) shouldBe true
        }

        it("rises toward X's height") {
            rig.xBody.setCenter(RIG_ARENA_X + RIG_PPM, RIG_FLOOR + 4f * RIG_PPM)
            rig.spawnBoss()
            landThenRise()

            rig.bossCenter().y shouldBe (RIG_FLOOR + 4f * RIG_PPM plusOrMinus TOLERANCE)
        }

        it("stops with the box top 0.5 tile under the ceiling") {
            rig.xBody.setCenter(RIG_ARENA_X + RIG_PPM, RIG_FLOOR + 20f * RIG_PPM)
            rig.spawnBoss()
            landThenRise()
            rig.ticks(4f)

            rig.bossBody().getMaxY() shouldBe (RIG_FLOOR + RIG_ARENA_HEIGHT - 0.5f * RIG_PPM plusOrMinus TOLERANCE)
            rig.bossBody().getHeight() shouldBe (BODY_HEIGHT plusOrMinus TOLERANCE)
        }
    }

    describe("the damage to X") {
        it("is low for the intro stage (X starts at 14): body, bomb and pillar 2, core shot 2") {
            val negotiator = PlayerDamageNegotiator(rig.x)
            negotiator.get(D1000(rig.services).also { it.init() }) shouldBe 2
            negotiator.get(D1000Bomb(rig.services).also { it.init() }) shouldBe 2
            negotiator.get(D1000Pillar(rig.services).also { it.init() }) shouldBe 2
            val coreShot = JunkShot(rig.services).also {
                it.spawn(
                    props(
                        ConstKeys.TYPE pairTo JunkShot.Type.CORE,
                        ConstKeys.POSITION pairTo Vector2(),
                        ConstKeys.TRAJECTORY pairTo Vector2()
                    )
                )
            }
            negotiator.get(coreShot) shouldBe 2
        }
    }

    describe("the stun") {
        it("lets a full charge hit the body before the stun and while it is down, not while it staggers") {
            rig.spawnBoss()
            landThenRise()
            rig.boss.bossComponent.phase = BossPhase.FIGHT
            rig.x.facing = Facing.RIGHT
            val shot = rig.fullCharge()
            rig.boss.canBeDamagedBy(shot) shouldBe true

            rig.boss.stun()
            rig.boss.canBeDamagedBy(shot) shouldBe false

            rig.ticks(1.5f)
            rig.boss.getCurrentHealth() shouldBe ConstVals.MAX_HEALTH - D1000.STUN_DAMAGE
            rig.boss.canBeDamagedBy(shot) shouldBe true
        }
    }
})
