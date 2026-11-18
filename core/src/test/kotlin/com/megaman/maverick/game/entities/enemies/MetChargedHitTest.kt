package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.entities.body
import com.megaman.maverick.game.entities.bosses.GutsTankFist
import com.megaman.maverick.game.entities.decorations.FallingPiece
import com.megaman.maverick.game.entities.hazards.MetHelmet
import com.megaman.maverick.game.entities.projectiles.BigAssMaverickRobotOrb
import com.megaman.maverick.game.entities.projectiles.PurpleBlast
import com.megaman.maverick.game.world.body.FixtureType
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.ints.shouldBeGreaterThan as intShouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith

val MET_T_MIN = MetHelmet.flightTime(1.5f)
val MET_T_MAX = MetHelmet.flightTime(3f) + 0.5f
val DESPERATE_KEYS = setOf("NoHelmetRun", "NoHelmetStand")

fun MetRig.floor() = block(-30f, 0f, 60f, 1f)

fun MetRig.stun(met: Met, full: Boolean = false, fromLeft: Boolean = true): Float {
    shootAt(met, full, fromLeft)
    ticksUntil(0.5f) { met.key() == "Stunned" } shouldBe true
    val start = time
    tick()
    return start
}

fun MetRig.stunAndRecover(met: Met, full: Boolean = false) {
    stun(met, full)
    ticksUntil(5f) { met.key() == "LayDown" } shouldBe true
}

fun MetRig.hitHelmet(helmet: MetHelmet, fromRight: Boolean = true) {
    val hitsBefore = helmet.hits
    val center = helmet.tiles()
    val side = if (fromRight) 1f else -1f
    chargedShot(false, center.x + side * 1f, center.y, -side)
    ticksUntil(0.3f) { helmet.hits > hitsBefore || !helmet.existenceState.liveNow } shouldBe true
}

class MetChargedHitTest : DescribeSpec({

    lateinit var rig: MetRig

    beforeEach {
        rig = MetRig()
        rig.floor()
    }

    afterEach { rig.dispose() }

    describe("a charged shot on the closed helmet") {
        listOf(false, true).forEach { full ->
            it("does no damage and stuns the Met (full=$full)") {
                val met = rig.met(0f, 1f)
                rig.stun(met, full)
                met.getCurrentHealth() shouldBe 30
                val helmets = rig.helmets()
                helmets shouldHaveSize 1
                helmets.first().owner shouldBe met
                helmets.first().hits shouldBe 1
            }

            it("does not push the Met back (full=$full)") {
                val met = rig.met(0f, 1f)
                val x = met.body.getX()
                rig.stun(met, full)
                repeat(48) {
                    rig.tick()
                    met.body.getX() shouldBe x
                }
            }

            it("does no damage through its bounce while the Met is stunned (full=$full)") {
                val met = rig.met(0f, 1f)
                rig.stun(met, full)
                rig.ticksUntil(2f) { met.key() != "Stunned" }
                met.getCurrentHealth() shouldBe 30
            }
        }

        it("does not stun a frozen Met") {
            val met = rig.met(0f, 1f)
            met.frozen = true
            rig.tick()
            rig.shootAt(met, full = false)
            rig.keysUntil(met, 1f) shouldNotContain "Stunned"
            rig.helmets().shouldBeEmpty()
        }

        it("passes over the dome when it only crosses the top corner of the body") {
            val met = rig.met(0f, 1f)
            val shot = rig.chargedShot(false, -1.5f, 2.3f, 1f)
            rig.ticks(0.4f)
            met.key() shouldBe "LayDown"
            rig.helmets().shouldBeEmpty()
            shot.body.physics.velocity.x shouldBeGreaterThan 0f
        }
    }

    describe("the stun") {
        it("lasts at least T_min when the helmet comes down early") {
            rig.block(-30f, 2.3f, 60f, 1f)
            val met = rig.met(0f, 1f)
            val start = rig.stun(met)
            val helmet = rig.helmetOf(met).shouldNotBeNull()
            rig.ticksUntil(MET_T_MIN - 0.2f) { helmet.status == MetHelmet.HelmetStatus.SEATED } shouldBe true
            rig.ticksWhile(met, "Stunned", 2f)
            (rig.time - start) shouldBeGreaterThan MET_T_MIN - 2f * MET_DELTA
            (rig.time - start) shouldBeLessThan MET_T_MIN + 2f * MET_DELTA
            met.key() shouldBe "HelmetLand"
        }

        it("ends at T_min with a fetch when the helmet rests") {
            val met = rig.met(0f, 1f)
            val start = rig.stun(met)
            val helmet = rig.helmetOf(met).shouldNotBeNull()
            rig.ticksWhile(met, "Stunned", 2f)
            (rig.time - start) shouldBeGreaterThan MET_T_MIN - 2f * MET_DELTA
            helmet.status shouldBe MetHelmet.HelmetStatus.RESTING
            met.key() shouldBe "WorriedRun"
        }

        it("ends at T_max with a fetch when the helmet is still in the air") {
            rig.dispose()
            rig = MetRig(cameraTiles = 400f)
            rig.block(-30f, 0f, 32f, 1f)
            val met = rig.met(1.4f, 1f)
            val start = rig.stun(met)
            rig.ticksWhile(met, "Stunned", 3f)
            val stunned = rig.time - start
            stunned shouldBeGreaterThan MET_T_MAX - 2f * MET_DELTA
            stunned shouldBeLessThan MET_T_MAX + 2f * MET_DELTA
            met.key() shouldBe "WorriedRun"
            rig.helmetOf(met).shouldNotBeNull().status shouldBe MetHelmet.HelmetStatus.FLYING
        }

        it("moves a seated helmet to HELMET_LAND, then SHIELDING") {
            rig.block(0.5f, 1f, 1f, 6f)
            val met = rig.met(0f, 1f)
            rig.stun(met)
            val keys = rig.keysUntil(met, 2.5f)
            var after = keys.indexOf("HelmetLand", false)
            withClue(keys.toString()) { (after >= 0).shouldBeTrue() }
            while (keys.get(after) == "HelmetLand") after++
            keys.get(after) shouldBe "LayDown"
            rig.helmets().shouldBeEmpty()
            met.getCurrentHealth() shouldBe 30
        }
    }

    describe("helmet hits") {
        it("break the helmet at the 3rd charged hit in one spawn and send the Met into DESPERATE_RUN") {
            val met = rig.met(0f, 1f)
            rig.stunAndRecover(met)
            rig.stunAndRecover(met)
            rig.shootAt(met, full = false)
            rig.ticksUntil(0.5f) { met.key() in DESPERATE_KEYS } shouldBe true
            rig.tick()
            rig.helmets().shouldBeEmpty()
            rig.liveOf<FallingPiece>(FallingPiece.TAG).size intShouldBeGreaterThan 0
            met.getCurrentHealth() shouldBe 30
        }

        it("count on the airborne helmet; the 3rd breaks it in the air and the Met goes desperate after T_min") {
            val met = rig.met(0f, 1f)
            val start = rig.stun(met, full = true)
            val helmet = rig.helmetOf(met).shouldNotBeNull()
            rig.ticks(0.15f)
            rig.hitHelmet(helmet)
            helmet.hits shouldBe 2
            rig.ticks(0.1f)
            val position = helmet.tiles()
            rig.hitHelmet(helmet)
            helmet.existenceState.liveAfterUpdate shouldBe false
            rig.tick()
            val pieces = rig.liveOf<FallingPiece>(FallingPiece.TAG)
            pieces.size intShouldBeGreaterThan 0
            pieces.forEach { piece ->
                piece.body.getCenter(Vector2()).scl(1f / MET_PPM).dst(position) shouldBeLessThan 1.5f
            }
            met.key() shouldBe "Stunned"
            rig.ticksUntil(2f) { met.key() != "Stunned" }
            (rig.time - start) shouldBeGreaterThan MET_T_MIN - 2f * MET_DELTA
            met.key() shouldStartWith "NoHelmet"
        }

        it("travel with the helmet: a Met that picks up a helmet with 2 hits breaks it at the next hit") {
            val met = rig.met(0f, 1f)
            rig.stun(met, full = true)
            val helmet = rig.helmetOf(met).shouldNotBeNull()
            rig.ticks(0.15f)
            rig.hitHelmet(helmet)
            helmet.hits shouldBe 2
            rig.ticksUntil(5f) { met.key() == "LayDown" } shouldBe true
            rig.shootAt(met, full = false)
            rig.ticksUntil(0.5f) { met.key() in DESPERATE_KEYS || met.key() == "Stunned" } shouldBe true
            met.key() shouldStartWith "NoHelmet"
        }

        it("do not count a charged shot on a stunned helmetless Met; it only damages it") {
            val met = rig.met(0f, 1f)
            rig.stun(met)
            val helmet = rig.helmetOf(met).shouldNotBeNull()
            rig.ticks(0.25f)
            rig.shootAt(met, full = false, fromLeft = false)
            rig.ticksUntil(0.3f) { met.getCurrentHealth() < 30 } shouldBe true
            met.getCurrentHealth() shouldBe 15
            helmet.hits shouldBe 1
            met.key() shouldBe "Stunned"
        }

        it("stun again on a charged hit during PICKUP_HELMET") {
            val met = rig.met(0f, 1f)
            rig.stun(met)
            rig.ticksUntil(4f) { met.key() == "Pickup" } shouldBe true
            rig.shootAt(met, full = false)
            rig.ticksUntil(0.3f) { met.key() != "Pickup" }
            met.key() shouldBe "Stunned"
            rig.tick()
            met.getCurrentHealth() shouldBe 15
            rig.helmetOf(met).shouldNotBeNull().hits shouldBe 2
        }

        it("break the helmet on a 3rd charged hit during PICKUP_HELMET") {
            val met = rig.met(0f, 1f)
            rig.stun(met, full = true)
            val helmet = rig.helmetOf(met).shouldNotBeNull()
            rig.ticks(0.15f)
            rig.hitHelmet(helmet)
            rig.ticksUntil(5f) { met.key() == "Pickup" } shouldBe true
            rig.shootAt(met, full = false)
            rig.ticksUntil(0.3f) { met.key() != "Pickup" }
            met.key() shouldStartWith "NoHelmet"
        }

        it("start again at 0 when a pooled Met spawns again") {
            val met = rig.met(0f, 1f)
            rig.stunAndRecover(met)
            rig.stunAndRecover(met)
            met.destroy()
            rig.tick()
            val again = rig.met(0f, 1f)
            (again === met) shouldBe true
            rig.ticks(0.2f)
            rig.stun(again)
        }
    }

    describe("damage") {
        it("an exposed Met takes the half-charge damage, is stunned and the hit counts") {
            val met = rig.met(0f, 1f)
            rig.ticksUntil(2f) { met.key() == "PopUp" } shouldBe true
            rig.shootAt(met, full = false)
            rig.ticksUntil(0.4f) { met.key() == "Stunned" } shouldBe true
            rig.tick()
            met.getCurrentHealth() shouldBe 15
            rig.helmetOf(met).shouldNotBeNull().hits shouldBe 1
        }

        it("an exposed Met dies when a full charge reaches its health") {
            val met = rig.met(0f, 1f)
            rig.ticksUntil(2f) { met.key() == "PopUp" } shouldBe true
            rig.shootAt(met, full = true)
            rig.ticksUntil(0.4f) { !met.existenceState.liveNow } shouldBe true
            rig.helmets().shouldBeEmpty()
        }

        it("a plain Bullet damages a stunned Met by 15") {
            val met = rig.met(0f, 1f)
            rig.stun(met)
            rig.ticks(0.25f)
            val center = met.tiles()
            rig.bullet(center.x + 1.2f, center.y, -1f)
            rig.ticksUntil(0.3f) { met.getCurrentHealth() < 30 } shouldBe true
            met.getCurrentHealth() shouldBe 15
        }

        listOf(false to 15, true to 0).forEach { (full, health) ->
            it("a run_only Met takes the charged-shot damage and is never stunned (full=$full)") {
                val met = rig.met(0f, 1f, Met.RUN_ONLY to true, com.megaman.maverick.game.ConstKeys.RIGHT to true)
                rig.ticks(0.2f)
                val keys = com.badlogic.gdx.utils.Array<String>()
                rig.shootAt(met, full, fromLeft = false)
                rig.ticksUntil(0.4f) {
                    keys.add(met.key())
                    met.getCurrentHealth() < 30
                } shouldBe true
                met.getCurrentHealth() shouldBe health
                rig.ticksUntil(0.5f) {
                    if (met.existenceState.liveNow) keys.add(met.key())
                    false
                }
                keys shouldNotContain "Stunned"
                rig.helmets().shouldBeEmpty()
            }
        }

        it("the instant-kill damagers still kill the Met") {
            listOf(GutsTankFist(rig.services), BigAssMaverickRobotOrb(rig.services), PurpleBlast(rig.services)).forEach {
                val met = rig.met(0f, 1f)
                rig.ticks(0.1f)
                withClue(it::class.simpleName) {
                    met.canBeDamagedBy(it) shouldBe true
                    met.takeDamageFrom(it)
                    met.isHealthDepleted() shouldBe true
                }
                met.destroy()
                rig.tick()
            }
        }
    }

    describe("a helmetless Met") {
        it("never shoots after a helmet break") {
            val met = rig.met(0f, 1f)
            rig.placePlayer(-8f, 1f)
            rig.stunAndRecover(met)
            rig.stunAndRecover(met)
            rig.shootAt(met, full = false)
            rig.ticksUntil(0.5f) { met.key() in DESPERATE_KEYS } shouldBe true
            rig.forgetBullets()
            rig.ticks(6f)
            met.existenceState.liveNow shouldBe true
            rig.bulletsOf(met) shouldBe 0
        }
    }
})
