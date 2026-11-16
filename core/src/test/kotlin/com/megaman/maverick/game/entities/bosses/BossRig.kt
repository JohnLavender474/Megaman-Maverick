package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.extensions.objectSetOf
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.random.SeededRandom
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
import com.megaman.maverick.game.audio.MegaAudioManager
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.megamanx.MegamanX
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.state.PlayPhase
import com.megaman.maverick.game.entities.state.PlayPhaseComponent
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.players.MegaPlayers
import com.megaman.maverick.game.testServices
import io.mockk.clearAllMocks
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import java.util.Random

const val BOSS_RIG_PPM = ConstVals.PPM.toFloat()

class BossRig {

    val spawned = Array<IGameEntity>()
    val queued = Array<Pair<IGameEntity, Properties>>()
    val destroyed = Array<IGameEntity>()
    val events = Array<Event>()
    val randomSource = Random(0L)
    val services: MegaServices
    val player: MegamanX
    val playerBody: Body

    private val points = PointsSystem()
    private val engine = mockk<GameEngine>(relaxed = true)
    private val audio = mockk<MegaAudioManager>(relaxed = true)
    private val assets = mockk<AssetManager>(relaxed = true)
    private val atlas = mockk<TextureAtlas>()
    private val texture = mockk<Texture>(relaxed = true)

    private val recorder = object : IEventListener {
        override val eventKeyMask = objectSetOf<Any>(
            EventType.BOSS_DEFEATED, EventType.SHAKE_CAM
        )

        override fun onEvent(event: Event) {
            events.add(event)
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
            true
        }
        every { engine.destroy(any()) } answers {
            val entity = firstArg<IGameEntity>()
            destroyed.add(entity)
            entity.existenceState = EntityExistenceState.DESTROY_QUEUED
            true
        }
        services = testServices(engine = engine, assets = assets, audio = audio, players = MegaPlayers(2),
            random = SeededRandom(randomSource)
        )
        every { texture.width } returns 1024
        every { texture.height } returns 1024
        every { atlas.findRegion(any()) } returns AtlasRegion(texture, 0, 0, 64, 64)
        every { services.assets.get(any<String>(), TextureAtlas::class.java) } returns atlas
        services.events.addListener(recorder)
        MegaEntityFactory.init(services)

        player = addPlayer(0)
        playerBody = player.getComponent(BodyComponent::class)!!.body
    }

    fun addPlayer(slot: Int): MegamanX {
        val player = MegamanX(services)
        player.existenceState = EntityExistenceState.LIVE
        player.addComponent(player.defineBodyComponent())
        player.addComponent(PlayPhaseComponent(PlayPhase.IN_PLAY))
        player.facing = Facing.RIGHT
        services.players.register(PlayerSlot(slot), player)
        return player
    }

    fun dispose() {
        spawned.forEach { if (it is MegaGameEntity) MegaGameEntities.remove(it) }
        spawned.clear()
        queued.clear()
        destroyed.clear()
        MegaEntityFactory.reset()
        clearAllMocks(answers = false, recordedCalls = true, childMocks = false)
    }

    fun checkPoints(entity: IGameEntity) {
        if (!points.contains(entity)) points.add(entity)
        points.update(0f)
    }

    fun runEvents() = services.events.run()

    val step: Float
        get() = services.settings.performance.fixedStep

    private var ticks = 0

    fun processSpawns() {
        while (!queued.isEmpty) {
            val (entity, properties) = queued.removeIndex(0)
            spawned.add(entity)
            entity.onSpawn(properties)
            entity.existenceState = EntityExistenceState.LIVE
        }
    }

    fun tick(delta: Float = step) {
        processSpawns()
        val live = Array<IGameEntity>()
        spawned.forEach { if (!destroyed.contains(it, true) && !live.contains(it, true)) live.add(it) }
        live.forEach { entity ->
            entity.getComponent(UpdatablesComponent::class)?.updatables?.values()?.forEach { it.update(delta) }
        }
        live.forEach { entity ->
            val body = entity.getComponent(BodyComponent::class)?.body ?: return@forEach
            body.preProcess()
            if (body.physics.gravityOn) body.physics.velocity.mulAdd(body.physics.gravity, delta)
            body.translate(body.physics.velocity.x * delta, body.physics.velocity.y * delta)
        }
        if (++ticks % 30 == 0) clearMocks(
            texture, engine, audio, assets, answers = false, recordedCalls = true, childMocks = false,
            verificationMarks = false, exclusionRules = false
        )
    }

    fun ticks(seconds: Float, each: () -> Unit = {}) {
        var time = 0f
        while (time < seconds) {
            each()
            tick()
            time += step
        }
    }

    fun ticksUntil(maxSeconds: Float, each: () -> Unit = {}, done: () -> Boolean): Boolean {
        var time = 0f
        while (time < maxSeconds) {
            if (done()) return true
            each()
            tick()
            time += step
        }
        return done()
    }

    fun eventsOf(type: EventType) = events.count { it.key == type }

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

    fun chargedExplosion(full: Boolean): ChargedShotExplosion {
        val explosion = MegaEntityFactory.fetch(ChargedShotExplosion::class)!!
        explosion.spawn(
            props(
                ConstKeys.OWNER pairTo player,
                ConstKeys.BOOLEAN pairTo full,
                ConstKeys.POSITION pairTo Vector2()
            )
        )
        processSpawns()
        return explosion
    }

    fun chargedShot(full: Boolean): ChargedShot {
        val shot = MegaEntityFactory.fetch(ChargedShot::class)!!
        shot.spawn(
            props(
                ConstKeys.OWNER pairTo player,
                ConstKeys.BOOLEAN pairTo full,
                ConstKeys.POSITION pairTo Vector2(),
                ConstKeys.TRAJECTORY pairTo Vector2(1f, 0f)
            )
        )
        processSpawns()
        return shot
    }
}
