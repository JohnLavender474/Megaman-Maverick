package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.animations.Animation
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.UtilMethods.getOverlapPushDirection
import com.maverick.game.engine.common.extensions.getTextureRegion
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.IGameShape2D
import com.maverick.game.engine.drawables.shapes.DrawableShapesComponent
import com.maverick.game.engine.drawables.shapes.IDrawableShape
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.drawables.sprites.setCenter
import com.maverick.game.engine.drawables.sprites.setSize
import com.maverick.game.engine.entities.contracts.IAnimatedEntity
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.explosions.MagmaGoopExplosion
import com.megaman.maverick.game.entities.hazards.SmallIceCube
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.extensions.toGdxRectangle
import com.megaman.maverick.game.utils.misc.DirectionPositionMapper
import com.megaman.maverick.game.world.body.*

class MagmaGoop(services: MegaServices) : AbstractProjectile(services), IAnimatedEntity {

    companion object {
        const val TAG = "MagmaGoop"
        private const val START_ROTATION = 135f
        private var region: TextureRegion? = null
    }

    private val trajectory = Vector2()
    private var rotation = 0f
    private var bounces = 0

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        if (region == null) region = services.assets.getTextureRegion(TextureAsset.PROJECTILES_2.source, TAG)
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        trajectory.set(spawnProps.get(ConstKeys.TRAJECTORY, Vector2::class)!!)
        rotation = spawnProps.get(ConstKeys.ROTATION, Float::class)!!
        bounces = 0
    }

    override fun hitBlock(blockFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) =
        explodeAndDie(thisShape, otherShape)

    override fun hitShield(shieldFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        explodeAndDie(thisShape, otherShape)
        requestToPlaySound(SoundAsset.DINK_SOUND, false)
    }

    override fun hitWater(waterFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        destroy()
        services.audio.playSound(SoundAsset.WHOOSH_SOUND, false)
    }

    override fun hitProjectile(projectileFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        val projectile = projectileFixture.getEntity()
        if (projectile is SmallIceCube) {
            destroy()
            services.audio.playSound(SoundAsset.WHOOSH_SOUND, false)
        }
    }

    override fun explodeAndDie(vararg params: Any?) {
        GameLogger.debug(TAG, "explodeAndDie(): params=$params")
        destroy()
        try {
            val thisShape = params[0] as IGameShape2D
            val otherShape = params[1] as IGameShape2D

            val direction = getOverlapPushDirection(thisShape, otherShape) ?: services.gravity.direction
            val position = DirectionPositionMapper.getPosition(direction)

            val overlap = GameObjectPools.fetch(Rectangle::class)
            val overlapping = Intersector.intersectRectangles(
                thisShape.toGdxRectangle(),
                otherShape.toGdxRectangle(),
                overlap
            )

            val spawn = if (overlapping) overlap.getPositionPoint(position) else thisShape.getCenter()
            GameLogger.debug(
                TAG,
                "explodeAndDie(): spawn=$spawn, overlapping=$overlapping, overlap=$overlap, " +
                    "direction=$direction, position=$position"
            )

            val explosion = MegaEntityFactory.fetch(MagmaGoopExplosion::class)!!
            explosion.spawn(props(ConstKeys.POSITION pairTo spawn, ConstKeys.DIRECTION pairTo direction))
        } catch (e: Exception) {
            GameLogger.error(TAG, "Error while exploding magma goop", e)
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.85f * ConstVals.PPM, 0.65f * ConstVals.PPM)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false

        body.preProcess.put(ConstKeys.DEFAULT) {
            if (services.camera.rotating) body.physics.velocity.setZero()
            else body.physics.velocity.set(trajectory)
        }

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.PROJECTILE, FixtureType.DAMAGER))
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 10))
        sprite.setSize(ConstVals.PPM.toFloat())
        val component = SpritesComponent(sprite)
        component.putPreProcess { _, _ ->
            sprite.setCenter(body.getCenter())
            sprite.setOriginCenter()
            sprite.rotation = START_ROTATION + rotation
        }
        return component
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(region!!, 2, 1, 0.1f, true)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }
}
