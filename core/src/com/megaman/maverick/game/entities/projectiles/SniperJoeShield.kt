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
import com.engine.drawables.sprites.setCenter
import com.engine.drawables.sprites.setSize
import com.engine.entities.IGameEntity
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
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType
import com.megaman.maverick.game.world.getEntity

class SniperJoeShield(game: MegamanMaverickGame) : AbstractProjectile(game), IFaceable {

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
        super.init()
        addComponent(defineUpdatablesComponent())
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

    override fun explodeAndDie(vararg params: Any?) {
        kill()
        val explosion = EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.EXPLOSION)!!
        game.engine.spawn(
            explosion, props(ConstKeys.POSITION to body.getCenter(), ConstKeys.SOUND to SoundAsset.EXPLOSION_2_SOUND)
        )
    }

    override fun onDamageInflictedTo(damageable: IDamageable) = explodeAndDie()

    override fun onDestroy() {
        super.onDestroy()
        GameLogger.debug(TAG, "Destroyed")
    }

    override fun hitBlock(blockFixture: IFixture) = explodeAndDie()

    override fun hitBody(bodyFixture: IFixture) {
        if (bodyFixture.getEntity() != owner) explodeAndDie()
    }

    override fun hitShield(shieldFixture: IFixture) {
        if (shieldFixture.getEntity() != owner) explodeAndDie()
    }

    private fun defineUpdatablesComponent(): UpdatablesComponent = UpdatablesComponent(this, {
        rotationTimer.update(it)
        if (rotationTimer.isFinished()) {
            thrownRotations.next()
            rotationTimer.reset()
        }
    })

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.5f * ConstVals.PPM, ConstVals.PPM.toFloat())

        addComponent(DrawableShapesComponent(this, debugShapeSuppliers = gdxArrayOf({ body }), debug = true))

        val bodyShapes = Array<IGameShape2D>()
        bodyShapes.add(body)

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle())
        body.addFixture(bodyFixture)

        val projectileFixture = Fixture(body, FixtureType.PROJECTILE, GameRectangle())
        body.addFixture(projectileFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle())
        body.addFixture(damagerFixture)

        val shieldFixture = Fixture(body, FixtureType.SHIELD, GameRectangle())
        body.addFixture(shieldFixture)

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.physics.velocity = trajectory

            val rotated = thrownRotations.getCurrent().equalsAny(1, 3)
            val size = if (rotated) Vector2(0.75f * ConstVals.PPM, 0.5f * ConstVals.PPM)
            else Vector2(0.5f * ConstVals.PPM, 0.75f * ConstVals.PPM)

            bodyShapes.forEach {
                it as GameRectangle
                it.setSize(size)
            }
        }

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(ConstVals.PPM.toFloat())
        val spritesComponent = SpritesComponent(this, sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setRegion(
                when (type) {
                    ORANGE_TYPE -> orangeRegion!!
                    BLUE_TYPE -> blueRegion!!
                    else -> throw IllegalArgumentException("Invalid type: $type")
                }
            )
            _sprite.setCenter(body.getCenter())
            _sprite.setFlip(facing == Facing.LEFT, false)
            _sprite.setOriginCenter()
            _sprite.rotation = thrownRotations.getCurrent()
        }
        return spritesComponent
    }
}