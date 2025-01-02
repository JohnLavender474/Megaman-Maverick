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
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.putAll
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setPosition
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
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getPositionPoint
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
        private const val STAND_DUR = 1.5f
        private const val MAX_RUN_DUR = 2f
        private const val WALL_SLIDE_DUR = 0.75f
        private const val STAND_SWING_DUR = 1f
        private const val MAX_JUMP_SPIN_DUR = 1.5f

        private const val GRAVITY = -0.15f
        private const val GROUND_GRAVITY = -0.01f
        private const val WALL_SLIDE_GRAVITY = -0.075f

        private const val DEFAULT_FRICTION_X = 6f
        private const val DEFAULT_FRICTION_Y = 1f
        private const val WALL_SLIDE_FRICTION_Y = 6f

        private const val REGION_TAG_SUFFIX = "_v2"

        private const val AXE_WALLSLIDE_REGION = "axe_wallslide"
        private const val AXE_SWING_REGION_1 = "axe_swing_1"
        private const val AXE_SWING_REGION_2 = "axe_swing_2"

        private val regions = ObjectMap<String, TextureRegion>()
        private val animDefs = ObjectMap<TimberWomanState, AnimationDef>()
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

    private val timers = OrderedMap<String, Timer>()

    private lateinit var stateMachine: StateMachine<TimberWomanState>
    private val currentState: TimberWomanState
        get() = stateMachine.getCurrent()

    private val leafSpawns = Array<Vector2>()

    override fun init() {
        GameLogger.debug(TAG, "init()")

        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES_2.source)

            val keys = gdxArrayOf(AXE_WALLSLIDE_REGION, AXE_SWING_REGION_1, AXE_SWING_REGION_2)
            TimberWomanState.entries.forEach { keys.add(it.name.lowercase()) }

            keys.forEach { key -> regions.put(key, atlas.findRegion("${TAG}${REGION_TAG_SUFFIX}/${key}")) }
        }

        if (animDefs.isEmpty) animDefs.putAll(
            TimberWomanState.STAND pairTo AnimationDef(7, 1, 0.125f, true),
            TimberWomanState.STAND_POUND pairTo AnimationDef(3, 2, 0.1f, false),
            TimberWomanState.STAND_SWING pairTo AnimationDef(3, 2, 0.1f, false),
            TimberWomanState.JUMP_SPIN pairTo AnimationDef(2, 2, 0.1f, true),
            TimberWomanState.JUMP_UP pairTo AnimationDef(2, 1, 0.1f, true),
            TimberWomanState.JUMP_DOWN pairTo AnimationDef(2, 1, 0.1f, true),
            TimberWomanState.RUN pairTo AnimationDef(2, 2, 0.1f, true),
            TimberWomanState.WALLSLIDE pairTo AnimationDef(),
        )

        if (timers.isEmpty) timers.putAll(
            ConstKeys.INIT pairTo Timer(INIT_DUR),
            ConstKeys.STAND pairTo Timer(STAND_DUR),
            ConstKeys.RUN pairTo Timer(MAX_RUN_DUR),
            "wallslide" pairTo Timer(WALL_SLIDE_DUR),
            "stand_swing" pairTo Timer(STAND_SWING_DUR),
            "jump_spin" pairTo Timer(MAX_JUMP_SPIN_DUR)
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
            if (key.toString().contains(LEAF_SPAWN)) {
                val spawn = (value as RectangleMapObject).rectangle.getCenter(false)
                leafSpawns.add(spawn)
            }
        }

        updateFacing()

        putProperty(ConstKeys.ENTITY_KILLED_BY_DEATH_FIXTURE, false)
    }

    override fun isReady(delta: Float) = timers[ConstKeys.INIT].isFinished()

    override fun onReady() {
        super.onReady()
        body.physics.gravityOn = true
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
        // TODO
    }

    private fun updateFacing() {
        when {
            megaman.body.getMaxX() < body.getX() -> facing = Facing.LEFT
            megaman.body.getX() > body.getMaxX() -> facing = Facing.RIGHT
        }
    }

    private fun spawnDeadlyLeaf(spawn: Vector2) {
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

            // TODO: not all states should update facing
            updateFacing()

            when (currentState) {
                TimberWomanState.INIT -> TODO()
                TimberWomanState.STAND -> TODO()
                TimberWomanState.STAND_SWING -> TODO()
                TimberWomanState.STAND_POUND -> TODO()
                TimberWomanState.WALLSLIDE -> TODO()
                TimberWomanState.RUN -> TODO()
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
        rightFixture.offsetFromBodyAttachment.x = BODY_WIDTH * ConstVals.PPM / 2f
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        body.addFixture(rightFixture)
        rightFixture.drawingColor = Color.ORANGE
        debugShapes.add { rightFixture }

        // TODO: shield and spin damager fixtures

        body.preProcess.put(ConstKeys.DEFAULT) {
            if (body.isSensing(BodySense.HEAD_TOUCHING_BLOCK) && body.physics.velocity.y > 0f)
                body.physics.velocity.y = 0f

            body.physics.gravity.y = when {
                currentState == TimberWomanState.WALLSLIDE -> WALL_SLIDE_GRAVITY * ConstVals.PPM
                body.isSensing(BodySense.FEET_ON_GROUND) -> GROUND_GRAVITY * ConstVals.PPM
                else -> GRAVITY * ConstVals.PPM
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
            TAG, GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 1))
                .apply { setSize(SPRITE_SIZE * ConstVals.PPM) }
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
                .apply { setSize(SPRITE_SIZE * ConstVals.PPM) }
        )
        .updatable { _, sprite ->
            // TODO
            sprite.hidden = true
        }

        // axe swing 1
        .sprite(
            AXE_SWING_REGION_1,
            GameSprite(regions[AXE_SWING_REGION_1], DrawingPriority(DrawingSection.PLAYGROUND, 2))
                .apply { setSize(SPRITE_SIZE * ConstVals.PPM) }
        )
        .updatable { _, sprite ->
            // TODO
            sprite.hidden = true
        }

        // axe swing 2
        .sprite(
            AXE_SWING_REGION_2,
            GameSprite(regions[AXE_SWING_REGION_2], DrawingPriority(DrawingSection.PLAYGROUND, 2))
                .apply { setSize(SPRITE_SIZE * ConstVals.PPM) }
        )
        .updatable { _, sprite ->
            // TODO
            sprite.hidden = true
        }

        // build
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier {
                    if (defeated) ConstKeys.DEFEATED
                    else when (currentState) {
                        TimberWomanState.INIT ->
                            if (body.isSensing(BodySense.FEET_ON_GROUND)) ConstKeys.INIT else ConstKeys.JUMP

                        else -> currentState.name.lowercase()
                    }
                    currentState.name.lowercase()
                }
                .applyToAnimations { animations ->
                    TimberWomanState.entries.forEach { state ->
                        val key = state.name.lowercase()
                        val def = animDefs[state]
                        animations.put(key, Animation(regions[key], def.rows, def.cols, def.durations, def.loop))
                    }
                }
                .build()
        )
        .build()
}
