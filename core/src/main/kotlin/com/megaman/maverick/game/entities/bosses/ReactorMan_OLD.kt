package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.Color
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
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.orderedMapOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.projectiles.ReactorManProjectile
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.MegaUtilMethods
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*

class ReactorMan_OLD(game: MegamanMaverickGame) : AbstractBoss(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "ReactorMan"

        private const val GRAVITY = -0.15f
        private const val GROUND_GRAVITY = -0.001f

        private const val STAND_MAX_DUR = 1f
        private const val STAND_MIN_DUR = 0.5f

        private const val INIT_DUR = 1f

        private const val RUN_DUR = 0.5f
        private const val RUN_MIN_SPEED = 6f
        private const val RUN_MAX_SPEED = 12f

        private const val JUMP_IMPULSE = 16f

        private const val THROW_DELAY = 0.25f
        private const val PROJECTILE_MIN_SPEED = 6f
        private const val PROJECTILE_MAX_SPEED = 12f

        private val animDefs = orderedMapOf(
            "stand" pairTo AnimationDef(1, 3, gdxArrayOf(1f, 0.1f, 0.1f), true),
            "defeated" pairTo AnimationDef(3, 1, 0.1f, true),
            "throw" pairTo AnimationDef(2, 1, 0.05f, false),
            "run" pairTo AnimationDef(2, 2, 0.1f, true),
            "jump" pairTo AnimationDef(),
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class ReactorManState { JUMP, RUN, STAND, THROW }

    override lateinit var facing: Facing

    private val runTimer = Timer(RUN_DUR)
    private val initTimer = Timer(INIT_DUR)
    private val throwTimer = Timer(THROW_DELAY)
    private val standTimer = Timer(STAND_MAX_DUR)

    private val projectilePosition: Vector2
        get() = body.getPositionPoint(Position.TOP_CENTER).add(
            0.15f * ConstVals.PPM * -facing.value,
            (if (body.isSensing(BodySense.FEET_ON_GROUND)) 0.1f else 0.25f) * ConstVals.PPM
        )

    private var jumped = false
    private var throwOnJump = true

    private var projectile: ReactorManProjectile? = null

    private lateinit var currentState: ReactorManState

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES_1.source)
            animDefs.keys().forEach { key -> regions.put(key, atlas.findRegion("$TAG/$key")) }
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)

        standTimer.reset()
        throwTimer.reset()
        initTimer.reset()
        runTimer.reset()

        jumped = false
        throwOnJump = true
        body.physics.gravityOn = true

        currentState = ReactorManState.STAND

        facing = if (megaman.body.getX() <= body.getX()) Facing.LEFT else Facing.RIGHT
    }

    override fun isReady(delta: Float) = initTimer.isFinished()

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        projectile?.destroy()
        projectile = null
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            projectile?.body?.setCenter(projectilePosition)

            if (betweenReadyAndEndBossSpawnEvent) return@add

            if (defeated) {
                body.physics.velocity.setZero()
                body.physics.gravityOn = false
                explodeOnDefeat(delta)
                return@add
            }

            if (!ready && body.isSensing(BodySense.FEET_ON_GROUND)) {
                initTimer.update(delta)
                return@add
            }

            when (currentState) {
                ReactorManState.STAND -> {
                    if (projectile == null) spawnProjectile()

                    if (megaman.body.getX() <= body.getX()) facing = Facing.LEFT
                    else if (megaman.body.getMaxX() >= body.getMaxX()) facing = Facing.RIGHT

                    if (body.isSensing(BodySense.FEET_ON_GROUND)) {
                        body.physics.velocity.setZero()

                        standTimer.update(delta)
                        if (standTimer.isFinished()) {
                            standTimer.resetDuration(getStandDur())
                            currentState = ReactorManState.JUMP
                        }
                    }
                }

                ReactorManState.JUMP -> {
                    if (!jumped) {
                        jump()
                        jumped = true
                        throwOnJump = !throwOnJump
                        return@add
                    }

                    if (megaman.body.getX() <= body.getX()) facing = Facing.LEFT
                    else if (megaman.body.getMaxX() >= body.getMaxX()) facing = Facing.RIGHT

                    if (throwOnJump) {
                        throwTimer.update(delta)
                        if (throwTimer.isJustFinished()) throwProjectile()
                    }

                    if (body.isSensing(BodySense.FEET_ON_GROUND) && body.physics.velocity.y <= 0f) {
                        jumped = false
                        throwTimer.reset()
                        currentState = if (throwOnJump) ReactorManState.RUN else ReactorManState.THROW
                    }
                }

                ReactorManState.THROW -> {
                    if (projectile != null) throwProjectile()

                    body.physics.velocity.setZero()

                    throwTimer.update(delta)
                    if (throwTimer.isFinished()) {
                        throwTimer.reset()

                        if (megaman.body.getX() <= body.getX()) facing = Facing.LEFT
                        else if (megaman.body.getMaxX() >= body.getMaxX()) facing = Facing.RIGHT

                        currentState = ReactorManState.RUN
                    }
                }

                ReactorManState.RUN -> {
                    runTimer.update(delta)
                    if (runTimer.isFinished() || shouldStopRunning()) {
                        runTimer.reset()
                        currentState = ReactorManState.STAND
                        return@add
                    }

                    val velocity = GameObjectPools.fetch(Vector2::class)
                        .set(getRunSpeed() * ConstVals.PPM * facing.value, 0f)
                    body.physics.velocity.set(velocity)
                }
            }
        }
    }

    private fun jump() {
        val impulse = MegaUtilMethods.calculateJumpImpulse(
            body.getPosition(),
            megaman.body.getPosition(),
            JUMP_IMPULSE * ConstVals.PPM
        )
        body.physics.velocity.set(impulse)
    }

    private fun spawnProjectile() {
        val projectile = MegaEntityFactory.fetch(ReactorManProjectile::class)!!
        projectile.spawn(
            props(
                ConstKeys.BIG pairTo true,
                ConstKeys.OWNER pairTo this,
                ConstKeys.ACTIVE pairTo false,
                ConstKeys.POSITION pairTo projectilePosition
            )
        )
        this.projectile = projectile
    }

    private fun throwProjectile() {
        projectile!!.let {
            val trajectory =
                megaman.body.getCenter().sub(body.getCenter()).nor().scl(getProjectileSpeed() * ConstVals.PPM)
            it.setTrajectory(trajectory)
            it.active = true
        }
        projectile = null
    }

    private fun shouldStopRunning() =
        (isFacing(Facing.LEFT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT)) ||
            (isFacing(Facing.RIGHT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT))

    private fun getStandDur() = STAND_MIN_DUR + (STAND_MAX_DUR - STAND_MIN_DUR) * getHealthRatio()

    private fun getRunSpeed() = RUN_MAX_SPEED - (RUN_MAX_SPEED - RUN_MIN_SPEED) * getHealthRatio()

    private fun getProjectileSpeed() =
        PROJECTILE_MAX_SPEED - (PROJECTILE_MAX_SPEED - PROJECTILE_MIN_SPEED) * getHealthRatio()

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(1.5f * ConstVals.PPM, 2f * ConstVals.PPM)
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val headFixture = Fixture(
            body, FixtureType.HEAD, GameRectangle().setSize(0.5f * ConstVals.PPM, 0.1f * ConstVals.PPM)
        )
        headFixture.offsetFromBodyAttachment.y = body.getHeight() / 2f
        body.addFixture(headFixture)
        headFixture.drawingColor = Color.ORANGE
        debugShapes.add { headFixture }

        val feetFixture = Fixture(
            body, FixtureType.FEET, GameRectangle().setSize(0.5f * ConstVals.PPM, 0.1f * ConstVals.PPM)
        )
        feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        body.addFixture(feetFixture)
        feetFixture.drawingColor = Color.GREEN
        debugShapes.add { feetFixture }

        val leftFixture = Fixture(
            body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM, 0.25f * ConstVals.PPM)
        )
        leftFixture.offsetFromBodyAttachment.x = -body.getWidth() / 2f
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        body.addFixture(leftFixture)
        leftFixture.drawingColor = Color.YELLOW
        debugShapes.add { leftFixture }

        val rightFixture = Fixture(
            body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM, 0.25f * ConstVals.PPM)
        )
        rightFixture.offsetFromBodyAttachment.x = body.getWidth() / 2f
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        body.addFixture(rightFixture)
        rightFixture.drawingColor = Color.YELLOW
        debugShapes.add { rightFixture }

        body.preProcess.put(ConstKeys.DEFAULT) {
            if (body.isSensing(BodySense.HEAD_TOUCHING_BLOCK) && body.physics.velocity.y > 0f)
                body.physics.velocity.y = 0f

            val gravity = if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_GRAVITY else GRAVITY
            body.physics.gravity.y = gravity * ConstVals.PPM
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE)
        )
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(
            TAG, GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 1))
                .also { sprite -> sprite.setSize(3f * ConstVals.PPM) }
        )
        .updatable { _, sprite ->
            val position = Position.BOTTOM_CENTER
            sprite.setPosition(body.getPositionPoint(position), position)

            sprite.setFlip(isFacing(Facing.RIGHT), false)

            sprite.hidden = damageBlink || game.isProperty(ConstKeys.ROOM_TRANSITION, true)
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier {
                    when {
                        defeated -> "defeated"
                        currentState == ReactorManState.STAND ->
                            if (body.isSensing(BodySense.FEET_ON_GROUND)) "stand" else "jump"

                        currentState == ReactorManState.JUMP && throwOnJump && throwTimer.isFinished() -> "throw"
                        else -> currentState.name.lowercase()
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

    override fun getTag() = TAG
}
