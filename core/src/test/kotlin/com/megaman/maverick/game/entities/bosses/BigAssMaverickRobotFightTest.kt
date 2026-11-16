package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.audio.AudioComponent
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Position
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.difficulty.DifficultyMode
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.bosses.bigassmaverickrobot.BigAssMaverickRobotTuning
import com.megaman.maverick.game.entities.bosses.bigassmaverickrobot.HandSide
import com.megaman.maverick.game.entities.bosses.bigassmaverickrobot.HandState
import com.megaman.maverick.game.entities.explosions.BigAssMaverickRobotOrbExplosion
import com.megaman.maverick.game.entities.explosions.Explosion
import com.megaman.maverick.game.entities.feetFixture
import com.megaman.maverick.game.entities.megamanx.MegamanX
import com.megaman.maverick.game.entities.projectiles.BigAssMaverickRobotOrb
import com.megaman.maverick.game.entities.projectiles.BigAssMaverickRobotOrb.OrbColor
import com.megaman.maverick.game.entities.projectiles.InvalidOrbSupplierException
import com.megaman.maverick.game.entities.state.BossPhase
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.players.PlayerDamageNegotiator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getPositionPoint
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.floats.shouldBeGreaterThanOrEqual
import io.kotest.matchers.floats.shouldBeLessThanOrEqual
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

private const val T = BOSS_RIG_PPM
private const val HEAD = "Head"

private fun rect(x: Float, y: Float) = RectangleMapObject(x * T, y * T, T, T)

private fun box(x: Float, y: Float, width: Float, height: Float) =
    RectangleMapObject(x * T, y * T, width * T, height * T)

class BigAssMaverickRobotFightTest : DescribeSpec({

    lateinit var rig: BossRig
    lateinit var boss: BigAssMaverickRobot

    fun hands() = rig.spawned.filterIsInstance<BigAssMaverickRobotHand>()
    fun hand(side: HandSide) = hands().first { it.side == side }
    fun blockOf(hand: BigAssMaverickRobotHand): Block =
        rig.spawned.filterIsInstance<Block>()[hands().indexOf(hand)]

    fun standOn(player: MegamanX, hand: BigAssMaverickRobotHand) {
        val top = blockOf(hand).getComponent(BodyComponent::class)!!.body.getPositionPoint(Position.TOP_CENTER)
        val feet = player.feetFixture.getShape().getCenter(Vector2())
        val body = player.getComponent(BodyComponent::class)!!.body
        body.translate(top.x - feet.x, top.y - 0.05f * T - feet.y)
        body.physics.velocity.setZero()
    }

    fun damager(hand: BigAssMaverickRobotHand): IFixture =
        hand.getComponent(BodyComponent::class)!!.body.fixtures[FixtureType.DAMAGER].first()

    fun soundsOf(entity: IGameEntity, sound: SoundAsset) =
        entity.getComponent(AudioComponent::class)!!.playSoundRequests.count { it.source == sound }

    fun place(x: Float, y: Float) = rig.playerBody.setBottomCenterToPoint(Vector2(x * T, y * T))

    fun hit(damage: Int) {
        check(damage % 3 == 0) { "a pellet does 3 on this boss: $damage is not a sum of pellets" }
        repeat(damage / 3) { boss.takeDamageFrom(rig.bullet()) }
        rig.checkPoints(boss)
        rig.runEvents()
    }

    beforeEach {
        rig = BossRig()
        place(322f, 55f)
        boss = BigAssMaverickRobot(rig.services)
        boss.spawn(
            props(
                ConstKeys.BOUNDS pairTo GameRectangle(321f * T, 67f * T, 8f * T, 12f * T),
                ConstKeys.TARGET pairTo rect(324.5f, 53f),
                ConstKeys.GROUND pairTo box(310f, 50f, 30f, 5f),
                ConstKeys.CEILING pairTo box(318f, 66.75f, 14f, 1f),
                BigAssMaverickRobot.LEFT_WALL pairTo box(316f, 55f, 2f, 12f),
                BigAssMaverickRobot.RIGHT_WALL pairTo box(332f, 55f, 2f, 12f),
                ConstKeys.LEFT pairTo rect(321.5f, 59f),
                ConstKeys.RIGHT pairTo rect(327.5f, 59f),
                "${ConstKeys.LEFT}_${ConstKeys.ARM}_${ConstKeys.ORIGIN}" pairTo rect(321.5f, 61f),
                "${ConstKeys.RIGHT}_${ConstKeys.ARM}_${ConstKeys.ORIGIN}" pairTo rect(327.5f, 61f)
            )
        ) shouldBe true
        withClue("entrance") {
            rig.ticksUntil(6f) { boss.bossComponent.phase == BossPhase.INTRO } shouldBe true
        }
        hands().forEach { it.state shouldBe HandState.IDLE }
    }

    afterEach { rig.dispose() }

    fun runAttack(hand: BigAssMaverickRobotHand, each: () -> Unit = {}, order: () -> Unit): Set<HandState> {
        val seen = mutableSetOf<HandState>()
        val block = blockOf(hand)
        order()
        var time = 0f
        while (time < 15f) {
            rig.tick()
            time += rig.step
            seen.add(hand.state)
            each()
            withClue("${hand.state} damager") { damager(hand).isActive() shouldBe hand.state.damager }
            withClue("${hand.state} platform") {
                block.getComponent(BodyComponent::class)!!.body.physics.collisionOn shouldBe hand.state.platform
            }
            if (hand.state == HandState.IDLE) break
        }
        withClue("states seen: $seen") { hand.state shouldBe HandState.IDLE }
        return seen
    }

    describe("the arena") {
        it("spans from the left wall to the right wall and from the ground to the ceiling") {
            boss.arena.getX() shouldBe (318f * T plusOrMinus 0.01f)
            boss.arena.getMaxX() shouldBe (332f * T plusOrMinus 0.01f)
            boss.arena.getY() shouldBe (55f * T plusOrMinus 0.01f)
            boss.arena.getMaxY() shouldBe (66.75f * T plusOrMinus 0.01f)
        }
    }

    describe("the entrance") {
        it("ends with both hands IDLE on the first ORBIT slot") {
            val slot = Vector2()
            hands().forEach {
                val center = it.getComponent(BodyComponent::class)!!.body.getCenter(Vector2())
                it.slotTarget(slot)
                center.dst(slot) shouldBe (0f plusOrMinus 0.25f * T)
            }
            hand(HandSide.LEFT).getComponent(BodyComponent::class)!!.body.getCenter(Vector2()).x shouldBe (320f * T plusOrMinus 0.25f * T)
        }
    }

    describe("the hand attacks") {
        it("a punch goes WIND_UP, LAUNCH, RECOIL, RETURN and back to IDLE") {
            val left = hand(HandSide.LEFT)
            val seen = runAttack(left) { left.punch(Vector2(322f * T, 56f * T), BigAssMaverickRobotTuning.NORMAL) }
            seen shouldContainAll setOf(HandState.WIND_UP, HandState.LAUNCH, HandState.RECOIL, HandState.RETURN)
        }

        fun pillarBounds(pillar: BigAssMaverickRobotOrbExplosion) =
            pillar.getComponent(BodyComponent::class)!!.body.getBounds(GameRectangle())

        fun checkPillars(): List<BigAssMaverickRobotOrbExplosion> {
            val pillars = rig.spawned.filterIsInstance<BigAssMaverickRobotOrbExplosion>()
            pillars.forEach { pillar ->
                val bounds = pillarBounds(pillar)
                val damager = pillar.getComponent(BodyComponent::class)!!.body.fixtures[FixtureType.DAMAGER].first()
                    .getShape().getBoundingRectangle(GameRectangle())
                withClue("pillar ${pillar.direction} at $bounds") {
                    when (pillar.direction) {
                        Direction.UP -> {
                            bounds.getY() shouldBe (55f * T plusOrMinus 0.01f)
                            (damager.getHeight() > damager.getWidth()) shouldBe true
                        }

                        Direction.RIGHT -> {
                            bounds.getX() shouldBe (318f * T plusOrMinus 0.01f)
                            (damager.getWidth() > damager.getHeight()) shouldBe true
                        }

                        Direction.LEFT -> {
                            bounds.getMaxX() shouldBe (332f * T plusOrMinus 0.01f)
                            (damager.getWidth() > damager.getHeight()) shouldBe true
                        }

                        Direction.DOWN -> throw AssertionError("no pillar points down")
                    }
                    bounds.getMaxY() shouldBeLessThanOrEqual 66.75f * T
                }
            }
            return pillars
        }

        fun checkRest(hand: BigAssMaverickRobotHand, rested: () -> Unit) {
            if (hand.state != HandState.REST) return
            standOn(rig.player, hand)
            withClue("a rider on the resting fist") { hand.isBeingStoodUpon() shouldBe true }
            val top = hand.getComponent(BodyComponent::class)!!.body.getPositionPoint(Position.TOP_CENTER)
            val blockTop = blockOf(hand).getComponent(BodyComponent::class)!!.body.getPositionPoint(Position.TOP_CENTER)
            withClue("the step follows the fist") {
                blockTop.x shouldBe (top.x plusOrMinus 0.1f * T)
                blockTop.y shouldBe (top.y - 0.5f * T plusOrMinus 0.1f * T)
            }
            rested()
        }

        it("a slam at a player on the floor aligns, locks with a flash, dives to the floor, and sends waves both ways around the room") {
            val right = hand(HandSide.RIGHT)
            var rest: GameRectangle? = null
            val seen = runAttack(right, each = {
                checkRest(right) { rest = right.getComponent(BodyComponent::class)!!.body.getBounds(GameRectangle()) }
            }) { right.slam(Vector2(327f * T, 55.75f * T), BigAssMaverickRobotTuning.NORMAL_PHASE_2) }
            seen shouldContainAll setOf(HandState.RISE, HandState.HANG, HandState.FALL, HandState.REST, HandState.RETURN)
            withClue("the fist rested with its feet on the floor") {
                rest!!.getY() shouldBe (54.5f * T plusOrMinus 0.01f)
            }
            withClue("the lock flash") {
                rig.spawned.filterIsInstance<BigAssMaverickRobotOrb>().count { it.owner == boss && it.hit } shouldBe 1
            }
            rig.ticks(3f)
            val pillars = checkPillars()
            withClue("2 waves of 8 pillars: one each 0.08 s over the 0.6 s wave life of normal phase 2") {
                pillars.size shouldBe 16
                withClue("8 to the left on the floor; 5 to the right on the floor up to the corner, 3 up the wall") {
                    pillars.count { it.direction == Direction.UP } shouldBe 13
                    pillars.count { it.direction == Direction.LEFT } shouldBe 3
                }
                pillars.count { it.direction == Direction.RIGHT } shouldBe 0
            }
            withClue("one hit sound per pillar and no electric pulse") {
                soundsOf(right, SoundAsset.SHOCK_WAVE_HIT_SOUND) shouldBe pillars.size
                soundsOf(right, SoundAsset.MM3_ELECTRIC_PULSE_SOUND) shouldBe 0
            }
            rig.runEvents()
            rig.eventsOf(EventType.SHAKE_CAM) shouldBeGreaterThan 0
        }

        it("a slam at a player in the air beside the fist dives to the wall, rests there as a step, and sends waves up and down the wall") {
            val right = hand(HandSide.RIGHT)
            var rest: GameRectangle? = null
            val seen = runAttack(right, each = {
                checkRest(right) { rest = right.getComponent(BodyComponent::class)!!.body.getBounds(GameRectangle()) }
            }) { right.slam(Vector2(323f * T, 59.5f * T), BigAssMaverickRobotTuning.NORMAL) }
            seen shouldContainAll setOf(HandState.RISE, HandState.HANG, HandState.FALL, HandState.REST, HandState.RETURN)
            withClue("the fist rested with its face on the left wall") {
                rest!!.getX() shouldBe (318f * T plusOrMinus 0.01f)
                rest!!.getCenter(Vector2()).y shouldBe (59.5f * T plusOrMinus 0.01f)
            }
            rig.ticks(3f)
            val pillars = checkPillars()
            val wall = pillars.filter { it.direction == Direction.RIGHT }
            withClue("one wave up the wall from the hit point to the wall top, the path end") {
                wall.count { pillarBounds(it).getCenter(Vector2()).y > 59.5f * T } shouldBe 7
            }
            withClue("one wave down the wall and along the floor, stopped by the 0.5 s wave life of normal phase 1") {
                wall.count { pillarBounds(it).getCenter(Vector2()).y < 59.5f * T } shouldBe 4
                pillars.count { it.direction == Direction.UP } shouldBe 3
                pillars.count { it.direction == Direction.LEFT } shouldBe 0
            }
            withClue("one hit sound per pillar and no electric pulse") {
                soundsOf(right, SoundAsset.SHOCK_WAVE_HIT_SOUND) shouldBe pillars.size
                soundsOf(right, SoundAsset.MM3_ELECTRIC_PULSE_SOUND) shouldBe 0
            }
        }
    }

    describe("the pincer clap") {
        fun center(hand: BigAssMaverickRobotHand) = hand.getComponent(BodyComponent::class)!!.body.getCenter(Vector2())

        fun startClap(seed: Long) {
            boss.bossComponent.phase = BossPhase.FIGHT
            hit(30)
            boss.healthPhase shouldBe 2
            rig.randomSource.setSeed(seed)
            withClue("a pincer clap starts") {
                rig.ticksUntil(90f) { hand(HandSide.LEFT).state == HandState.CLAP_ALIGN } shouldBe true
            }
            hand(HandSide.RIGHT).state shouldBe HandState.CLAP_ALIGN
        }

        it("closes twice on the player center, aims the second clap again at the moved player, and returns both hands to IDLE") {
            startClap(3L)
            val left = hand(HandSide.LEFT)
            val right = hand(HandSide.RIGHT)
            val firstY = rig.playerBody.getCenter(Vector2()).y
            rig.runEvents()
            val shakes = rig.eventsOf(EventType.SHAKE_CAM)
            val spawnedBefore = rig.spawned.size
            val impacts = mutableListOf<Pair<Vector2, Vector2>>()
            val seen = mutableSetOf<HandState>()
            var secondY = 0f
            var wasImpact = false
            var lastLeftX = center(left).x
            var lastRightX = center(right).x
            rig.ticksUntil(15f, each = {
                hands().forEach { hand ->
                    seen.add(hand.state)
                    withClue("${hand.side} ${hand.state} damager") { damager(hand).isActive() shouldBe hand.state.damager }
                }
                if (left.state.name.startsWith("CLAP") || right.state.name.startsWith("CLAP"))
                    withClue("both fists lock, close and hit together") { left.state shouldBe right.state }
                if (left.state == HandState.CLAP_CLOSE) withClue("no fist moves away from the meet point") {
                    center(left).x shouldBeGreaterThanOrEqual lastLeftX - 0.001f
                    center(right).x shouldBeLessThanOrEqual lastRightX + 0.001f
                }
                lastLeftX = center(left).x
                lastRightX = center(right).x
                val impact = left.state == HandState.CLAP_IMPACT && right.state == HandState.CLAP_IMPACT
                if (impact && !wasImpact) {
                    impacts.add(center(left) to center(right))
                    if (impacts.size == 1) {
                        place(328f, 56f)
                        secondY = rig.playerBody.getCenter(Vector2()).y
                    }
                }
                wasImpact = impact
            }) { left.state == HandState.IDLE && right.state == HandState.IDLE } shouldBe true
            seen shouldContainAll setOf(
                HandState.CLAP_ALIGN, HandState.CLAP_LOCK, HandState.CLAP_CLOSE, HandState.CLAP_IMPACT, HandState.RETURN
            )
            withClue("impacts: $impacts") {
                impacts.size shouldBe 2
                val (firstLeft, firstRight) = impacts[0]
                firstLeft.x shouldBe (321f * T plusOrMinus 0.1f * T)
                firstRight.x shouldBe (323f * T plusOrMinus 0.1f * T)
                firstLeft.y shouldBe (firstY plusOrMinus 0.1f * T)
                firstRight.y shouldBe (firstY plusOrMinus 0.1f * T)
                val (secondLeft, secondRight) = impacts[1]
                secondLeft.x shouldBe (327f * T plusOrMinus 0.1f * T)
                secondRight.x shouldBe (329f * T plusOrMinus 0.1f * T)
                secondLeft.y shouldBe (secondY plusOrMinus 0.1f * T)
                secondRight.y shouldBe (secondY plusOrMinus 0.1f * T)
            }
            withClue("2 lock flashes and 1 impact flash per clap, and no volley orb during the clap") {
                val flashes = (spawnedBefore until rig.spawned.size).map { rig.spawned[it] }
                    .filterIsInstance<BigAssMaverickRobotOrb>().filter { it.owner == boss }
                flashes.size shouldBe 6
                flashes.forEach { it.hit shouldBe true }
            }
            rig.runEvents()
            withClue("a camera shake at each impact") { rig.eventsOf(EventType.SHAKE_CAM) shouldBe shakes + 2 }
        }

        it("keeps both hands moving through a head stun in the clap") {
            startClap(3L)
            val left = hand(HandSide.LEFT)
            val right = hand(HandSide.RIGHT)
            rig.ticksUntil(5f) { left.state == HandState.CLAP_CLOSE } shouldBe true
            boss.takeDamageFrom(rig.chargedShot(full = true)) shouldBe true
            var met = false
            rig.ticks(0.48f, each = {
                withClue("the head is stunned") { boss.invincible shouldBe true }
                if (left.state == HandState.CLAP_IMPACT && right.state == HandState.CLAP_IMPACT) met = true
            })
            withClue("both hands reached CLAP_IMPACT during the stun") { met shouldBe true }
            rig.ticksUntil(10f) { left.state == HandState.IDLE && right.state == HandState.IDLE } shouldBe true
        }

        it("never claps while a player stands on a hand") {
            val left = hand(HandSide.LEFT)
            val right = hand(HandSide.RIGHT)
            boss.bossComponent.phase = BossPhase.FIGHT
            hit(30)
            boss.healthPhase shouldBe 2
            rig.randomSource.setSeed(3L)
            var rightAttacked = false
            rig.ticks(30f, each = {
                standOn(rig.player, left)
                withClue("left hand state") { left.state shouldBe HandState.IDLE }
                withClue("right hand state") { right.state.name.startsWith("CLAP") shouldBe false }
                if (right.state != HandState.IDLE) rightAttacked = true
            })
            rightAttacked shouldBe true
        }

        it("never claps in phase 1") {
            val left = hand(HandSide.LEFT)
            val right = hand(HandSide.RIGHT)
            boss.bossComponent.phase = BossPhase.FIGHT
            rig.randomSource.setSeed(3L)
            val seen = mutableSetOf<HandState>()
            rig.ticks(60f, each = {
                seen.add(left.state)
                seen.add(right.state)
            })
            boss.healthPhase shouldBe 1
            withClue("states seen: $seen") {
                seen shouldContainAll setOf(HandState.WIND_UP, HandState.RISE)
                seen.none { it.name.startsWith("CLAP") } shouldBe true
            }
        }
    }

    describe("the damage to the player") {
        it("is 1 bit from each damager of this fight, and 3 from an orb of another boss") {
            val negotiator = PlayerDamageNegotiator(rig.player)
            val right = hand(HandSide.RIGHT)
            right.slam(Vector2(327f * T, 55.75f * T), BigAssMaverickRobotTuning.NORMAL)
            rig.ticksUntil(10f) { rig.spawned.any { it is BigAssMaverickRobotOrbExplosion } } shouldBe true
            boss.bossComponent.phase = BossPhase.FIGHT
            rig.ticksUntil(10f) {
                rig.spawned.any { it is BigAssMaverickRobotOrb && it.owner == boss && !it.hit }
            } shouldBe true
            val orbs = rig.spawned.filterIsInstance<BigAssMaverickRobotOrb>()
            withClue("an orb of the head, a lock flash and an arm orb") {
                orbs.any { it.owner == boss && !it.hit } shouldBe true
                orbs.any { it.owner == boss && it.hit } shouldBe true
                orbs.any { it.owner is BigAssMaverickRobotHand } shouldBe true
            }
            val damagers = listOf<IDamagerEntity>(boss) + hands() + orbs +
                rig.spawned.filterIsInstance<BigAssMaverickRobotOrbExplosion>()
            damagers.forEach { withClue(it) { negotiator.get(it) shouldBe 1 } }

            listOf(GutsTank(rig.services), WilyFinalBoss(rig.services)).forEach { other ->
                val orb = MegaEntityFactory.fetch(BigAssMaverickRobotOrb::class)!!
                orb.spawn(props(ConstKeys.OWNER pairTo other, ConstKeys.POSITION pairTo Vector2()))
                rig.processSpawns()
                withClue("an orb of ${other::class.simpleName}") { negotiator.get(orb) shouldBe 3 }
            }
        }
    }

    describe("the aimed orb") {
        fun orb(vararg extra: Pair<String, Any>): BigAssMaverickRobotOrb {
            val orb = MegaEntityFactory.fetch(BigAssMaverickRobotOrb::class)!!
            val properties = props(
                ConstKeys.OWNER pairTo boss,
                ConstKeys.POSITION pairTo Vector2(325f * T, 60f * T),
                ConstKeys.DELAY pairTo 0.3f
            )
            extra.forEach { (key, value) -> properties.put(key, value) }
            orb.spawn(properties)
            rig.processSpawns()
            return orb
        }

        fun velocity(orb: BigAssMaverickRobotOrb) = orb.getComponent(BodyComponent::class)!!.body.physics.velocity

        it("takes its trajectory from the supplier when its hold ends, from its position at that time") {
            val calls = mutableListOf<Vector2>()
            val orb = orb(ConstKeys.SUPPLIER to { from: Vector2, out: Vector2 ->
                calls.add(Vector2(from))
                out.set(-3f * T, 2f * T)
            })
            rig.ticks(0.25f)
            withClue("no call and no motion during the hold") {
                calls.size shouldBe 0
                velocity(orb).isZero shouldBe true
            }
            rig.ticks(0.1f)
            calls.size shouldBe 1
            calls[0].x shouldBe (325f * T plusOrMinus 0.01f)
            calls[0].y shouldBe (60f * T plusOrMinus 0.01f)
            velocity(orb).x shouldBe (-3f * T plusOrMinus 0.01f)
            velocity(orb).y shouldBe (2f * T plusOrMinus 0.01f)
            rig.ticks(0.2f)
            calls.size shouldBe 1
        }

        it("keeps its spawn trajectory when it has no supplier, as for GutsTank and WilyFinalBoss") {
            val orb = orb(ConstKeys.TRAJECTORY to Vector2(4f * T, -1f * T))
            rig.ticks(0.25f)
            velocity(orb).isZero shouldBe true
            rig.ticks(0.1f)
            velocity(orb).x shouldBe (4f * T plusOrMinus 0.01f)
            velocity(orb).y shouldBe (-1f * T plusOrMinus 0.01f)
        }

        it("throws InvalidOrbSupplierException at spawn for a supplier that is not a (from, out) -> out function") {
            val e = shouldThrow<InvalidOrbSupplierException> { orb(ConstKeys.SUPPLIER to { Vector2() }) }
            e.cause.shouldBeInstanceOf<ClassCastException>()
            e.message!! shouldContain "(from: Vector2, out: Vector2) -> Vector2"
        }
    }

    describe("the orb volleys") {
        listOf(
            Triple(false, 1, 2),
            Triple(false, 2, 3),
            Triple(true, 1, 3),
            Triple(true, 2, 4)
        ).forEach { (hard, phase, expected) ->
            it("fire $expected orbs per volley in ${if (hard) "hard" else "normal"} phase $phase, one for each aim of the row") {
                if (hard) rig.services.gameState.setDifficultyMode(DifficultyMode.HARD)
                val second = rig.addPlayer(1)
                val left = hand(HandSide.LEFT)
                val right = hand(HandSide.RIGHT)
                fun holdHands() {
                    standOn(rig.player, left)
                    standOn(second, right)
                }
                boss.bossComponent.phase = BossPhase.FIGHT
                if (phase == 2) hit(30)
                boss.healthPhase shouldBe phase
                val row = boss.tuning
                row shouldBe BigAssMaverickRobotTuning.of(hard, phase)
                fun headOrbs() = rig.spawned.count { it is BigAssMaverickRobotOrb && it.owner == boss && !it.hit }
                rig.ticksUntil(15f, each = ::holdHands) { headOrbs() > 0 } shouldBe true
                rig.ticks(row.volleyDur + row.volleyDelay * 0.5f, each = {
                    holdHands()
                    withClue("no hand attack while both hands carry a player") {
                        left.state shouldBe HandState.IDLE
                        right.state shouldBe HandState.IDLE
                    }
                })
                withClue("orbs of the first volley") { headOrbs() shouldBe expected }
                withClue("each aim of the row fires once") { headOrbs() shouldBe row.orbAims.size }
            }
        }
    }

    describe("the conductor") {
        it("never attacks with a hand that a player stands on, and the partner braces during a wind-up") {
            val left = hand(HandSide.LEFT)
            val right = hand(HandSide.RIGHT)
            rig.randomSource.setSeed(7L)
            boss.bossComponent.phase = BossPhase.FIGHT
            var rightAttacked = false
            var braced = false
            rig.ticks(20f, each = {
                standOn(rig.player, left)
                withClue("left hand state") { left.state shouldBe HandState.IDLE }
                if (right.state != HandState.IDLE) rightAttacked = true
                if (right.state == HandState.WIND_UP && left.partnerOffset.y < 0f) braced = true
            })
            rightAttacked shouldBe true
            withClue("a punch by the right hand braces the left hand") { braced shouldBe true }
        }

        it("moves the hand target to the next player only when an attack is ordered") {
            val second = rig.addPlayer(1)
            val left = hand(HandSide.LEFT)
            val right = hand(HandSide.RIGHT)
            boss.bossComponent.phase = BossPhase.FIGHT
            rig.ticks(4.5f, each = {
                standOn(rig.player, left)
                standOn(second, right)
            })
            withClue("no free hand, so no attack") {
                left.state shouldBe HandState.IDLE
                right.state shouldBe HandState.IDLE
            }
            place(318.5f, 55f)
            rig.ticksUntil(3f, each = { standOn(second, right) }) { left.state != HandState.IDLE } shouldBe true
            right.state shouldBe HandState.IDLE
            when (left.state) {
                HandState.WIND_UP -> withClue("the punch goes to the first player, on the left") {
                    (left.attackDirection.x < 0f) shouldBe true
                }

                HandState.RISE -> {
                    rig.ticksUntil(3f, each = { standOn(second, right) }) { left.state == HandState.HANG } shouldBe true
                    withClue("the slam goes to the first player, on the left") {
                        (left.getComponent(BodyComponent::class)!!.body.getCenter(Vector2()).x < 323f * T) shouldBe true
                    }
                }

                else -> throw AssertionError("unexpected first attack state ${left.state}")
            }
        }

        it("blocks a second hit in the same frame as a health phase roll-over") {
            boss.bossComponent.phase = BossPhase.FIGHT
            hit(27)
            boss.getCurrentHealth() shouldBe 3
            rig.ticksUntil(2f) { !boss.invincible } shouldBe true
            val first = rig.bullet()
            val second = rig.bullet()
            boss.canBeDamagedBy(first) shouldBe true
            boss.takeDamageFrom(first) shouldBe true
            boss.healthPhase shouldBe 2
            boss.getCurrentHealth() shouldBe 30
            boss.canBeDamagedBy(second) shouldBe false
            if (boss.canBeDamagedBy(second)) boss.takeDamageFrom(second)
            boss.getCurrentHealth() shouldBe 30
        }

        it("shoots mixed-aim orbs that take their aim from the boss when they launch") {
            boss.bossComponent.phase = BossPhase.FIGHT
            place(318.5f, 55f)
            rig.ticks(6.5f)
            val orbs = rig.spawned.filterIsInstance<BigAssMaverickRobotOrb>().filter { it.owner == boss }
            orbs.size shouldBe 2
            orbs.count { !it.getComponent(BodyComponent::class)!!.body.physics.velocity.isZero } shouldBeGreaterThan 0
        }
    }

    describe("phase 2") {
        it("starts with a shake, orange arm orbs and the phase-2 tuning row") {
            boss.bossComponent.phase = BossPhase.FIGHT
            rig.runEvents()
            val shakes = rig.eventsOf(EventType.SHAKE_CAM)
            boss.tuning shouldBe BigAssMaverickRobotTuning.NORMAL
            hit(30)
            boss.healthPhase shouldBe 2
            boss.tuning shouldBe BigAssMaverickRobotTuning.NORMAL_PHASE_2
            rig.eventsOf(EventType.SHAKE_CAM) shouldBe shakes + 1
            val armOrbs = rig.spawned.filterIsInstance<BigAssMaverickRobotOrb>().filter { it.owner is BigAssMaverickRobotHand }
            armOrbs.size shouldBe 8
            armOrbs.forEach { it.color shouldBe OrbColor.ORANGE }
        }
    }

    describe("the death") {
        it("pops the chains one by one, ends with a last blast, and destroys the boss after 4 s") {
            boss.bossComponent.phase = BossPhase.FIGHT
            hit(60)
            boss.defeated shouldBe true
            rig.eventsOf(EventType.BOSS_DEFEATED) shouldBe 1
            hands().forEach { it.onBossDefeated(boss) }
            val armOrbs = rig.spawned.filterIsInstance<BigAssMaverickRobotOrb>().filter { it.owner is BigAssMaverickRobotHand }
            armOrbs.forEach { it.onBossDefeated(boss) }
            armOrbs.count { it.hit } shouldBe 0
            rig.tick()
            armOrbs.count { it.hit } shouldBe 2
            rig.ticks(0.2f)
            armOrbs.count { it.hit } shouldBe 4
            rig.ticks(0.5f)
            armOrbs.count { it.hit } shouldBe 8
            hands().forEach { it.state shouldBe HandState.DEFEATED }

            rig.ticks(3.4f - 0.7f - rig.step)
            val before = rig.spawned.count { it is Explosion }
            rig.ticks(0.2f)
            withClue("the last blast of 5 at 3.5 s, with the defeat explosions that keep coming") {
                setOf(5, 7) shouldContain (rig.spawned.count { it is Explosion } - before)
                soundsOf(boss, SoundAsset.BASSY_BLAST_SOUND) shouldBe 1
            }
            rig.destroyed.contains(boss, true) shouldBe false
            rig.ticks(0.5f)
            rig.destroyed.contains(boss, true) shouldBe true
        }

        it("shows the defeated face, blinks the head each 0.05 s and keeps the explosions coming for the whole 4 s") {
            boss.bossComponent.phase = BossPhase.FIGHT
            hit(60)
            boss.defeated shouldBe true
            val sprites = boss.getComponent(SpritesComponent::class)!!
            val head = sprites.sprites[HEAD]
            val face = boss.getComponent(AnimationsComponent::class)!!.getAnimator(HEAD) as Animator
            val explosionsPerHalfSecond = mutableListOf<Int>()
            var explosions = rig.spawned.count { it is Explosion }
            val runs = mutableListOf<Int>()
            var run = 0
            var lastHidden: Boolean? = null
            var time = 0f
            var nextWindow = 0.5f
            while (time < BigAssMaverickRobot.DEFEAT_DUR - rig.step) {
                rig.tick()
                time += rig.step
                sprites.preProcess(rig.step)
                withClue("the face at $time s") { face.keySupplier(face.currentKey) shouldBe "defeated" }
                if (head.hidden == lastHidden) run++ else {
                    if (lastHidden != null) runs.add(run)
                    run = 1
                    lastHidden = head.hidden
                }
                if (time >= nextWindow) {
                    val now = rig.spawned.count { it is Explosion }
                    explosionsPerHalfSecond.add(now - explosions)
                    explosions = now
                    nextWindow += 0.5f
                }
            }
            val ticksPerBlink = BigAssMaverickRobot.DEFEAT_BLINK / rig.step
            withClue("runs of shown and hidden ticks: $runs") {
                runs.size shouldBeGreaterThan (BigAssMaverickRobot.DEFEAT_DUR / BigAssMaverickRobot.DEFEAT_BLINK).toInt() - 4
                runs.forEach { it.toFloat() shouldBe (ticksPerBlink plusOrMinus 1f) }
            }
            withClue("explosions in each 0.5 s: $explosionsPerHalfSecond") {
                explosionsPerHalfSecond.size shouldBe 7
                explosionsPerHalfSecond.forEach { it shouldBeGreaterThan 0 }
            }
        }

        it("drops a fist that dives to a wall when the boss dies straight down, inside the arena") {
            val right = hand(HandSide.RIGHT)
            right.slam(Vector2(323f * T, 59.5f * T), BigAssMaverickRobotTuning.NORMAL)
            rig.ticksUntil(5f) { right.state == HandState.FALL } shouldBe true
            rig.ticks(0.05f)
            right.state shouldBe HandState.FALL
            val body = right.getComponent(BodyComponent::class)!!.body
            val diveX = body.getCenter(Vector2()).x
            boss.bossComponent.phase = BossPhase.FIGHT
            hit(60)
            boss.defeated shouldBe true
            hands().forEach { it.onBossDefeated(boss) }
            right.state shouldBe HandState.DEFEATED
            rig.ticks(1.5f)
            val bounds = body.getBounds(GameRectangle())
            withClue("the fist at $bounds") {
                bounds.getCenter(Vector2()).x shouldBe (diveX plusOrMinus 0.01f * T)
                bounds.getX() shouldBeGreaterThanOrEqual boss.arena.getX()
                bounds.getMaxX() shouldBeLessThanOrEqual boss.arena.getMaxX()
            }
        }
    }
})
