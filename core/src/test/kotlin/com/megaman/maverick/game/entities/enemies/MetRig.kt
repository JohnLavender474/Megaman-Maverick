package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.camera.CameraView
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.cullables.CullablesSystem
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.cullables.GameEntityCuller
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.points.Points
import com.maverick.game.engine.points.PointsComponent
import com.maverick.game.engine.points.PointsSystem
import com.maverick.game.engine.systems.GameSystem
import com.maverick.game.engine.updatables.UpdatablesSystem
import com.maverick.game.engine.world.WorldSystem
import com.maverick.game.engine.world.container.SimpleGridWorldContainer
import com.maverick.game.engine.world.contacts.ContactRuleDispatcher
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.difficulty.DifficultyMode
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.hazards.MetHelmet
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.megamanx.MegamanX
import com.megaman.maverick.game.entities.state.PlayPhase
import com.megaman.maverick.game.entities.state.PlayPhaseComponent
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.entities.EntityExistenceState
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.sensors.Death
import com.megaman.maverick.game.players.MegaPlayers
import com.megaman.maverick.game.players.WorldGravity
import com.megaman.maverick.game.state.GameMode
import com.megaman.maverick.game.state.Performance
import com.megaman.maverick.game.testServices
import com.megaman.maverick.game.world.collisions.MegaCollisionHandler
import com.megaman.maverick.game.world.contacts.MegaContactFilter
import com.megaman.maverick.game.world.contacts.megaContactRules
import com.badlogic.gdx.utils.ObjectSet
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk

const val MET_PPM = ConstVals.PPM.toFloat()
const val MET_DELTA = 1f / 60f

class MetRig(cameraTiles: Float = 60f) {

    val players = MegaPlayers()
    val gravity = WorldGravity(players)
    val container = SimpleGridWorldContainer(ConstVals.PPM)
    val camera = OrthographicCamera().apply {
        viewportWidth = cameraTiles * MET_PPM
        viewportHeight = cameraTiles * MET_PPM
    }
    val engine: GameEngine
    val services: MegaServices
    val player: MegamanX
    val playerBody: Body
    var time = 0f
        private set

    init {
        val world = WorldSystem(
            ppm = ConstVals.PPM,
            fixedStep = Performance.MEDIUM.fixedStep,
            worldContainerSupplier = { container },
            contactListener = ContactRuleDispatcher(megaContactRules(players, gravity, GameMode.SINGLE.rules) { _, _ -> false }),
            collisionHandler = MegaCollisionHandler(),
            contactFilter = MegaContactFilter()
        )
        val culler = object : GameEntityCuller {
            override fun cull(entity: IGameEntity) {
                entity.destroy()
            }
        }
        engine = GameEngine(Array<GameSystem>().apply {
            add(world)
            add(CullablesSystem(culler))
            add(PointsSystem())
            add(UpdatablesSystem())
        })
        services = testServices(engine = engine, players = players, gravity = gravity, gameCamera = CameraView(camera))
        services.levelState.worldContainer = container
        val texture = mockk<Texture>(relaxed = true)
        every { texture.width } returns 1024
        every { texture.height } returns 1024
        val atlas = mockk<TextureAtlas>()
        every { atlas.findRegion(any()) } returns AtlasRegion(texture, 0, 0, 64, 64)
        every { services.assets.get(any<String>(), TextureAtlas::class.java) } returns atlas
        MegaEntityFactory.init(services)
        player = MegamanX(services)
        player.existenceState = EntityExistenceState.LIVE
        player.addComponent(player.defineBodyComponent())
        player.addComponent(PlayPhaseComponent(PlayPhase.IN_PLAY))
        player.addComponent(PointsComponent().apply {
            putPoints(ConstKeys.HEALTH, Points(max = ConstVals.MAX_HEALTH, current = ConstVals.MAX_HEALTH, min = 0))
        })
        playerBody = player.getComponent(BodyComponent::class)!!.body
        player.facing = Facing.RIGHT
        players.register(PlayerSlot(0), player)
        placePlayer(-20f, 1f)
        lookAt(0f, 0f)
    }

    fun dispose() {
        engine.reset()
        MegaEntityFactory.reset()
        clearAllMocks(answers = false)
    }

    fun hardMode() = services.gameState.setDifficultyMode(DifficultyMode.HARD)

    fun lookAt(x: Float, y: Float) {
        camera.position.set(x * MET_PPM, y * MET_PPM, 0f)
    }

    fun placePlayer(x: Float, y: Float) {
        playerBody.setX(x * MET_PPM)
        playerBody.setY(y * MET_PPM)
    }

    fun block(x: Float, y: Float, width: Float, height: Float): Block {
        val block = Block(services)
        block.spawn(props(ConstKeys.BOUNDS pairTo GameRectangle(x * MET_PPM, y * MET_PPM, width * MET_PPM, height * MET_PPM)))
        return block
    }

    fun death(x: Float, y: Float, width: Float, height: Float) {
        Death(services).spawn(
            props(ConstKeys.BOUNDS pairTo GameRectangle(x * MET_PPM, y * MET_PPM, width * MET_PPM, height * MET_PPM))
        )
    }

    fun metProps(x: Float, y: Float, vararg extra: Pair<String, Any>): Properties {
        val spawnProps = props(
            ConstKeys.POSITION pairTo Vector2(x * MET_PPM, y * MET_PPM),
            ConstKeys.CULL_OUT_OF_BOUNDS pairTo false,
            ConstKeys.CULL_EVENTS pairTo false
        )
        extra.forEach { (key, value) -> spawnProps.put(key, value) }
        return spawnProps
    }

    fun met(x: Float, y: Float, vararg extra: Pair<String, Any>): Met {
        val met = MegaEntityFactory.fetch(Met::class)!!
        met.spawn(metProps(x, y, *extra)) shouldBe true
        engine.update(0f)
        return met
    }

    fun chargedShot(full: Boolean, x: Float, y: Float, direction: Float = 1f) =
        chargedShot(full, x, y, Vector2(direction, 0f))

    fun chargedShot(full: Boolean, x: Float, y: Float, direction: Vector2): ChargedShot {
        val shot = MegaEntityFactory.fetch(ChargedShot::class)!!
        shot.spawn(
            props(
                ConstKeys.OWNER pairTo player,
                ConstKeys.BOOLEAN pairTo full,
                ConstKeys.POSITION pairTo Vector2(x * MET_PPM, y * MET_PPM),
                ConstKeys.TRAJECTORY pairTo direction.cpy().scl(MegamanValues.BULLET_VEL * MET_PPM)
            )
        )
        return shot
    }

    fun bullet(x: Float, y: Float, direction: Float = 1f): Bullet {
        val shot = MegaEntityFactory.fetch(Bullet::class)!!
        shot.spawn(
            props(
                ConstKeys.OWNER pairTo player,
                ConstKeys.POSITION pairTo Vector2(x * MET_PPM, y * MET_PPM),
                ConstKeys.TRAJECTORY pairTo Vector2(direction * MegamanValues.BULLET_VEL * MET_PPM, 0f)
            )
        )
        return shot
    }

    fun shootAt(met: Met, full: Boolean, fromLeft: Boolean = true): ChargedShot {
        val center = met.body.getCenter(Vector2())
        val side = if (fromLeft) -1f else 1f
        return chargedShot(full, center.x / MET_PPM + side * 1.2f, center.y / MET_PPM, -side)
    }

    private val bulletsSeen = ObjectSet<Bullet>()

    fun tick() {
        engine.update(MET_DELTA)
        time += MET_DELTA
        MegaGameEntities.getOfTag(Bullet.TAG).forEach { if (it is Bullet) bulletsSeen.add(it) }
    }

    fun bulletsOf(met: Met) = bulletsSeen.count { it.owner === met }

    fun forgetBullets() = bulletsSeen.clear()

    fun keysUntil(met: Met, seconds: Float, out: Array<String> = Array()): Array<String> {
        val end = time + seconds
        while (time < end) {
            tick()
            out.add(met.key())
        }
        return out
    }

    fun ticksWhile(met: Met, key: String, maxSeconds: Float): Float {
        val start = time
        ticksUntil(maxSeconds) { met.key() != key }
        return time - start
    }

    fun ticks(seconds: Float) {
        val end = time + seconds
        while (time < end) tick()
    }

    fun ticksUntil(maxSeconds: Float, done: () -> Boolean): Boolean {
        val end = time + maxSeconds
        while (time < end) {
            if (done()) return true
            tick()
        }
        return done()
    }

    fun helmets() = MegaGameEntities.getOfTag(MetHelmet.TAG).filterIsInstance<MetHelmet>()

    fun helmetOf(met: Met) = helmets().firstOrNull { it.owner === met }

    inline fun <reified T> liveOf(tag: String) = MegaGameEntities.getOfTag(tag).filterIsInstance<T>()
}

fun Met.key(): String {
    val animator = getComponent(AnimationsComponent::class)!!.getAnimator(AnimationsComponent.DEFAULT_KEY) as Animator
    return animator.keySupplier(animator.currentKey)!!
}

fun MetHelmet.sprite() = getComponent(SpritesComponent::class)!!.let {
    it.preProcess(MET_DELTA)
    defaultSprite
}

fun MetHelmet.tiles() = body.getCenter(Vector2()).scl(1f / MET_PPM)

fun Met.tiles() = body.getCenter(Vector2()).scl(1f / MET_PPM)
