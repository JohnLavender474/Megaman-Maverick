package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectSet
import com.maverick.game.engine.camera.CameraShake
import com.maverick.game.engine.common.extensions.objectSetOf
import com.maverick.game.engine.events.Event
import com.maverick.game.engine.events.IEventListener
import com.maverick.game.engine.world.body.BodyComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.difficulty.DifficultyMode
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.decorations.D1000DebrisWarning
import com.megaman.maverick.game.entities.hazards.D1000CeilingDebris
import com.megaman.maverick.game.entities.hazards.D1000Shockwave
import com.megaman.maverick.game.entities.state.BossPhase
import com.megaman.maverick.game.entities.state.StunComponent
import com.megaman.maverick.game.entities.state.stunned
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.misc.StunType
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.setBodySense
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import io.mockk.unmockkAll
import kotlin.math.abs
import com.megaman.maverick.game.entities.bosses.d1000.D1000Tier
import com.megaman.maverick.game.entities.bosses.d1000.D1000Action
import com.megaman.maverick.game.entities.bosses.d1000.D1000Director

private const val TOLERANCE = 0.01f
private const val ENTRY_TILES = 10.75f
private const val NEAR_X_TILES = ENTRY_TILES - 1f
private const val LEFT_RAM_STOP = RIG_ARENA_X + 2.1875f * RIG_PPM

class D1000SlamTest : DescribeSpec({

    lateinit var rig: D1000Rig
    val events = mutableListOf<Event>()

    val opening = d1000Context(cyclesSinceCombo = D1000Director.comboGap(D1000Tier.NORMAL, false))

    beforeEach {
        rig = D1000Rig()
        events.clear()
        rig.x.addComponent(rig.x.defineStunComponent())
        rig.services.events.addListener(object : IEventListener {
            override val eventKeyMask: ObjectSet<Any> = objectSetOf(EventType.SHAKE_CAM, EventType.STUN_PLAYER)
            override fun onEvent(event: Event) {
                events.add(event)
            }
        })
        rig.services.events.addListener(rig.x)
    }

    afterEach {
        rig.dispose()
        MegaEntityFactory.reset()
        unmockkAll()
    }

    fun tick() {
        rig.tick()
        rig.services.events.run()
    }

    fun ticksUntil(maxSeconds: Float, done: () -> Boolean): Boolean {
        var time = 0f
        while (time < maxSeconds) {
            if (done()) return true
            tick()
            time += RIG_DELTA
        }
        return done()
    }

    fun ticks(seconds: Float) {
        var time = 0f
        while (time < seconds) {
            tick()
            time += RIG_DELTA
        }
    }

    fun stuns() = events.filter { it.key == EventType.STUN_PLAYER }

    fun shakes() = events.filter { it.key == EventType.SHAKE_CAM }

    fun ground(onGround: Boolean) = rig.xBody.setBodySense(BodySense.FEET_ON_GROUND, onGround)

    fun stun() = rig.x.getComponent(StunComponent::class)!!

    fun tiles(px: Float) = (px - RIG_ARENA_X) / RIG_PPM

    fun overdriveFight() {
        rig.spawnBoss()
        rig.hitEye(3)
        rig.ticks(ENTRY_SECONDS)
        rig.boss.bossComponent.phase = BossPhase.FIGHT
        rig.ticks(1.15f)
    }

    describe("the crouch slam") {
        it("shakes the camera on every frame for the whole shake, so the shake shows") {
            ground(true)
            rig.placeX(NEAR_X_TILES, 0f)
            rig.startFight()
            ticksUntil(2f) { stuns().isNotEmpty() } shouldBe true
            val shake = shakes().last()
            val duration = shake.getProperty(ConstKeys.DURATION, Float::class)!!
            val interval = shake.getProperty(ConstKeys.INTERVAL, Float::class)!!
            val shakeY = shake.getProperty(ConstKeys.Y, Float::class)!!
            (interval < RIG_DELTA) shouldBe true
            (shakeY >= 0.125f) shouldBe true

            val camera = OrthographicCamera()
            val part = CameraShake(RIG_PPM)
            part.startShake(duration, interval, shake.getProperty(ConstKeys.X, Float::class)!!, shakeY)
            var last = 0f
            var frames = 0
            while (frames < (duration / RIG_DELTA).toInt() - 1) {
                camera.position.set(0f, 0f, 0f)
                part.update(camera, RIG_DELTA)
                val y = camera.position.y
                abs(y) shouldBe (shakeY * RIG_PPM plusOrMinus TOLERANCE)
                if (frames > 0) (y * last < 0f) shouldBe true
                last = y
                frames++
            }
        }

        it("stuns X on the ground for 0.5 s") {
            ground(true)
            rig.placeX(NEAR_X_TILES, 0f)
            rig.startFight()
            ticksUntil(2f) { stuns().isNotEmpty() } shouldBe true
            val event = stuns().single()
            event.getProperty(ConstKeys.TYPE, StunType::class) shouldBe StunType.STUN_BOUNCE_IF_ON_SURFACE
            event.getProperty(ConstKeys.DURATION, Float::class) shouldBe 0.5f
            rig.x.stunned shouldBe true
            stun().update(0.45f, true)
            rig.x.stunned shouldBe true
            stun().update(0.1f, true)
            rig.x.stunned shouldBe false
        }

        it("stuns X for 0.75 s on Hard") {
            rig.services.gameState.setDifficultyMode(DifficultyMode.HARD)
            ground(true)
            rig.placeX(NEAR_X_TILES, 0f)
            rig.startFight()
            ticksUntil(2f) { stuns().isNotEmpty() } shouldBe true
            stuns().single().getProperty(ConstKeys.DURATION, Float::class) shouldBe 0.75f
            stun().update(0.7f, true)
            rig.x.stunned shouldBe true
            stun().update(0.1f, true)
            rig.x.stunned shouldBe false
        }

        it("does not stun X in the air: the jump is the dodge") {
            ground(false)
            rig.placeX(NEAR_X_TILES, 0f)
            rig.startFight()
            ticksUntil(2f) { stuns().isNotEmpty() } shouldBe true
            rig.x.stunned shouldBe false
        }

        it("drops junk at X and X +- 2.5 tiles (Hard: X, X +- 1.75 and X +- 3.5)") {
            listOf(false to listOf(-2.5f, 0f, 2.5f), true to listOf(-3.5f, -1.75f, 0f, 1.75f, 3.5f)).forEach { (hard, offsets) ->
                rig.dispose()
                MegaEntityFactory.reset()
                rig = D1000Rig()
                if (hard) rig.services.gameState.setDifficultyMode(DifficultyMode.HARD)
                rig.placeX(NEAR_X_TILES, 0f)
                rig.startFight()
                rig.ticksUntil(2f) { rig.spawnedOf<D1000DebrisWarning>().isNotEmpty() } shouldBe true
                rig.spawnedOf<D1000DebrisWarning>().map { tiles(it.position.x) } shouldBe offsets.map { NEAR_X_TILES + it }
            }
        }

        it("in Overdrive, the shockwaves leave only after X's stun ends (0.5 s + 0.4 s)") {
            ground(true)
            rig.placeX(NEAR_X_TILES, 0f)
            overdriveFight()
            ticksUntil(2f) { stuns().isNotEmpty() } shouldBe true
            ticks(0.85f)
            rig.spawnedOf<D1000Shockwave>().size shouldBe 0
            ticksUntil(0.1f) { rig.spawnedOf<D1000Shockwave>().size == 2 } shouldBe true
        }
    }

    describe("the ram") {
        it("the wall hit shakes the camera and stuns X on the ground") {
            ground(true)
            rig.startFight()
            rig.rollFor(D1000Action.RAM, opening)
            ticksUntil(4f) { rig.bossCenter().x <= LEFT_RAM_STOP + TOLERANCE } shouldBe true
            tick()
            stuns().size shouldBe 1
            shakes().last().getProperty(ConstKeys.X, Float::class)!! shouldBe (0.1875f plusOrMinus TOLERANCE)
            rig.x.stunned shouldBe true
        }

        it("Ricochet Ram: the first wall hit shakes but does not stun; the last wall hit stuns") {
            ground(true)
            overdriveFight()
            tick()
            events.clear()
            rig.rollFor(D1000Action.RAM, opening.copy(tier = D1000Tier.OVERDRIVE))
            ticksUntil(4f) { rig.bossCenter().x <= LEFT_RAM_STOP + TOLERANCE } shouldBe true
            tick()
            stuns().size shouldBe 0
            shakes().size shouldBe 1
            ticksUntil(4f) { stuns().isNotEmpty() } shouldBe true
            shakes().size shouldBe 2
        }
    }

    describe("the debris warning") {
        it("the junk drops loose under the ceiling, hangs and wobbles, then falls from the hang point") {
            rig.startFight()
            rig.rollFor(D1000Action.RAM, opening)
            rig.ticksUntil(4f) { rig.spawnedOf<D1000DebrisWarning>().isNotEmpty() } shouldBe true
            val warning = rig.spawnedOf<D1000DebrisWarning>().first()
            val chunk = warning.sprites["chunk"]
            fun junk() = rig.spawnedOf<D1000CeilingDebris>()
            warning.chunkPoint.y shouldBe (warning.position.y plusOrMinus 1f)
            rig.ticks(D1000DebrisWarning.LOOSE_DUR)
            warning.chunkPoint.y shouldBe (warning.hangPoint.y plusOrMinus TOLERANCE)
            (warning.position.y - warning.hangPoint.y) shouldBe (D1000DebrisWarning.HANG_DROP plusOrMinus TOLERANCE)
            val angles = mutableListOf<Float>()
            while (junk().isEmpty() && angles.size < 60) {
                rig.tick()
                angles.add(chunk.rotation)
                warning.chunkPoint.y shouldBe (warning.hangPoint.y plusOrMinus TOLERANCE)
            }
            (angles.max() > 5f && angles.min() < -5f) shouldBe true
            junk().size shouldBe rig.spawnedOf<D1000DebrisWarning>().size
            val center = junk().first().getComponent(BodyComponent::class)!!.body.getCenter(Vector2())
            center.x shouldBe (warning.hangPoint.x plusOrMinus TOLERANCE)
            center.y shouldBe (warning.hangPoint.y plusOrMinus TOLERANCE)
        }
    }
})
