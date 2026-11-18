package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.entities.body
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.hazards.MetHelmet
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.isSensing
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.ints.shouldBeGreaterThan as intShouldBeGreaterThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class MetMovementTest : DescribeSpec({

    lateinit var rig: MetRig

    beforeEach { rig = MetRig(cameraTiles = 400f) }

    afterEach { rig.dispose() }

    fun pitLevel() {
        rig.block(-30f, 0f, 32f, 1f)
        rig.death(2f, -3f, 20f, 1f)
        rig.placePlayer(15f, 1f)
    }

    describe("a running Met") {
        it("stops with its whole body on the ledge at a PIT, shields and keeps its facing") {
            pitLevel()
            val met = rig.met(0f, 1f)
            val keys = rig.keysUntil(met, 4f)
            keys shouldContain "Run"
            met.body.getMaxX() shouldBeLessThan 2f * MET_PPM
            met.body.getMaxX() shouldBeGreaterThan 1.5f * MET_PPM
            met.body.getY() shouldBe (MET_PPM plusOrMinus 0.01f)
            met.facing shouldBe Facing.RIGHT
            met.key() shouldBe "LayDown"
        }

        it("stops at a WALL that no jump clears and keeps its facing") {
            rig.block(-30f, 0f, 60f, 1f)
            rig.block(2f, 1f, 1f, 8f)
            rig.placePlayer(15f, 1f)
            val met = rig.met(0f, 1f)
            val keys = rig.keysUntil(met, 4f)
            keys shouldContain "Run"
            met.body.getMaxX() shouldBeLessThan 2f * MET_PPM + 0.01f
            met.body.getMaxX() shouldBeGreaterThan 1.5f * MET_PPM
            met.facing shouldBe Facing.RIGHT
            met.key() shouldBe "LayDown"
        }

        it("walks on at FLOOR") {
            rig.block(-30f, 0f, 60f, 1f)
            rig.placePlayer(15f, 1f)
            val met = rig.met(0f, 1f)
            rig.ticksUntil(3f) { met.key() == "Run" } shouldBe true
            rig.ticks(0.2f)
            met.body.physics.velocity.x shouldBe (8f * MET_PPM plusOrMinus 0.01f)
            met.tiles().x shouldBeGreaterThan 0.5f
        }

        it("walks off at SAFE_DROP and lands on the lower floor") {
            rig.block(-30f, 0f, 32f, 1f)
            rig.block(2f, -2f, 28f, 1f)
            rig.placePlayer(15f, -1f)
            val met = rig.met(0f, 1f)
            rig.ticks(5f)
            met.body.getY() shouldBe (-MET_PPM plusOrMinus 0.05f * MET_PPM)
            met.body.getX() shouldBeGreaterThan 2f * MET_PPM
        }

        it("goes straight to SHIELDING after POP_UP when its way is blocked, with no Run frame") {
            pitLevel()
            val met = rig.met(1.44f, 1f)
            val keys = rig.keysUntil(met, 3f)
            keys shouldContain "PopUp"
            keys shouldNotContain "Run"
            var after = keys.indexOf("PopUp", false)
            while (keys.get(after) == "PopUp") after++
            keys.get(after) shouldBe "LayDown"
            met.body.getMaxX() shouldBeLessThan 2f * MET_PPM
        }

        it("keeps its run speed when it leaves the ground with run_only") {
            rig.block(-30f, 0f, 32f, 1f)
            rig.block(-30f, -20f, 80f, 1f)
            val met = rig.met(0f, 1f, Met.RUN_ONLY to true, ConstKeys.RIGHT to true)
            var airborne = 0
            rig.ticksUntil(2f) {
                if (met.body.getY() < 0.9f * MET_PPM && met.body.getY() > -10f * MET_PPM) {
                    airborne++
                    met.body.physics.velocity.x shouldBe (8f * MET_PPM plusOrMinus 0.01f)
                }
                met.body.getY() < -10f * MET_PPM
            } shouldBe true
            airborne intShouldBeGreaterThan 0
        }
    }

    describe("a desperate Met") {
        it("stands at a PIT, faces the player and never shoots") {
            pitLevel()
            val met = rig.met(1.44f, 1f, ConstKeys.RIGHT to true)
            rig.ticks(0.2f)
            rig.stun(met, full = true, fromLeft = false)
            val helmet = rig.helmetOf(met).shouldNotBeNull()
            rig.ticks(0.15f)
            rig.hitHelmet(helmet, fromRight = false)
            rig.ticks(0.1f)
            rig.hitHelmet(helmet, fromRight = false)
            rig.ticksUntil(2f) { met.key() in DESPERATE_KEYS } shouldBe true
            rig.forgetBullets()
            val x = met.body.getX()
            var runFrames = 0
            repeat(180) {
                rig.tick()
                (met.key() in DESPERATE_KEYS) shouldBe true
                runFrames = if (met.key() == "NoHelmetRun") runFrames + 1 else 0
                withClue("consecutive NoHelmetRun frames") { (runFrames <= 1) shouldBe true }
                met.facing shouldBe Facing.RIGHT
                met.body.getX() shouldBe (x plusOrMinus 0.01f)
            }
            rig.bulletsOf(met) shouldBe 0
        }
    }

    describe("a lost helmet") {
        it("gives CRY when the way to the helmet is blocked; the helmet is released, then DESPERATE_RUN") {
            rig.block(-30f, 0f, 32f, 1f)
            val met = rig.met(1.4f, 1f)
            rig.stun(met)
            val helmet = rig.helmetOf(met).shouldNotBeNull()
            rig.ticksUntil(3f) { met.key() == "Cry" } shouldBe true
            helmet.owner.shouldBeNull()
            val cry = rig.ticksWhile(met, "Cry", 2f)
            cry shouldBe (1f plusOrMinus 2f * MET_DELTA)
            met.key() shouldBe "NoHelmetRun"
        }

        it("gives CRY when the helmet is culled") {
            rig.block(-30f, 0f, 60f, 1f)
            val met = rig.met(0f, 1f)
            rig.stun(met)
            rig.helmetOf(met).shouldNotBeNull().destroy()
            val keys = rig.keysUntil(met, MET_T_MIN + 0.2f)
            keys shouldContain "Cry"
            keys shouldNotContain "WorriedRun"
        }

        it("gives CRY after the 4 s fetch timeout") {
            rig.block(-30f, 0f, 60f, 1f)
            val met = rig.met(0f, 1f, ConstKeys.SPEED to 0.2f)
            val start = rig.stun(met)
            val helmet = rig.helmetOf(met).shouldNotBeNull()
            rig.ticksUntil(2f) { met.key() == "WorriedRun" } shouldBe true
            val fetch = rig.ticksWhile(met, "WorriedRun", 6f)
            withClue("fetch from ${rig.time - start - fetch}") { fetch shouldBe (4f plusOrMinus 0.1f) }
            met.key() shouldBe "Cry"
            helmet.owner.shouldBeNull()
            helmet.status shouldBe MetHelmet.HelmetStatus.RESTING
        }

        it("leaves a Met that never shoots") {
            rig.block(-30f, 0f, 60f, 1f)
            rig.placePlayer(-8f, 1f)
            val met = rig.met(0f, 1f)
            rig.stun(met)
            rig.helmetOf(met).shouldNotBeNull().destroy()
            rig.ticksUntil(2f) { met.key() == "Cry" } shouldBe true
            rig.forgetBullets()
            rig.ticks(6f)
            rig.bulletsOf(met) shouldBe 0
        }

        it("is fetched after T_min when it rests: touching it gives PICKUP_HELMET, then SHIELDING") {
            rig.block(-30f, 0f, 60f, 1f)
            val met = rig.met(0f, 1f)
            val start = rig.stun(met)
            val helmet = rig.helmetOf(met).shouldNotBeNull()
            rig.ticksWhile(met, "Stunned", 2f)
            (rig.time - start) shouldBeGreaterThan MET_T_MIN - 2f * MET_DELTA
            helmet.status shouldBe MetHelmet.HelmetStatus.RESTING
            met.key() shouldBe "WorriedRun"
            val keys = Array<String>()
            rig.ticksUntil(3f) {
                keys.add(met.key())
                met.key() == "LayDown"
            } shouldBe true
            keys shouldContain "Pickup"
            helmet.existenceState.liveNow shouldBe false
            met.body.isSensing(BodySense.FEET_ON_GROUND) shouldBe true
        }
    }
})
