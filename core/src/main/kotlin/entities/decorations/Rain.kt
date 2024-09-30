package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.isAny
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.getOverlapPushDirection
import com.mega.game.engine.common.getRandom
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
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.DecorationsFactory
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.setHitByBlockReceiver
import com.megaman.maverick.game.world.body.setHitByBodyReceiver

class RainDrop(game: MegamanMaverickGame) : MegaGameEntity(game), ICullableEntity, IBodyEntity, ISpritesEntity {

    companion object {
        const val TAG = "RainDrop"
        private const val SPLASH_DUR = 0.3f
        private var region: TextureRegion? = null
    }

    private val splashTimer = Timer(SPLASH_DUR)
    private var splashed = false

    override fun init() {
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.DECORATIONS_1.source, TAG)
        super.init()
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineCullablesComponent())
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
    }

    private fun splash(rotation: Float = 0f) {
        GameLogger.debug(TAG, "splash")
        destroy()
        val splash = EntityFactories.fetch(EntityType.DECORATION, DecorationsFactory.SPLASH)!!
        splash.spawn(props(ConstKeys.POSITION pairTo body.getBottomCenterPoint() /*ConstKeys.ROTATION pairTo rotation*/))
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.1f * ConstVals.PPM)
        body.physics.takeFrictionFromOthers = false

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle(body))
        bodyFixture.setHitByBlockReceiver {
            val direction = getOverlapPushDirection(bodyFixture.getShape(), it.body)
            splash(direction?.rotation ?: 0f)
        }
        bodyFixture.setHitByBodyReceiver {
            if (!it.isAny(RainFall::class, RainDrop::class)) {
                val direction = getOverlapPushDirection(bodyFixture.getShape(), it.body)
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
            _sprite.setPosition(body.getBottomCenterPoint(), Position.BOTTOM_CENTER)
        }
        return spritesComponent
    }

    private fun defineCullablesComponent(): CullablesComponent {
        val cullOutOfBounds = getGameCameraCullingLogic(this)
        return CullablesComponent(objectMapOf(ConstKeys.CULL_OUT_OF_BOUNDS pairTo cullOutOfBounds))
    }

    override fun getTag() = TAG

    override fun getEntityType() = EntityType.DECORATION
}

class RainFall(game: MegamanMaverickGame) : MegaGameEntity(game), ICullableEntity {

    companion object {
        const val TAG = "RainFall"
        private const val VELOCITY = 15f

        // private const val ANGLE = 140f
        private const val DELAY_DUR = 0.025f
    }

    private val delayTimer = Timer(DELAY_DUR)
    private lateinit var bounds: GameRectangle

    override fun init() {
        super.init()
        addComponent(defineUpdatablesComponent())
        addComponent(defineCullablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn: spawnProps=$spawnProps")
        super.onSpawn(spawnProps)
        bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        delayTimer.reset()
    }

    private fun spawnRainDrop() {
        GameLogger.debug(TAG, "spawnRainDrop")
        val rainDrop = EntityFactories.fetch(EntityType.DECORATION, DecorationsFactory.RAIN_DROP)!!
        val spawnX = getRandom(bounds.x, bounds.getMaxX())
        rainDrop.spawn(
            props(
                ConstKeys.POSITION pairTo Vector2(spawnX, bounds.getTopCenterPoint().y),
                ConstKeys.TRAJECTORY pairTo Vector2(0f, -VELOCITY * ConstVals.PPM)//.rotateDeg(ANGLE)
            )
        )
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        delayTimer.update(delta)
        if (delayTimer.isFinished()) {
            spawnRainDrop()
            delayTimer.reset()
        }
    })

    private fun defineCullablesComponent(): CullablesComponent {
        val cullOutOfBounds = getGameCameraCullingLogic(game.getGameCamera(), { bounds })
        return CullablesComponent(objectMapOf(ConstKeys.CULL_OUT_OF_BOUNDS pairTo cullOutOfBounds))
    }

    override fun getTag() = TAG

    override fun getEntityType() = EntityType.DECORATION
}
