package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods.getOverlapPushDirection
import com.mega.game.engine.common.UtilMethods.getSingleMostDirectionFromStartToTarget
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.IGameShape2D
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.contracts.IFireEntity
import com.megaman.maverick.game.entities.explosions.MagmaExplosion
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.*

class SpitFireball(game: MegamanMaverickGame) : AbstractProjectile(game), IFireEntity, IAnimatedEntity {

    companion object {
        const val TAG = "SpitFireball"
        private const val FIREBALLS_TO_SPAWN = 2
        private const val FIREBALL_IMPULSE = 3f
        private const val FIREBALL_GRAVITY = -0.15f
        private const val FIREBALL_CULL_TIME = 0.15f
        private val angles = ObjectMap<Direction, Array<Float>>()
        private var region: TextureRegion? = null
    }

    private val blockIds = ObjectSet<Int>()

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.PROJECTILES_2.source, TAG)
        if (angles.isEmpty) {
            angles.put(Direction.UP, gdxArrayOf(45f, 315f))
            angles.put(Direction.LEFT, gdxArrayOf(45f, 135f))
            angles.put(Direction.DOWN, gdxArrayOf(135f, 225f))
            angles.put(Direction.RIGHT, gdxArrayOf(225f, 315f))
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        val trajectory = spawnProps.get(ConstKeys.TRAJECTORY, Vector2::class)!!
        body.physics.velocity.set(trajectory)

        (spawnProps.get(ConstKeys.BLOCKS) as Iterable<Int>).forEach { id -> blockIds.add(id) }
    }

    override fun hitBlock(blockFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        val id = blockFixture.getEntity().id
        if (blockIds.isEmpty || blockIds.contains(id)) {
            GameLogger.debug(TAG, "hitBlock(): id=$id, blockIds=$blockIds")
            explodeAndDie(thisShape, otherShape)
        } else GameLogger.debug(TAG, "hitBlock(): ignore hit by block: id=$id, blockIds=$blockIds")
    }

    override fun hitShield(shieldFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        val shieldEntity = shieldFixture.getEntity()
        if (shieldEntity is Axe) return

        val explosionDamager = shieldEntity !is Megaman

        explodeAndDie(thisShape, otherShape, explosionDamager)
    }

    override fun explodeAndDie(vararg params: Any?) {
        GameLogger.debug(TAG, "explodeAndDie(): params=$params")

        destroy()

        val explosion = MegaEntityFactory.fetch(MagmaExplosion::class)!!
        explosion.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.POSITION pairTo body.getCenter(),
                ConstKeys.ACTIVE pairTo if (params.size >= 3) params[2] as Boolean else true,
            )
        )

        val thisShape = params[0] as IGameShape2D
        val otherShape = params[1] as IGameShape2D
        val direction = getOverlapPushDirection(thisShape, otherShape) ?: getSingleMostDirectionFromStartToTarget(
            thisShape.getCenter(),
            otherShape.getCenter()
        )

        for (i in 0 until FIREBALLS_TO_SPAWN) {
            val angle = angles[direction]!![i]

            val trajectory = GameObjectPools.fetch(Vector2::class)
                .set(0f, FIREBALL_IMPULSE * ConstVals.PPM)
                .rotateDeg(angle)

            val gravity = GameObjectPools.fetch(Vector2::class)
                .set(0f, FIREBALL_GRAVITY * ConstVals.PPM)

            val fireball = MegaEntityFactory.fetch(Fireball::class)!!
            fireball.spawn(
                props(
                    ConstKeys.OWNER pairTo owner,
                    ConstKeys.GRAVITY pairTo gravity,
                    ConstKeys.TRAJECTORY pairTo trajectory,
                    ConstKeys.POSITION pairTo body.getCenter(),
                    ConstKeys.CULL_TIME pairTo FIREBALL_CULL_TIME
                )
            )
        }
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        blockIds.clear()
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.setSize(1.25f * ConstVals.PPM, ConstVals.PPM.toFloat())

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.PROJECTILE, FixtureType.DAMAGER))
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(
            TAG, GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 5))
                .also { sprite -> sprite.setSize(2f * ConstVals.PPM) })
        .preProcess { _, sprite ->
            sprite.setCenter(body.getCenter())
            sprite.setOriginCenter()
            sprite.rotation = body.physics.velocity.angleDeg()
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG).animator(Animator(Animation(region!!, 2, 1, 0.05f, true))).build()
}
