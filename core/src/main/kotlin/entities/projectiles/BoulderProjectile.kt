package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.getOverlapPushDirection
import com.mega.game.engine.common.getRandom
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.shapes.IGameShape2D
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.GameEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getEntity

class BoulderProjectile(game: MegamanMaverickGame) : AbstractProjectile(game) {

    companion object {
        const val TAG = "BoulderProjectile"
        private const val MEDIUM_MAX_X = 6f
        private const val MEDIUM_MIN_X = 2.5f
        private const val MEDIUM_MAX_Y = 15f
        private const val MEDIUM_MIN_Y = 9f
        private const val SMALL_MAX_X = 3f
        private const val SMALL_MIN_X = 1f
        private const val SMALL_MAX_Y = 7f
        private const val SMALL_MIN_Y = 5f
        private const val SPAWN_EXPLODE_DELAY = 0.25f
        private const val GRAVITY = -0.25f
        private const val LARGE_SIZE = 2f
        private const val MEDIUM_SIZE = 1f
        private const val SMALL_SIZE = 0.5f
        private const val MEDIUM_ROTATION_SPEED = 360f
        private const val SMALL_ROTATION_SPEED = 720f
        private var largeRegion: TextureRegion? = null
        private var mediumRegion: TextureRegion? = null
        private var smallRegion: TextureRegion? = null
    }

    override var owner: GameEntity? = null

    lateinit var size: Size
        private set

    private val spawnExplodeDelay = Timer(SPAWN_EXPLODE_DELAY)

    override fun init() {
        if (largeRegion == null || mediumRegion == null || smallRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.PLATFORMS_1.source)
            largeRegion = atlas.findRegion("Boulder/Large")
            mediumRegion = atlas.findRegion("Boulder/Medium")
            smallRegion = atlas.findRegion("Boulder/Small")
        }
        super.init()
        addComponent(defineUpdatablesComponent())
        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body }), debug = true))
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "Spawn props = $spawnProps")
        super.onSpawn(spawnProps)
        size = spawnProps.getOrDefault(ConstKeys.SIZE, Size.LARGE, Size::class)
        body.setSize(
            when (size) {
                Size.LARGE -> LARGE_SIZE
                Size.MEDIUM -> MEDIUM_SIZE
                Size.SMALL -> SMALL_SIZE
            } * ConstVals.PPM
        )
        val trajectory = spawnProps.get(ConstKeys.TRAJECTORY, Vector2::class)!!
        body.physics.velocity.set(trajectory)
        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)
        spawnExplodeDelay.reset()
    }

    override fun explodeAndDie(vararg params: Any?) {
        destroy()
        val disintegration = EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.DISINTEGRATION)
        disintegration!!.spawn(props(ConstKeys.POSITION pairTo body.getCenter()))
        if (overlapsGameCamera() && size == Size.SMALL) requestToPlaySound(SoundAsset.THUMP_SOUND, false)
    }

    private fun breakApart(shape: IGameShape2D) {
        GameLogger.debug(TAG, "Breaking apart")
        val trajectories = Array<Vector2>()
        when (size) {
            Size.LARGE -> {
                for (i in 0 until 4) {
                    var x = getRandom(MEDIUM_MIN_X, MEDIUM_MAX_X)
                    val y = getRandom(MEDIUM_MIN_Y, MEDIUM_MAX_Y)
                    if (i >= 2) x *= -1f
                    trajectories.add(Vector2(x, y).scl(ConstVals.PPM.toFloat()))
                }
            }

            Size.MEDIUM -> {
                for (i in 0 until 4) {
                    var x = getRandom(SMALL_MIN_X, SMALL_MAX_X)
                    val y = getRandom(SMALL_MIN_Y, SMALL_MAX_Y)
                    if (i >= 2) x *= -1f
                    trajectories.add(Vector2(x, y).scl(ConstVals.PPM.toFloat()))
                }
            }

            Size.SMALL -> throw IllegalStateException("Cannot break apart a small boulder")
        }
        val direction = getOverlapPushDirection(body, shape) ?: return
        trajectories.forEach { trajectory ->
            val rotatedTrajectory = trajectory.rotateDeg(direction.rotation)
            val boulder = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.BOULDER_PROJECTILE)
            boulder!!.spawn(
                props(
                    ConstKeys.POSITION pairTo body.getCenter(),
                    ConstKeys.SIZE pairTo (if (size == Size.LARGE) Size.MEDIUM else Size.SMALL),
                    ConstKeys.TRAJECTORY pairTo rotatedTrajectory
                )
            )
        }
        if (size == Size.LARGE) requestToPlaySound(SoundAsset.QUAKE_SOUND, false)
        explodeAndDie()
    }

    override fun hitBody(bodyFixture: IFixture) {
        if (!spawnExplodeDelay.isFinished() || bodyFixture.getEntity() is BoulderProjectile) return
        GameLogger.debug(TAG, "Hit body: $bodyFixture")
        super.hitBody(bodyFixture)
        when (size) {
            Size.LARGE, Size.MEDIUM -> breakApart(bodyFixture.getShape())
            Size.SMALL -> explodeAndDie()
        }
    }

    override fun hitBlock(blockFixture: IFixture) {
        if (!spawnExplodeDelay.isFinished()) return
        GameLogger.debug(TAG, "Hit block: $blockFixture")
        super.hitBlock(blockFixture)
        when (size) {
            Size.LARGE, Size.MEDIUM -> breakApart(blockFixture.getShape())
            Size.SMALL -> explodeAndDie()
        }
    }

    override fun hitShield(shieldFixture: IFixture) {
        if (!spawnExplodeDelay.isFinished()) return
        GameLogger.debug(TAG, "Hit shield: $shieldFixture")
        super.hitShield(shieldFixture)
        when (size) {
            Size.LARGE, Size.MEDIUM -> breakApart(shieldFixture.getShape())
            Size.SMALL -> explodeAndDie()
        }
    }

    override fun hitWater(waterFixture: IFixture) {
        GameLogger.debug(TAG, "Hit water: $waterFixture")
        super.hitWater(waterFixture)
        body.physics.velocity.x = 0f
        // TODO: boulder should sink or float in water?
    }

    override fun onDamageInflictedTo(damageable: IDamageable) {
        if (damageable is IBodyEntity) {
            GameLogger.debug(TAG, "On damage inflicted pairTo: $damageable")
            super.onDamageInflictedTo(damageable)
            when (size) {
                Size.LARGE, Size.MEDIUM -> breakApart(damageable.body)
                Size.SMALL -> explodeAndDie()
            }
        }
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        spawnExplodeDelay.update(delta)
    })

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.physics.applyFrictionX = false
body.physics.applyFrictionY = false
        body.physics.gravity.y = GRAVITY * ConstVals.PPM

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle())
        body.addFixture(bodyFixture)

        val projectileFixture = Fixture(body, FixtureType.PROJECTILE, GameRectangle())
        body.addFixture(projectileFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle())
        body.addFixture(damagerFixture)

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.fixtures.forEach { (_, fixture) ->
                val shape = (fixture as Fixture).rawShape as GameRectangle
                if (fixture.getType() == FixtureType.DAMAGER)
                    shape.setSize(body.width * 1.05f, body.height * 1.05f)
                else shape.set(body)
            }
        }

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(2f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { delta, _sprite ->
            val region = when (size) {
                Size.LARGE -> largeRegion
                Size.MEDIUM -> mediumRegion
                Size.SMALL -> smallRegion
            }
            _sprite.setRegion(region!!)
            _sprite.setOriginCenter()
            _sprite.rotation = when (size) {
                Size.LARGE -> 0f
                Size.MEDIUM -> _sprite.rotation + MEDIUM_ROTATION_SPEED * delta
                Size.SMALL -> _sprite.rotation + SMALL_ROTATION_SPEED * delta
            }
            _sprite.setCenter(body.getCenter())
        }
        return spritesComponent
    }
}
