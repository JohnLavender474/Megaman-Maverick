package com.megaman.maverick.game.entities.bosses.elecdevil

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.audio.AudioComponent
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameLine
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.events.Event
import com.maverick.game.engine.events.IEventListener
import com.maverick.game.engine.common.extensions.objectSetOf
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.bosses.ElecDevil
import com.megaman.maverick.game.entities.bosses.ElecDevilBodyPiece
import com.megaman.maverick.game.entities.bosses.ElecDevilState
import com.megaman.maverick.game.entities.bosses.bigassmaverickrobot.OrbAim
import com.megaman.maverick.game.entities.decorations.WilyGenerator
import com.megaman.maverick.game.entities.projectiles.ElecBall
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.world.body.FixtureType
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import kotlin.math.abs
import kotlin.math.max

private const val PIECES = 24
private const val DIRECTION_TOLERANCE = 0.001f
private const val ANGLE_TOLERANCE = 0.05f
private const val SPEED_TOLERANCE = 0.01f

private const val FAN = 0
private const val RING = 1
private const val STALL = 2
private const val BOUNCE = 3

private fun directionOf(ball: ElecBall) = Vector2(ball.getComponent(BodyComponent::class)!!.body.physics.velocity).nor()

private fun speedOf(ball: ElecBall) = ball.getComponent(BodyComponent::class)!!.body.physics.velocity.len()

private fun shouldPointAlong(actual: Vector2, expected: Vector2, label: String) =
    withClue("$label: expected $expected, got $actual") {
        abs(actual.x - expected.x) shouldBe (0f plusOrMinus DIRECTION_TOLERANCE)
        abs(actual.y - expected.y) shouldBe (0f plusOrMinus DIRECTION_TOLERANCE)
    }

class ElecDevilFightTest : DescribeSpec({

    lateinit var fight: ElecDevilRig

    afterEach { fight.dispose() }

    fun durationTolerance() = 2f * fight.rig.step

    fun newBalls(before: Int) = fight.balls().drop(before)

    fun untilBalls(before: Int, count: Int) = withClue("$count balls spawn") {
        fight.rig.ticksUntil(2f) { newBalls(before).size >= count } shouldBe true
    }

    describe("the attack cycle") {
        it("runs the master order in phase 1 with the times of the normal phase-1 row") {
            fight = ElecDevilRig()
            fight.startFight()
            fight.state shouldBe ElecDevilState.LAUNCH
            val order = mutableListOf(fight.state)
            repeat(8) { order.add(fight.untilStateChange()) }
            order shouldBe listOf(
                ElecDevilState.LAUNCH,
                ElecDevilState.APPEAR,
                ElecDevilState.STAND,
                ElecDevilState.CHARGE,
                ElecDevilState.STAND,
                ElecDevilState.HAND,
                ElecDevilState.STAND,
                ElecDevilState.TURN_TO_PIECES,
                ElecDevilState.LAUNCH
            )
            fight.secondsIn(ElecDevilState.APPEAR) shouldBe
                (ElecDevilTuning.NORMAL.appearDur plusOrMinus durationTolerance())
            fight.secondsIn(ElecDevilState.STAND) shouldBe
                (ElecDevilTuning.NORMAL.standDur plusOrMinus durationTolerance())
            fight.secondsIn(ElecDevilState.CHARGE) shouldBe
                (ElecDevilTuning.NORMAL.chargeDur plusOrMinus durationTolerance())
            fight.secondsIn(ElecDevilState.TURN_TO_PIECES) shouldBe
                (ElecDevilTuning.NORMAL.turnToPiecesDur plusOrMinus durationTolerance())
        }
    }

    describe("the phase-2 rollover") {
        it("refills the bar, flashes the cells, plays the tell and then uses the phase-2 row") {
            fight = ElecDevilRig()
            fight.startFight()
            fight.untilState(ElecDevilState.STAND)
            val sounds = fight.sounds(SoundAsset.MM3_ELECTRIC_PULSE_SOUND)
            fight.boss.isPhaseFlashOn() shouldBe false
            fight.toPhase2()
            fight.boss.getCurrentHealth() shouldBe fight.boss.getMaxHealth()
            fight.sounds(SoundAsset.MM3_ELECTRIC_PULSE_SOUND) shouldBe sounds + 1
            fight.rig.tick()
            fight.boss.isPhaseFlashOn() shouldBe true
            fight.rig.ticks(ElecDevil.PHASE_FLASH_DUR)
            fight.boss.isPhaseFlashOn() shouldBe false
            fight.untilState(ElecDevilState.TURN_TO_PIECES)
            withClue("phase 2 picks LAUNCH for its move slot") {
                fight.untilStateChange() shouldBe ElecDevilState.LAUNCH
            }
            fight.secondsIn(ElecDevilState.APPEAR) shouldBe
                (ElecDevilTuning.NORMAL_PHASE_2.appearDur plusOrMinus durationTolerance())
        }

        it("powers down each generator at the phase-2 start: no light events and no sparks after it") {
            fight = ElecDevilRig()
            val generator = MegaEntityFactory.fetch(WilyGenerator::class)!!
            generator.spawn(
                props(ConstKeys.BOUNDS pairTo GameRectangle(6f * T, T, 9f * T, 9f * T), ConstKeys.KEYS pairTo "1")
            ) shouldBe true
            fight.rig.processSpawns()
            var lights = 0
            fight.rig.services.events.addListener(object : IEventListener {
                override val eventKeyMask = objectSetOf<Any>(EventType.ADD_LIGHT_SOURCE)

                override fun onEvent(event: Event) {
                    if (event.properties.get(ConstKeys.OWNER) == generator) lights++
                }
            })
            val sprites = generator.getComponent(SpritesComponent::class)!!
            fun tickAndDraw(seconds: Float) = fight.rig.ticks(seconds) {
                fight.rig.runEvents()
                sprites.preProcess(fight.rig.step)
            }

            fight.startFight()
            fight.untilState(ElecDevilState.STAND)
            tickAndDraw(0.5f)
            generator.state shouldBe WilyGenerator.GeneratorState.ON
            lights shouldBeGreaterThan 0
            fight.toPhase2()
            generator.state shouldBe WilyGenerator.GeneratorState.POWER_DOWN
            tickAndDraw(1f)
            generator.state shouldBe WilyGenerator.GeneratorState.OFF
            fight.rig.runEvents()
            lights = 0
            var sparks = 0
            tickAndDraw(3f)
            sprites.sprites.forEach { entry ->
                if (entry.key.toString().startsWith("spark") && !entry.value.hidden) sparks++
            }
            fight.rig.runEvents()
            lights shouldBe 0
            sparks shouldBe 0
        }
    }

    describe("the eye patterns") {
        it("lists Fan, Ring and Stall in the phase-1 rows and also Bounce in the phase-2 rows") {
            val phase1 = listOf(EyePattern.FAN, EyePattern.RING, EyePattern.STALL)
            ElecDevilTuning.NORMAL.eyePatterns.toList() shouldBe phase1
            ElecDevilTuning.HARD.eyePatterns.toList() shouldBe phase1
            ElecDevilTuning.NORMAL_PHASE_2.eyePatterns.toList() shouldBe phase1 + EyePattern.BOUNCE
            ElecDevilTuning.HARD_PHASE_2.eyePatterns.toList() shouldBe phase1 + EyePattern.BOUNCE
        }

        it("picks each CHARGE pattern from the row with services.random, never the previous one") {
            fight = ElecDevilRig()
            fight.startFight()
            fight.untilEyePattern(FAN)
            fight.boss.getEyePattern() shouldBe EyePattern.FAN
            fight.random.scriptedIntRanges.last() shouldBe (0..2)
            fight.untilEyePattern(0)
            withClue("the candidates leave out the previous Fan") {
                fight.random.scriptedIntRanges.last() shouldBe (0..1)
                fight.boss.getEyePattern() shouldBe EyePattern.RING
            }
            fight.untilState(ElecDevilState.APPEAR)
            fight.untilState(ElecDevilState.STAND)
            fight.toPhase2()
            fight.random.ints.addLast(2)
            fight.untilState(ElecDevilState.CHARGE)
            withClue("the phase-2 candidates leave out the previous Ring") {
                fight.random.scriptedIntRanges.last() shouldBe (0..2)
                fight.boss.getEyePattern() shouldBe EyePattern.BOUNCE
            }
        }

        it("Fan: 3 balls EYE_SPREAD_SHOT_GAP apart in the aim order, at the targets of the end of CHARGE") {
            fight = ElecDevilRig()
            fight.startFight()
            val velocity = Vector2(4f * T, 0f)
            fight.untilEyePattern(FAN)
            fight.placePlayer(8f, 2f)
            fight.rig.playerBody.physics.velocity.set(velocity)
            val playerCenter = fight.rig.playerBody.getCenter(Vector2())
            val before = fight.balls().size
            val volts = fight.sounds(SoundAsset.VOLT_SOUND)
            fight.random.rolls.clear()
            fight.random.rolls.addLast(0.75f)
            fight.untilStateChange() shouldBe ElecDevilState.STAND
            fight.random.rolls.clear()
            fight.placePlayer(3f, 6f)
            fight.rig.playerBody.physics.velocity.setZero()
            untilBalls(before, 3)
            fight.rig.tick()
            val balls = newBalls(before)
            balls.size shouldBe 3

            val aims = ElecDevilTuning.NORMAL.eyeSpreadAims
            aims.toList() shouldBe listOf(OrbAim.LEAD, OrbAim.CURRENT, OrbAim.NEAR)
            val eye = fight.eyeCenter()
            balls.forEachIndexed { index, ball ->
                val expected = OrbAim.point(
                    aims[index], playerCenter, velocity, ElecDevilTuning.NORMAL.eyeSpreadLeadTime, fight.room,
                    { _, _ -> 0.75f }, Vector2()
                )
                shouldPointAlong(directionOf(ball), expected.sub(eye).nor(), "${aims[index]}")
                fight.animationKeyOf(ball) shouldBe "orb"
            }
            val times = balls.map { fight.rig.spawnTimes[it] }
            times.zipWithNext().forEach { (a, b) ->
                (b - a) shouldBe (ElecDevil.EYE_SPREAD_SHOT_GAP plusOrMinus fight.rig.step)
            }
            fight.sounds(SoundAsset.VOLT_SOUND) shouldBe volts + 3
            val lead = OrbAim.point(
                OrbAim.LEAD, playerCenter, velocity, ElecDevilTuning.NORMAL.eyeSpreadLeadTime, fight.room,
                { _, _ -> 0f }, Vector2()
            )
            lead.x shouldBe (playerCenter.x + velocity.x * ElecDevilTuning.NORMAL.eyeSpreadLeadTime plusOrMinus 0.01f)
        }

        it("Ring: RING_BALLS red balls at once, evenly spaced, one at the player, at RING_SPEED, 1 VOLT sound") {
            fight = ElecDevilRig()
            fight.startFight()
            fight.untilEyePattern(RING)
            fight.boss.getEyePattern() shouldBe EyePattern.RING
            fight.placePlayer(9f, 2f)
            val before = fight.balls().size
            val volts = fight.sounds(SoundAsset.VOLT_SOUND)
            fight.untilStateChange() shouldBe ElecDevilState.STAND
            fight.rig.tick()
            val balls = newBalls(before)
            balls.size shouldBe ElecDevil.RING_BALLS
            balls.map { fight.rig.spawnTimes[it] }.distinct().size shouldBe 1
            fight.rig.ticks(0.5f)
            newBalls(before).size shouldBe ElecDevil.RING_BALLS
            fight.sounds(SoundAsset.VOLT_SOUND) shouldBe volts + 1
            balls.forEach { ball ->
                speedOf(ball) shouldBe (ElecDevil.RING_SPEED * T plusOrMinus SPEED_TOLERANCE * T)
                fight.animationKeyOf(ball) shouldBe "orb_red"
            }
            val angles = balls.map { directionOf(it).angleDeg() }.sorted()
            val step = 360f / ElecDevil.RING_BALLS
            (angles.zipWithNext().map { (a, b) -> b - a } + (angles.first() + 360f - angles.last())).forEach {
                it shouldBe (step plusOrMinus ANGLE_TOLERANCE)
            }
            val atPlayer = fight.rig.playerBody.getCenter(Vector2()).sub(fight.eyeCenter()).angleDeg()
            withClue("a ball at $atPlayer deg among $angles") {
                angles.count { abs(it - atPlayer) < ANGLE_TOLERANCE } shouldBe 1
            }
        }

        it("Stall: 3 green balls at STALL_ANGLES toward the player, stop, wait, then dart at the player of that moment") {
            fight = ElecDevilRig()
            fight.startFight()
            fight.untilEyePattern(STALL)
            fight.boss.getEyePattern() shouldBe EyePattern.STALL
            fight.placePlayer(12f, 2f)
            val before = fight.balls().size
            val volts = fight.sounds(SoundAsset.VOLT_SOUND)
            fight.untilStateChange() shouldBe ElecDevilState.STAND
            untilBalls(before, ElecDevil.STALL_ANGLES.size)
            fight.rig.tick()
            val balls = newBalls(before)
            balls.size shouldBe ElecDevil.STALL_ANGLES.size
            fight.sounds(SoundAsset.VOLT_SOUND) shouldBe volts + ElecDevil.STALL_ANGLES.size
            val eye = fight.eyeCenter()
            withClue("the player is right of the eye") { fight.rig.playerBody.getCenter(Vector2()).x shouldBeGreaterThan eye.x }
            balls.forEachIndexed { index, ball ->
                val angle = ElecDevil.STALL_ANGLES[index]
                shouldPointAlong(
                    directionOf(ball), Vector2(MathUtils.cosDeg(angle), MathUtils.sinDeg(angle)), "ball $index"
                )
                speedOf(ball) shouldBe (ElecDevil.STALL_SPEED * T plusOrMinus SPEED_TOLERANCE * T)
                fight.animationKeyOf(ball) shouldBe "orb_green"
            }
            (fight.rig.spawnTimes[balls[1]] - fight.rig.spawnTimes[balls[0]]) shouldBe
                (ElecDevil.EYE_SPREAD_SHOT_GAP plusOrMinus fight.rig.step)

            val first = balls.first()
            val spawned = fight.rig.spawnTimes[first]
            fun untilBallAge(age: Float) = fight.rig.ticksUntil(2f) { fight.rig.time - spawned >= age } shouldBe true
            untilBallAge(ElecDevil.STALL_MOVE_DUR + ElecDevil.STALL_PAUSE_DUR / 4f)
            speedOf(first) shouldBe 0f
            fight.placePlayer(6f, 3f)
            untilBallAge(ElecDevil.STALL_MOVE_DUR + ElecDevil.STALL_PAUSE_DUR * 3f / 4f)
            speedOf(first) shouldBe 0f
            untilBallAge(ElecDevil.STALL_MOVE_DUR + ElecDevil.STALL_PAUSE_DUR + 2f * fight.rig.step)
            val toPlayer = fight.rig.playerBody.getCenter(Vector2()).sub(fight.center(first)).nor()
            shouldPointAlong(directionOf(first), toPlayer, "the dart")
            speedOf(first) shouldBe (ElecDevil.STALL_DART_SPEED * T plusOrMinus SPEED_TOLERANCE * T)
        }

        it("Bounce: 2 yellow balls at the floor mirror of the current and the lead point, one bounce, then a hit") {
            fight = ElecDevilRig()
            fight.startFight()
            fight.untilState(ElecDevilState.APPEAR)
            fight.untilState(ElecDevilState.STAND)
            fight.toPhase2()
            fight.random.ints.addLast(BOUNCE)
            fight.untilState(ElecDevilState.CHARGE)
            fight.boss.getEyePattern() shouldBe EyePattern.BOUNCE
            val velocity = Vector2(3f * T, 0f)
            fight.placePlayer(10f, 2f)
            fight.rig.playerBody.physics.velocity.set(velocity)
            val playerCenter = fight.rig.playerBody.getCenter(Vector2())
            val before = fight.balls().size
            fight.untilStateChange() shouldBe ElecDevilState.STAND
            untilBalls(before, 2)
            fight.rig.tick()
            val balls = newBalls(before)
            balls.size shouldBe 2
            (fight.rig.spawnTimes[balls[1]] - fight.rig.spawnTimes[balls[0]]) shouldBe
                (ElecDevil.BOUNCE_SHOT_GAP plusOrMinus fight.rig.step)
            val mirrorY = fight.bodyOf(fight.activeBody()).getY() + ElecBall.BODY_SIZE * T / 2f
            val eye = fight.eyeCenter()
            listOf(OrbAim.CURRENT, OrbAim.LEAD).forEachIndexed { index, aim ->
                val point = OrbAim.point(
                    aim, playerCenter, velocity, ElecDevilTuning.NORMAL_PHASE_2.eyeSpreadLeadTime, fight.room,
                    { _, _ -> 0f }, Vector2()
                )
                point.y = 2f * mirrorY - point.y
                shouldPointAlong(directionOf(balls[index]), point.sub(eye).nor(), "$aim")
                speedOf(balls[index]) shouldBe (ElecDevil.BOUNCE_SPEED * T plusOrMinus SPEED_TOLERANCE * T)
                fight.animationKeyOf(balls[index]) shouldBe "orb_yellow"
            }

            val ball = balls.first()
            val ballBody = fight.bodyOf(ball)
            val floor = GameRectangle(0f, 0f, ROOM_WIDTH * T, T)
            val floorFixture = Fixture(Body(BodyType.STATIC), FixtureType.BLOCK, floor)
            val hits = { ball.getComponent(AudioComponent::class)!!.playSoundRequests.count {
                it.source == SoundAsset.ASTEROID_EXPLODE_SOUND
            } }
            val incoming = Vector2(ballBody.physics.velocity)
            incoming.y shouldBeLessThan 0f
            ball.hitBlock(floorFixture, ballBody.getBounds(GameRectangle()), floor)
            fight.rig.tick()
            withClue("the first block contact bounces") {
                ballBody.physics.velocity.x shouldBe (incoming.x plusOrMinus SPEED_TOLERANCE)
                ballBody.physics.velocity.y shouldBe (-incoming.y plusOrMinus SPEED_TOLERANCE)
                hits() shouldBe 0
            }
            fight.rig.ticks(0.3f)
            ball.hitBlock(floorFixture, ballBody.getBounds(GameRectangle()), floor)
            fight.rig.tick()
            withClue("the second block contact hits") {
                ballBody.physics.velocity.len() shouldBe 0f
                hits() shouldBe 1
            }
        }
    }

    describe("the body animation") {
        it("plays charge_<color> in CHARGE, shoot_<color> while the shots are due, then the state animation") {
            fight = ElecDevilRig()
            fight.startFight()
            fight.untilEyePattern(FAN)
            val chargeKeys = mutableSetOf<String?>()
            while (fight.state == ElecDevilState.CHARGE) {
                chargeKeys.add(fight.bodyAnimationKey())
                fight.rig.tick()
            }
            chargeKeys shouldBe setOf("charge_blue")
            fight.state shouldBe ElecDevilState.STAND
            val standKeys = mutableListOf<String?>()
            while (fight.state == ElecDevilState.STAND) {
                standKeys.add(fight.bodyAnimationKey())
                fight.rig.tick()
            }
            val shootTicks = standKeys.takeWhile { it == "shoot_blue" }.size
            val shootDur = max(
                ElecDevilTuning.NORMAL.eyeSpreadAims.size * ElecDevil.EYE_SPREAD_SHOT_GAP, ElecDevil.EYE_SHOOT_MIN_DUR
            )
            withClue("keys in STAND after CHARGE: $standKeys") {
                (shootTicks * fight.rig.step) shouldBe (shootDur plusOrMinus durationTolerance())
                standKeys.drop(shootTicks).toSet() shouldBe setOf("stand")
            }
            fight.state shouldBe ElecDevilState.HAND
            val otherKeys = mutableSetOf<String?>()
            fight.rig.ticksUntil(10f, each = { otherKeys.add(fight.bodyAnimationKey()) }) {
                fight.state == ElecDevilState.TURN_TO_PIECES
            } shouldBe true
            otherKeys.filter { it != null && it.startsWith("shoot") } shouldBe emptyList()
        }

        it("plays the state animation in the first STAND, before any CHARGE") {
            fight = ElecDevilRig()
            fight.startFight()
            fight.untilState(ElecDevilState.APPEAR)
            fight.untilState(ElecDevilState.STAND)
            val keys = mutableSetOf<String?>()
            while (fight.state == ElecDevilState.STAND) {
                keys.add(fight.bodyAnimationKey())
                fight.rig.tick()
            }
            keys shouldBe setOf("stand")
        }

        it("plays shoot_red for EYE_SHOOT_MIN_DUR after a Ring, whose shots leave at once") {
            fight = ElecDevilRig()
            fight.startFight()
            fight.untilEyePattern(RING)
            fight.bodyAnimationKey() shouldBe "charge_red"
            fight.untilStateChange() shouldBe ElecDevilState.STAND
            val standKeys = mutableListOf<String?>()
            while (fight.state == ElecDevilState.STAND) {
                standKeys.add(fight.bodyAnimationKey())
                fight.rig.tick()
            }
            val shootTicks = standKeys.takeWhile { it == "shoot_red" }.size
            (shootTicks * fight.rig.step) shouldBe (ElecDevil.EYE_SHOOT_MIN_DUR plusOrMinus durationTolerance())
        }
    }

    describe("Live Wire") {
        it("arcs a launched piece to the piece before it, with no chain of 3, during the first launch") {
            fight = ElecDevilRig()
            fight.startFight()
            var arcsSeen = 0
            fight.rig.ticksUntil(30f, each = {
                fight.rig.spawned.filterIsInstance<ElecDevilBodyPiece>()
                    .filter { !fight.rig.destroyed.contains(it, true) }
                    .forEach { piece ->
                        val linked = piece.linkedPiece ?: return@forEach
                        linked.linkedPiece shouldBe null
                        if (fight.rig.destroyed.contains(linked, true)) return@forEach
                        val arc = piece.getComponent(BodyComponent::class)!!.body.fixtures[FixtureType.DAMAGER]
                            .first { it.getShape() is GameLine }
                        if (arc.isActive()) arcsSeen++
                    }
            }) { fight.state == ElecDevilState.APPEAR } shouldBe true
            val pieces = fight.rig.spawned.filterIsInstance<ElecDevilBodyPiece>()
            pieces.size shouldBe PIECES
            val linked = pieces.indices.filter { pieces[it].linkedPiece != null }
            linked.size shouldBeLessThanOrEqual ElecDevilTuning.NORMAL.liveWireLinks
            linked.size shouldBeGreaterThan 0
            linked.forEach { index -> pieces[index].linkedPiece shouldBe pieces[index - 1] }
            arcsSeen shouldBeGreaterThan 0
        }
    }
})
