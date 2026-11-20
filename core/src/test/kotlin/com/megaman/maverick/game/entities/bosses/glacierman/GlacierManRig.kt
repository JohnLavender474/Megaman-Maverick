package com.megaman.maverick.game.entities.bosses.glacierman

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Queue
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.animations.AnimationsSystem
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.audio.AudioComponent
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.extensions.objectSetOf
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.random.GameRandom
import com.maverick.game.engine.common.random.SeededRandom
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.entities.EntityExistenceState
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.events.Event
import com.maverick.game.engine.events.IEventListener
import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.points.PointsSystem
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.audio.MegaAudioManager
import com.megaman.maverick.game.difficulty.DifficultyMode
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.bosses.GlacierMan
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.megamanx.MegamanX
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.state.BossPhase
import com.megaman.maverick.game.entities.state.PlayPhase
import com.megaman.maverick.game.entities.state.PlayPhaseComponent
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.players.MegaPlayers
import com.megaman.maverick.game.testServices
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.setBodySense
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import java.util.Random
import kotlin.math.abs

const val T = ConstVals.PPM.toFloat()
const val STEP = 1f / 60f
const val FLOOR = 2f * T
const val LEFT_INNER = 2f * T
const val RIGHT_INNER = 26f * T
const val ARENA_HEIGHT = 14f * T
const val BOSS_START_X = 20f
const val PASS = 0f
const val FAIL = 0.999f

private const val CLEAR_EVERY = 30
private const val CONTACT = 0.01f

data class TuningRow(val name: String, val hard: Boolean, val phase2: Boolean) {
    val tuning: GlacierManTuning
        get() = when {
            hard && phase2 -> GlacierManTuning.HARD_PHASE_2
            hard -> GlacierManTuning.HARD
            phase2 -> GlacierManTuning.NORMAL_PHASE_2
            else -> GlacierManTuning.NORMAL
        }
}

class GlacierManRandom : GameRandom {

    enum class Kind { FLOAT, INT, SHUFFLE }

    class Call(val kind: Kind, val min: Float, val max: Float, val value: Float, val items: List<Any?>)

    val calls = Array<Call>()
    val rolls = Queue<Float>()
    var unitDefault: () -> Float = { FAIL }
    val picks = Queue<Float>()
    val fractions = Queue<Float>()
    val ints = Queue<Int>()
    var shuffler: ((Array<Any?>) -> Unit)? = null
    private val delegate = SeededRandom(Random(0L))

    override fun nextFloat(min: Float, max: Float): Float {
        val value = when {
            min == 0f && max == 1f -> if (rolls.isEmpty) unitDefault() else rolls.removeFirst()
            min == 0f -> if (picks.isEmpty) delegate.nextFloat(min, max) else max * picks.removeFirst()
            fractions.isEmpty -> delegate.nextFloat(min, max)
            else -> min + (max - min) * fractions.removeFirst()
        }
        calls.add(Call(Kind.FLOAT, min, max, value, emptyList()))
        return value
    }

    override fun nextInt(min: Int, maxInclusive: Int): Int {
        val value = if (ints.isEmpty) delegate.nextInt(min, maxInclusive) else ints.removeFirst()
        if (value !in min..maxInclusive) throw IllegalStateException("scripted $value is not in $min..$maxInclusive")
        calls.add(Call(Kind.INT, min.toFloat(), maxInclusive.toFloat(), value.toFloat(), emptyList()))
        return value
    }

    override fun nextBoolean() = delegate.nextBoolean()

    override fun <T> pick(array: Array<T>) = delegate.pick(array)

    override fun <T> pick(list: List<T>) = delegate.pick(list)

    override fun <T> pickOf(vararg values: T) = delegate.pick(values.toList())

    @Suppress("UNCHECKED_CAST")
    override fun <T> shuffle(array: Array<T>) {
        val custom = shuffler
        if (custom != null) custom(array as Array<Any?>) else delegate.shuffle(array)
        calls.add(Call(Kind.SHUFFLE, 0f, 0f, 0f, array.toList()))
    }

    fun floatCalls(min: Float, max: Float) = calls.filter { it.kind == Kind.FLOAT && it.min == min && it.max == max }

    fun pickCalls() = calls.filter { it.kind == Kind.FLOAT && it.min == 0f && it.max != 1f }
}

class SpawnRecord(
    val entity: IGameEntity,
    val props: Properties,
    val time: Float,
    val callIndex: Int,
    val bossKey: String?,
    val bossFacing: Facing?
) {
    val position: Vector2
        get() = props.get(ConstKeys.POSITION, Vector2::class)!!

    val trajectory: Vector2
        get() = props.get(ConstKeys.TRAJECTORY, Vector2::class)!!
}

internal class GlacierManRig(hard: Boolean = false) {

    val random = GlacierManRandom()
    val services: MegaServices
    val player: MegamanX
    val playerBody: Body
    val spawned = Array<IGameEntity>()
    val spawnLog = Array<SpawnRecord>()
    val destroyed = Array<IGameEntity>()
    val events = Array<Event>()
    val eventTimes = Array<Float>()
    val alsoTick = Array<IGameEntity>()
    val walls = Array<GameRectangle>()
    val audioSounds = Array<Any?>()
    lateinit var boss: GlacierMan

    var time = 0f
        private set

    private val queued = Array<Pair<IGameEntity, Properties>>()
    private val animations = AnimationsSystem()
    private val points = PointsSystem()
    private val engine = mockk<GameEngine>(relaxed = true)
    private val audio = mockk<MegaAudioManager>(relaxed = true)
    private val assets = mockk<AssetManager>(relaxed = true)
    private val atlas = mockk<TextureAtlas>()
    private val texture = mockk<Texture>(relaxed = true)
    private var ticks = 0

    private val recorder = object : IEventListener {
        override val eventKeyMask = objectSetOf<Any>(*EventType.entries.toTypedArray())

        override fun onEvent(event: Event) {
            events.add(event)
            eventTimes.add(time)
        }
    }

    init {
        every { engine.spawn(any(), any()) } answers {
            val entity = firstArg<IGameEntity>()
            val properties = secondArg<Properties>()
            if (!entity.initialized) {
                entity.init()
                entity.initialized = true
            }
            if (!entity.canSpawn(properties)) return@answers false
            entity.existenceState = EntityExistenceState.SPAWN_QUEUED
            queued.add(entity to properties)
            val bossNow = if (this@GlacierManRig::boss.isInitialized && entity !== boss) boss else null
            spawnLog.add(
                SpawnRecord(
                    entity, copyOf(properties), time, random.calls.size,
                    bossNow?.let { animKey }, bossNow?.facing
                )
            )
            true
        }
        every { engine.destroy(any()) } answers {
            val entity = firstArg<IGameEntity>()
            destroyed.add(entity)
            entity.existenceState = EntityExistenceState.DESTROY_QUEUED
            true
        }
        every { audio.playSound(any<Any>(), any<Boolean>()) } answers { audioSounds.add(firstArg<Any?>()) }
        services = testServices(engine = engine, assets = assets, audio = audio, players = MegaPlayers(2), random = random)
        if (hard) services.gameState.setDifficultyMode(DifficultyMode.HARD)
        every { texture.width } returns 1024
        every { texture.height } returns 1024
        every { atlas.findRegion(any()) } returns AtlasRegion(texture, 0, 0, 64, 48)
        every { services.assets.get(any<String>(), TextureAtlas::class.java) } returns atlas
        services.events.addListener(recorder)
        MegaEntityFactory.init(services)

        player = MegamanX(services)
        player.existenceState = EntityExistenceState.LIVE
        player.addComponent(player.defineBodyComponent())
        player.addComponent(PlayPhaseComponent(PlayPhase.IN_PLAY))
        player.addComponent(player.defineFreezeComponent())
        player.facing = Facing.RIGHT
        services.players.register(PlayerSlot(0), player)
        playerBody = player.getComponent(BodyComponent::class)!!.body
        place(6f)

        walls.add(GameRectangle(0f, FLOOR, LEFT_INNER, 20f * T))
        walls.add(GameRectangle(RIGHT_INNER, FLOOR, 2f * T, 20f * T))
    }

    fun dispose() {
        spawned.forEach { if (it is MegaGameEntity) MegaGameEntities.remove(it) }
        spawned.clear()
        spawnLog.clear()
        queued.clear()
        destroyed.clear()
        alsoTick.clear()
        MegaEntityFactory.reset()
        clearAllMocks(answers = false, recordedCalls = true, childMocks = false)
    }

    val bossBody: Body
        get() = boss.getComponent(BodyComponent::class)!!.body

    fun bossCenter(): Vector2 = bossBody.getCenter(Vector2())

    fun place(tilesX: Float, topOverFloor: Float = 1.25f) {
        playerBody.setBottomCenterToPoint(Vector2(tilesX * T, FLOOR + topOverFloor * T - playerBody.getHeight()))
    }

    fun placePlayerCenter(x: Float, y: Float) = playerBody.setCenter(x, y)

    fun placeBoss(tilesX: Float) = bossBody.setX(tilesX * T - bossBody.getWidth() / 2f)

    fun placeBossLeft(x: Float) = bossBody.setX(x)

    fun wallProps(): Properties {
        val out = Properties()
        for (i in 0 until walls.size) {
            val wall = walls[i]
            out.put("${ConstKeys.WALL}_${i + 1}", RectangleMapObject(wall.getX(), wall.getY(), wall.getWidth(), wall.getHeight()))
        }
        return out
    }

    fun spawnProps(bossX: Float = BOSS_START_X): Properties {
        val out = props(ConstKeys.BOUNDS pairTo GameRectangle(bossX * T - 0.625f * T, FLOOR, 1.25f * T, 1.75f * T))
        out.putAll(wallProps())
        return out
    }

    fun spawnBoss(props: Properties = spawnProps()) {
        boss = GlacierMan(services)
        boss.spawn(props) shouldBe true
        processSpawns()
    }

    fun startFight(phase2: Boolean = false, props: Properties = spawnProps()) {
        spawnBoss(props)
        if (phase2) toPhase2()
        enterFight(phase2)
    }

    fun enterFight(phase2: Boolean = false) {
        withClue("the entrance") {
            ticksUntil(5f) { boss.bossComponent.phase == BossPhase.INTRO } shouldBe true
        }
        boss.bossComponent.phase = BossPhase.FIGHT
        if (phase2) withClue("the phase-2 pose") { ticksUntil(3f) { animKey == "fist_flash" } shouldBe true }
        withClue("the first STAND") { ticksUntil(3f) { animKey == "stand" } shouldBe true }
    }

    fun toPhase2() {
        repeat(15) { boss.takeDamageFrom(bullet()) }
        boss.healthPhase shouldBe 2
    }

    fun bullet(): Bullet {
        val shot = MegaEntityFactory.fetch(Bullet::class)!!
        shot.spawn(
            props(
                ConstKeys.OWNER pairTo player,
                ConstKeys.POSITION pairTo Vector2(),
                ConstKeys.TRAJECTORY pairTo Vector2()
            )
        )
        processSpawns()
        return shot
    }

    fun addBlock(x: Float, y: Float, width: Float, height: Float): Block {
        val block = MegaEntityFactory.fetch(Block::class)!!
        block.spawn(props(ConstKeys.BOUNDS pairTo GameRectangle(x, y, width, height)))
        processSpawns()
        return block
    }

    fun checkPoints() {
        if (!points.contains(boss)) points.add(boss)
        points.update(0f)
    }

    fun processSpawns() {
        while (!queued.isEmpty) {
            val (entity, properties) = queued.removeIndex(0)
            spawned.add(entity)
            entity.onSpawn(properties)
            entity.existenceState = EntityExistenceState.LIVE
        }
    }

    fun processDestroys() {
        for (i in 0 until destroyed.size) {
            val entity = destroyed[i]
            if (entity.existenceState != EntityExistenceState.DESTROY_QUEUED) continue
            entity.existenceState = EntityExistenceState.UNSPAWNED
            entity.onDestroy()
        }
    }

    fun tick() {
        processSpawns()
        worldStep(bossBody)
        boss.getComponent(UpdatablesComponent::class)!!.updatables.values().forEach { it.update(STEP) }
        for (i in 0 until alsoTick.size) {
            val entity = alsoTick[i]
            if (!entity.existenceState.liveNow) continue
            entity.getComponent(UpdatablesComponent::class)?.updatables?.values()?.forEach { it.update(STEP) }
        }
        if (!animations.contains(boss)) animations.add(boss)
        animations.update(STEP)
        boss.getComponent(SpritesComponent::class)!!.preProcess(STEP)
        services.events.run()
        processSpawns()
        processDestroys()
        time += STEP
        if (++ticks % CLEAR_EVERY == 0) clearMocks(
            texture, engine, audio, assets, answers = false, recordedCalls = true, childMocks = false,
            verificationMarks = false, exclusionRules = false
        )
    }

    private fun worldStep(body: Body) {
        body.preProcess()
        body.process(STEP)
        SENSES.forEach { body.setBodySense(it, false) }
        val physics = body.physics
        if (!physics.collisionOn) return
        if (body.getY() <= FLOOR) {
            body.setY(FLOOR)
            if (physics.velocity.y < 0f) physics.velocity.y = 0f
        }
        val left = leftInner()
        val right = rightInner()
        if (body.getX() <= left) {
            body.setX(left)
            if (physics.velocity.x < 0f) physics.velocity.x = 0f
        }
        if (body.getMaxX() >= right) {
            body.setX(right - body.getWidth())
            if (physics.velocity.x > 0f) physics.velocity.x = 0f
        }
        if (abs(body.getY() - FLOOR) < CONTACT) body.setBodySense(BodySense.FEET_ON_GROUND, true)
        if (body.getX() - left < CONTACT) body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_LEFT, true)
        if (right - body.getMaxX() < CONTACT) body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_RIGHT, true)
    }

    private fun leftInner(): Float {
        var x = -Float.MAX_VALUE
        val center = bossBody.getCenter(Vector2()).x
        walls.forEach { if (it.getMaxX() <= center) x = maxOf(x, it.getMaxX()) }
        return x
    }

    private fun rightInner(): Float {
        var x = Float.MAX_VALUE
        val center = bossBody.getCenter(Vector2()).x
        walls.forEach { if (it.getX() >= center) x = minOf(x, it.getX()) }
        return x
    }

    fun holdInAir(tilesOverFloor: Float) {
        bossBody.setY(FLOOR + tilesOverFloor * T)
        bossBody.physics.velocity.y = 0f
    }

    fun ticks(seconds: Float, each: () -> Unit = {}) {
        var elapsed = 0f
        while (elapsed < seconds - STEP / 2f) {
            each()
            tick()
            elapsed += STEP
        }
    }

    fun ticksUntil(maxSeconds: Float, each: () -> Unit = {}, done: () -> Boolean): Boolean {
        var elapsed = 0f
        while (elapsed < maxSeconds) {
            if (done()) return true
            each()
            tick()
            elapsed += STEP
        }
        return done()
    }

    fun secondsWhile(maxSeconds: Float = 10f, each: () -> Unit = {}, condition: () -> Boolean): Float {
        var count = 0
        while (condition() && count * STEP < maxSeconds) {
            each()
            tick()
            count++
        }
        return count * STEP
    }

    fun untilKey(key: String, maxSeconds: Float = 5f) =
        withClue("reaches the key $key (now $animKey)") { ticksUntil(maxSeconds) { baseKey == key } shouldBe true }

    fun untilKeyChange(maxSeconds: Float = 5f): String? {
        val from = animKey
        withClue("leaves the key $from") { ticksUntil(maxSeconds) { animKey != from } shouldBe true }
        return animKey
    }

    fun leaveStand(fraction: Float, maxSeconds: Float = 3f): String? {
        pick(fraction)
        withClue("STAND ends") { ticksUntil(maxSeconds) { baseKey != "stand" } shouldBe true }
        return baseKey
    }

    fun untilStand(maxSeconds: Float = 8f) =
        withClue("back to STAND (now $animKey)") { ticksUntil(maxSeconds) { baseKey == "stand" } shouldBe true }

    fun lastPickTotal(): Float = random.pickCalls().last().max

    fun rolls(vararg values: Float) {
        random.rolls.clear()
        values.forEach { random.rolls.addLast(it) }
    }

    fun pick(fraction: Float) {
        random.picks.clear()
        random.picks.addLast(fraction)
    }

    fun animator(): Animator {
        var found: Animator? = null
        boss.getComponent(AnimationsComponent::class)!!.forEachAnimator { _, _, animator -> found = animator as Animator }
        return found!!
    }

    val animKey: String?
        get() {
            val animator = animator()
            return animator.keySupplier(animator.currentKey)
        }

    val baseKey: String?
        get() = animKey?.removeSuffix("_up")?.removeSuffix("_shoot")

    fun sprite(): GameSprite = boss.getComponent(SpritesComponent::class)!!.sprites.values().first()

    fun sounds(sound: SoundAsset) =
        boss.getComponent(AudioComponent::class)!!.playSoundRequests.count { it.source == sound }

    fun eventsOf(type: EventType) = events.count { it.key == type }

    inline fun <reified E> spawnsOf() = spawnLog.filter { it.entity is E }

    inline fun <reified E> spawnedOf() = spawned.filterIsInstance<E>()

    private fun copyOf(properties: Properties): Properties {
        val out = Properties()
        properties.forEach { key, value -> out.put(key, if (value is Vector2) Vector2(value) else value) }
        return out
    }

    companion object {
        private val SENSES = listOf(
            BodySense.FEET_ON_GROUND, BodySense.SIDE_TOUCHING_BLOCK_LEFT, BodySense.SIDE_TOUCHING_BLOCK_RIGHT
        )
    }
}
