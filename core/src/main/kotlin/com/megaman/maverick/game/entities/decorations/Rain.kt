package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods.getRandom
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.ProcessState
import com.mega.game.engine.common.extensions.*
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.events.Event
import com.mega.game.engine.events.IEventListener
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.decorations.RainDrop.RainDropType
import com.megaman.maverick.game.entities.decorations.Splash.SplashType
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.extensions.toGameRectangle
import com.megaman.maverick.game.world.body.*

class RainDrop(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity, IEventListener {

    companion object {
        const val TAG = "RainDrop"
        private const val SPLASH_DUR = 0.3f
        private const val DEFAULT_DEATH_Y_OFFSET = -20f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    internal enum class RainDropType { BLUE, PURPLE }

    override val eventKeyMask = objectSetOf<Any>(EventType.PLAYER_SPAWN, EventType.GATE_INIT_OPENING)

    private lateinit var type: RainDropType

    private val ignoreIds = ObjectSet<Int>()

    private val splashTimer = Timer(SPLASH_DUR)
    private var splashed = false

    private var deathY = 0f

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.DECORATIONS_1.source)
            RainDropType.entries.forEach { type ->
                val key = type.name.lowercase()
                regions.put(key, atlas.findRegion("$TAG/$key"))
            }
        }
        super.init()
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = when {
            spawnProps.containsKey(ConstKeys.BOUNDS) ->
                spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()

            else -> spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        }
        body.setCenter(spawn)

        if (spawnProps.containsKey(ConstKeys.IGNORE)) {
            val ignoreIds = spawnProps.get(ConstKeys.IGNORE) as ObjectSet<Int>
            this.ignoreIds.addAll(ignoreIds)
        }

        val type= spawnProps.get(ConstKeys.TYPE)
        this.type = if (type is String) RainDropType.valueOf(type.uppercase()) else type as RainDropType

        val trajectory = when {
            spawnProps.containsKey(ConstKeys.TRAJECTORY) -> spawnProps.get(ConstKeys.TRAJECTORY, Vector2::class)!!
            else -> {
                val velX = spawnProps.getOrDefault("${ConstKeys.VELOCITY}_${ConstKeys.X}", 0f, Float::class)
                val velY = spawnProps.getOrDefault("${ConstKeys.VELOCITY}_${ConstKeys.Y}", 0f, Float::class)
                GameObjectPools.fetch(Vector2::class).set(velX, velY).scl(ConstVals.PPM.toFloat())
            }
        }
        body.physics.velocity.set(trajectory)

        body.physics.gravityOn = true

        splashTimer.setToEnd()
        splashed = false

        deathY = spawnProps.getOrDefault(
            "${ConstKeys.DEATH}_${ConstKeys.Y}",
            body.getY() + DEFAULT_DEATH_Y_OFFSET * ConstVals.PPM,
            Float::class
        )
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        ignoreIds.clear()
    }

    override fun onEvent(event: Event) {
        if (event.key.equalsAny(EventType.PLAYER_SPAWN, EventType.GATE_INIT_OPENING)) destroy()
    }

    private fun splash() {
        GameLogger.debug(TAG, "splash()")

        destroy()

        val splashType = when (type) {
            RainDropType.BLUE -> SplashType.BLUE_RAIN
            RainDropType.PURPLE -> SplashType.PURPLE_RAIN
        }

        val splash = MegaEntityFactory.fetch(Splash::class)!!
        splash.spawn(
            props(
                ConstKeys.SOUND pairTo false,
                ConstKeys.TYPE pairTo splashType,
                ConstKeys.POSITION pairTo body.getPositionPoint(Position.BOTTOM_CENTER)
            )
        )
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({
        if (body.getBounds().getY() <= deathY) {
            GameLogger.debug(TAG, "update(): destroy: body below death y")
            destroy()
        }
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.1f * ConstVals.PPM, 0.5f * ConstVals.PPM)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle(body))
        bodyFixture.setHitByBlockReceiver(ProcessState.BEGIN) { block, _ ->
            if (!ignoreIds.contains(block.id)) splash()
        }
        bodyFixture.setHitByBodyReceiver receiver@{ it, state ->
            val mapObjId = (it as MegaGameEntity).id
            if (state == ProcessState.BEGIN &&
                !ignoreIds.contains(mapObjId) &&
                !it.isAny(RainFall::class, RainDrop::class)
            ) splash()
        }
        body.addFixture(bodyFixture)

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 1))
        sprite.setSize(ConstVals.PPM.toFloat())
        val component = SpritesComponent(sprite)
        component.putPreProcess { _, _ ->
            val region = regions[type.name.lowercase()]
            sprite.setRegion(region)

            val position = Position.BOTTOM_CENTER
            sprite.setPosition(body.getPositionPoint(position), position)

            sprite.setOriginCenter()
            sprite.rotation = body.physics.velocity.angleDeg() + 90f
        }
        return component
    }

    override fun getTag() = TAG

    override fun getType() = EntityType.DECORATION
}

class RainFall(game: MegamanMaverickGame) : MegaGameEntity(game), ICullableEntity {

    companion object {
        const val TAG = "RainFall"

        private const val VELOCITY = 16f

        private const val DEFAULT_ANGLE = 180f

        private const val MAX_DELAY_DUR = 0.1f
        private const val MIN_DELAY_DUR = 0.025f

        private const val MAX_RAIN_DROPS = 30

        private const val SPAWN_AREA_BUFFER_WIDTH = 5f
        private const val SPAWN_AREA_BUFFER_HEIGHT = 25f
    }

    private lateinit var rainDropType: RainDropType

    private val rainSpawners = Array<GameRectangle>()
    private val rainSpawnArea = GameRectangle().setSize(
        (ConstVals.VIEW_WIDTH + SPAWN_AREA_BUFFER_WIDTH) * ConstVals.PPM,
        (ConstVals.VIEW_HEIGHT + SPAWN_AREA_BUFFER_HEIGHT) * ConstVals.PPM
    )
    private lateinit var rainSpawnDelay: Timer

    private val rainFallBounds = GameRectangle()
    private var rainFallAngle = DEFAULT_ANGLE

    private val ignoreHitIds = ObjectSet<Int>()

    private var deathY = 0f

    override fun init() {
        super.init()
        addComponent(defineUpdatablesComponent())
        addComponent(defineCullablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        rainDropType = RainDropType.valueOf(
            spawnProps.getOrDefault(ConstKeys.TYPE, RainDropType.BLUE.name, String::class).uppercase()
        )

        spawnProps.forEach { key, value ->
            if (key.toString().contains(ConstKeys.SPAWN) && value is RectangleMapObject) {
                val rainSpawner = value.rectangle.toGameRectangle(false)
                GameLogger.debug(TAG, "onSpawn(): adding rain spawner $rainSpawner")
                rainSpawners.add(rainSpawner)
            } else if (key.toString().contains(ConstKeys.IGNORE) && value is RectangleMapObject) {
                val id = value.properties.get(ConstKeys.ID, Int::class.java)
                ignoreHitIds.add(id)
            }
        }

        rainFallAngle = spawnProps.getOrDefault(ConstKeys.ANGLE, DEFAULT_ANGLE, Float::class)

        rainSpawnDelay = Timer(getRandom(MIN_DELAY_DUR, MAX_DELAY_DUR))

        rainFallBounds.set(spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!)
        deathY = spawnProps.getOrDefault("${ConstKeys.DEATH}_${ConstKeys.Y}", rainFallBounds.getY(), Float::class)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        ignoreHitIds.clear()
        rainSpawners.clear()
    }

    private fun spawnRainDrops() {
        GameLogger.debug(TAG, "spawnRainDrops()")

        rainSpawnArea.setCenter(megaman.body.getCenter())

        rainSpawners.forEach { rainSpawner ->
            if (!rainSpawner.overlaps(rainSpawnArea)) return@forEach

            val spawn = GameObjectPools.fetch(Vector2::class)
                .setX(getRandom(rainSpawner.getX(), rainSpawner.getMaxX()))
                .setY(rainSpawner.getPositionPoint(Position.TOP_CENTER).y)

            val trajectory = GameObjectPools.fetch(Vector2::class)
                .set(0f, VELOCITY * ConstVals.PPM)
                .rotateDeg(rainFallAngle)

            val rainDrop = MegaEntityFactory.fetch(RainDrop::class)!!
            rainDrop.spawn(
                props(
                    ConstKeys.POSITION pairTo spawn,
                    ConstKeys.TYPE pairTo rainDropType,
                    ConstKeys.IGNORE pairTo ignoreHitIds,
                    ConstKeys.TRAJECTORY pairTo trajectory,
                    "${ConstKeys.DEATH}_${ConstKeys.Y}" pairTo deathY,
                )
            )
        }
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        val canSpawn = MegaGameEntities.getOfTag(RainDrop.TAG).size < MAX_RAIN_DROPS

        rainSpawnDelay.update(delta)
        if (canSpawn && rainSpawnDelay.isFinished()) {
            spawnRainDrops()

            val duration = getRandom(MIN_DELAY_DUR, MAX_DELAY_DUR)
            rainSpawnDelay.resetDuration(duration)
        }
    })

    private fun defineCullablesComponent(): CullablesComponent {
        val cullOutOfBounds = getGameCameraCullingLogic(game.getGameCamera(), { rainFallBounds })
        return CullablesComponent(objectMapOf(ConstKeys.CULL_OUT_OF_BOUNDS pairTo cullOutOfBounds))
    }

    override fun getTag() = TAG

    override fun getType() = EntityType.DECORATION
}


