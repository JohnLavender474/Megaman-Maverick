package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.animations.*
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.equalsAny
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.getRandom
import com.mega.game.engine.common.getRandomBool
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.interfaces.UpdateFunction
import com.mega.game.engine.common.objects.GamePair
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.shapes.toGameRectangle
import com.mega.game.engine.common.time.TimeMarkedRunnable
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.state.StateMachine
import com.mega.game.engine.state.StateMachineBuilder
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.decorations.Splash.SplashType
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.DecorationsFactory
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.utils.MegaUtilMethods
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.isSensing
import kotlin.math.abs
import kotlin.reflect.KClass

class DesertMan(game: MegamanMaverickGame) : AbstractBoss(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "DesertMan"

        private const val STAND_DUR = 1f

        private const val DANCE_DUR = 2.5f
        private const val DANCE_FLASH_DELAY = 0.75f
        private const val DANCE_SPAWN_NEEDLES_DELAY = 0.5f

        private const val TORNADO_DUR = 1.25f
        private const val TORNADO_START_DUR = 0.2f
        private const val TORNADO_SAND_DELAY = 0.1f
        private const val TORNADO_Y_THRESHOLD = 3f
        private const val TORNADO_IMPULSE_X = 50f
        private const val TORNADO_HEIGHT = 2f

        private const val JUMP_MAX_IMPULSE_X = 10f
        private const val JUMP_IMPULSE_Y = 14f

        private const val WALL_SLIDE_DUR = 0.5f
        private const val WALL_SLIDE_FRICTION_Y = 6f

        private const val PUNCH_DUR = 0.2f
        private const val PUNCH_PERCENTAGE = 0.75f
        private const val PUNCH_Y_THRESHOLD = 1.5f
        private const val SHORT_PUNCH_WIDTH = 0.75f
        private const val SHORT_PUNCH_X_THRESHOLD = 1.5f
        private const val LONG_PUNCH_X_THRESHOLD = 6f
        private const val LONG_PUNCH_EXTRA_WIDTH = 1.25f

        private const val BODY_WIDTH = 1.15f
        private const val BODY_HEIGHT = 1.5f
        private const val VEL_CLAMP_X = 50f
        private const val VEL_CLAMP_Y = 25f
        private const val DEFAULT_FRICTION_X = 5f
        private const val DEFAULT_FRICTION_Y = 1f
        private const val SAND_FRICTION_X = 10f
        private const val GRAVITY = -0.15f

        private const val ARM_HEIGHT = 0.5f
        private const val ARM_OFFSET_Y = -0.25f
        private const val ARM_EXTENSIONS_COUNT = 2

        private const val SPRITE_SIZE = 3f
        private const val SPRITE_Y_OFFSET = -0.25f

        private const val NEEDLES = 9
        private const val NEEDLE_GRAV = -0.1f
        private const val NEEDLE_IMPULSE = 15f
        private const val NEEDLE_Y_OFFSET = 0.1f
        private val NEEDLE_ANGLES = gdxArrayOf(90f, 70f, 45f, 15f, 0f, 345f, 315f, 290f, 270f)
        private val NEEDLE_X_OFFSETS = gdxArrayOf(-0.2f, -0.15f, -0.1f, -0.05f, 0f, 0.05f, 0.1f, 0.15f, 0.2f)

        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class DesertManState { INIT, STAND, JUMP, WALL_SLIDE, DANCE, PUNCH_LONG, PUNCH_SHORT, TORNADO }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class pairTo dmgNeg(1),
        Fireball::class pairTo dmgNeg(2),
        ChargedShot::class pairTo dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) 2 else 1
        },
        ChargedShotExplosion::class pairTo dmgNeg {
            it as ChargedShotExplosion
            if (it.fullyCharged) 2 else 1
        })
    override lateinit var facing: Facing

    private val timers = OrderedMap<String, Timer>()

    private val leftArmExtensions = Array<GameRectangle>()
    private val rightArmExtensions = Array<GameRectangle>()

    private lateinit var stateMachine: StateMachine<DesertManState>
    private val currentState: DesertManState
        get() = stateMachine.getCurrent()
    private var previousAttackState: DesertManState? = null

    private lateinit var leftWallBounds: GameRectangle
    private lateinit var rightWallBounds: GameRectangle

    private var longPunchExtensionCount = 0
    private var longPunchingForward = false

    private var danceFlash = false

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES_2.source)
            DesertManState.entries.forEach { t ->
                val key = t.name.lowercase()
                regions.put(key, atlas.findRegion("$TAG/$key"))
            }
            regions.put("dance_flash", atlas.findRegion("$TAG/dance_flash"))
            regions.put("tornado_start", atlas.findRegion("$TAG/tornado_start"))
            regions.put("tornado_punch", atlas.findRegion("$TAG/tornado_punch"))
            regions.put("arm_extension", atlas.findRegion("$TAG/arm_extension"))
            regions.put("arm_extension_punch", atlas.findRegion("$TAG/arm_extension_punch"))
            regions.put("defeated", atlas.findRegion("$TAG/defeated"))
        }
        super.init()
        addComponent(defineAnimationsComponent())
        stateMachine = buildStateMachine()
        buildTimers()
        buildArmExtensions()
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)

        stateMachine.reset()
        timers.forEach { t -> t.value.reset() }

        facing = if (getMegaman().body.x < body.x) Facing.LEFT else Facing.RIGHT
        longPunchExtensionCount = 0
        longPunchingForward = false
        danceFlash = false

        body.physics.velocity.setZero()
        body.physics.gravityOn = false
        body.physics.defaultFrictionOnSelf.y = DEFAULT_FRICTION_Y

        leftWallBounds = spawnProps.get(
            "${ConstKeys.LEFT}_${ConstKeys.WALL}", RectangleMapObject::class
        )!!.rectangle.toGameRectangle()
        rightWallBounds = spawnProps.get(
            "${ConstKeys.RIGHT}_${ConstKeys.WALL}", RectangleMapObject::class
        )!!.rectangle.toGameRectangle()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (!ready) {
                body.physics.velocity.setZero()
                body.physics.gravityOn = false
                return@add
            }
            if (defeated) {
                body.physics.velocity.setZero()
                body.physics.gravityOn = false
                explodeOnDefeat(delta)
                return@add
            }

            when (currentState) {
                DesertManState.INIT -> {
                    body.physics.gravityOn = true
                    if (body.isSensing(BodySense.FEET_ON_SAND)) stateMachine.next()
                }

                DesertManState.DANCE -> {
                    updateFacing()

                    if (danceFlash) {
                        val danceNeedlesTimer = timers["dance_needles"]
                        danceNeedlesTimer.update(delta)
                        if (danceNeedlesTimer.isFinished()) {
                            spawnNeedles()
                            danceNeedlesTimer.reset()
                        }
                    }

                    val danceTimer = timers["dance"]
                    danceTimer.update(delta)
                    if (danceTimer.isFinished()) stateMachine.next()
                }

                DesertManState.STAND -> {
                    updateFacing()
                    if (!body.isSensing(BodySense.FEET_ON_SAND)) return@add

                    val timer = timers["stand"]
                    timer.update(delta)
                    if (timer.isFinished()) stateMachine.next()
                }

                DesertManState.PUNCH_SHORT -> {
                    val timer = timers["punch"]
                    timer.update(delta)
                    if (timer.isFinished()) stateMachine.next()
                }

                DesertManState.PUNCH_LONG -> {
                    updateArmExtensions()

                    val timer = timers["punch"]
                    timer.update(delta)
                    if (timer.isFinished()) {
                        GameLogger.debug(TAG, "update(): PUNCH_LONG: timer finished")

                        if (longPunchExtensionCount > ARM_EXTENSIONS_COUNT || !canExtendArm()) {
                            longPunchingForward = false
                            GameLogger.debug(
                                TAG, "update(): PUNCH_LONG: " +
                                    "longPunchExtensionCount >= ARM_EXTENSIONS_COUNT || !canExtendArm(): " +
                                    "longPunchingForward=$longPunchingForward"
                            )
                        }

                        if (!longPunchingForward && longPunchExtensionCount <= 0) {
                            stateMachine.next()
                            GameLogger.debug(
                                TAG, "update(): PUNCH_LONG: " +
                                    "!longPunchingForward && longPunchExtensionCount < 0: set state machine to next"
                            )
                        }

                        if (longPunchingForward) {
                            longPunchExtensionCount++
                            GameLogger.debug(
                                TAG,
                                "update(): PUNCH_LONG: increment longPunchExtensionCount=$longPunchExtensionCount"
                            )
                        } else {
                            longPunchExtensionCount--
                            GameLogger.debug(
                                TAG,
                                "update(): PUNCH_LONG: decrement longPunchExtensionCount=$longPunchExtensionCount"
                            )
                        }

                        timer.reset()
                    }
                }

                DesertManState.JUMP -> {
                    facing = if (getMegaman().body.x < body.x) Facing.LEFT else Facing.RIGHT
                    if (shouldFinishJumping()) stateMachine.next()
                }

                DesertManState.WALL_SLIDE -> {
                    body.physics.velocity.x = 0f
                    if (shouldGoToStandState()) stateMachine.next()
                    val timer = timers["wall_slide"]
                    timer.update(delta)
                    if (timer.isFinished()) stateMachine.next()
                }

                DesertManState.TORNADO -> {
                    if (isHittingAgainstLeftWall() || isHittingAgainstRightWall()) stateMachine.next()

                    val tornadoSandTimer = timers["tornado_sand"]
                    tornadoSandTimer.update(delta)
                    if (tornadoSandTimer.isFinished()) {
                        spawnSandSplash()
                        tornadoSandTimer.reset()
                    }

                    val tornadoStartTimer = timers["tornado_start"]
                    tornadoStartTimer.update(delta)
                    if (!tornadoStartTimer.isFinished()) {
                        body.physics.velocity.x = 0f
                        return@add
                    }

                    updateFacing()
                    body.physics.velocity.x += TORNADO_IMPULSE_X * ConstVals.PPM * facing.value * delta

                    val tornadoTimer = timers["tornado"]
                    tornadoTimer.update(delta)
                    if (tornadoTimer.isFinished()) stateMachine.next()
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(BODY_WIDTH * ConstVals.PPM, BODY_HEIGHT * ConstVals.PPM)
        body.physics.velocityClamp.set(VEL_CLAMP_X * ConstVals.PPM, VEL_CLAMP_Y * ConstVals.PPM)
        body.physics.receiveFrictionX = false
        body.putProperty("${ConstKeys.TAKE_FRICTION}_${ConstKeys.SAND}", false)
        body.color = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBodyBounds() }

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(ConstVals.PPM.toFloat(), 0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyCenter.y = -BODY_HEIGHT * ConstVals.PPM / 2f
        feetFixture.putProperty(ConstKeys.STICK_TO_BLOCK, false)
        body.addFixture(feetFixture)
        feetFixture.rawShape.color = Color.GREEN
        debugShapes.add { feetFixture.getShape() }

        val headFixture =
            Fixture(body, FixtureType.HEAD, GameRectangle().setSize(ConstVals.PPM.toFloat(), 0.1f * ConstVals.PPM))
        headFixture.offsetFromBodyCenter.y = BODY_HEIGHT * ConstVals.PPM / 2f
        body.addFixture(headFixture)
        headFixture.rawShape.color = Color.ORANGE
        debugShapes.add { headFixture.getShape() }

        val leftFixture =
            Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM, ConstVals.PPM.toFloat()))
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        leftFixture.offsetFromBodyCenter.x = -BODY_WIDTH * ConstVals.PPM / 2f
        body.addFixture(leftFixture)
        leftFixture.rawShape.color = Color.BLUE
        body.addFixture(leftFixture)

        val rightFixture =
            Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM, ConstVals.PPM.toFloat()))
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        rightFixture.offsetFromBodyCenter.x = BODY_WIDTH * ConstVals.PPM / 2f
        body.addFixture(rightFixture)
        rightFixture.rawShape.color = Color.BLUE
        body.addFixture(rightFixture)

        val bodyFixture =
            Fixture(body, FixtureType.BODY, GameRectangle().setWidth(BODY_WIDTH * ConstVals.PPM))
        bodyFixture.attachedToBody = false
        body.addFixture(bodyFixture)
        bodyFixture.rawShape.color = Color.RED
        debugShapes.add { bodyFixture.getShape() }

        val damagerFixture =
            Fixture(body, FixtureType.DAMAGER, GameRectangle().setWidth(BODY_WIDTH * ConstVals.PPM))
        damagerFixture.attachedToBody = false
        body.addFixture(damagerFixture)

        val damageableFixture =
            Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().setWidth(BODY_WIDTH * ConstVals.PPM))
        damageableFixture.attachedToBody = false
        body.addFixture(damageableFixture)

        val armDamageableFixture =
            Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().setHeight(ARM_HEIGHT * ConstVals.PPM))
        armDamageableFixture.attachedToBody = false
        body.addFixture(armDamageableFixture)
        debugShapes.add { if (isPunching()) armDamageableFixture.getShape() else null }

        val armDamagerFixture =
            Fixture(body, FixtureType.DAMAGER, GameRectangle().setHeight(ARM_HEIGHT * ConstVals.PPM))
        armDamagerFixture.attachedToBody = false
        body.addFixture(armDamagerFixture)
        debugShapes.add { if (isPunching()) armDamagerFixture.getShape() else null }

        val dynamicHeightFixtures = gdxArrayOf(bodyFixture, damagerFixture, damageableFixture)
        val armFixtures = gdxArrayOf(armDamageableFixture, armDamagerFixture)
        body.preProcess.put(ConstKeys.DEFAULT) {
            val height = if (currentState == DesertManState.TORNADO) TORNADO_HEIGHT else BODY_HEIGHT
            dynamicHeightFixtures.forEach {
                val shape = it.rawShape as GameRectangle
                shape.setHeight(height * ConstVals.PPM)
                shape.setBottomCenterToPoint(body.getBottomCenterPoint())
            }

            armFixtures.forEach { t ->
                t.active = isPunching()
                if (isPunching()) {
                    val shape = t.rawShape as GameRectangle
                    shape.setWidth(
                        if (currentState == DesertManState.PUNCH_SHORT || isTornadoPunching())
                            SHORT_PUNCH_WIDTH * ConstVals.PPM
                        else (longPunchExtensionCount * SPRITE_SIZE * ConstVals.PPM) +
                            (LONG_PUNCH_EXTRA_WIDTH * ConstVals.PPM)
                    )

                    if (currentState == DesertManState.TORNADO) {
                        if (isFacing(Facing.LEFT)) shape.setCenterRightToPoint(body.getTopLeftPoint())
                        else shape.setCenterLeftToPoint(body.getTopRightPoint())
                    } else {
                        if (isFacing(Facing.LEFT)) shape.setCenterRightToPoint(body.getCenterLeftPoint())
                        else shape.setCenterLeftToPoint(body.getCenterRightPoint())

                        shape.y += ARM_OFFSET_Y * ConstVals.PPM
                    }
                }
            }

            if (body.isSensing(BodySense.HEAD_TOUCHING_BLOCK) && body.physics.velocity.y > 0f)
                body.physics.velocity.y = 0f

            body.physics.gravity.y = if (currentState == DesertManState.WALL_SLIDE) 0f else GRAVITY * ConstVals.PPM

            if (body.isSensing(BodySense.FEET_ON_SAND)) {
                body.physics.gravityOn = false
                body.physics.defaultFrictionOnSelf.x =
                    if (currentState == DesertManState.TORNADO) DEFAULT_FRICTION_X else SAND_FRICTION_X
                if (body.physics.velocity.y < 0f) body.physics.velocity.y = 0f
            } else {
                body.physics.gravityOn = true
                body.physics.defaultFrictionOnSelf.x = DEFAULT_FRICTION_X
            }
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprites = OrderedMap<String, GameSprite>()
        val updateFunctions = ObjectMap<String, UpdateFunction<GameSprite>>()

        val mainSprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 0))
        mainSprite.setSize(SPRITE_SIZE * ConstVals.PPM)
        sprites.put("main", mainSprite)
        updateFunctions.put("main") { _, _sprite ->
            _sprite.setPosition(body.getBottomCenterPoint(), Position.BOTTOM_CENTER)
            _sprite.translateY(SPRITE_Y_OFFSET * ConstVals.PPM)
            val flipX =
                if (currentState == DesertManState.WALL_SLIDE) body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT)
                else isFacing(Facing.RIGHT)
            _sprite.setFlip(flipX, false)
            _sprite.hidden = damageBlink || !ready
        }

        for (i in 1..ARM_EXTENSIONS_COUNT) {
            val armExtensionSprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 0))
            armExtensionSprite.setSize(SPRITE_SIZE * ConstVals.PPM)
            sprites.put("arm_$i", armExtensionSprite)

            updateFunctions.put("arm_$i") { _, _sprite ->
                val position =
                    (if (isFacing(Facing.LEFT)) leftArmExtensions[i - 1]
                    else rightArmExtensions[i - 1]).getBottomCenterPoint()
                _sprite.setPosition(position, Position.BOTTOM_CENTER)
                _sprite.translateY(SPRITE_Y_OFFSET * ConstVals.PPM)
                _sprite.setFlip(isFacing(Facing.RIGHT), false)
                _sprite.hidden = defeated || currentState != DesertManState.PUNCH_LONG || longPunchExtensionCount < i
            }
        }

        return SpritesComponent(sprites, updateFunctions)
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animators = Array<GamePair<() -> GameSprite, IAnimator>>()

        val mainSprite = sprites["main"]
        val mainSpriteKeySupplier: () -> String? = {
            if (defeated) "defeated"
            else when (currentState) {
                DesertManState.INIT -> "jump"
                DesertManState.DANCE -> if (danceFlash) "dance_flash" else "dance"
                DesertManState.TORNADO -> {
                    if (!timers["tornado_start"].isFinished()) "tornado_start"
                    else if (!timers["tornado_punch"].isFinished()) "tornado_punch"
                    else "tornado"
                }

                else -> currentState.name.lowercase()
            }
        }
        val mainSpriteAnimations = objectMapOf<String, IAnimation>(
            "stand" pairTo Animation(regions["stand"], 2, 1, gdxArrayOf(0.5f, 0.1f), true),
            "jump" pairTo Animation(regions["jump"]),
            "wall_slide" pairTo Animation(regions["wall_slide"]),
            "dance" pairTo Animation(regions["dance"], 2, 1, 0.1f, true),
            "dance_flash" pairTo Animation(regions["dance_flash"], 2, 2, 0.05f, true),
            "tornado" pairTo Animation(regions["tornado"], 2, 1, 0.1f, true),
            "tornado_punch" pairTo Animation(regions["tornado_punch"], 2, 2, 0.05f, true),
            "tornado_start" pairTo Animation(regions["tornado_start"], 2, 1, 0.1f, false),
            "punch_short" pairTo Animation(regions["punch_short"], 2, 1, 0.05f, true),
            "punch_long" pairTo Animation(regions["punch_long"], 3, 1, 0.05f, false),
            "defeated" pairTo Animation(regions["defeated"], 3, 1, 0.1f, true)
        )
        val mainSpriteAnimator = Animator(mainSpriteKeySupplier, mainSpriteAnimations)
        animators.add({ mainSprite } pairTo mainSpriteAnimator)

        for (i in 1..ARM_EXTENSIONS_COUNT) {
            val armSprite = sprites["arm_$i"]
            val armSpriteKeySupplier: () -> String? = {
                if (i == longPunchExtensionCount) {
                    if (longPunchingForward) "forward" else "back"
                } else "middle"
            }
            val armAnimations = objectMapOf<String, IAnimation>(
                "forward" pairTo Animation(regions["arm_extension_punch"], 4, 2, 0.0125f, false),
                "back" pairTo Animation(regions["arm_extension_punch"], 4, 2, 0.0125f, false).reversed(),
                "middle" pairTo Animation(regions["arm_extension"])
            )
            val armAnimator = Animator(armSpriteKeySupplier, armAnimations)
            animators.add({ armSprite } pairTo armAnimator)
        }

        return AnimationsComponent(animators)
    }

    private fun buildStateMachine(): StateMachine<DesertManState> {
        val builder = StateMachineBuilder<DesertManState>()
        DesertManState.entries.forEach { t -> builder.state(t.name, t) }
        builder.setOnChangeState(this::onChangeState)
        builder.initialState(DesertManState.INIT.name)
            // INIT -> STAND
            .transition(DesertManState.INIT.name, DesertManState.STAND.name) { true }
            // STAND -> PUNCH, JUMP, TORNADO, DANCE
            .transition(DesertManState.STAND.name, DesertManState.PUNCH_SHORT.name) {
                previousAttackState != DesertManState.PUNCH_SHORT &&
                    isMegamanInShortPunchXRange() &&
                    isMegamanInPunchYRange() &&
                    getRandom(0f, 1f) <= PUNCH_PERCENTAGE
            }
            .transition(DesertManState.STAND.name, DesertManState.PUNCH_LONG.name) {
                previousAttackState != DesertManState.PUNCH_LONG &&
                    isMegamanInLongPunchXRange() &&
                    isMegamanInPunchYRange() &&
                    getRandom(0f, 1f) <= PUNCH_PERCENTAGE
            }
            .transition(DesertManState.STAND.name, DesertManState.TORNADO.name) {
                previousAttackState != DesertManState.TORNADO && isMegamanInTornadoYRange() &&
                    !isHittingAgainstLeftWall() && !isHittingAgainstRightWall()
            }
            .transition(DesertManState.STAND.name, DesertManState.DANCE.name) {
                previousAttackState != DesertManState.DANCE && getRandomBool()
            }
            .transition(DesertManState.STAND.name, DesertManState.JUMP.name) { true }
            // PUNCH -> STAND
            .transition(DesertManState.PUNCH_SHORT.name, DesertManState.STAND.name) { true }
            .transition(DesertManState.PUNCH_LONG.name, DesertManState.STAND.name) { true }
            // JUMP -> WALL_SLIDE, STAND
            .transition(DesertManState.JUMP.name, DesertManState.STAND.name) { shouldGoToStandState() }
            .transition(DesertManState.JUMP.name, DesertManState.WALL_SLIDE.name) { isWallSliding() }
            // WALL_SLIDE -> STAND, JUMP
            .transition(DesertManState.WALL_SLIDE.name, DesertManState.STAND.name) { shouldGoToStandState() }
            .transition(DesertManState.WALL_SLIDE.name, DesertManState.JUMP.name) { true }
            // TORNADO -> STAND
            .transition(DesertManState.TORNADO.name, DesertManState.STAND.name) { true }
            // DANCE -> STAND
            .transition(DesertManState.DANCE.name, DesertManState.STAND.name) { true }
        return builder.build()
    }

    private fun onChangeState(current: DesertManState, previous: DesertManState) {
        GameLogger.debug(TAG, "onChangeState: current=$current, previous=$previous")

        when (previous) {
            DesertManState.STAND -> previousAttackState = current
            DesertManState.INIT -> {
                body.physics.gravityOn = true
                GameLogger.debug(
                    TAG, "onChangeState(): ending INIT state: " +
                        "body gravity turned on"
                )
            }

            DesertManState.JUMP -> {
                body.physics.applyFrictionX = true
                GameLogger.debug(
                    TAG, "onChangeState(): ending JUMP state: " +
                        "body.physics.applyFrictionX=${body.physics.applyFrictionX}"
                )
            }

            DesertManState.WALL_SLIDE -> {
                body.physics.defaultFrictionOnSelf.y = DEFAULT_FRICTION_Y
                GameLogger.debug(
                    TAG, "onChangeState(): ending WALL_SLIDE state: " +
                        "body.physics.defaultFrictionOnSelf.y=${body.physics.defaultFrictionOnSelf.y}"
                )
            }

            else -> GameLogger.debug(TAG, "onChangeState: ending $previous state: nothing to do")
        }

        when (current) {
            DesertManState.STAND -> {
                timers["stand"].reset()
                GameLogger.debug(
                    TAG, "onChangeState(): setting up STAND state: " +
                        "stand timer reset"
                )
            }

            DesertManState.JUMP -> {
                body.physics.applyFrictionX = false
                val impulse = jump(getMegaman().body.getCenter())
                facing = if (impulse.x < 0f) Facing.LEFT else Facing.RIGHT
                GameLogger.debug(
                    TAG, "onChangeState(): setting up JUMP state: " +
                        "body.physics.applyFrictionX=${body.physics.applyFrictionX}, " +
                        "body.physics.velocity=${body.physics.velocity}, " +
                        "facing=$facing"
                )
            }

            DesertManState.WALL_SLIDE -> {
                timers["wall_slide"].reset()
                body.physics.defaultFrictionOnSelf.y = WALL_SLIDE_FRICTION_Y
                GameLogger.debug(
                    TAG, "onChangeState(): setting up WALL_SLIDE state: " +
                        "wall_slide timer reset, " +
                        "body.physics.defaultFrictionOnSelf.y=${body.physics.defaultFrictionOnSelf.y}"
                )
            }

            DesertManState.PUNCH_SHORT -> {
                updateFacing()
                timers["punch"].reset()
                GameLogger.debug(
                    TAG, "onChangeState(): setting up PUNCH_SHORT state: " +
                        "update facing=$facing, " +
                        "punch timer reset"
                )
            }

            DesertManState.PUNCH_LONG -> {
                updateFacing()
                updateArmExtensions()
                timers["punch"].reset()
                longPunchExtensionCount = 0
                longPunchingForward = true
                GameLogger.debug(
                    TAG,
                    "onChangeState(): setting up PUNCH_LONG state: " +
                        "update facing=$facing, " +
                        "updated arm extensions " +
                        "punch timer reset, " +
                        "longPunchExtensionCount=$longPunchExtensionCount, " +
                        "longPunchingForward=$longPunchingForward, " +
                        "[leftArmExtensions=$leftArmExtensions, rightArmExtensions=$rightArmExtensions]"
                )
            }

            DesertManState.TORNADO -> {
                timers["tornado"].reset()
                timers["tornado_punch"].reset()
                timers["tornado_start"].reset()
                timers["tornado_sand"].reset()
                requestToPlaySound(SoundAsset.WIND_1_SOUND, false)
                GameLogger.debug(
                    TAG,
                    "onChangeState(): setting up TORNADO state: " +
                        "tornado timer reset, " +
                        "tornado_punch timer reset, " +
                        "tornado_start timer reset, " +
                        "tornado_sand timer reset, " +
                        "play wind 1 sound"
                )
            }

            DesertManState.DANCE -> {
                danceFlash = false
                timers["dance"].reset()
                timers["dance_needles"].reset()
                GameLogger.debug(
                    TAG, "onChangeState(): setting up DANCE state: " +
                        "dance timer reset, " +
                        "dance_needles timer reset"
                )
            }

            else -> GameLogger.debug(TAG, "onChangeState(): setting up $previous state: nothing to do")
        }
    }

    private fun spawnNeedles() {
        for (i in 0 until NEEDLES) {
            val angle = NEEDLE_ANGLES[i]
            val xOffset = NEEDLE_X_OFFSETS[i]
            val position = body.getCenter().add(xOffset * ConstVals.PPM, NEEDLE_Y_OFFSET * ConstVals.PPM)
            val impulse = Vector2(0f, NEEDLE_IMPULSE * ConstVals.PPM).rotateDeg(angle)

            val needle = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.NEEDLE)!!
            needle.spawn(
                props(
                    ConstKeys.OWNER pairTo this,
                    ConstKeys.POSITION pairTo position,
                    ConstKeys.IMPULSE pairTo impulse,
                    ConstKeys.GRAVITY pairTo NEEDLE_GRAV * ConstVals.PPM
                )
            )
        }

        requestToPlaySound(SoundAsset.THUMP_SOUND, false)
    }

    private fun spawnSandSplash() {
        val splash = EntityFactories.fetch(EntityType.DECORATION, DecorationsFactory.SPLASH)!!
        splash.spawn(
            props(
                ConstKeys.POSITION pairTo body.getBottomCenterPoint(),
                ConstKeys.TYPE pairTo SplashType.SAND,
            )
        )
        GameLogger.debug(TAG, "spawnSandSplash()")
    }

    private fun jump(target: Vector2): Vector2 {
        val impulse = MegaUtilMethods.calculateJumpImpulse(body.getCenter(), target, JUMP_IMPULSE_Y * ConstVals.PPM)
        impulse.x = impulse.x.coerceIn(-JUMP_MAX_IMPULSE_X * ConstVals.PPM, JUMP_MAX_IMPULSE_X * ConstVals.PPM)
        body.physics.velocity.set(impulse.x, impulse.y)
        return impulse
    }

    private fun shouldFinishJumping() = isWallSliding() || shouldGoToStandState()

    private fun isHittingAgainstLeftWall() =
        body.physics.velocity.x <= 0f && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT)

    private fun isHittingAgainstRightWall() =
        body.physics.velocity.x >= 0f && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT)

    private fun isWallSliding() = body.physics.velocity.y < 0f && !body.isSensing(BodySense.FEET_ON_SAND) &&
        (isHittingAgainstLeftWall() || isHittingAgainstRightWall())

    private fun shouldGoToStandState() = body.physics.velocity.y <= 0f && body.isSensing(BodySense.FEET_ON_SAND)

    private fun isMegamanInShortPunchXRange() =
        abs(getMegaman().body.x - body.x) <= SHORT_PUNCH_X_THRESHOLD * ConstVals.PPM

    private fun isMegamanInLongPunchXRange() =
        abs(getMegaman().body.x - body.x) <= LONG_PUNCH_X_THRESHOLD * ConstVals.PPM

    private fun isMegamanInPunchYRange() = abs(getMegaman().body.y - body.y) <= PUNCH_Y_THRESHOLD * ConstVals.PPM

    private fun isPunching() =
        currentState.equalsAny(DesertManState.PUNCH_SHORT, DesertManState.PUNCH_LONG) || isTornadoPunching()

    private fun isMegamanInTornadoYRange() =
        abs(getMegaman().body.y - body.y) <= TORNADO_Y_THRESHOLD * ConstVals.PPM

    private fun isTornadoPunching() = currentState == DesertManState.TORNADO && !timers["tornado_punch"].isFinished()

    private fun updateFacing() {
        if (getMegaman().body.getMaxX() < body.x) facing = Facing.LEFT
        else if (getMegaman().body.x > body.getMaxX()) facing = Facing.RIGHT
    }

    private fun buildTimers() {
        timers.put("stand", Timer(STAND_DUR))
        timers.put("punch", Timer(PUNCH_DUR))
        timers.put("tornado", Timer(TORNADO_DUR))
        timers.put("tornado_punch", Timer(PUNCH_DUR))
        timers.put("tornado_start", Timer(TORNADO_START_DUR))
        timers.put("tornado_sand", Timer(TORNADO_SAND_DELAY))
        timers.put("wall_slide", Timer(WALL_SLIDE_DUR))
        timers.put("dance", Timer(DANCE_DUR, TimeMarkedRunnable(DANCE_FLASH_DELAY) { danceFlash = true }))
        timers.put("dance_needles", Timer(DANCE_SPAWN_NEEDLES_DELAY))
    }

    private fun buildArmExtensions() = (0 until ARM_EXTENSIONS_COUNT).forEach { i ->
        leftArmExtensions.add(GameRectangle().setSize(SPRITE_SIZE))
        rightArmExtensions.add(GameRectangle().setSize(SPRITE_SIZE))
    }

    private fun updateArmExtensions() {
        for (i in 1..ARM_EXTENSIONS_COUNT) {
            val xOffset = i * SPRITE_SIZE * ConstVals.PPM

            val leftCenter = body.getBottomLeftPoint().sub(xOffset, 0f)
            leftArmExtensions[i - 1].setBottomLeftToPoint(leftCenter)

            val rightCenter = body.getBottomRightPoint().add(xOffset, 0f)
            rightArmExtensions[i - 1].setBottomRightToPoint(rightCenter)
        }
    }

    private fun canExtendArm(): Boolean {
        if (longPunchExtensionCount >= ARM_EXTENSIONS_COUNT || longPunchExtensionCount < 0) {
            GameLogger.debug(
                TAG, "canExtendArm(): " +
                    "return false because index >= ARM_EXTENSIONS_COUNT: " +
                    "longPunchExtensionCount=$longPunchExtensionCount"
            )
            return false
        }

        val wall: GameRectangle
        val pointToCheck: Vector2
        if (isFacing(Facing.LEFT)) {
            wall = leftWallBounds
            pointToCheck = leftArmExtensions[longPunchExtensionCount].getCenterRightPoint()
        } else {
            wall = rightWallBounds
            pointToCheck = rightArmExtensions[longPunchExtensionCount].getCenterLeftPoint()
        }

        val canExtendArm = !wall.contains(pointToCheck)
        GameLogger.debug(TAG, "canExtendArm(): canExtendArm=$canExtendArm, pointToCheck=$pointToCheck, wall=$wall")
        return canExtendArm
    }
}
