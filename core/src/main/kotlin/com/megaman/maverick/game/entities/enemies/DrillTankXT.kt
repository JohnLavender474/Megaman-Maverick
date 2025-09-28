package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.ProcessState
import com.mega.game.engine.common.extensions.*
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.TimeMarkedRunnable
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IDrawableShapesEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.explosions.AsteroidExplosion
import com.megaman.maverick.game.entities.explosions.Explosion
import com.megaman.maverick.game.entities.projectiles.Rock
import com.megaman.maverick.game.entities.projectiles.Rock.RockSize
import com.megaman.maverick.game.utils.AnimationUtils
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getBoundingRectangle
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.misc.FacingUtils
import com.megaman.maverick.game.world.body.*

class DrillTankXT(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IDrawableShapesEntity, IFaceable {

    companion object {
        const val TAG = "DrillTankXT"

        private const val AWAKEN_RADIUS = 6f
        private const val AWAKEN_DUR = 0.25f

        private const val DRILL_DUR = 1f
        private const val DRILL_ROCKS = 4
        private const val DRILL_OFFSET_X = 1f
        private const val DRILL_OFFSET_Y = -0.35f
        private const val DRILL_WIDTH = 2f
        private const val DRILL_HEIGHT = 0.5f
        private const val DRILL_ROCK_MIN_X_IMPULSE = -2f
        private const val DRILL_ROCK_MAX_X_IMPULSE = 8f
        private const val DRILL_ROCK_MIN_Y_IMPULSE = 4f
        private const val DRILL_ROCK_MAX_Y_IMPULSE = 12f

        private const val DIE_DUR = 1f
        private const val DIE_EXPLOSIONS = 4

        private const val MOVE_SPEED = 2f
        private const val MOVE_PAUSE_ON_SWAP_FACING = 0.5f

        private const val GRAVITY = 0.15f
        private const val GROUND_GRAVITY = 0.01f

        private val animDefs = orderedMapOf(
            "sleep" pairTo AnimationDef(),
            "surprised" pairTo AnimationDef(),
            "move" pairTo AnimationDef(3, 1, 0.1f, true),
            "drill" pairTo AnimationDef(3, 1, 0.05f, true),
            "die" pairTo AnimationDef()
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class DrillTankXTState { SLEEP, SURPRISED, MOVE, DRILL, DIE }

    override lateinit var facing: Facing

    private lateinit var state: DrillTankXTState
    private val stateTimers = orderedMapOf(
        DrillTankXTState.SURPRISED pairTo Timer(AWAKEN_DUR),
        DrillTankXTState.DRILL pairTo Timer(DRILL_DUR).also { timer ->
            val offset = DRILL_DUR / DRILL_ROCKS
            for (i in 0 until DRILL_ROCKS) {
                val time = i * offset
                val runnable = TimeMarkedRunnable(time) { spawnRock(i % 2 == 0) }
                timer.addRunnable(runnable)
            }
        },
        DrillTankXTState.DIE pairTo Timer(DIE_DUR).also { timer ->
            val offset = DIE_DUR / DIE_EXPLOSIONS
            for (i in 0 until DIE_EXPLOSIONS) {
                val time = i * offset
                val runnable = TimeMarkedRunnable(time) { spawnExplosion() }
                timer.addRunnable(runnable)
            }
        }
    )

    private val movePauseOnSwapFacing = Timer(MOVE_PAUSE_ON_SWAP_FACING)

    private val awakenArea = GameCircle().setRadius(AWAKEN_RADIUS * ConstVals.PPM)

    private lateinit var drillTipFixture: Fixture
    private val drillBlocks = ObjectSet<Block>()
    private val canDrillBlock: Boolean
        get() = !drillBlocks.isEmpty && body.isSensing(BodySense.FEET_ON_GROUND)

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            AnimationUtils.loadRegions(TAG, atlas, animDefs.keys(), regions)
        }
        super.init()
        addComponent(defineAnimationsComponent())
        addDebugShapeSupplier { awakenArea }
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)

        state = DrillTankXTState.SLEEP
        stateTimers.values().forEach { it.reset() }

        FacingUtils.setFacingOf(this)
        movePauseOnSwapFacing.setToEnd()
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        drillBlocks.clear()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            val stateTimer = stateTimers[state]
            if (stateTimer != null) {
                stateTimer.update(delta)
                if (stateTimer.isFinished()) onFinishStateTimer(stateTimer)
            }

            movePauseOnSwapFacing.update(delta)

            when (state) {
                DrillTankXTState.SLEEP -> {
                    body.physics.velocity.setZero()

                    awakenArea.setCenter(body.getCenter())

                    if (megaman.body.getBounds().overlaps(awakenArea)) {
                        state = DrillTankXTState.SURPRISED
                        GameLogger.debug(TAG, "update(): SLEEP -> SURPRISED")
                    }
                }
                DrillTankXTState.MOVE -> {
                    if (movePauseOnSwapFacing.isFinished())
                        body.physics.velocity.x = MOVE_SPEED * ConstVals.PPM * facing.value
                    else body.physics.velocity.x = 0f

                    if (canDrillBlock) {
                        drillBlocks.clear()
                        state = DrillTankXTState.DRILL
                        GameLogger.debug(TAG, "update(): MOVE -> DRILL")
                    }
                }
                DrillTankXTState.SURPRISED, DrillTankXTState.DRILL, DrillTankXTState.DIE ->
                    body.physics.velocity.setZero()
            }
        }
    }

    private fun onFinishStateTimer(timer: Timer) {
        GameLogger.debug(TAG, "onFinishStateTimer(): state=$state")

        if (state != DrillTankXTState.DIE) timer.reset()

        when (state) {
            DrillTankXTState.SURPRISED -> {
                FacingUtils.setFacingOf(this)
                state = DrillTankXTState.MOVE
                GameLogger.debug(TAG, "onFinshStateTimer(): go to MOVE state")
            }
            DrillTankXTState.DRILL -> {
                swapFacing()
                drillBlocks.clear()
                state = DrillTankXTState.MOVE
                GameLogger.debug(TAG, "onFinshStateTimer(): go to MOVE state")
            }
            DrillTankXTState.DIE -> destroy()
            else -> {}
        }
    }

    override fun swapFacing() {
        GameLogger.debug(TAG, "swapFacing()")
        super.swapFacing()
        movePauseOnSwapFacing.reset()
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.setSize(1.75f * ConstVals.PPM, 1.5f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        drillTipFixture = Fixture(body, FixtureType.CONSUMER, GameRectangle().setSize(0.1f * ConstVals.PPM))
        drillTipFixture.setFilter { fixture -> fixture.getType() == FixtureType.BLOCK && fixture.getEntity() is Block }
        drillTipFixture.setConsumer { processState, fixture ->
            val block = fixture.getEntity() as Block
            if (processState == ProcessState.BEGIN) drillBlocks.add(block)
            else if (processState == ProcessState.END) drillBlocks.remove(block)
        }
        body.addFixture(drillTipFixture)
        debugShapes.add { drillTipFixture.getShape() }

        val drillDamagerFixture = Fixture(
            body,
            FixtureType.DAMAGER,
            GameRectangle().setSize(DRILL_WIDTH * ConstVals.PPM, DRILL_HEIGHT * ConstVals.PPM)
        )
        drillDamagerFixture.attachedToBody = false
        body.addFixture(drillDamagerFixture)
        debugShapes.add { drillDamagerFixture }

        val drillShieldFixture = Fixture(
            body,
            FixtureType.SHIELD,
            GameRectangle().setSize(DRILL_WIDTH * ConstVals.PPM, DRILL_HEIGHT * ConstVals.PPM)
        )
        drillShieldFixture.attachedToBody = false
        body.addFixture(drillShieldFixture)
        debugShapes.add { drillShieldFixture }

        val feetFixture = Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        body.addFixture(feetFixture)
        feetFixture.drawingColor = Color.GREEN
        debugShapes.add { feetFixture }

        val leftConsumerFixture = Fixture(
            body, FixtureType.CONSUMER, GameRectangle().setSize(
                0.1f * ConstVals.PPM, 0.1f * ConstVals.PPM
            )
        )
        leftConsumerFixture.offsetFromBodyAttachment.set(
            (-0.25f * ConstVals.PPM) - (body.getWidth() / 2f),
            -body.getHeight() / 2f
        )
        leftConsumerFixture.setFilter { fixture -> fixture.getType().equalsAny(FixtureType.BLOCK, FixtureType.DEATH) }
        leftConsumerFixture.setConsumer { _, fixture ->
            when (fixture.getType()) {
                FixtureType.BLOCK -> leftConsumerFixture.putProperty(ConstKeys.BLOCK, true)
                FixtureType.DEATH -> leftConsumerFixture.putProperty(ConstKeys.DEATH, true)
            }
        }
        body.addFixture(leftConsumerFixture)
        debugShapes.add { leftConsumerFixture }

        val rightConsumerFixture = Fixture(
            body, FixtureType.CONSUMER, GameRectangle().setSize(
                0.1f * ConstVals.PPM, 0.1f * ConstVals.PPM
            )
        )
        rightConsumerFixture.offsetFromBodyAttachment.set(
            (0.25f * ConstVals.PPM) + (body.getWidth() / 2f),
            -body.getHeight() / 2f
        )
        rightConsumerFixture.setFilter { fixture -> fixture.getType().equalsAny(FixtureType.BLOCK, FixtureType.DEATH) }
        rightConsumerFixture.setConsumer { _, fixture ->
            when (fixture.getType()) {
                FixtureType.BLOCK -> rightConsumerFixture.putProperty(ConstKeys.BLOCK, true)
                FixtureType.DEATH -> rightConsumerFixture.putProperty(ConstKeys.DEATH, true)
            }
        }
        body.addFixture(rightConsumerFixture)
        debugShapes.add { rightConsumerFixture }

        body.preProcess.put(ConstKeys.DEFAULT) {
            val active = state != DrillTankXTState.DIE
            body.forEachFixture { fixture ->
                fixture.setActive(active)
                fixture.putProperty("${ConstKeys.ROCK}_${ConstKeys.IGNORE}", true)
            }

            drillTipFixture.offsetFromBodyAttachment.x =
                facing.value * ((body.getWidth() / 2f) + (DRILL_OFFSET_X * ConstVals.PPM))
            drillTipFixture.offsetFromBodyAttachment.y = DRILL_OFFSET_Y * ConstVals.PPM

            val drillFixturesPosition = if (isFacing(Facing.LEFT)) Position.CENTER_LEFT else Position.CENTER_RIGHT

            val drillTip = drillTipFixture.getShape().getBoundingRectangle()

            val drillDamager = drillDamagerFixture.rawShape as GameRectangle
            drillDamager.positionOnPoint(
                drillTip.getPositionPoint(drillFixturesPosition.opposite()),
                drillFixturesPosition
            )

            val drillShield = drillShieldFixture.rawShape as GameRectangle
            drillShield.positionOnPoint(
                drillTip.getPositionPoint(drillFixturesPosition.opposite()),
                drillFixturesPosition
            )

            leftConsumerFixture.putProperty(ConstKeys.BLOCK, false)
            leftConsumerFixture.putProperty(ConstKeys.DEATH, false)
            rightConsumerFixture.putProperty(ConstKeys.BLOCK, false)
            rightConsumerFixture.putProperty(ConstKeys.DEATH, false)
        }

        body.postProcess.put(ConstKeys.DEFAULT) {
            val gravity = if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_GRAVITY else GRAVITY
            body.physics.gravity.y = -gravity * ConstVals.PPM

            if (state == DrillTankXTState.MOVE) {
                if (isFacing(Facing.LEFT)) {
                    if (leftConsumerFixture.getProperty(ConstKeys.BLOCK, Boolean::class) != true ||
                        leftConsumerFixture.getProperty(ConstKeys.DEATH, Boolean::class) == true
                    ) swapFacing()
                } else if (isFacing(Facing.RIGHT)) {
                    if (rightConsumerFixture.getProperty(ConstKeys.BLOCK, Boolean::class) != true ||
                        rightConsumerFixture.getProperty(ConstKeys.DEATH, Boolean::class) == true
                    ) swapFacing()
                }
            }

            val drillBlockIter = drillBlocks.iterator()
            while (drillBlockIter.hasNext) {
                val drillBlock = drillBlockIter.next()
                if (!drillTipFixture.getShape().overlaps(drillBlock.body.getBounds())) drillBlockIter.remove()
            }
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE, FixtureType.SHIELD)
        )
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().also { sprite -> sprite.setSize(3f * ConstVals.PPM, 2f * ConstVals.PPM) })
        .updatable { _, sprite ->
            val position = if (isFacing(Facing.LEFT)) Position.BOTTOM_RIGHT else Position.BOTTOM_LEFT
            sprite.setPosition(body.getPositionPoint(position), position)
            sprite.setFlip(isFacing(Facing.RIGHT), false)
            sprite.hidden = damageBlink
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier { state.name.lowercase() }
                .applyToAnimations { animations ->
                    AnimationUtils.loadAnimationDefs(animDefs, animations, regions)
                }
                .build()
        )
        .build()

    private fun spawnRock(big: Boolean) {
        val drillTip = drillTipFixture.getShape().getBoundingRectangle()

        val position = drillTip.getCenter().add(0.5f * ConstVals.PPM * -facing.value, 0f)

        val impulse = GameObjectPools.fetch(Vector2::class)
            .setX(-facing.value * UtilMethods.getRandom(DRILL_ROCK_MIN_X_IMPULSE, DRILL_ROCK_MAX_X_IMPULSE))
            .setY(UtilMethods.getRandom(DRILL_ROCK_MIN_Y_IMPULSE, DRILL_ROCK_MAX_Y_IMPULSE))
            .scl(ConstVals.PPM.toFloat())

        GameLogger.debug(
            TAG,
            "spawnRock(): big=$big, rockRosition=$position, rockImpulse=$impulse, body.center=${body.getCenter()}"
        )

        val rockSize = if (big) RockSize.BIG else RockSize.SMALL

        val rock = MegaEntityFactory.fetch(Rock::class)!!
        rock.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.SIZE pairTo rockSize,
                ConstKeys.IMPULSE pairTo impulse,
                ConstKeys.POSITION pairTo position
            )
        )

        val explosion = MegaEntityFactory.fetch(AsteroidExplosion::class)!!
        explosion.spawn(props(ConstKeys.POSITION pairTo drillTip.getCenter(), ConstKeys.OWNER pairTo this))
    }

    private fun spawnExplosion() {
        val position = Position.entries.random()

        val spawn = body.getCenter().add(
            (position.x - 1) * 0.75f * ConstVals.PPM, (position.y - 1) * 0.75f * ConstVals.PPM
        )

        GameLogger.debug(TAG, "spawnExplosion(): position=$position, spawn=$spawn")

        val explosion = MegaEntityFactory.fetch(Explosion::class)!!
        explosion.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.SOUND pairTo SoundAsset.EXPLOSION_2_SOUND,
            )
        )
    }
}
