package com.megaman.maverick.game.entities.bosses.desertman

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Queue
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.animations.Animation
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
import com.maverick.game.engine.world.body.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.audio.MegaAudioManager
import com.megaman.maverick.game.difficulty.DifficultyMode
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.bosses.DesertMan
import com.megaman.maverick.game.entities.bosses.DesertMan.DesertManState
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
const val ART_PX = DesertMan.SPRITE_SIZE * T / 64f
const val FLOOR = 4f * T
const val LEFT_WALL = 2f * T
const val RIGHT_WALL = 22f * T
const val PASS = 0f
const val FAIL = 0.999f

private const val CLEAR_EVERY = 30
private const val CONTACT = 0.01f

class ScriptedRandom : GameRandom {

    val rolls = Queue<Float>()
    private val delegate = SeededRandom(Random(0L))

    override fun nextFloat(min: Float, max: Float) =
        if (min == 0f && max == 1f) (if (rolls.isEmpty) FAIL else rolls.removeFirst())
        else delegate.nextFloat(min, max)

    override fun nextInt(min: Int, maxInclusive: Int) = delegate.nextInt(min, maxInclusive)

    override fun nextBoolean() = delegate.nextBoolean()

    override fun <T> pick(array: Array<T>) = delegate.pick(array)

    override fun <T> pick(list: List<T>) = delegate.pick(list)

    override fun <T> pickOf(vararg values: T) = delegate.pick(values.toList())

    override fun <T> shuffle(array: Array<T>) = delegate.shuffle(array)
}

internal class DesertManRig(hard: Boolean = false) {

    val spawned = Array<IGameEntity>()
    val spawnLog = Array<Pair<IGameEntity, Properties>>()
    val events = Array<Event>()
    val lights = Array<Pair<DesertManState, Vector2>>()
    val random = ScriptedRandom()
    val services: MegaServices
    val player: MegamanX
    val playerBody: Body
    lateinit var boss: DesertMan

    private val queued = Array<Pair<IGameEntity, Properties>>()
    private val destroyed = Array<IGameEntity>()
    private val animations = AnimationsSystem()
    private val points = PointsSystem()
    private val engine = mockk<GameEngine>(relaxed = true)
    private val audio = mockk<MegaAudioManager>(relaxed = true)
    private val assets = mockk<AssetManager>(relaxed = true)
    private val atlas = mockk<TextureAtlas>()
    private val texture = mockk<Texture>(relaxed = true)
    private var ticks = 0

    private val recorder = object : IEventListener {
        override val eventKeyMask = objectSetOf<Any>(EventType.SHAKE_CAM, EventType.ADD_LIGHT_SOURCE)

        override fun onEvent(event: Event) {
            events.add(event)
            if (event.key == EventType.ADD_LIGHT_SOURCE)
                lights.add(state to Vector2(event.properties.get(ConstKeys.CENTER, Vector2::class)!!))
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
            spawnLog.add(entity to properties)
            true
        }
        every { engine.destroy(any()) } answers {
            val entity = firstArg<IGameEntity>()
            destroyed.add(entity)
            entity.existenceState = EntityExistenceState.DESTROY_QUEUED
            true
        }
        services = testServices(engine = engine, assets = assets, audio = audio, players = MegaPlayers(2), random = random)
        if (hard) services.gameState.setDifficultyMode(DifficultyMode.HARD)
        every { texture.width } returns 1024
        every { texture.height } returns 1024
        every { atlas.findRegion(any()) } returns AtlasRegion(texture, 0, 0, 64, 64)
        every { atlas.findRegion("${DesertMan.TAG}/punch_arm") } returns AtlasRegion(texture, 0, 49, 192, 5)
        every { services.assets.get(any<String>(), TextureAtlas::class.java) } returns atlas
        services.events.addListener(recorder)
        MegaEntityFactory.init(services)

        player = MegamanX(services)
        player.existenceState = EntityExistenceState.LIVE
        player.addComponent(player.defineBodyComponent())
        player.addComponent(PlayPhaseComponent(PlayPhase.IN_PLAY))
        player.facing = Facing.RIGHT
        services.players.register(PlayerSlot(0), player)
        playerBody = player.getComponent(BodyComponent::class)!!.body
        place(4f, 0f)
    }

    fun dispose() {
        spawned.forEach { if (it is MegaGameEntity) MegaGameEntities.remove(it) }
        spawned.clear()
        spawnLog.clear()
        queued.clear()
        destroyed.clear()
        MegaEntityFactory.reset()
        clearAllMocks(answers = false, recordedCalls = true, childMocks = false)
    }

    val bossBody: Body
        get() = boss.getComponent(BodyComponent::class)!!.body

    val state: DesertManState
        get() = boss.currentState

    fun bossCenter(): Vector2 = bossBody.getCenter(Vector2())

    fun place(tilesX: Float, tilesOverFloor: Float) =
        playerBody.setBottomCenterToPoint(Vector2(tilesX * T, FLOOR + tilesOverFloor * T))

    fun placeBoss(tilesX: Float) = bossBody.setX(tilesX * T - bossBody.getWidth() / 2f)

    fun spawnProps(dark: Boolean = false) = props(
        ConstKeys.BOUNDS pairTo GameRectangle(12f * T - 0.75f * T, FLOOR + 5f * T, 1.5f * T, 1.75f * T),
        "${ConstKeys.LEFT}_${ConstKeys.WALL}" pairTo RectangleMapObject(0f, FLOOR, LEFT_WALL, 10f * T),
        "${ConstKeys.RIGHT}_${ConstKeys.WALL}" pairTo RectangleMapObject(RIGHT_WALL, FLOOR, 2f * T, 10f * T),
        ConstKeys.DARKNESS pairTo dark,
        "${ConstKeys.LIGHT}_${ConstKeys.SOURCE}_${ConstKeys.KEYS}" pairTo "1,2"
    )

    fun spawnBoss(dark: Boolean = false) {
        boss = DesertMan(services)
        boss.spawn(spawnProps(dark)) shouldBe true
        processSpawns()
    }

    fun startFight(phase2: Boolean = false, dark: Boolean = false) {
        spawnBoss(dark)
        if (phase2) toPhase2()
        enterFight()
    }

    fun respawn() {
        boss.destroy()
        boss.onDestroy()
        boss.spawn(spawnProps()) shouldBe true
        processSpawns()
        enterFight()
    }

    fun enterFight() {
        withClue("the intro") {
            ticksUntil(5f) { boss.bossComponent.phase == BossPhase.INTRO } shouldBe true
        }
        boss.bossComponent.phase = BossPhase.FIGHT
        withClue("the first STAND") { ticksUntil(2f) { state == DesertManState.STAND } shouldBe true }
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

    fun rolls(vararg values: Float) {
        random.rolls.clear()
        values.forEach { random.rolls.addLast(it) }
    }

    fun attack(target: DesertManState, vararg values: Float) {
        rolls(*values)
        withClue("STAND ends in $target") {
            ticksUntil(3f) { state != DesertManState.STAND } shouldBe true
            state shouldBe target
        }
        random.rolls.clear()
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

    fun tick() {
        processSpawns()
        worldStep(bossBody)
        boss.getComponent(UpdatablesComponent::class)!!.updatables.values().forEach { it.update(STEP) }
        if (!animations.contains(boss)) animations.add(boss)
        animations.update(STEP)
        boss.getComponent(SpritesComponent::class)!!.preProcess(STEP)
        services.events.run()
        processSpawns()
        if (++ticks % CLEAR_EVERY == 0) clearMocks(
            texture, engine, audio, assets, answers = false, recordedCalls = true, childMocks = false,
            verificationMarks = false, exclusionRules = false
        )
    }

    private fun worldStep(body: Body) {
        body.preProcess()
        val physics = body.physics
        if (physics.gravityOn) physics.velocity.mulAdd(physics.gravity, STEP)
        physics.velocity.x = physics.velocity.x.coerceIn(-physics.velocityClamp.x, physics.velocityClamp.x)
        physics.velocity.y = physics.velocity.y.coerceIn(-physics.velocityClamp.y, physics.velocityClamp.y)
        body.translate(physics.velocity.x * STEP, physics.velocity.y * STEP)
        SENSES.forEach { body.setBodySense(it, false) }
        if (!physics.collisionOn) return
        if (body.getY() <= FLOOR) {
            body.setY(FLOOR)
            if (physics.velocity.y < 0f) physics.velocity.y = 0f
        }
        if (body.getX() <= LEFT_WALL) {
            body.setX(LEFT_WALL)
            if (physics.velocity.x < 0f) physics.velocity.x = 0f
        }
        if (body.getMaxX() >= RIGHT_WALL) {
            body.setX(RIGHT_WALL - body.getWidth())
            if (physics.velocity.x > 0f) physics.velocity.x = 0f
        }
        if (fixture(FixtureType.FEET).isActive() && abs(body.getY() - FLOOR) < CONTACT) {
            body.setBodySense(BodySense.FEET_ON_SAND, true)
            body.setBodySense(BodySense.FEET_ON_GROUND, true)
        }
        val sides = body.fixtures[FixtureType.SIDE]
        if (sides.all { it.isActive() }) {
            if (body.getX() - LEFT_WALL < CONTACT) body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_LEFT, true)
            if (RIGHT_WALL - body.getMaxX() < CONTACT) body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_RIGHT, true)
        }
    }

    fun fixture(type: FixtureType) = bossBody.fixtures[type].first()

    fun fixtures(type: FixtureType): List<IFixture> = bossBody.fixtures[type].toList()

    val armDamager: IFixture
        get() = fixtures(FixtureType.DAMAGER)[1]

    val fistDamager: IFixture
        get() = fixtures(FixtureType.DAMAGER)[2]

    val armDamageable: IFixture
        get() = fixtures(FixtureType.DAMAGEABLE)[1]

    val fistDamageable: IFixture
        get() = fixtures(FixtureType.DAMAGEABLE)[2]

    fun rect(fixture: IFixture) = fixture.getShape() as GameRectangle

    fun ticks(seconds: Float, each: () -> Unit = {}) {
        var time = 0f
        while (time < seconds - STEP / 2f) {
            tick()
            each()
            time += STEP
        }
    }

    fun ticksUntil(maxSeconds: Float, each: () -> Unit = {}, done: () -> Boolean): Boolean {
        var time = 0f
        while (time < maxSeconds) {
            if (done()) return true
            tick()
            each()
            time += STEP
        }
        return done()
    }

    fun secondsWhile(maxSeconds: Float = 10f, each: () -> Unit = {}, condition: () -> Boolean): Float {
        var count = 0
        while (condition() && count * STEP < maxSeconds) {
            tick()
            each()
            count++
        }
        return count * STEP
    }

    fun animator(): Animator {
        var found: Animator? = null
        boss.getComponent(AnimationsComponent::class)!!.forEachAnimator { _, _, animator -> found = animator as Animator }
        return found!!
    }

    val animKey: String?
        get() = animator().currentKey

    val animIndex: Int
        get() = (animator().currentAnimation as Animation).getIndex()

    fun sprite(key: Any): GameSprite = boss.getComponent(SpritesComponent::class)!!.sprites[key]!!

    fun sounds(sound: SoundAsset) =
        boss.getComponent(AudioComponent::class)!!.playSoundRequests.count { it.source == sound }

    fun eventsOf(type: EventType) = events.count { it.key == type }

    inline fun <reified E> spawnedOf() = spawned.filterIsInstance<E>()

    inline fun <reified E> spawnPositionsOf(): List<Vector2> =
        spawnLog.filter { it.first is E }.map { it.second.get(ConstKeys.POSITION, Vector2::class)!! }

    companion object {
        private val SENSES = listOf(
            BodySense.FEET_ON_SAND, BodySense.FEET_ON_GROUND, BodySense.SIDE_TOUCHING_BLOCK_LEFT,
            BodySense.SIDE_TOUCHING_BLOCK_RIGHT
        )
    }
}
