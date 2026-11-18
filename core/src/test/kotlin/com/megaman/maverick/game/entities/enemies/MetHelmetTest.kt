package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.entities.body
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.decorations.FallingPiece
import com.megaman.maverick.game.entities.hazards.MetHelmet
import com.megaman.maverick.game.world.body.FixtureType
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.ints.shouldBeGreaterThan as intShouldBeGreaterThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldNotBeInstanceOf
import kotlin.math.abs

class MetHelmetTest : DescribeSpec({

    lateinit var rig: MetRig

    beforeEach {
        rig = MetRig()
        rig.floor()
    }

    afterEach { rig.dispose() }

    fun launch(met: Met, full: Boolean = false, fromLeft: Boolean = true): MetHelmet {
        rig.stun(met, full, fromLeft)
        return rig.helmetOf(met).shouldNotBeNull()
    }

    fun freeRestingHelmet(): MetHelmet {
        val met = rig.met(0f, 1f)
        val helmet = launch(met)
        met.destroy()
        rig.tick()
        rig.ticksUntil(2f) { helmet.status == MetHelmet.HelmetStatus.RESTING } shouldBe true
        return helmet
    }

    describe("the launch") {
        listOf(false to 1.5f, true to 3f).forEach { (full, apex) ->
            it("reaches an apex of $apex tiles (full=$full)") {
                val met = rig.met(0f, 1f)
                val helmet = launch(met, full)
                val start = met.body.getY() + 0.5f * MET_PPM
                var top = start
                rig.ticksUntil(2f) {
                    top = maxOf(top, helmet.body.getY())
                    helmet.status != MetHelmet.HelmetStatus.FLYING
                }
                (top - start) / MET_PPM shouldBe (apex plusOrMinus 0.15f)
            }
        }

        listOf(false to 2.3f, true to 4.9f).forEach { (full, distance) ->
            listOf(true, false).forEach { fromLeft ->
                it("drifts away from the shot side and rests about $distance tiles away (full=$full, fromLeft=$fromLeft)") {
                    val met = rig.met(0f, 1f)
                    val helmet = launch(met, full, fromLeft)
                    rig.ticksUntil(2f) { helmet.status == MetHelmet.HelmetStatus.RESTING } shouldBe true
                    val away = if (fromLeft) 1f else -1f
                    val offset = (helmet.tiles().x - met.tiles().x) * away
                    offset shouldBe (distance plusOrMinus 0.4f)
                    abs(offset) shouldBeGreaterThan 1f
                }
            }
        }

        it("lets the launching shot pass: it bounces once and does not count again") {
            val met = rig.met(0f, 1f)
            val shot = rig.shootAt(met, full = true)
            rig.ticksUntil(0.5f) { met.key() == "Stunned" } shouldBe true
            rig.tick()
            val helmet = rig.helmetOf(met).shouldNotBeNull()
            var reversed = 0
            var last = shot.body.physics.velocity.x
            rig.ticksUntil(0.4f) {
                val vx = shot.body.physics.velocity.x
                if (shot.existenceState.liveNow && vx * last < 0f) reversed++
                last = vx
                false
            }
            helmet.hits shouldBe 1
            reversed shouldBe 0
            shot.body.physics.velocity.x shouldBeLessThan 0f
        }

        it("lets the launching shot pass when it bounces up through the rising helmet") {
            val met = rig.met(0f, 1f)
            val shot = rig.chargedShot(true, 0f, 3.5f, Vector2(0f, -1f))
            rig.ticksUntil(0.5f) { met.key() == "Stunned" } shouldBe true
            rig.tick()
            val helmet = rig.helmetOf(met).shouldNotBeNull()
            var reversed = 0
            var last = shot.body.physics.velocity.y
            rig.ticksUntil(0.4f) {
                val vy = shot.body.physics.velocity.y
                if (shot.existenceState.liveNow && vy * last < 0f) reversed++
                last = vy
                false
            }
            helmet.hits shouldBe 1
            reversed shouldBe 0
            shot.body.physics.velocity.y shouldBeGreaterThan 0f
            met.getCurrentHealth() shouldBe 30
        }
    }

    describe("the helmet body") {
        it("does no damage to anyone") {
            val met = rig.met(0f, 1f)
            val helmet = launch(met)
            helmet.shouldNotBeInstanceOf<IDamagerEntity>()
            helmet.body.fixtures.get(FixtureType.DAMAGER).shouldBeNull()
        }

        it("goes down when a block is on its head") {
            rig.block(-30f, 2.3f, 60f, 1f)
            val met = rig.met(0f, 1f)
            val helmet = launch(met)
            var top = helmet.body.getMaxY()
            rig.ticksUntil(MET_T_MIN - 0.2f) {
                top = maxOf(top, helmet.body.getMaxY())
                helmet.status == MetHelmet.HelmetStatus.SEATED
            } shouldBe true
            helmet.body.getY() shouldBe (1.5f * MET_PPM plusOrMinus 0.01f)
            top shouldBeLessThan 2.3f * MET_PPM + 0.01f
        }

        it("rests on a block under its feet") {
            val met = rig.met(0f, 1f)
            val helmet = launch(met)
            rig.ticksUntil(2f) { helmet.status == MetHelmet.HelmetStatus.RESTING } shouldBe true
            helmet.body.getY() shouldBe (MET_PPM plusOrMinus 0.1f * MET_PPM)
            helmet.body.physics.velocity.x shouldBe 0f
        }

        it("is released by a destroyed Met: it falls free, rests on a block and is culled off camera") {
            val met = rig.met(0f, 1f)
            val helmet = launch(met)
            met.destroy()
            rig.tick()
            helmet.owner.shouldBeNull()
            rig.ticksUntil(2f) { helmet.status == MetHelmet.HelmetStatus.RESTING } shouldBe true
            helmet.body.getY() shouldBe (MET_PPM plusOrMinus 0.1f * MET_PPM)
            rig.ticks(1.5f)
            helmet.existenceState.liveNow shouldBe true
            rig.lookAt(500f, 500f)
            rig.ticksUntil(1.5f) { !helmet.existenceState.liveNow } shouldBe true
        }
    }

    describe("hits on the helmet") {
        it("break a helmet with no owner on its own 3rd charged hit, with debris at the helmet") {
            val met = rig.met(0f, 1f)
            val helmet = launch(met, full = true)
            met.destroy()
            rig.ticks(0.1f)
            rig.hitHelmet(helmet)
            helmet.hits shouldBe 2
            rig.ticks(0.1f)
            val position = helmet.tiles()
            rig.hitHelmet(helmet)
            helmet.existenceState.liveAfterUpdate shouldBe false
            rig.tick()
            val pieces = rig.liveOf<FallingPiece>(FallingPiece.TAG)
            pieces.size intShouldBeGreaterThan 0
            pieces.forEach { it.body.getCenter(Vector2()).scl(1f / MET_PPM).dst(position) shouldBeLessThan 1.5f }
        }

        it("pass through a free helmet that rests on the ground; a flying one is solid to them") {
            val met = rig.met(0f, 1f)
            val flying = launch(met)
            rig.tick()
            flying.body.fixtures.get(FixtureType.SHIELD).first().isActive() shouldBe true
            flying.body.fixtures.get(FixtureType.BODY).first().isActive() shouldBe true
            met.destroy()
            rig.ticksUntil(2f) { flying.status == MetHelmet.HelmetStatus.RESTING } shouldBe true
            rig.tick()
            flying.body.fixtures.get(FixtureType.SHIELD).first().isActive() shouldBe false
            flying.body.fixtures.get(FixtureType.BODY).first().isActive() shouldBe false
            val center = flying.tiles()
            val shot = rig.chargedShot(false, center.x - 1.5f, 1.45f, 1f)
            rig.ticks(0.3f)
            flying.hits shouldBe 1
            shot.body.physics.velocity.x shouldBeGreaterThan 0f
            shot.body.getX() shouldBeGreaterThan flying.body.getMaxX()
        }
    }

    describe("the look") {
        it("turns a flying helmet in 45-degree steps and draws a resting one upright") {
            val met = rig.met(0f, 1f)
            val helmet = launch(met)
            val angles = HashSet<Float>()
            rig.ticksUntil(2f) {
                if (helmet.status == MetHelmet.HelmetStatus.FLYING) {
                    val rotation = helmet.sprite().rotation
                    withClue("rotation $rotation") { (rotation % 45f) shouldBe 0f }
                    angles.add(rotation)
                }
                helmet.status == MetHelmet.HelmetStatus.RESTING
            } shouldBe true
            angles.size intShouldBeGreaterThan 2
            rig.tick()
            helmet.sprite().rotation shouldBe 0f
        }

        it("draws a seated helmet upright") {
            rig.block(-30f, 2.3f, 60f, 1f)
            val met = rig.met(0f, 1f)
            val helmet = launch(met)
            var seated = 0
            var wasSeated = false
            rig.ticksUntil(2f) {
                val isSeated = helmet.existenceState.liveNow && helmet.status == MetHelmet.HelmetStatus.SEATED
                if (isSeated && wasSeated) {
                    seated++
                    helmet.sprite().rotation shouldBe 0f
                }
                wasSeated = isSeated
                !helmet.existenceState.liveNow
            } shouldBe true
            seated intShouldBeGreaterThan 0
        }

        it("draws a free resting helmet behind, upside down, with a slow brightness pulse") {
            val helmet = freeRestingHelmet()
            val brightness = ArrayList<Float>()
            repeat(80) {
                rig.tick()
                val sprite = helmet.sprite()
                sprite.priority.value shouldBe -2
                sprite.isFlipY shouldBe true
                brightness.add(sprite.color.r)
            }
            brightness.min() shouldBeGreaterThan 0.59f
            brightness.max() shouldBeLessThan 1.01f
            (brightness.max() - brightness.min()) shouldBeGreaterThan 0.3f
        }

        it("draws an owned resting helmet in front, upright side up, at full brightness") {
            val met = rig.met(0f, 1f)
            val helmet = launch(met)
            rig.ticksUntil(2f) { helmet.status == MetHelmet.HelmetStatus.RESTING } shouldBe true
            val sprite = helmet.sprite()
            sprite.priority.value shouldBe 5
            sprite.isFlipY shouldBe false
            sprite.color.r shouldBe 1f
        }
    }

    describe("a free helmet") {
        it("is claimed and put on by a desperate Met that touches it") {
            rig.placePlayer(-20f, 1f)
            val owner = rig.met(-6f, 1f, ConstKeys.RIGHT to false)
            val free = launch(owner)
            owner.destroy()
            rig.ticksUntil(2f) { free.status == MetHelmet.HelmetStatus.RESTING } shouldBe true

            val met = rig.met(0f, 1f)
            rig.stun(met, full = true)
            val helmet = rig.helmetOf(met).shouldNotBeNull()
            rig.ticks(0.15f)
            rig.hitHelmet(helmet)
            rig.ticks(0.1f)
            rig.hitHelmet(helmet)
            rig.ticksUntil(2f) { met.key() in DESPERATE_KEYS } shouldBe true

            rig.ticksUntil(4f) { met.key() == "Pickup" } shouldBe true
            free.existenceState.liveAfterUpdate shouldBe false
            rig.ticksUntil(1f) { met.key() == "LayDown" } shouldBe true
        }

        it("that another Met consumes in the same frame is not claimed") {
            rig.dispose()
            rig = MetRig()
            rig.block(-30f, 0f, 34f, 1f)
            rig.placePlayer(20f, 1f)
            val picker = rig.met(1f, 1f, Met.RUNNING_ALLOWED to false, ConstKeys.RIGHT to true)
            val standing = rig.met(3.44f, 1f, ConstKeys.RIGHT to true)

            rig.ticks(0.2f)
            rig.stun(standing, fromLeft = false)
            val broken = rig.helmetOf(standing).shouldNotBeNull()
            rig.ticks(0.15f)
            rig.hitHelmet(broken, fromRight = false)
            rig.ticks(0.1f)
            rig.hitHelmet(broken, fromRight = false)
            rig.ticksUntil(2f) { standing.key() in DESPERATE_KEYS } shouldBe true

            rig.ticksUntil(3f) { picker.key() == "PopUp" }
            rig.ticksUntil(3f) { picker.key() == "LayDown" } shouldBe true
            rig.stun(picker)
            val keys = com.badlogic.gdx.utils.Array<String>()
            rig.ticksUntil(4f) {
                keys.add(standing.key())
                picker.key() == "Pickup"
            } shouldBe true
            rig.keysUntil(standing, 1f, keys)
            keys shouldNotContain "Pickup"
            standing.key() shouldBe "NoHelmetStand"
        }
    }
})
