package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.IGameShape2D
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.toGdxRectangle
import com.megaman.maverick.game.world.body.*

class MagmaMeteor(game: MegamanMaverickGame) : AbstractProjectile(game), IAnimatedEntity, IDirectional {

    companion object {
        const val TAG = "MagmaMeteor"
        private const val DEFAULT_CULL_TIME = 0.5f
        private const val METEOR_SPEED = 10f
        private const val SPRITE_ROTATION_OFFSET = 135f
        private var region: TextureRegion? = null
    }

    // when direction is
    //  - up: rotation = 0
    //  - down: rotation = 180
    //  - left: rotation = 135 (diagonal left)
    //  - right: rotation = 225 (diagonal right)
    override var direction: Direction
        get() = body.direction
        set(value) {
            body.direction = value
        }

    private val rotation: Float
        get() = when (direction) {
            Direction.UP -> 0f
            Direction.DOWN -> 180f
            Direction.LEFT -> 135f
            Direction.RIGHT -> 225f
        }
    private var collideBodies: Array<IBodyEntity>? = null

    override fun init() {
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.PROJECTILES_2.source, TAG)
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.putIfAbsent(ConstKeys.CULL_TIME, DEFAULT_CULL_TIME)

        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")

        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        val rawDirection = spawnProps.get(ConstKeys.DIRECTION)!!
        direction = when (rawDirection) {
            is String -> Direction.valueOf(rawDirection.uppercase())
            is Direction -> rawDirection
            else -> {
                GameLogger.error(TAG, "onSpawn(): invalid direction value: $rawDirection")
                Direction.LEFT // return left by default on error
            }
        }

        body.physics.velocity.set(0f, METEOR_SPEED * ConstVals.PPM).rotateDeg(rotation)

        val rawCollideBounds = spawnProps.get("${ConstKeys.COLLIDE}_${ConstKeys.BODIES}")
        collideBodies = when (rawCollideBounds) {
            is IBodyEntity -> gdxArrayOf(rawCollideBounds)
            is Array<*> -> rawCollideBounds as Array<IBodyEntity>
            else -> null
        }
    }

    override fun hitBlock(
        blockFixture: IFixture,
        thisShape: IGameShape2D,
        otherShape: IGameShape2D
    ) {
        GameLogger.debug(TAG, "hitBlock(): blockFixture=$blockFixture, thisShape=$thisShape, otherShape=$otherShape")

        var shouldExplode = true
        collideBodies?.let { shouldExplode = it.contains(blockFixture.getEntity() as IBodyEntity) }
        if (shouldExplode) explodeAndDie(thisShape, otherShape)
    }

    override fun explodeAndDie(vararg params: Any?) {
        GameLogger.debug(TAG, "explodeAndDie(): params=$params")

        destroy()

        val thisShape = params[0] as IGameShape2D
        val otherShape = params[1] as IGameShape2D
        val overlap = GameObjectPools.fetch(Rectangle::class)

        val spawn = when {
            Intersector.intersectRectangles(
                thisShape.toGdxRectangle(),
                otherShape.toGdxRectangle(),
                overlap
            ) -> overlap.getCenter()

            else -> thisShape.getCenter()
        }

        val offset = GameObjectPools.fetch(Vector2::class)
        when (direction) {
            Direction.LEFT -> offset.set(-overlap.width / 2f, 0f)
            Direction.RIGHT -> offset.set(overlap.width / 2f, 0f)
            else -> offset.setZero()
        }
        spawn.add(offset)

        val explosion = EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.MAGMA_EXPLOSION)!!
        explosion.spawn(props(ConstKeys.POSITION pairTo spawn))
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ConstVals.PPM.toFloat())
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.PROJECTILE, FixtureType.DAMAGER)
        )
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 10))
        sprite.setSize(2f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.setOriginCenter()
            sprite.rotation = rotation + SPRITE_ROTATION_OFFSET

            val position = when (direction) {
                Direction.UP -> Position.TOP_CENTER
                Direction.LEFT -> Position.BOTTOM_LEFT
                Direction.DOWN -> Position.BOTTOM_CENTER
                Direction.RIGHT -> Position.BOTTOM_RIGHT
            }
            sprite.setPosition(body.getPositionPoint(position), position)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(region!!, 2, 1, 0.1f, true)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }
}
