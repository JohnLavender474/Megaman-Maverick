package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
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
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.common.shapes.GameRectangle
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
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.explosions.MagmaExplosion
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.*

class MagmaMeteor(game: MegamanMaverickGame) : AbstractProjectile(game), IAnimatedEntity, IDirectional {

    companion object {
        const val TAG = "MagmaMeteor"
        private const val DEFAULT_CULL_TIME = 2f
        private const val METEOR_SPEED = 10f
        private const val SPRITE_ROTATION_OFFSET = 135f
        private const val MAX_METEORS = 10
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

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.PROJECTILES_2.source, TAG)
        super.init()
        addComponent(defineUpdatablesComponent())
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

        val rawCollideBounds = spawnProps.get("${ConstKeys.COLLIDE}_${ConstKeys.BODIES}")
        collideBodies = when (rawCollideBounds) {
            is IBodyEntity -> gdxArrayOf(rawCollideBounds)
            is Array<*> -> rawCollideBounds as Array<IBodyEntity>
            else -> null
        }

        val meteors = MegaGameEntities.getOfTag(TAG)
        if (meteors.size > MAX_METEORS) {
            val iter = meteors.iterator()
            while (iter.hasNext) {
                val meteor = iter.next() as MagmaMeteor
                if (meteor.dead) continue
                meteor.explodeAndDie()
                break
            }
        }
    }

    override fun hitBlock(
        blockFixture: IFixture,
        thisShape: IGameShape2D,
        otherShape: IGameShape2D
    ) {
        GameLogger.debug(TAG, "hitBlock(): blockFixture=$blockFixture, thisShape=$thisShape, otherShape=$otherShape")
        val shouldExplode = collideBodies?.contains(blockFixture.getEntity() as IBodyEntity) ?: true
        if (shouldExplode) explodeAndDie()
    }

    override fun explodeAndDie(vararg params: Any?) {
        GameLogger.debug(TAG, "explodeAndDie()")
        destroy()
        val explosion = MegaEntityFactory.fetch(MagmaExplosion::class)!!
        explosion.spawn(props(ConstKeys.POSITION pairTo body.getCenter()))
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({
        if (game.isCameraRotating()) body.physics.velocity.setZero()
        else body.physics.velocity.set(0f, METEOR_SPEED * ConstVals.PPM).rotateDeg(rotation)
    })

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ConstVals.PPM.toFloat())
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }
        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        val shieldFixture = Fixture(body, FixtureType.SHIELD, GameCircle().setRadius(0.5f * ConstVals.PPM))
        body.addFixture(shieldFixture)
        shieldFixture.drawingColor = Color.BLUE
        debugShapes.add { shieldFixture }

        val feetFixture = Fixture(
            body,
            FixtureType.FEET,
            GameRectangle().setSize(0.5f * ConstVals.PPM, 0.1f * ConstVals.PPM)
        )
        feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        body.addFixture(feetFixture)
        feetFixture.drawingColor = Color.GREEN
        debugShapes.add { feetFixture }

        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.PROJECTILE, FixtureType.DAMAGER)
        )
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 10))
        sprite.setSize(2f * ConstVals.PPM)
        val component = SpritesComponent(sprite)
        component.putPreProcess { _, _ ->
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
        return component
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(region!!, 2, 1, 0.1f, true)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }
}
