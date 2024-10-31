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
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.entities.projectiles.Needle
import com.megaman.maverick.game.utils.MegaUtilMethods
import com.megaman.maverick.game.world.body.*
import kotlin.math.abs
import kotlin.reflect.KClass

class DesertMan(game: MegamanMaverickGame) : AbstractBoss(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "DesertMan"

        private const val GRAVITY = -0.15f

        private const val STAND_DUR = 0.75f

        private const val TORNADO_DUR = 2f
        private const val TORNADO_START_DUR = 0.2f

        private const val TORNADO_IMPULSE_X = 30f

        private const val JUMP_MAX_IMPULSE_X = 30f
        private const val JUMP_IMPULSE_Y = 18f

        private const val PUNCH_DUR = 0.1f
        private const val PUNCH_Y_THRESHOLD = 0.5f
        private const val SHORT_PUNCH_WIDTH = 0.75f
        private const val LONG_PUNCH_WIDTH = 1f
        private const val SHORT_PUNCH_X_THRESHOLD = 2f
        private const val LONG_PUNCH_X_THRESHOLD = 4f

        private const val BODY_WIDTH = 1.15f
        private const val BODY_HEIGHT = 1.5f
        private const val VEL_CLAMP_X = 35f
        private const val VEL_CLAMP_Y = 25f
        private const val DEFAULT_FRICTION_X = 5f
        private const val SAND_FRICTION_X = 10f

        private const val ARM_EXTENSIONS_COUNT = 3

        private const val SPRITE_SIZE = 3f
        private const val SPRITE_Y_OFFSET = -0.25f

        private const val DANCE_DUR = 3.75f
        private const val NEEDLES_KEY = "needles"
        private const val NEEDLE_COUNT = 14
        private const val NEEDLE_SPAWN_DUR = 1f
        private const val NEEDLE_BLINK_DUR = 0.05f
        private const val NEEDLE_DROP_DUR = 3.5f

        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class DesertManState { INIT, STAND, JUMP, DANCE, PUNCH_LONG, PUNCH_SHORT, TORNADO }

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

    private val needleSpawns = Array<Vector2>()
    private val needles = Array<Needle>()
    private var needleBlink = false

    private val leftArmExtensions = Array<GameRectangle>()
    private val rightArmExtensions = Array<GameRectangle>()

    private lateinit var stateMachine: StateMachine<DesertManState>
    private val currentState: DesertManState
        get() = stateMachine.getCurrent()
    private var previousAttackState: DesertManState? = null

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
        timers.forEach { t ->
            if (t.key.equalsAny("tornado_punch", "needle_spawn")) t.value.setToEnd()
            else t.value.reset()
        }

        facing = if (getMegaman().body.x < body.x) Facing.LEFT else Facing.RIGHT
        longPunchExtensionCount = 0
        longPunchingForward = false
        danceFlash = false
        needleBlink = false

        body.physics.velocity.setZero()
        body.physics.gravityOn = false

        val needlesRect = spawnProps.get(NEEDLES_KEY, RectangleMapObject::class)!!.rectangle.toGameRectangle()
        val needleSpawnOffset = needlesRect.width / NEEDLE_COUNT
        (0 until NEEDLE_COUNT).forEach { t ->
            val x = (needleSpawnOffset / 2f) + t * needleSpawnOffset
            val y = needlesRect.getMaxY()
            needleSpawns.add(Vector2(x, y))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        needles.forEach { it.destroy() }
        needles.clear()
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
                explodeOnDefeat(delta)
                return@add
            }

            val needleSpawnTimer = timers["needle_spawn"]
            needleSpawnTimer.update(delta)
            if (!needleSpawnTimer.isFinished()) {
                val needleBlinkTimer = timers["needle_blink"]
                needleBlinkTimer.update(delta)
                if (needleBlinkTimer.isFinished()) {
                    needleBlink = !needleBlink
                    needles.forEach { t ->
                        t.firstSprite!!.hidden = needleBlink
                        needleBlinkTimer.reset()
                    }
                }
            } else if (needleSpawnTimer.isJustFinished()) needles.forEach { t ->
                t.damagerFixture.active = true
                t.firstSprite!!.hidden = false
            }

            when (currentState) {
                DesertManState.INIT -> {
                    body.physics.gravityOn = true
                    if (body.isSensing(BodySense.FEET_ON_GROUND)) stateMachine.next()
                }

                DesertManState.DANCE -> {
                    val danceTimer = timers["dance"]
                    danceTimer.update(delta)
                    if (danceFlash) timers["needles_drop"].update(delta)
                    if (danceTimer.isFinished()) stateMachine.next()
                }

                DesertManState.STAND,
                DesertManState.PUNCH_SHORT,
                DesertManState.PUNCH_LONG -> {
                    if (currentState == DesertManState.PUNCH_LONG) updateArmExtensions()
                    else if (currentState.equalsAny(DesertManState.STAND, DesertManState.DANCE)) {
                        updateFacing()
                        if (!body.isSensing(BodySense.FEET_ON_SAND)) return@add
                    }

                    val key = currentState.name.lowercase()
                    val timer = timers[key]
                    timer.update(delta)
                    if (timer.isFinished()) stateMachine.next()
                }

                DesertManState.JUMP -> {
                    facing = if (getMegaman().body.x < body.x) Facing.LEFT else Facing.RIGHT
                    if (body.physics.velocity.y <= 0f && body.isSensing(BodySense.FEET_ON_SAND)) stateMachine.next()
                }

                DesertManState.TORNADO -> {
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
        body.physics.gravity.y = GRAVITY * ConstVals.PPM
        body.physics.receiveFrictionX = false
        body.putProperty("${ConstKeys.TAKE_FRICTION}_${ConstKeys.SAND}", false)

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBodyBounds() }

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(ConstVals.PPM.toFloat(), 0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyCenter.y = -0.875f * ConstVals.PPM
        feetFixture.putProperty(ConstKeys.STICK_TO_BLOCK, false)
        body.addFixture(feetFixture)
        feetFixture.rawShape.color = Color.GREEN
        debugShapes.add { feetFixture.getShape() }

        val headFixture =
            Fixture(body, FixtureType.HEAD, GameRectangle().setSize(ConstVals.PPM.toFloat(), 0.1f * ConstVals.PPM))
        headFixture.offsetFromBodyCenter.y = 0.875f * ConstVals.PPM
        body.addFixture(headFixture)
        headFixture.rawShape.color = Color.ORANGE
        debugShapes.add { headFixture.getShape() }

        val armDamageable = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().setHeight(0.25f * ConstVals.PPM))
        armDamageable.attachedToBody = false
        body.addFixture(armDamageable)
        debugShapes.add { if (armDamageable.isActive()) armDamageable.getShape() else null }

        val armDamager = Fixture(body, FixtureType.DAMAGER, GameRectangle().setHeight(0.25f * ConstVals.PPM))
        armDamager.attachedToBody = false
        body.addFixture(armDamager)
        debugShapes.add { if (armDamager.isActive()) armDamager.getShape() else null }

        val armFixtures = gdxArrayOf(armDamageable, armDamager)
        body.preProcess.put(ConstKeys.DEFAULT) {
            val armActive = isPunching()
            armFixtures.forEach { t ->
                t.active = armActive
                if (armActive) {
                    val shape = t.rawShape as GameRectangle
                    shape.setWidth(
                        if (currentState == DesertManState.PUNCH_SHORT || isTornadoPunching())
                            SHORT_PUNCH_WIDTH * ConstVals.PPM
                        else longPunchExtensionCount * LONG_PUNCH_WIDTH * ConstVals.PPM
                    )

                    if (isFacing(Facing.LEFT)) shape.setCenterRightToPoint(body.getCenterLeftPoint())
                    else shape.setCenterLeftToPoint(body.getCenterRightPoint())
                }
            }

            if (body.isSensing(BodySense.HEAD_TOUCHING_BLOCK) && body.physics.velocity.y > 0f)
                body.physics.velocity.y = 0f

            if (body.isSensing(BodySense.FEET_ON_SAND) || currentState == DesertManState.TORNADO) {
                body.physics.gravityOn = false
                body.physics.defaultFrictionOnSelf.x = DEFAULT_FRICTION_X
                if (body.physics.velocity.y < 0f) body.physics.velocity.y = 0f
            } else {
                body.physics.gravityOn = true
                body.physics.defaultFrictionOnSelf.x = SAND_FRICTION_X
            }
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this, body, BodyFixtureDef.of(
                FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE
            )
        )
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
            _sprite.setFlip(isFacing(Facing.RIGHT), false)
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
                _sprite.hidden = currentState != DesertManState.PUNCH_LONG || longPunchExtensionCount < i
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
            "stand" pairTo Animation(regions["stand"], 2, 1, gdxArrayOf(1f, 0.15f), true),
            "jump" pairTo Animation(regions["jump"]),
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
                    isMegamanInPunchYRange()
            }
            .transition(DesertManState.STAND.name, DesertManState.PUNCH_LONG.name) {
                previousAttackState != DesertManState.PUNCH_LONG &&
                    isMegamanInLongPunchXRange() &&
                    isMegamanInPunchYRange()
            }
            .transition(DesertManState.STAND.name, DesertManState.TORNADO.name) {
                previousAttackState != DesertManState.TORNADO &&
                    getMegaman().body.isSensing(BodySense.FEET_ON_SAND) &&
                    getRandomBool()
            }
            .transition(DesertManState.STAND.name, DesertManState.DANCE.name) {
                previousAttackState != DesertManState.DANCE && getRandomBool()
            }
            .transition(DesertManState.STAND.name, DesertManState.JUMP.name) { true }
            // PUNCH -> STAND
            .transition(DesertManState.PUNCH_SHORT.name, DesertManState.STAND.name) { true }
            .transition(DesertManState.PUNCH_LONG.name, DesertManState.STAND.name) { true }
            // JUMP -> STAND
            .transition(DesertManState.JUMP.name, DesertManState.STAND.name) { true }
            // TORNADO -> STAND
            .transition(DesertManState.TORNADO.name, DesertManState.STAND.name) { true }
            // DANCE -> STAND
            .transition(DesertManState.DANCE.name, DesertManState.STAND.name) { true }
        return builder.build()
    }

    private fun onChangeState(current: DesertManState, previous: DesertManState) {
        GameLogger.debug(TAG, "onChangeState: current=$current, previous=$previous")

        if (previous == DesertManState.STAND) previousAttackState = current
        else if (previous == DesertManState.INIT) {
            body.physics.gravityOn = true
            GameLogger.debug(
                TAG, "onChangeState(): ending INIT state: " +
                    "body gravity turned on"
            )
        } else if (previous == DesertManState.JUMP) {
            body.physics.applyFrictionX = true
            GameLogger.debug(
                TAG, "onChangeState(): ending JUMP state: " +
                    "body.physics.applyFrictionX=${body.physics.applyFrictionX}"
            )
        } else if (previous == DesertManState.DANCE) {
            danceFlash = false
            spawnNeedles()
            GameLogger.debug(
                TAG, "onChangeState(): ending DANCE state: " +
                    "reset dance timer, " +
                    "danceFlash=$danceFlash"
            )
        }

        if (current == DesertManState.STAND) {
            timers["stand"].reset()
            GameLogger.debug(
                TAG, "onChangeState(): setting up STAND state: " +
                    "stand timer reset"
            )
        }
        if (current == DesertManState.JUMP) {
            body.physics.applyFrictionX = false
            val impulse = jump(getMegaman().body.getCenter())
            facing = if (impulse.x < 0f) Facing.LEFT else Facing.RIGHT
            GameLogger.debug(
                TAG, "onChangeState(): setting up JUMP state: " +
                    "body.physics.applyFrictionX=${body.physics.applyFrictionX}, " +
                    "body.physics.velocity=${body.physics.velocity}, " +
                    "facing=$facing"
            )
        } else if (current == DesertManState.PUNCH_LONG) {
            timers["punch_long"].reset()
            longPunchExtensionCount = 0
            longPunchingForward = true
            updateArmExtensions()
            GameLogger.debug(
                TAG,
                "onChangeState(): setting up PUNCH_LONG state: " +
                    "punch_long timer reset, " +
                    "longPunchExtensionCount=$longPunchExtensionCount, " +
                    "longPunchingForward=$longPunchingForward, " +
                    "updated arm extensions " +
                    "[leftArmExtensions=$leftArmExtensions, rightArmExtensions=$rightArmExtensions]"
            )
        } else if (current == DesertManState.PUNCH_SHORT) {
            timers["punch_short"].reset()
            GameLogger.debug(
                TAG, "onChangeState(): setting up PUNCH_SHORT state: " +
                    "punch_short timer reset"
            )
        } else if (current == DesertManState.TORNADO) {
            timers["tornado"].reset()
            timers["tornado_punch"].reset()
            timers["tornado_start"].reset()
            GameLogger.debug(
                TAG,
                "onChangeState(): setting up TORNADO state: " +
                    "tornado timer reset, " +
                    "tornado_punch timer reset, " +
                    "tornado_start timer reset"
            )
        } else if (current == DesertManState.DANCE) {
            setToDropNeedles()
            timers["needle_drop"].reset()
            timers["dance"].reset()
        }
    }

    private fun jump(target: Vector2): Vector2 {
        val impulse = MegaUtilMethods.calculateJumpImpulse(body.getCenter(), target, JUMP_IMPULSE_Y * ConstVals.PPM)
        impulse.x = impulse.x.coerceIn(-JUMP_MAX_IMPULSE_X * ConstVals.PPM, JUMP_MAX_IMPULSE_X * ConstVals.PPM)
        body.physics.velocity.set(impulse.x, impulse.y)
        return impulse
    }

    private fun setToDropNeedles() {
        val runnables = Array<TimeMarkedRunnable>()
        val increment = NEEDLE_DROP_DUR / NEEDLE_COUNT
        for (i in 1..NEEDLE_COUNT) {
            val time = increment * i
            val runnable = TimeMarkedRunnable(time) {
                val needle = needles.pop()
                needle.body.physics.gravity.y = GRAVITY * ConstVals.PPM
            }
            runnables.add(runnable)
        }
        val timer = timers["needle_drop"]
        timer.setRunnables(runnables)
    }

    private fun spawnNeedles() {
        needleSpawns.forEach { spawn ->
            val needle = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.NEEDLE)!! as Needle
            needle.spawn(
                props(
                    ConstKeys.POSITION pairTo spawn,
                    "${ConstKeys.BODY}_${ConstKeys.POSITION}" pairTo Position.TOP_CENTER,
                    "${ConstKeys.DAMAGER}_${ConstKeys.ACTIVE}" pairTo false
                )
            )
            needles.add(needle)
        }
    }

    private fun isMegamanInShortPunchXRange() =
        abs(getMegaman().body.x - body.x) <= SHORT_PUNCH_X_THRESHOLD * ConstVals.PPM

    private fun isMegamanInLongPunchXRange() =
        abs(getMegaman().body.x - body.x) <= LONG_PUNCH_X_THRESHOLD * ConstVals.PPM

    private fun isMegamanInPunchYRange() = abs(getMegaman().body.y - body.y) <= PUNCH_Y_THRESHOLD * ConstVals.PPM

    private fun isPunching() =
        currentState.equalsAny(DesertManState.PUNCH_SHORT, DesertManState.PUNCH_LONG) || isTornadoPunching()

    private fun isTornadoPunching() = currentState == DesertManState.TORNADO && !timers["tornado_punch"].isFinished()

    private fun updateFacing() {
        if (getMegaman().body.getMaxX() < body.x) facing = Facing.LEFT
        else if (getMegaman().body.x > body.getMaxX()) facing = Facing.RIGHT
    }

    private fun buildTimers() {
        timers.put("stand", Timer(STAND_DUR))
        timers.put("punch_short", Timer(PUNCH_DUR))
        timers.put("tornado", Timer(TORNADO_DUR))
        timers.put("tornado_punch", Timer(PUNCH_DUR))
        timers.put("tornado_start", Timer(TORNADO_START_DUR))
        timers.put("dance", Timer(DANCE_DUR, TimeMarkedRunnable(0.2f) { danceFlash = true }))
        timers.put("needle_spawn", Timer(NEEDLE_SPAWN_DUR))
        timers.put("needle_blink", Timer(NEEDLE_BLINK_DUR))
        timers.put("needle_drop", Timer(NEEDLE_DROP_DUR))

        val longPunchTimer = Timer((2 * ARM_EXTENSIONS_COUNT) * PUNCH_DUR)
        val longPunchRunnables = Array<TimeMarkedRunnable>()
        for (i in 1..ARM_EXTENSIONS_COUNT) {
            val runnable = TimeMarkedRunnable(i * PUNCH_DUR) {
                longPunchingForward = true
                longPunchExtensionCount++
            }
            longPunchRunnables.add(runnable)
        }
        for (i in 1..ARM_EXTENSIONS_COUNT) {
            val runnable = TimeMarkedRunnable((ARM_EXTENSIONS_COUNT * PUNCH_DUR) + i * PUNCH_DUR) {
                longPunchingForward = false
                longPunchExtensionCount--
            }
            longPunchRunnables.add(runnable)
        }
        longPunchTimer.setRunnables(longPunchRunnables)
        timers.put("punch_long", longPunchTimer)
    }

    private fun buildArmExtensions() = (0 until ARM_EXTENSIONS_COUNT).forEach { i ->
        leftArmExtensions.add(GameRectangle().setSize(SPRITE_SIZE))
        rightArmExtensions.add(GameRectangle().setSize(SPRITE_SIZE))
    }

    private fun updateArmExtensions() {
        for (i in 0 until ARM_EXTENSIONS_COUNT) {
            val xOffset = (i + 1) * SPRITE_SIZE * ConstVals.PPM

            val leftCenter = body.getBottomCenterPoint().sub(xOffset, 0f)
            leftArmExtensions[i].setBottomCenterToPoint(leftCenter)

            val rightCenter = body.getBottomCenterPoint().add(xOffset, 0f)
            rightArmExtensions[i].setBottomCenterToPoint(rightCenter)
        }
    }
}
