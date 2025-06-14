package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods.getOverlapPushDirection
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.isAny
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.common.shapes.IGameShape2D
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.blocks.AbstractBlock
import com.megaman.maverick.game.entities.blocks.LadderTop
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.world.body.*

class MoonScythe(game: MegamanMaverickGame) : AbstractProjectile(game), IAnimatedEntity {

    companion object {
        const val TAG = "MoonScythe"
        private const val ROTATIONS_PER_SEC = 2.5f
        private const val FADE_DUR = 0.25f
        private const val MAX_BOUNCES = 4
        private const val SPAWN_TRAIL_DELAY = 0.1f
        private const val DEBUG_FADING = false
        private val BLOCK_FILTERS = gdxArrayOf(AbstractBlock::class, LadderTop::class)
        private var region: TextureRegion? = null
    }

    private val fadeTimer = Timer(FADE_DUR)
    private val spawnTrailDelay = Timer(SPAWN_TRAIL_DELAY)

    private val trajectory = Vector2()

    private val shouldDebug: Boolean
        get() = !fade || DEBUG_FADING

    private var fade = false
    private var rotation = 0f
    private var bounces = 0

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.PROJECTILES_2.source, TAG)
        super.init()
        addComponent(defineUpdatablesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        fade = spawnProps.getOrDefault(ConstKeys.FADE, false, Boolean::class)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        fadeTimer.reset()
        spawnTrailDelay.reset()

        trajectory.set(spawnProps.get(ConstKeys.TRAJECTORY, Vector2::class)!!)

        rotation = spawnProps.getOrDefault(ConstKeys.ROTATION, 0f, Float::class)
        bounces = 0

        if (shouldDebug) GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
    }

    override fun hitBlock(blockFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) =
        hit(thisShape, otherShape)

    override fun hitShield(shieldFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        val entity = shieldFixture.getEntity()
        if (entity.isAny(MoonScythe::class, SharpStar::class, Asteroid::class)) return
        hit(thisShape, otherShape)
    }

    private fun hit(thisShape: IGameShape2D, otherShape: IGameShape2D) {
        if (shouldDebug) GameLogger.debug(TAG, "hit(): thisShape=$thisShape, otherShape=$otherShape")

        bounces++
        if (bounces > MAX_BOUNCES) {
            if (shouldDebug) GameLogger.debug(TAG, "hit(): start to fade")
            fade = true
        }

        if (shouldDebug) GameLogger.debug(TAG, "hit(): old trajectory = $trajectory")

        val direction = getOverlapPushDirection(thisShape, otherShape)
        when {
            direction == null -> trajectory.scl(-1f)
            direction.isVertical() -> trajectory.y *= -1f
            else -> trajectory.x *= -1f
        }

        if (shouldDebug) GameLogger.debug(TAG, "hit(): direction=$direction, trajectory=$trajectory")
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        rotation += ROTATIONS_PER_SEC * 360f * delta * movementScalar

        when {
            fade -> {
                fadeTimer.update(delta)
                if (fadeTimer.isFinished()) destroy()
            }
            else -> {
                spawnTrailDelay.update(delta)
                if (spawnTrailDelay.isFinished()) {
                    val position = body.getCenter()

                    val trajectory = GameObjectPools.fetch(Vector2::class).setZero()

                    val scythe = MegaEntityFactory.fetch(MoonScythe::class)!!
                    scythe.spawn(
                        props(
                            ConstKeys.FADE pairTo true,
                            ConstKeys.OWNER pairTo owner,
                            ConstKeys.POSITION pairTo position,
                            ConstKeys.ROTATION pairTo rotation,
                            ConstKeys.TRAJECTORY pairTo trajectory,
                        )
                    )

                    spawnTrailDelay.reset()
                }
            }
        }
    })

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(ConstVals.PPM.toFloat())
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.preProcess.put(ConstKeys.DEFAULT) {
            when {
                canMove && !fade -> body.physics.velocity.set(trajectory).scl(movementScalar)
                else -> body.physics.velocity.setZero()
            }
        }
        body.addBlockFilter filter@{ block, _ -> return@filter BLOCK_FILTERS.contains(block::class) }

        val debugShapes = gdxArrayOf<() -> IDrawableShape?>()

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameCircle().setRadius(0.5f * ConstVals.PPM))
        body.addFixture(damagerFixture)
        debugShapes.add { damagerFixture }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.PROJECTILE, FixtureType.SHIELD))
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 10))
        sprite.setSize(3.5f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.setCenter(body.getCenter())

            sprite.setOriginCenter()
            sprite.rotation = rotation

            val alpha = if (fade) 1f - fadeTimer.getRatio() else 1f
            sprite.setAlpha(alpha)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(region!!, 2, 1, 0.1f, true)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }
}
