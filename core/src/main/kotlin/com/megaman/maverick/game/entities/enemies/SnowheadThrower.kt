package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.orderedMapOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.TimeMarkedRunnable
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.explosions.SnowballExplosion
import com.megaman.maverick.game.entities.projectiles.Snowhead
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*

class SnowheadThrower(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.SMALL), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "SnowheadThrower"

        private const val HEADLESS_DUR = 0.75f
        private const val SPAWN_HEAD_DUR = 0.75f
        private const val THROW_HEAD_DUR = 0.5f
        private const val THROW_HEAD_DELAY = 0.1f
        private const val THROW_HEAD_TIME = THROW_HEAD_DELAY + 0.1f

        private const val SNOWHEAD_SIZE = 1f
        private const val SNOWHEAD_X_VEL = 8f
        private const val SNOWHEAD_Y_VEL = 8f

        private val animDefs = orderedMapOf(
            SnowheadThrowerState.HEADLESS pairTo AnimationDef(),
            SnowheadThrowerState.SPAWN_HEAD pairTo AnimationDef(),
            SnowheadThrowerState.THROW_HEAD pairTo AnimationDef(1, 5, 0.1f, false)
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class SnowheadThrowerState { HEADLESS, SPAWN_HEAD, THROW_HEAD }

    override lateinit var facing: Facing

    private lateinit var state: SnowheadThrowerState

    private var snowhead: Snowhead? = null

    private val headlessTimer = Timer(HEADLESS_DUR)
    private val spawnHeadTimer = Timer(SPAWN_HEAD_DUR)
    private val throwHeadTimer = Timer(THROW_HEAD_DELAY + THROW_HEAD_DUR)
        .addRunnables(TimeMarkedRunnable(THROW_HEAD_TIME) { throwHead() })

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            animDefs.keys().forEach { state ->
                val key = state.name.lowercase()
                val region = atlas.findRegion("$TAG/$key")
                regions.put(key, region)
            }
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)

        state = SnowheadThrowerState.HEADLESS

        headlessTimer.reset()
        spawnHeadTimer.reset()
        throwHeadTimer.reset()

        facing = if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        snowhead?.let { it.body.physics.gravityOn = true }
        snowhead = null

        if (isHealthDepleted()) explode()
    }

    override fun explode(explosionProps: Properties?) {
        val explosion = MegaEntityFactory.fetch(SnowballExplosion::class)!!
        explosion.spawn(props(ConstKeys.POSITION pairTo body.getCenter()))
    }

    private fun spawnHead() {
        val spawn = body.getPositionPoint(Position.TOP_CENTER).add(0f, 0.45f * ConstVals.PPM)

        val snowhead = MegaEntityFactory.fetch(Snowhead::class)!!
        snowhead.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.FACING pairTo facing,
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.GRAVITY_ON pairTo false,
                ConstKeys.SIZE pairTo SNOWHEAD_SIZE,
                ConstKeys.TYPE pairTo ConstKeys.ATTACHED
            )
        )

        this.snowhead = snowhead
    }

    private fun throwHead() {
        val trajectory = GameObjectPools.fetch(Vector2::class)
            .set(SNOWHEAD_X_VEL * facing.value, SNOWHEAD_Y_VEL)
            .scl(ConstVals.PPM.toFloat())

        snowhead!!.let { head ->
            head.body.let {
                it.physics.gravityOn = true
                it.physics.velocity.set(trajectory)
            }
            head.type = ConstKeys.DEFAULT
            head.facing = if (trajectory.x > 0f) Facing.RIGHT else Facing.LEFT
        }

        snowhead = null
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            when (state) {
                SnowheadThrowerState.HEADLESS -> {
                    facing = if (megaman.body.getCenter().x < body.getCenter().x) Facing.LEFT else Facing.RIGHT

                    spawnHeadTimer.update(delta)

                    if (spawnHeadTimer.isFinished()) {
                        spawnHead()
                        spawnHeadTimer.reset()
                        state = SnowheadThrowerState.SPAWN_HEAD
                    }
                }

                SnowheadThrowerState.SPAWN_HEAD -> {
                    if (snowhead!!.dead == true) {
                        snowhead = null
                        spawnHeadTimer.reset()
                        state = SnowheadThrowerState.HEADLESS
                    }

                    facing = if (megaman.body.getCenter().x < body.getCenter().x) Facing.LEFT else Facing.RIGHT

                    spawnHeadTimer.update(delta)

                    if (spawnHeadTimer.isFinished()) {
                        spawnHeadTimer.reset()
                        state = SnowheadThrowerState.THROW_HEAD
                    }
                }

                SnowheadThrowerState.THROW_HEAD -> {
                    throwHeadTimer.update(delta)

                    if (throwHeadTimer.isFinished()) {
                        throwHeadTimer.reset()
                        state = SnowheadThrowerState.HEADLESS
                    }
                }
            }

            snowhead?.facing = facing
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ConstVals.PPM.toFloat(), 1.125f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this, body, BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE)
        )
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().also { sprite -> sprite.setSize(2f * ConstVals.PPM) })
        .updatable { _, sprite ->
            val position = Position.BOTTOM_CENTER
            sprite.setPosition(body.getPositionPoint(position), position)

            sprite.hidden = damageBlink

            sprite.setFlip(isFacing(Facing.RIGHT), false)
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier { state.name.lowercase() }
                .applyToAnimations { animations ->
                    animDefs.forEach { entry ->
                        val key = entry.key.name.lowercase()
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

