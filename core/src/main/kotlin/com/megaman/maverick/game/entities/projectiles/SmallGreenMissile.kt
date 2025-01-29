package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.IGameShape2D
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.explosions.SpreadExplosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.world.body.*
import kotlin.math.abs

class SmallGreenMissile(game: MegamanMaverickGame) : AbstractProjectile(game), IDirectional {

    companion object {
        const val TAG = "SmallGreenMissile"
        const val WAVE_EXPLOSION = "wave_explosion"
        const val DEFAULT_EXPLOSION = "default_explosion"
        private const val GRAVITY = 0.15f
        private var region: TextureRegion? = null
    }

    override var direction: Direction
        get() = body.direction
        set(value) {
            body.direction = value
        }

    private lateinit var explosionType: String

    override fun init() {
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.PROJECTILES_2.source, TAG)
        super.init()
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        direction = spawnProps.getOrDefault(ConstKeys.DIRECTION, Direction.DOWN, Direction::class)

        val gravityOn = spawnProps.getOrDefault(ConstKeys.GRAVITY_ON, true, Boolean::class)
        body.physics.gravityOn = gravityOn

        val trajectory = spawnProps.getOrDefault(ConstKeys.TRAJECTORY, Vector2.Zero, Vector2::class)
        body.physics.velocity.set(trajectory)

        explosionType = spawnProps.getOrDefault(ConstKeys.EXPLOSION, DEFAULT_EXPLOSION, String::class)
    }

    override fun onDamageInflictedTo(damageable: IDamageable) {
        if (explosionType == DEFAULT_EXPLOSION && damageable is IBodyEntity) explodeAndDie()
    }

    override fun hitBlock(blockFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) = explodeAndDie()

    override fun hitSand(sandFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) = explodeAndDie()

    override fun hitShield(shieldFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        val left = body.getX() < shieldFixture.getShape().getX()
        body.physics.velocity.x = if (left) -abs(body.physics.velocity.x) else abs(body.physics.velocity.x)

        requestToPlaySound(SoundAsset.DINK_SOUND, false)
    }

    override fun explodeAndDie(vararg params: Any?) {
        destroy()

        when (explosionType) {
            DEFAULT_EXPLOSION -> {
                val explosion = EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.EXPLOSION)!!
                explosion.spawn(props(ConstKeys.OWNER pairTo owner, ConstKeys.POSITION pairTo body.getCenter()))

                if (overlapsGameCamera()) playSoundNow(SoundAsset.EXPLOSION_2_SOUND, false)
            }

            WAVE_EXPLOSION -> {
                val explosion = MegaEntityFactory.fetch(SpreadExplosion::class)!!
                explosion.spawn(
                    props(
                        ConstKeys.OWNER pairTo owner,
                        ConstKeys.POSITION pairTo body.getPositionPoint(Position.BOTTOM_CENTER)
                    )
                )

                if (overlapsGameCamera()) playSoundNow(SoundAsset.EXPLOSION_2_SOUND, false)
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.5f * ConstVals.PPM, 0.75f * ConstVals.PPM)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        body.preProcess.put(ConstKeys.DEFAULT) {
            val gravity = body.physics.gravity
            when (direction) {
                Direction.UP -> gravity.set(0f, -GRAVITY)
                Direction.DOWN -> gravity.set(0f, GRAVITY)
                Direction.LEFT -> gravity.set(GRAVITY, 0f)
                Direction.RIGHT -> gravity.set(-GRAVITY, 0f)
            }.scl(ConstVals.PPM.toFloat())
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this, body, BodyFixtureDef.of(FixtureType.PROJECTILE, FixtureType.DAMAGER, FixtureType.SHIELD)
        )
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(region!!, DrawingPriority(DrawingSection.PLAYGROUND, 10))
        sprite.setSize(ConstVals.PPM.toFloat())
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.setOriginCenter()
            sprite.rotation = body.physics.velocity.angleDeg() + 270f
            sprite.setCenter(body.getCenter())
        }
        return spritesComponent
    }
}
