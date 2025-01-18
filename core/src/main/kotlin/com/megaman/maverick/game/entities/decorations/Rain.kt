package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods.getOverlapPushDirection
import com.mega.game.engine.common.UtilMethods.getRandom
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.ProcessState
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.isAny
import com.mega.game.engine.common.extensions.objectMapOf
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
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.DecorationsFactory
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.extensions.toGameRectangle
import com.megaman.maverick.game.world.body.*

class RainDrop(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity {

    companion object {
        const val TAG = "RainDrop"
        private const val SPLASH_DUR = 0.3f
        private var region: TextureRegion? = null
    }

    private val splashTimer = Timer(SPLASH_DUR)
    private var splashed = false
    private var deathY = 0f

    override fun init() {
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.DECORATIONS_1.source, TAG)
        super.init()
        addComponent(defineUpdatablesComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)
        val trajectory = spawnProps.get(ConstKeys.TRAJECTORY, Vector2::class)!!
        body.physics.velocity.set(trajectory)
        body.physics.gravityOn = true
        splashTimer.setToEnd()
        splashed = false
        deathY = spawnProps.get("${ConstKeys.DEATH}_${ConstKeys.Y}", Float::class)!!
    }

    private fun splash(rotation: Float = 0f) {
        GameLogger.debug(TAG, "splash()")
        destroy()
        val splash = EntityFactories.fetch(EntityType.DECORATION, DecorationsFactory.SPLASH)!!
        splash.spawn(
            props(
                ConstKeys.POSITION pairTo body.getPositionPoint(Position.BOTTOM_CENTER),
                ConstKeys.TYPE pairTo Splash.SplashType.WHITE
            )
        )
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({
        if (body.getY() <= deathY) {
            GameLogger.debug(TAG, "update(): below death y $deathY, destroying $this")
            destroy()
        }
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.1f * ConstVals.PPM)
        body.physics.applyFrictionX = false

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle(body))
        bodyFixture.setHitByBlockReceiver(ProcessState.BEGIN) { block, _ ->
            GameLogger.debug(TAG, "hit block $block")
            val direction = getOverlapPushDirection(bodyFixture.getShape(), block.body.getBounds())
            splash(direction?.rotation ?: 0f)
        }
        bodyFixture.setHitByBodyReceiver {
            if (!it.isAny(RainFall::class, RainDrop::class)) {
                GameLogger.debug(TAG, "hit body $it")
                val direction = getOverlapPushDirection(bodyFixture.getShape(), it.body.getBounds())
                splash(direction?.rotation ?: 0f)
            }
        }
        body.addFixture(bodyFixture)

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(region!!, DrawingPriority(DrawingSection.FOREGROUND, 10))
        sprite.setSize(0.1f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setPosition(body.getPositionPoint(Position.BOTTOM_CENTER), Position.BOTTOM_CENTER)
        }
        return spritesComponent
    }

    override fun getTag() = TAG

    override fun getType() = EntityType.DECORATION
}

class RainFall(game: MegamanMaverickGame) : MegaGameEntity(game), ICullableEntity {

    companion object {
        const val TAG = "RainFall"
        private const val VELOCITY = 15f
        private const val ANGLE = 140f
        private const val MIN_DELAY_DUR = 0.025f
        private const val MAX_DELAY_DUR = 0.1f
    }

    private val rainSpawners = Array<GameRectangle>()
    private lateinit var delayTimer: Timer
    private lateinit var cullBounds: GameRectangle
    private var deathY = 0f

    override fun init() {
        super.init()
        addComponent(defineUpdatablesComponent())
        addComponent(defineCullablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        GameLogger.debug(TAG, "onSpawn(): Megaman's position = ${megaman.body.getCenter()}")
        super.onSpawn(spawnProps)
        cullBounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        spawnProps.forEach { key, value ->
            if (key.toString().contains(ConstKeys.SPAWN) && value is RectangleMapObject) {
                val rainSpawner = value.rectangle.toGameRectangle()
                GameLogger.debug(TAG, "onSpawn(): adding rain spawner $rainSpawner")
                rainSpawners.add(rainSpawner)
            }
        }
        deathY = spawnProps.getOrDefault("${ConstKeys.DEATH}_${ConstKeys.Y}", cullBounds.getY(), Float::class)
        delayTimer = Timer(getRandom(MIN_DELAY_DUR, MAX_DELAY_DUR))
    }

    private fun spawnRainDrops() {
        GameLogger.debug(TAG, "spawnRainDrops()")
        rainSpawners.forEach { rainSpawner ->
            val rainDrop = EntityFactories.fetch(EntityType.DECORATION, DecorationsFactory.RAIN_DROP)!!
            rainDrop.spawn(
                props(
                    ConstKeys.POSITION pairTo Vector2(
                        getRandom(rainSpawner.getX(), rainSpawner.getMaxX()),
                        rainSpawner.getPositionPoint(Position.TOP_CENTER).y
                    ),
                    ConstKeys.TRAJECTORY pairTo Vector2(0f, VELOCITY * ConstVals.PPM).rotateDeg(ANGLE),
                    "${ConstKeys.DEATH}_${ConstKeys.Y}" pairTo deathY
                )
            )
        }
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        delayTimer.update(delta)
        if (delayTimer.isFinished()) {
            spawnRainDrops()
            delayTimer.resetDuration(getRandom(MIN_DELAY_DUR, MAX_DELAY_DUR))
        }
    })

    private fun defineCullablesComponent(): CullablesComponent {
        val cullOutOfBounds = getGameCameraCullingLogic(game.getGameCamera(), { cullBounds })
        return CullablesComponent(objectMapOf(ConstKeys.CULL_OUT_OF_BOUNDS pairTo cullOutOfBounds))
    }

    override fun getTag() = TAG

    override fun getType() = EntityType.DECORATION
}
