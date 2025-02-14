package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.extensions.equalsAny
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Loop
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
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.*

class SniperJoeShield(game: MegamanMaverickGame) : AbstractProjectile(game), IFaceable {

    override var owner: IGameEntity? = null

    companion object {
        const val TAG = "SniperJoeShield"
        const val ORANGE_TYPE = "Orange"
        const val BLUE_TYPE = "Blue"
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
            orangeRegion = atlas.findRegion("$TAG/$ORANGE_TYPE")
            blueRegion = atlas.findRegion("$TAG/$BLUE_TYPE")
        }
        super.init()
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "Spawn props = $spawnProps")
        super.onSpawn(spawnProps)
        val spawn =
            if (spawnProps.containsKey(ConstKeys.BOUNDS))
                spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
            else spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)
        trajectory = spawnProps.get(ConstKeys.TRAJECTORY, Vector2::class)!!
        owner = spawnProps.get(ConstKeys.OWNER, GameEntity::class)!!
        type = spawnProps.getOrDefault(ConstKeys.TYPE, ORANGE_TYPE, String::class)
        facing = Facing.valueOf(spawnProps.getOrDefault(ConstKeys.FACING, "left", String::class).uppercase())
        rotationTimer.reset()
        thrownRotations.reset()
    }

    override fun explodeAndDie(vararg params: Any?) {
        destroy()
        val explosion = EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.EXPLOSION)!!
        explosion.spawn(
            props(
                ConstKeys.POSITION pairTo body.getCenter(),
                ConstKeys.SOUND pairTo SoundAsset.EXPLOSION_2_SOUND
            )
        )
    }

    override fun onDamageInflictedTo(damageable: IDamageable) = explodeAndDie()

    override fun onDestroy() {
        super.onDestroy()
        GameLogger.debug(TAG, "Destroyed")
    }

    override fun hitBlock(blockFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) = explodeAndDie()

    override fun hitBody(bodyFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        if (bodyFixture.getEntity() != owner) explodeAndDie()
    }

    override fun hitShield(shieldFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        if (shieldFixture.getEntity() != owner) explodeAndDie()
    }

    private fun defineUpdatablesComponent(): UpdatablesComponent = UpdatablesComponent({
        rotationTimer.update(it)
        if (rotationTimer.isFinished()) {
            thrownRotations.next()
            rotationTimer.reset()
        }
    })

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.5f * ConstVals.PPM, ConstVals.PPM.toFloat())
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false

        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body.getBounds() }), debug = true))

        val bodyShapes = Array<IGameShape2D>()

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle())
        body.addFixture(bodyFixture)

        val projectileFixture = Fixture(body, FixtureType.PROJECTILE, GameRectangle())
        body.addFixture(projectileFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle())
        body.addFixture(damagerFixture)

        val shieldFixture = Fixture(body, FixtureType.SHIELD, GameRectangle())
        body.addFixture(shieldFixture)

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.physics.velocity.set(trajectory)

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
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.setRegion(
                when (type) {
                    ORANGE_TYPE -> orangeRegion!!
                    BLUE_TYPE -> blueRegion!!
                    else -> throw IllegalArgumentException("Invalid type: $type")
                }
            )
            sprite.setCenter(body.getCenter())
            sprite.setFlip(facing == Facing.LEFT, false)
            sprite.setOriginCenter()
            sprite.rotation = thrownRotations.getCurrent()
        }
        return spritesComponent
    }
}
