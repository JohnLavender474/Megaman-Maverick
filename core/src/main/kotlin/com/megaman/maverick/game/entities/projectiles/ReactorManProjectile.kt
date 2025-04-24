package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods.getOverlapPushDirection
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.orderedMapOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.shapes.IGameShape2D
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.GameEntity
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.world.body.*

class ReactorManProjectile(game: MegamanMaverickGame) : AbstractProjectile(game), IAnimatedEntity {

    companion object {
        const val TAG = "ReactorManProjectile"

        private const val BIG_GRAVITY = -0.05f
        private const val SMALL_GRAVITY = -0.15f

        private const val GROW_DUR = 0.4f
        private const val DIE_DUR = 0.05f

        private const val BIG_SIZE = 1f
        private const val SMALL_SIZE = 0.5f

        private val shatterTrajectories = objectMapOf(
            Direction.UP pairTo gdxArrayOf(
                Vector2(5f, 9f),
                Vector2(0f, 9f),
                Vector2(-5f, 9f)
            ),
            Direction.DOWN pairTo gdxArrayOf(
                Vector2(5f, -9f),
                Vector2(0f, -9f),
                Vector2(-5f, -9f)
            ),
            Direction.LEFT pairTo gdxArrayOf(
                Vector2(-9f, 5f),
                Vector2(-9f, 0f),
                Vector2(-9f, -5f)
            ),
            Direction.RIGHT pairTo gdxArrayOf(
                Vector2(9f, 5f),
                Vector2(9f, 0f),
                Vector2(9f, -5f)
            )
        )

        private val animDefs = orderedMapOf(
            "big" pairTo AnimationDef(1, 3, 0.1f, true),
            "small" pairTo AnimationDef(2, 2, 0.1f, true),
            "die" pairTo AnimationDef(1, 2, 0.05f, false)
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    var active = false

    private var big = false

    private val growTimer = Timer(GROW_DUR)

    private val dyingTimer = Timer(DIE_DUR)
    private var dying = false

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.PROJECTILES_1.source)
            animDefs.keys().forEach { key -> regions.put(key, atlas.findRegion("$TAG/$key")) }
        }
        super.init()
        addComponent(defineUpdatablesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        owner = spawnProps.get(ConstKeys.OWNER, GameEntity::class)

        big = spawnProps.get(ConstKeys.BIG, Boolean::class)!!

        val grow = spawnProps.getOrDefault(ConstKeys.GROW, false, Boolean::class)
        if (big && grow) growTimer.reset() else growTimer.setToEnd()

        val size = if (big && !grow) BIG_SIZE else SMALL_SIZE
        body.setSize(size * ConstVals.PPM)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        val trajectory = spawnProps.getOrDefault(ConstKeys.TRAJECTORY, Vector2.Zero, Vector2::class)
        body.physics.velocity.set(trajectory)

        val gravityOn = spawnProps.getOrDefault(ConstKeys.GRAVITY_ON, false, Boolean::class)
        body.physics.gravityOn = gravityOn

        active = spawnProps.getOrDefault(ConstKeys.ACTIVE, false, Boolean::class)

        dyingTimer.reset()
        dying = false
    }

    fun setTrajectory(trajectory: Vector2) {
        body.physics.velocity.set(trajectory)
    }

    override fun hitBlock(blockFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        if (!active) return
        if (big) shatter(blockFixture.getShape())
        explodeAndDie()
    }

    override fun explodeAndDie(vararg params: Any?) {
        when {
            big -> destroy()
            else -> {
                body.physics.velocity.setZero()
                body.physics.gravityOn = false
                dying = true
            }
        }
    }

    private fun shatter(shape: IGameShape2D) {
        playSoundNow(SoundAsset.BURST_SOUND, false)

        val direction = getOverlapPushDirection(body.getBounds(), shape) ?: Direction.UP

        shatterTrajectories.get(direction).forEach { trajectory ->
            val projectile = MegaEntityFactory.fetch(ReactorManProjectile::class)!!
            projectile.spawn(
                props(
                    ConstKeys.POSITION pairTo when (direction) {
                        Direction.UP -> body.getPositionPoint(Position.TOP_CENTER)
                        Direction.DOWN -> body.getPositionPoint(Position.BOTTOM_CENTER)
                        Direction.LEFT -> body.getPositionPoint(Position.CENTER_LEFT)
                        Direction.RIGHT -> body.getPositionPoint(Position.CENTER_RIGHT)
                    },
                    ConstKeys.BIG pairTo false,
                    ConstKeys.OWNER pairTo owner,
                    ConstKeys.ACTIVE pairTo true,
                    ConstKeys.GRAVITY_ON pairTo true,
                    ConstKeys.TRAJECTORY pairTo trajectory.cpy().scl(ConstVals.PPM.toFloat()),
                )
            )
        }
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        growTimer.update(delta)

        if (dying) {
            dyingTimer.update(delta)
            if (dyingTimer.isFinished()) destroy()
        }
    })

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.physics.gravity.y = ConstVals.PPM * if (big && growTimer.isFinished()) BIG_GRAVITY else SMALL_GRAVITY

            val size = if (big && growTimer.isFinished()) BIG_SIZE else SMALL_SIZE
            val center = body.getCenter()
            body.setSize(size * ConstVals.PPM)
            body.setCenter(center)

            body.forEachFixture {
                val shape = (it as Fixture).rawShape as GameRectangle
                shape.setSize(size * ConstVals.PPM)
            }
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body.getBounds() }), debug = true))

        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.DAMAGER, FixtureType.PROJECTILE))
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(
            TAG, GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 5))
                .also { sprite -> sprite.setSize(ConstVals.PPM.toFloat()) }
        )
        .updatable { _, sprite -> sprite.setCenter(body.getCenter()) }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier {
                    when {
                        big && growTimer.isFinished() -> "big"
                        !dying -> "small"
                        else -> "die"
                    }
                }
                .applyToAnimations { animations ->
                    animDefs.forEach { entry ->
                        val key = entry.key
                        val (rows, cols, durations, loop) = entry.value
                        animations.put(key, Animation(regions[key], rows, cols, durations, loop))
                    }
                }
                .build()
        )
        .build()
}
