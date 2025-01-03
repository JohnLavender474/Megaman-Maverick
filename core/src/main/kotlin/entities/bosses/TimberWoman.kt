package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.*
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.TimeMarkedRunnable
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
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
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.extensions.toGameRectangle
import com.megaman.maverick.game.world.body.*
import kotlin.reflect.KClass

class TimberWoman(game: MegamanMaverickGame) : AbstractBoss(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "TimberWoman"

        private const val LEAF_SPAWN = "leaf_spawn"

        private const val BODY_WIDTH = 1.5f
        private const val BODY_HEIGHT = 1.75f

        private const val VEL_CLAMP_X = 50f
        private const val VEL_CLAMP_Y = 25f

        private const val SPRITE_SIZE = 3.5f

        private const val INIT_DUR = 1f
        private const val STAND_DUR = 1.8f
        private const val MAX_RUN_DUR = 2f
        private const val WALL_SLIDE_DUR = 0.75f
        private const val STAND_SWING_DUR = 1f
        private const val STAND_POUND_DUR = 1f
        private const val MAX_JUMP_SPIN_DUR = 1.5f

        private const val STAND_SWING_GROUND_BURST_TIME = 0.35f

        private const val GRAVITY = -0.15f
        private const val GROUND_GRAVITY = -0.01f
        private const val WALL_SLIDE_GRAVITY = -0.075f

        private const val DEFAULT_FRICTION_X = 6f
        private const val DEFAULT_FRICTION_Y = 1f
        private const val WALL_SLIDE_FRICTION_Y = 6f

        private const val AXE_SWING_DAMAGER_WIDTH_1 = 1.25f
        private const val AXE_SWING_DAMAGER_HEIGHT_1 = 2f
        private const val AXE_SWING_DAMAGER_ANIM_INDEX_1 = 2

        private const val AXE_SWING_DAMAGER_WIDTH_2 = 1.75f
        private const val AXE_SWING_DAMAGER_HEIGHT_2 = 0.5f
        private const val AXE_SWING_DAMAGER_ANIM_INDEX_MAX_2 = 5

        private const val AXE_WALLSLIDE_REGION = "axe_wallslide"
        private const val AXE_SWING_REGION_1 = "axe_swing1"
        private const val AXE_SWING_1_INDEX = 2
        private const val AXE_SWING_REGION_2 = "axe_swing2"
        private val AXE_SWING_2_INDICES = objectSetOf(3, 4, 5)

        private const val REGION_TAG_SUFFIX = "_v2"

        private val regions = ObjectMap<String, TextureRegion>()
        private val animDefs = ObjectMap<String, AnimationDef>()
    }

    private enum class TimberWomanState {
        INIT, STAND, STAND_SWING, STAND_POUND, WALLSLIDE, RUN, JUMP_UP, JUMP_DOWN, JUMP_SPIN
    }

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
        }
    )
    override lateinit var facing: Facing

    private lateinit var stateMachine: StateMachine<TimberWomanState>
    private val currentState: TimberWomanState
        get() = stateMachine.getCurrent()
    private val timers = OrderedMap<TimberWomanState, Timer>()

    private val leafSpawns = Array<Vector2>()
    private val walls = Array<GameRectangle>()

    override fun init() {
        GameLogger.debug(TAG, "init()")

        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES_2.source)

            val keys = gdxArrayOf(ConstKeys.DEFEATED, AXE_WALLSLIDE_REGION, AXE_SWING_REGION_1, AXE_SWING_REGION_2)
            TimberWomanState.entries.forEach { keys.add(it.name.lowercase()) }

            keys.forEach { key -> regions.put(key, atlas.findRegion("${TAG}${REGION_TAG_SUFFIX}/${key}")) }
        }

        if (animDefs.isEmpty) animDefs.putAll(
            TimberWomanState.INIT.name.lowercase() pairTo AnimationDef(7, 1, 0.25f, true), // TODO: replace init def
            TimberWomanState.STAND.name.lowercase() pairTo AnimationDef(7, 1, 0.25f, true),
            TimberWomanState.STAND_POUND.name.lowercase() pairTo AnimationDef(3, 2, 0.1f, false),
            TimberWomanState.STAND_SWING.name.lowercase() pairTo AnimationDef(
                2,
                4,
                gdxArrayOf(0.1f, 0.1f, 0.1f, 0.1f, 0.1f, 0.25f, 0.1f, 0.1f),
                false
            ),
            TimberWomanState.JUMP_SPIN.name.lowercase() pairTo AnimationDef(2, 2, 0.1f, true),
            TimberWomanState.JUMP_UP.name.lowercase() pairTo AnimationDef(2, 1, 0.1f, true),
            TimberWomanState.JUMP_DOWN.name.lowercase() pairTo AnimationDef(2, 1, 0.1f, true),
            TimberWomanState.RUN.name.lowercase() pairTo AnimationDef(2, 2, 0.1f, true),
            TimberWomanState.WALLSLIDE.name.lowercase() pairTo AnimationDef(),
            ConstKeys.DEFEATED pairTo AnimationDef()
        )

        if (timers.isEmpty) timers.putAll(
            TimberWomanState.INIT pairTo Timer(INIT_DUR),
            TimberWomanState.STAND pairTo Timer(STAND_DUR),
            TimberWomanState.RUN pairTo Timer(MAX_RUN_DUR),
            TimberWomanState.WALLSLIDE pairTo Timer(WALL_SLIDE_DUR),
            TimberWomanState.STAND_SWING pairTo Timer(STAND_SWING_DUR)
                .setRunnables(TimeMarkedRunnable(STAND_SWING_GROUND_BURST_TIME) { groundBurst() }),
            TimberWomanState.STAND_POUND pairTo Timer(STAND_POUND_DUR),
            TimberWomanState.JUMP_SPIN pairTo Timer(MAX_JUMP_SPIN_DUR)
        )

        stateMachine = buildStateMachine()

        super.init()

        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)
        body.physics.defaultFrictionOnSelf.x = DEFAULT_FRICTION_X
        body.physics.defaultFrictionOnSelf.y = DEFAULT_FRICTION_Y

        stateMachine.reset()

        timers.values().forEach { it.reset() }

        spawnProps.forEach { key, value ->
            when {
                key.toString().contains(LEAF_SPAWN) -> {
                    val spawn = (value as RectangleMapObject).rectangle.getCenter(false)
                    leafSpawns.add(spawn)
                }

                key.toString().contains(ConstKeys.WALL) -> {
                    val bounds = (value as RectangleMapObject).rectangle.toGameRectangle(false)
                    walls.add(bounds)
                }
            }
        }

        updateFacing()

        putProperty(ConstKeys.ENTITY_KILLED_BY_DEATH_FIXTURE, false)
    }

    override fun isReady(delta: Float) = timers[TimberWomanState.INIT].isFinished()

    override fun onReady() {
        super.onReady()
        body.physics.gravityOn = true
    }

    override fun onDestroy() {
        super.onDestroy()
        leafSpawns.clear()
        walls.clear()
    }

    private fun buildStateMachine(): StateMachine<TimberWomanState> {
        val builder = StateMachineBuilder<TimberWomanState>()
        TimberWomanState.entries.forEach { builder.state(it.name, it) }
        builder.setOnChangeState(this::onChangeState)
        builder.initialState(TimberWomanState.INIT.name)
            .transition(TimberWomanState.INIT.name, TimberWomanState.STAND.name) { ready }
            .transition(TimberWomanState.STAND.name, TimberWomanState.STAND_SWING.name) { true /* TODO */ }
            .transition(TimberWomanState.STAND_SWING.name, TimberWomanState.STAND.name) { true /* TODO */ }
        return builder.build()
    }

    private fun onChangeState(current: TimberWomanState, previous: TimberWomanState) {
        GameLogger.debug(TAG, "onChangeState(): current=$current, previous=$previous")

        timers.values().forEach { it.reset() }
    }

    private fun updateFacing() {
        when (currentState) {
            TimberWomanState.STAND_SWING -> {
                val maxSwingX = when (facing) {
                    Facing.LEFT -> body.getX() - AXE_SWING_DAMAGER_WIDTH_2 * ConstVals.PPM
                    Facing.RIGHT -> body.getMaxX() + AXE_SWING_DAMAGER_WIDTH_2 * ConstVals.PPM
                }
                val maxSwingPoint = GameObjectPools.fetch(Vector2::class).set(maxSwingX, body.getCenter().y)
                if (walls.any { it.contains(maxSwingPoint) }) facing = facing.opposite()
            }

            else -> when {
                megaman.body.getMaxX() < body.getX() -> facing = Facing.LEFT
                megaman.body.getX() > body.getMaxX() -> facing = Facing.RIGHT
            }
        }
    }

    private fun spawnDeadlyLeaf(spawn: Vector2) {
        GameLogger.debug(TAG, "spawnDeadlyLeaf(): spawn=$spawn")
        // TODO
    }

    private fun groundBurst() {
        GameLogger.debug(TAG, "groundBurst()")
        // TODO
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (betweenReadyAndEndBossSpawnEvent) return@add

            if (defeated) {
                body.physics.velocity.setZero()
                body.physics.gravityOn = false
                explodeOnDefeat(delta)
                return@add
            }

            when (currentState) {
                TimberWomanState.INIT,
                TimberWomanState.STAND,
                TimberWomanState.STAND_SWING,
                TimberWomanState.STAND_POUND,
                TimberWomanState.RUN -> {
                    updateFacing()

                    val timer = timers[currentState]
                    if (body.isSensing(BodySense.FEET_ON_GROUND)) timer.update(delta)
                    if (timer.isFinished()) stateMachine.next()
                }

                TimberWomanState.WALLSLIDE -> TODO()
                TimberWomanState.JUMP_UP -> TODO()
                TimberWomanState.JUMP_DOWN -> TODO()
                TimberWomanState.JUMP_SPIN -> TODO()
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(BODY_WIDTH * ConstVals.PPM, BODY_HEIGHT * ConstVals.PPM)
        body.physics.velocityClamp.set(VEL_CLAMP_X * ConstVals.PPM, VEL_CLAMP_Y * ConstVals.PPM)
        body.physics.receiveFrictionX = false

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.75f * ConstVals.PPM, 0.2f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = -BODY_HEIGHT * ConstVals.PPM / 2f
        body.addFixture(feetFixture)
        feetFixture.drawingColor = Color.GREEN
        debugShapes.add { feetFixture }

        val headFixture =
            Fixture(body, FixtureType.HEAD, GameRectangle().setSize(ConstVals.PPM.toFloat(), 0.2f * ConstVals.PPM))
        headFixture.offsetFromBodyAttachment.y = BODY_HEIGHT * ConstVals.PPM / 2f
        body.addFixture(headFixture)
        headFixture.drawingColor = Color.YELLOW
        debugShapes.add { headFixture }

        val leftFixture =
            Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM, ConstVals.PPM.toFloat()))
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        leftFixture.offsetFromBodyAttachment.x = -BODY_WIDTH * ConstVals.PPM / 2f
        body.addFixture(leftFixture)
        leftFixture.drawingColor = Color.ORANGE
        debugShapes.add { leftFixture }

        val rightFixture =
            Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM, ConstVals.PPM.toFloat()))
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        rightFixture.offsetFromBodyAttachment.x = BODY_WIDTH * ConstVals.PPM / 2f
        body.addFixture(rightFixture)
        rightFixture.drawingColor = Color.ORANGE
        debugShapes.add { rightFixture }

        val standSwingDamagerBounds = GameRectangle()
        val standSwingDamagerFixture = Fixture(body, FixtureType.DAMAGER, standSwingDamagerBounds)
        standSwingDamagerFixture.attachedToBody = false
        body.addFixture(standSwingDamagerFixture)
        debugShapes.add { if (standSwingDamagerFixture.isActive()) standSwingDamagerFixture else null }

        // TODO: axe shield

        body.preProcess.put(ConstKeys.DEFAULT) {
            if (body.isSensing(BodySense.HEAD_TOUCHING_BLOCK) && body.physics.velocity.y > 0f)
                body.physics.velocity.y = 0f

            body.physics.gravity.y = when {
                currentState == TimberWomanState.WALLSLIDE -> WALL_SLIDE_GRAVITY * ConstVals.PPM
                body.isSensing(BodySense.FEET_ON_GROUND) -> GROUND_GRAVITY * ConstVals.PPM
                else -> GRAVITY * ConstVals.PPM
            }

            val standSwingDamagerDef = when (currentState) {
                TimberWomanState.STAND_SWING -> {
                    val animIndex = ((animators[TAG] as Animator).currentAnimation as Animation).getIndex()
                    when {
                        animIndex < AXE_SWING_DAMAGER_ANIM_INDEX_1 -> -1
                        animIndex == AXE_SWING_DAMAGER_ANIM_INDEX_1 -> 1
                        animIndex <= AXE_SWING_DAMAGER_ANIM_INDEX_MAX_2 -> 2
                        else -> -1
                    }
                }

                else -> -1
            }

            standSwingDamagerFixture.setActive(standSwingDamagerDef != -1)

            if (standSwingDamagerDef == 1) {
                standSwingDamagerBounds.setSize(
                    AXE_SWING_DAMAGER_WIDTH_1 * ConstVals.PPM,
                    AXE_SWING_DAMAGER_HEIGHT_1 * ConstVals.PPM
                )

                val position = if (isFacing(Facing.LEFT)) Position.CENTER_LEFT else Position.CENTER_RIGHT
                standSwingDamagerBounds.positionOnPoint(
                    body.getBounds().getPositionPoint(position),
                    position.opposite()
                )
            } else if (standSwingDamagerDef == 2) {
                standSwingDamagerBounds.setSize(
                    AXE_SWING_DAMAGER_WIDTH_2 * ConstVals.PPM,
                    AXE_SWING_DAMAGER_HEIGHT_2 * ConstVals.PPM
                )

                val position = if (isFacing(Facing.LEFT)) Position.BOTTOM_LEFT else Position.CENTER_RIGHT
                standSwingDamagerBounds.positionOnPoint(
                    body.getBounds().getPositionPoint(position),
                    position.flipHorizontally()
                )
            }
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this, body, BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE)
        )
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        // main
        .sprite(
            TAG,
            GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 1))
                .also { sprite -> sprite.setSize(SPRITE_SIZE * ConstVals.PPM) }
        )
        .updatable { _, sprite ->
            sprite.setPosition(body.getPositionPoint(Position.BOTTOM_CENTER), Position.BOTTOM_CENTER)

            val flipX = when (currentState) {
                TimberWomanState.WALLSLIDE -> body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT)
                TimberWomanState.STAND_POUND -> false
                else -> isFacing(Facing.LEFT)
            }
            sprite.setFlip(flipX, false)

            sprite.hidden = damageBlink || game.isProperty(ConstKeys.ROOM_TRANSITION, true)
        }

        // axe wallslide
        .sprite(
            AXE_WALLSLIDE_REGION,
            GameSprite(regions[AXE_WALLSLIDE_REGION], DrawingPriority(DrawingSection.PLAYGROUND, 2))
                .also { sprite -> sprite.setSize(SPRITE_SIZE * ConstVals.PPM) }
        )
        .updatable { _, sprite ->
            val show = currentState == TimberWomanState.WALLSLIDE

            sprite.hidden = !show

            if (show) {
                val anchor = sprites[TAG].boundingRectangle.getPositionPoint(Position.BOTTOM_CENTER)
                sprite.setPosition(anchor, Position.TOP_CENTER)

                sprite.setFlip(isFacing(Facing.LEFT), false)
            }
        }

        // axe swing 1
        .sprite(
            AXE_SWING_REGION_1,
            GameSprite(regions[AXE_SWING_REGION_1], DrawingPriority(DrawingSection.PLAYGROUND, 2))
                .also { sprite -> sprite.setSize(SPRITE_SIZE * ConstVals.PPM) }
        )
        .updatable { _, sprite ->
            val show = when {
                currentState != TimberWomanState.STAND_SWING -> false
                else -> {
                    val animation = (animators[TAG] as Animator).currentAnimation as Animation
                    val index = animation.getIndex()
                    index == AXE_SWING_1_INDEX
                }
            }

            sprite.hidden = !show

            if (show) {
                val position = if (isFacing(Facing.LEFT)) Position.CENTER_LEFT else Position.CENTER_RIGHT
                val anchor = sprites[TAG].boundingRectangle.getPositionPoint(position)
                sprite.setPosition(anchor, position.opposite())

                sprite.setFlip(isFacing(Facing.LEFT), false)
            }
        }

        // axe swing 2
        .sprite(
            AXE_SWING_REGION_2,
            GameSprite(regions[AXE_SWING_REGION_2], DrawingPriority(DrawingSection.PLAYGROUND, 2))
                .also { sprite -> sprite.setSize(SPRITE_SIZE * ConstVals.PPM) }
        )
        .updatable { _, sprite ->
            val show = when {
                currentState != TimberWomanState.STAND_SWING -> false
                else -> {
                    val animation = (animators[TAG] as Animator).currentAnimation as Animation
                    val index = animation.getIndex()
                    AXE_SWING_2_INDICES.contains(index)
                }
            }

            sprite.hidden = !show

            if (show) {
                val position = if (isFacing(Facing.LEFT)) Position.CENTER_LEFT else Position.CENTER_RIGHT
                val anchor = sprites[TAG].boundingRectangle.getPositionPoint(position)
                sprite.setPosition(anchor, position.opposite())

                sprite.setFlip(isFacing(Facing.LEFT), false)
            }
        }

        // build
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier {
                    when {
                        defeated -> ConstKeys.DEFEATED
                        else -> when (currentState) {
                            TimberWomanState.INIT -> when {
                                body.isSensing(BodySense.FEET_ON_GROUND) -> TimberWomanState.INIT.name.lowercase()
                                else -> TimberWomanState.JUMP_DOWN.name.lowercase()
                            }

                            else -> currentState.name.lowercase()
                        }
                    }
                }
                .applyToAnimations { animations ->
                    val keys = gdxArrayOf(ConstKeys.DEFEATED)
                    TimberWomanState.entries.forEach { state -> keys.add(state.name.lowercase()) }
                    keys.forEach { key ->
                        val def = animDefs[key]
                        GameLogger.debug(TAG, "defineAnimationsComponent(): putting animation: key=$key, def=$def")
                        animations.put(key, Animation(regions[key], def.rows, def.cols, def.durations, def.loop))
                    }
                }
                .setOnChangeKeyListener { currentKey, nextKey ->
                    GameLogger.debug(
                        TAG,
                        "defineAnimationsComponent(): on change key listener: currentKey=$currentKey, nextKey=$nextKey"
                    )
                }
                .build()
        )
        .build()

    override fun getTag() = TAG
}
