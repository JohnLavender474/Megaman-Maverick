package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.animations.AnimationsSystem
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.random.SeededRandom
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.entities.EntityExistenceState
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.points.PointsSystem
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.audio.MegaAudioManager
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.bosses.d1000.D1000Action
import com.megaman.maverick.game.entities.bosses.d1000.D1000Context
import com.megaman.maverick.game.entities.bosses.d1000.D1000Director
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.decorations.D1000BombMarker
import com.megaman.maverick.game.entities.decorations.D1000DebrisWarning
import com.megaman.maverick.game.entities.decorations.D1000RubblePile
import com.megaman.maverick.game.entities.decorations.FallingPiece
import com.megaman.maverick.game.entities.hazards.D1000CeilingDebris
import com.megaman.maverick.game.entities.hazards.D1000Pillar
import com.megaman.maverick.game.entities.hazards.D1000Shockwave
import com.megaman.maverick.game.entities.megamanx.MegamanX
import com.megaman.maverick.game.entities.players.PlayerSabreSlash
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.D1000Bomb
import com.megaman.maverick.game.entities.projectiles.EnemyShot
import com.megaman.maverick.game.entities.state.BossPhase
import com.megaman.maverick.game.entities.state.PlayPhase
import com.megaman.maverick.game.entities.state.PlayPhaseComponent
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.testServices
import io.mockk.clearAllMocks
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import java.util.Random
import kotlin.math.sqrt

private const val CLEAR_EVERY = 30
private const val HALF_HEALTH_HITS_DAMAGE = D1000.HALF_HEALTH / 3
const val ENTRY_SECONDS = 6f
const val RIG_DELTA = 1f / 60f
const val RIG_TEXTURE_SIZE = 1024
const val RIG_PPM = ConstVals.PPM.toFloat()
const val RIG_ARENA_X = 10f * RIG_PPM
const val RIG_FLOOR = 4f * RIG_PPM
const val RIG_ARENA_WIDTH = 21.5f * RIG_PPM
const val RIG_ARENA_HEIGHT = 12f * RIG_PPM

class D1000Rig(val arenaHeight: Float = RIG_ARENA_HEIGHT, val arenaWidth: Float = RIG_ARENA_WIDTH) {

    val spawned = Array<IGameEntity>()
    val destroyed = Array<IGameEntity>()
    val randomSource = Random(0L)
    val services: MegaServices
    val x: MegamanX
    val xBody: Body
    lateinit var boss: D1000
    var flyBombs = true
    var flyShots = true
    var tickWaves = false

    private val animations = AnimationsSystem()
    private val points = PointsSystem()
    private val texture = mockk<Texture>(relaxed = true)
    private val engine = mockk<GameEngine>(relaxed = true)
    private val audio = mockk<MegaAudioManager>(relaxed = true)
    private val assets = mockk<AssetManager>(relaxed = true)
    private val atlas = mockk<TextureAtlas>()
    private var ticksSinceClear = 0

    init {
        every { engine.spawn(any(), any()) } answers {
            val entity = firstArg<IGameEntity>()
            val properties = secondArg<Properties>()
            if (!entity.initialized) {
                entity.init()
                entity.initialized = true
            }
            entity.onSpawn(properties)
            entity.existenceState = EntityExistenceState.LIVE
            spawned.add(entity)
            true
        }
        every { engine.destroy(any()) } answers {
            val entity = firstArg<IGameEntity>()
            destroyed.add(entity)
            entity.existenceState = EntityExistenceState.DESTROY_QUEUED
            true
        }
        services = testServices(engine = engine, assets = assets, audio = audio, random = SeededRandom(randomSource))
        every { texture.width } returns RIG_TEXTURE_SIZE
        every { texture.height } returns RIG_TEXTURE_SIZE
        every { atlas.findRegion(any()) } returns AtlasRegion(texture, 0, 0, 64, 64)
        every { services.assets.get(any<String>(), TextureAtlas::class.java) } returns atlas
        services.levelState.currentLevel = LevelDefinition.X6_INTRO
        MegaEntityFactory.init(services)

        x = MegamanX(services)
        x.existenceState = EntityExistenceState.LIVE
        x.addComponent(x.defineBodyComponent())
        x.addComponent(PlayPhaseComponent(PlayPhase.IN_PLAY))
        xBody = x.getComponent(BodyComponent::class)!!.body
        x.facing = Facing.RIGHT
        placeX(1f, 0f)
        services.players.register(PlayerSlot(0), x)
    }

    fun dispose() {
        spawned.forEach { if (it is MegaGameEntity) MegaGameEntities.remove(it) }
        if (this::boss.isInitialized) MegaGameEntities.remove(boss)
        spawned.clear()
        destroyed.clear()
        clearAllMocks(answers = false, recordedCalls = true, childMocks = false)
    }

    fun placeX(tilesFromWall: Float, tilesOverFloor: Float) =
        xBody.setCenter(RIG_ARENA_X + tilesFromWall * RIG_PPM, RIG_FLOOR + tilesOverFloor * RIG_PPM + xBody.getHeight() / 2f)

    val arena get() = GameRectangle(RIG_ARENA_X, RIG_FLOOR, arenaWidth, arenaHeight)

    fun bossBody() = boss.getComponent(BodyComponent::class)!!.body

    fun bossCenter(): Vector2 = bossBody().getCenter(Vector2())

    fun core() = spawned.filterIsInstance<D1000Core>().last()

    inline fun <reified T> spawnedOf() = spawned.filterIsInstance<T>()

    fun spawnBoss() {
        boss = D1000(services)
        boss.spawn(props(ConstKeys.BOUNDS pairTo arena))
    }

    fun tick() {
        val live = Array<IGameEntity>()
        live.add(boss)
        spawned.forEach { entity ->
            val telegraph = entity is D1000DebrisWarning || entity is D1000BombMarker || entity is D1000Pillar ||
                entity is FallingPiece || entity is D1000RubblePile
            val ticked = entity is D1000Core || telegraph || (flyBombs && entity is D1000Bomb) ||
                (flyShots && (entity is EnemyShot || entity is D1000CeilingDebris)) || (tickWaves && entity is D1000Shockwave)
            if (ticked && !destroyed.contains(entity, true)) live.add(entity)
        }
        if (!points.contains(boss)) points.add(boss)
        points.update(RIG_DELTA)
        live.forEach { entity ->
            if (entity is D1000Bomb || entity is EnemyShot || entity is FallingPiece || entity is D1000CeilingDebris) fly(entity.getComponent(BodyComponent::class)!!.body)
            entity.getComponent(UpdatablesComponent::class)?.updatables?.values()?.forEach { it.update(RIG_DELTA) }
            entity.getComponent(BodyComponent::class)?.body?.preProcess()
        }
        live.forEach { if (!animations.contains(it)) animations.add(it) }
        animations.update(RIG_DELTA)
        live.forEach { it.getComponent(SpritesComponent::class)?.preProcess(RIG_DELTA) }
        if (++ticksSinceClear >= CLEAR_EVERY) {
            ticksSinceClear = 0
            clearMocks(texture, engine, audio, assets, answers = false, recordedCalls = true, childMocks = false,
                verificationMarks = false, exclusionRules = false)
        }
    }

    private fun fly(body: Body) {
        body.physics.velocity.mulAdd(body.physics.gravity, RIG_DELTA)
        body.translate(body.physics.velocity.x * RIG_DELTA, body.physics.velocity.y * RIG_DELTA)
    }

    fun ticks(seconds: Float) {
        var time = 0f
        while (time < seconds) {
            tick()
            time += RIG_DELTA
        }
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

    fun startFight() {
        spawnBoss()
        ticks(ENTRY_SECONDS)
        boss.bossComponent.phase = BossPhase.FIGHT
    }

    fun rollFor(action: D1000Action, context: D1000Context) {
        val r = (0 until D1000Director.ROLLS).first { D1000Director.choose(context.copy(r = it))!!.action == action }
        val seed = (0L until Long.MAX_VALUE).first { Random(it).nextInt(D1000Director.ROLLS) == r }
        randomSource.setSeed(seed)
    }

    fun landingX(bomb: D1000Bomb): Float {
        val body = bomb.getComponent(BodyComponent::class)!!.body
        val drop = body.getCenter(Vector2()).y - body.getHeight() / 2f - RIG_FLOOR
        return body.getCenter(Vector2()).x + body.physics.velocity.x * sqrt(2f * drop / -body.physics.gravity.y)
    }

    fun hitEye(times: Int) = repeat(times) {
        boss.takeEyeDamage(fullCharge(), HALF_HEALTH_HITS_DAMAGE)
        ticks(0.6f)
    }

    fun sabre() = PlayerSabreSlash(services)

    fun bullet(): Bullet {
        val shot = MegaEntityFactory.fetch(Bullet::class)!!
        shot.spawn(
            props(
                ConstKeys.OWNER pairTo x,
                ConstKeys.POSITION pairTo Vector2(),
                ConstKeys.TRAJECTORY pairTo Vector2()
            )
        )
        return shot
    }

    fun halfCharge(): ChargedShot {
        val shot = MegaEntityFactory.fetch(ChargedShot::class)!!
        shot.spawn(
            props(
                ConstKeys.OWNER pairTo x,
                ConstKeys.BOOLEAN pairTo false,
                ConstKeys.POSITION pairTo Vector2(),
                ConstKeys.TRAJECTORY pairTo Vector2(1f, 0f)
            )
        )
        return shot
    }

    fun fullCharge(): ChargedShot {
        val shot = MegaEntityFactory.fetch(ChargedShot::class)!!
        shot.spawn(
            props(
                ConstKeys.OWNER pairTo x,
                ConstKeys.BOOLEAN pairTo true,
                ConstKeys.POSITION pairTo Vector2(),
                ConstKeys.TRAJECTORY pairTo Vector2(1f, 0f)
            )
        )
        return shot
    }
}
