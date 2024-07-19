package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.engine.common.GameLogger
import com.engine.common.enums.Size
import com.engine.common.extensions.gdxArrayOf
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.getOverlapPushDirection
import com.engine.common.getRandom
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.shapes.IGameShape2D
import com.engine.common.time.Timer
import com.engine.damage.IDamageable
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setCenter
import com.engine.drawables.sprites.setSize
import com.engine.entities.GameEntity
import com.engine.entities.IGameEntity
import com.engine.entities.contracts.IBodyEntity
import com.engine.updatables.UpdatablesComponent
import com.engine.world.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType
import com.megaman.maverick.game.world.getEntity

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

    override var owner: IGameEntity? = null

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
        addComponents(defineProjectileComponents())
        addComponent(defineUpdatablesComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(DrawableShapesComponent(this, debugShapeSuppliers = gdxArrayOf({ body }), debug = true))
    }

    override fun spawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "Spawn props = $spawnProps")
        super.spawn(spawnProps)
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
        kill()
        val disintegration = EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.DISINTEGRATION)
        game.engine.spawn(disintegration!!, props(ConstKeys.POSITION to body.getCenter()))
        if (size == Size.SMALL) requestToPlaySound(SoundAsset.THUMP_SOUND, false)
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
            game.engine.spawn(
                boulder!!, props(
                    ConstKeys.POSITION to body.getCenter(),
                    ConstKeys.SIZE to (if (size == Size.LARGE) Size.MEDIUM else Size.SMALL),
                    ConstKeys.TRAJECTORY to rotatedTrajectory
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
            GameLogger.debug(TAG, "On damage inflicted to: $damageable")
            super.onDamageInflictedTo(damageable)
            when (size) {
                Size.LARGE, Size.MEDIUM -> breakApart(damageable.body)
                Size.SMALL -> explodeAndDie()
            }
        }
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent(this, { delta ->
        spawnExplodeDelay.update(delta)
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
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
                if (fixture.type == FixtureType.DAMAGER)
                    shape.setSize(body.width * 1.05f, body.height * 1.05f)
                else shape.set(body)
            }
        }

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(2f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(this, sprite)
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