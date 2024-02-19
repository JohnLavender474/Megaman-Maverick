package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.engine.common.GameLogger
import com.engine.common.enums.Facing
import com.engine.common.extensions.equalsAny
import com.engine.common.extensions.gdxArrayOf
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.interfaces.IFaceable
import com.engine.common.objects.Loop
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.shapes.IGameShape2D
import com.engine.common.time.Timer
import com.engine.damage.IDamageable
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setSize
import com.engine.entities.GameEntity
import com.engine.entities.IGameEntity
import com.engine.updatables.UpdatablesComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.IProjectileEntity
import com.megaman.maverick.game.entities.contracts.defineProjectileComponents
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType

class SniperJoeShield(game: MegamanMaverickGame) : GameEntity(game), IProjectileEntity, IFaceable {

    override var owner: IGameEntity? = null

    companion object {
        const val TAG = "SniperJoeShield"
        const val ORANGE_TYPE = "orange"
        const val BLUE_TYPE = "blue"
        private const val ROTATION_DURATION = 0.1f
        private var orangeRegion: TextureRegion? = null
        private var blueRegion: TextureRegion? = null
    }

    override lateinit var facing: Facing

    private lateinit var type: String
    private lateinit var trajectory: Vector2

    private val rotationTimer = Timer(ROTATION_DURATION)
    private val thrownRotations = Loop(gdxArrayOf(0f, 90f, 180f, 270f))

    override fun init() {
        if (orangeRegion == null || blueRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ITEMS_1.source)
            orangeRegion = atlas.findRegion("SniperJoeShield/Orange")
            blueRegion = atlas.findRegion("SniperJoeShield/Blue")
        }
        defineProjectileComponents().forEach { addComponent(it) }
        addComponent(defineUpdatablesComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
    }

    override fun spawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "Spawn props = $spawnProps")
        super.spawn(spawnProps)
        val spawn =
            if (spawnProps.containsKey(ConstKeys.BOUNDS)) spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
                .getCenter()
            else spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)
        trajectory = spawnProps.get(ConstKeys.TRAJECTORY, Vector2::class)!!
        owner = spawnProps.get(ConstKeys.OWNER, IGameEntity::class)!!
        type = spawnProps.getOrDefault(ConstKeys.TYPE, ORANGE_TYPE, String::class)
        facing = Facing.valueOf(spawnProps.getOrDefault(ConstKeys.FACING, "left", String::class).uppercase())
        rotationTimer.reset()
        thrownRotations.reset()
    }

    override fun explodeAndDie() {
        kill()
        val explosion = EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.EXPLOSION)!!
        game.gameEngine.spawn(
            explosion, props(ConstKeys.POSITION to body.getCenter(), ConstKeys.SOUND to SoundAsset.EXPLOSION_2_SOUND)
        )
    }

    override fun onDamageInflictedTo(damageable: IDamageable) = explodeAndDie()

    override fun onDestroy() {
        super<GameEntity>.onDestroy()
        GameLogger.debug(TAG, "Destroyed")
    }

    private fun defineUpdatablesComponent(): UpdatablesComponent = UpdatablesComponent(this, {
        rotationTimer.update(it)
        if (rotationTimer.isFinished()) {
            thrownRotations.next()
            rotationTimer.reset()
        }
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.5f * ConstVals.PPM, ConstVals.PPM.toFloat())

        addComponent(DrawableShapesComponent(this, debugShapeSuppliers = gdxArrayOf({ body }), debug = true))

        val bodyShapes = Array<IGameShape2D>()
        bodyShapes.add(body)

        // body fixture
        val bodyFixture = Fixture(GameRectangle(), FixtureType.BODY)
        body.addFixture(bodyFixture)
        bodyShapes.add(bodyFixture.shape)

        // damagerFixture
        val damagerFixture = Fixture(GameRectangle(), FixtureType.DAMAGER)
        body.addFixture(damagerFixture)
        bodyShapes.add(damagerFixture.shape)

        // shield fixture
        val shieldFixture = Fixture(GameRectangle(), FixtureType.SHIELD)
        body.addFixture(shieldFixture)
        bodyShapes.add(shieldFixture.shape)

        // pre-process
        body.preProcess.put(ConstKeys.DEFAULT) {
            body.physics.velocity = trajectory

            val rotated = thrownRotations.getCurrent().equalsAny(1, 3)
            val size = if (rotated) Vector2(ConstVals.PPM.toFloat(), 0.5f * ConstVals.PPM)
            else Vector2(0.5f * ConstVals.PPM, ConstVals.PPM.toFloat())

            bodyShapes.forEach {
                it as GameRectangle
                it.setSize(size)
            }
        }

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(ConstVals.PPM.toFloat())

        val spritesComponent = SpritesComponent(this, TAG to sprite)
        spritesComponent.putUpdateFunction(TAG) { _, _sprite ->
            _sprite as GameSprite
            _sprite.setRegion(
                when (type) {
                    ORANGE_TYPE -> orangeRegion!!
                    BLUE_TYPE -> blueRegion!!
                    else -> throw IllegalArgumentException("Invalid type: $type")
                }
            )
            val center = body.getCenter()
            _sprite.setCenter(center.x, center.y)
            _sprite.setFlip(facing == Facing.LEFT, false)
            _sprite.setOriginCenter()
            _sprite.rotation = thrownRotations.getCurrent()
        }

        return spritesComponent
    }
}