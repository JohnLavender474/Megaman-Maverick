package com.megaman.maverick.game.entities.bosses.elecdevil

import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Queue
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.audio.AudioComponent
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.random.GameRandom
import com.maverick.game.engine.common.random.SeededRandom
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.world.body.BodyComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.entities.bosses.BOSS_RIG_PPM
import com.megaman.maverick.game.entities.bosses.BossRig
import com.megaman.maverick.game.entities.bosses.ElecDevil
import com.megaman.maverick.game.entities.bosses.ElecDevilBody
import com.megaman.maverick.game.entities.bosses.ElecDevilClaw
import com.megaman.maverick.game.entities.bosses.ElecDevilState
import com.megaman.maverick.game.entities.projectiles.ElecBall
import com.megaman.maverick.game.entities.state.BossPhase
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.setBodySense
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import java.util.Random

const val T = BOSS_RIG_PPM
const val ROOM_WIDTH = 16f
const val ROOM_HEIGHT = 14f

class RangeScriptedRandom : GameRandom {

    val rolls = Queue<Float>()
    val ints = Queue<Int>()
    val scriptedIntRanges = Array<IntRange>()
    private val delegate = SeededRandom(Random(0L))

    override fun nextFloat(min: Float, max: Float) =
        if (rolls.isEmpty) delegate.nextFloat(min, max) else min + (max - min) * rolls.removeFirst()

    override fun nextInt(min: Int, maxInclusive: Int): Int {
        if (ints.isEmpty) return delegate.nextInt(min, maxInclusive)
        val value = ints.removeFirst()
        if (value !in min..maxInclusive) throw IllegalStateException("scripted $value is not in $min..$maxInclusive")
        scriptedIntRanges.add(min..maxInclusive)
        return value
    }

    override fun nextBoolean() = delegate.nextBoolean()

    override fun <T> pick(array: Array<T>) = delegate.pick(array)

    override fun <T> pick(list: List<T>) = delegate.pick(list)

    override fun <T> pickOf(vararg values: T) = delegate.pick(values.toList())

    override fun <T> shuffle(array: Array<T>) = delegate.shuffle(array)
}

class ElecDevilRig {

    val random = RangeScriptedRandom()
    val rig = BossRig(random)
    val room = GameRectangle(0f, 0f, ROOM_WIDTH * T, ROOM_HEIGHT * T)
    lateinit var boss: ElecDevil

    val state: ElecDevilState
        get() = boss.getCurrentState()

    fun dispose() = rig.dispose()

    fun spawnBoss() {
        rig.playerBody.setBodySense(BodySense.FEET_ON_GROUND, true)
        placePlayer(8f, 2f)
        boss = ElecDevil(rig.services)
        boss.spawn(
            props(
                ConstKeys.ROOM pairTo RectangleMapObject(room.getX(), room.getY(), room.getWidth(), room.getHeight()),
                ConstKeys.LEFT pairTo RectangleMapObject(2f * T, T, T, T),
                ConstKeys.RIGHT pairTo RectangleMapObject(12f * T, T, T, T),
                "${ConstKeys.START}_${ConstKeys.X}" pairTo RectangleMapObject(18f * T, T, T, T),
                "${ConstKeys.FROM}_${ConstKeys.LEFT}" pairTo false,
                "${ConstKeys.LIGHT}_${ConstKeys.SOURCE}_${ConstKeys.KEYS}" pairTo "1"
            )
        ) shouldBe true
        rig.processSpawns()
    }

    fun startFight() {
        spawnBoss()
        withClue("the entrance ends") {
            rig.ticksUntil(3f) { boss.bossComponent.phase == BossPhase.INTRO } shouldBe true
        }
        boss.bossComponent.phase = BossPhase.FIGHT
    }

    fun placePlayer(tilesX: Float, tilesY: Float) = rig.playerBody.setCenter(tilesX * T, tilesY * T)

    fun untilState(target: ElecDevilState, maxSeconds: Float = 30f) = withClue("reaches $target") {
        rig.ticksUntil(maxSeconds) { state == target } shouldBe true
    }

    fun untilStateChange(maxSeconds: Float = 30f): ElecDevilState {
        val from = state
        withClue("leaves $from") { rig.ticksUntil(maxSeconds) { state != from } shouldBe true }
        return state
    }

    fun secondsIn(target: ElecDevilState): Float {
        untilState(target)
        var seconds = 0f
        while (state == target && seconds < 10f) {
            rig.tick()
            seconds += rig.step
        }
        return seconds
    }

    fun toPhase2() {
        repeat(15) { boss.takeDamageFrom(rig.bullet()) }
        boss.healthPhase shouldBe 2
    }

    fun sounds(sound: SoundAsset) =
        boss.getComponent(AudioComponent::class)!!.playSoundRequests.count { it.source == sound }

    fun center(entity: IGameEntity): Vector2 =
        entity.getComponent(BodyComponent::class)!!.body.getCenter(Vector2())

    fun bodyOf(entity: IGameEntity) = entity.getComponent(BodyComponent::class)!!.body

    fun activeBody() = rig.spawned.filterIsInstance<ElecDevilBody>().single { it.on }

    fun bodyAnimationKey(): String? {
        val body = rig.spawned.filterIsInstance<ElecDevilBody>().firstOrNull { it.on } ?: return null
        val animator = body.getComponent(AnimationsComponent::class)!!.getAnimator(ElecDevilBody.TAG) as Animator
        return animator.keySupplier(animator.currentKey)
    }

    fun animationKeyOf(ball: ElecBall): String? {
        val animator = ball.getComponent(AnimationsComponent::class)!!.getAnimator(ElecBall.TAG) as Animator
        return animator.keySupplier(animator.currentKey)
    }

    fun eyeCenter(): Vector2 =
        bodyOf(boss).fixtures[FixtureType.DAMAGEABLE].first().getShape().getCenter(Vector2())

    fun handFixtures() = bodyOf(boss).fixtures[FixtureType.SHIELD].toList() +
        bodyOf(boss).fixtures[FixtureType.DAMAGER].toList()

    fun handCenter(): Vector2 = bodyOf(boss).fixtures[FixtureType.SHIELD].first().getShape().getCenter(Vector2())

    fun balls() = rig.spawned.filterIsInstance<ElecBall>()

    fun liveClaw() = rig.spawned.filterIsInstance<ElecDevilClaw>().firstOrNull { !rig.destroyed.contains(it, true) }

    fun untilEyePattern(index: Int) {
        untilState(ElecDevilState.APPEAR)
        untilState(ElecDevilState.STAND)
        random.ints.addLast(index)
        untilState(ElecDevilState.CHARGE)
        random.ints.isEmpty shouldBe true
    }
}
