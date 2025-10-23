package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.UtilMethods.getOverlapPushDirection
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.orderedMapOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.shapes.IGameShape2D
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.GameEntity
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.contracts.IProjectileEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.explosions.SnowballExplosion
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.world.body.*

class Snowhead(game: MegamanMaverickGame) : AbstractProjectile(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "Snowhead"
        private const val GRAVITY = -0.2f
        private val snowballTrajs = gdxArrayOf(
            Vector2(-6f, 6f), Vector2(-3f, 8f), Vector2(3f, 8f), Vector2(6f, 6f),
        )
        private val bodySizes = objectMapOf(
            ConstKeys.DEFAULT pairTo 1f,
            ConstKeys.ATTACHED pairTo 1f,
            ConstKeys.BLANK pairTo 0.75f
        )
        private val spriteSizes = objectMapOf(
            ConstKeys.DEFAULT pairTo Vector2(2f, 1f),
            ConstKeys.ATTACHED pairTo Vector2(2f, 1f),
            ConstKeys.BLANK pairTo Vector2(1.5f, 0.75f)
        )
        private val animDefs = orderedMapOf(
            ConstKeys.ATTACHED pairTo AnimationDef(3, 1, 0.1f, false),
            ConstKeys.DEFAULT pairTo AnimationDef(),
            ConstKeys.BLANK pairTo AnimationDef()
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    override lateinit var facing: Facing

    lateinit var type: String

    private val tempSnowballsArray = Array<Snowball>()

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.PROJECTILES_2.source)
            animDefs.keys().forEach { key ->
                val region = atlas.findRegion("$TAG/$key")
                regions.put(key, region)
            }
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        type = spawnProps.getOrDefault(ConstKeys.TYPE, ConstKeys.DEFAULT, String::class)

        val bodySize = bodySizes[type]!!
        body.setSize(bodySize * ConstVals.PPM)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        val trajectory = spawnProps.getOrDefault(ConstKeys.TRAJECTORY, Vector2.Zero, Vector2::class)
        body.physics.velocity.set(trajectory)

        body.physics.gravityOn = spawnProps.getOrDefault(ConstKeys.GRAVITY_ON, true, Boolean::class)

        facing = spawnProps.getOrDefault(
            ConstKeys.FACING, if (trajectory.x > 0f) Facing.RIGHT else Facing.LEFT, Facing::class
        )

        owner = spawnProps.get(ConstKeys.OWNER, GameEntity::class)
    }

    private fun bounceBullets(collisionShape: IGameShape2D) {
        val direction = getOverlapPushDirection(body.getBounds(), collisionShape) ?: Direction.UP

        val spawn = when (direction) {
            Direction.UP -> body.getPositionPoint(Position.TOP_CENTER).add(0f, 0.1f * ConstVals.PPM)
            Direction.DOWN -> body.getPositionPoint(Position.BOTTOM_CENTER).sub(0f, 0.1f * ConstVals.PPM)
            Direction.LEFT -> body.getPositionPoint(Position.CENTER_LEFT).sub(0.1f * ConstVals.PPM, 0f)
            Direction.RIGHT -> body.getPositionPoint(Position.CENTER_RIGHT).add(0.1f * ConstVals.PPM, 0f)
        }

        val gravity = GameObjectPools.fetch(Vector2::class).set(0f, GRAVITY * ConstVals.PPM)

        val snowballs = MegaEntityFactory.fetch(snowballTrajs.size, tempSnowballsArray, Snowball::class)
        for (i in 0 until snowballs.size) {
            val trajectory = snowballTrajs[i].cpy()
                .rotateDeg(direction.rotation)
                .scl(ConstVals.PPM.toFloat())

            snowballs[i].spawn(
                props(
                    ConstKeys.OWNER pairTo owner,
                    ConstKeys.POSITION pairTo spawn,
                    ConstKeys.GRAVITY pairTo gravity,
                    ConstKeys.GRAVITY_ON pairTo true,
                    ConstKeys.TRAJECTORY pairTo trajectory
                )
            )
        }

        tempSnowballsArray.clear()

        if (overlapsGameCamera()) playSoundNow(SoundAsset.CHILL_SHOOT_SOUND, false)
    }

    override fun explodeAndDie(vararg params: Any?) {
        destroy()
        val explosion = MegaEntityFactory.fetch(SnowballExplosion::class)!!
        explosion.spawn(props(ConstKeys.POSITION pairTo body.getCenter()))
    }

    override fun hitBlock(blockFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        bounceBullets(blockFixture.getShape())
        explodeAndDie()
    }

    override fun hitWater(waterFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        bounceBullets(waterFixture.getShape())
        explodeAndDie()
    }

    override fun hitShield(shieldFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        if (shieldFixture.getEntity() == owner) return
        bounceBullets(shieldFixture.getShape())
        explodeAndDie()
    }

    override fun hitProjectile(projectileFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        val projectile = projectileFixture.getEntity() as IProjectileEntity
        if (projectile.owner == owner) return
        if (projectile.owner == megaman) explodeAndDie()
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ConstVals.PPM.toFloat())
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.physics.gravity.y = GRAVITY * ConstVals.PPM

        body.preProcess.put(ConstKeys.DEFAULT) {
            val size = bodySizes[type]!!
            body.setSize(size * ConstVals.PPM)
            body.forEachFixture { fixture -> ((fixture as Fixture).rawShape as GameRectangle).set(body) }
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body.getBounds() }), debug = true))

        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.PROJECTILE, FixtureType.DAMAGER))
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 5)))
        .preProcess { _, sprite ->
            val size = GameObjectPools.fetch(Vector2::class)
                .set(spriteSizes[type]!!)
                .scl(ConstVals.PPM.toFloat())
            sprite.setSize(size)
            sprite.setCenter(body.getCenter())
            sprite.setFlip(isFacing(Facing.RIGHT), false)
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier { type }
                .applyToAnimations { animations ->
                    animDefs.forEach { entry ->
                        val key = entry.key
                        val def = entry.value
                        val region = regions[key]
                        try {
                            animations.put(key, Animation(region, def.rows, def.cols, def.durations, def.loop))
                        } catch (e: Exception) {
                            throw IllegalStateException("Failed to create animation for key=$key", e)
                        }
                    }
                }
                .build()
        )
        .build()
}
