package com.megaman.maverick.game.entities.megaman.components

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.ProcessState
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.bosses.GutsTank
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.AButtonTask
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.megaman.constants.MegamanWeapon
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.misc.DirectionPositionMapper
import com.megaman.maverick.game.world.body.*
import kotlin.math.abs

const val MEGAMAN_BODY_WIDTH = 1f
const val MEGAMAN_BODY_HEIGHT = 1.5f

// slightly less than 1 so that Megaman can slide under spaces that are 1 tile in height
const val GROUNDSLIDE_CROUCH_HEIGHT = 0.9f

val BEHAVIORS_TO_END_ON_BOUNCE = gdxArrayOf(
    BehaviorType.WALL_SLIDING,
    BehaviorType.AIR_DASHING,
    BehaviorType.GROUND_SLIDING,
    BehaviorType.SWIMMING,
    BehaviorType.JETPACKING,
    BehaviorType.CLIMBING,
    BehaviorType.CROUCHING
)

val Megaman.feetFixture: Fixture
    get() = body.getProperty(ConstKeys.FEET) as Fixture

val Megaman.leftSideFixture: Fixture
    get() = body.getProperty("${ConstKeys.LEFT}_${ConstKeys.SIDE}", Fixture::class)!!

val Megaman.rightSideFixture: Fixture
    get() = body.getProperty("${ConstKeys.RIGHT}_${ConstKeys.SIDE}", Fixture::class)!!

val Megaman.headFixture: Fixture
    get() = body.getProperty(ConstKeys.HEAD, Fixture::class)!!

val Megaman.bodyFixture: Fixture
    get() = body.getProperty(ConstKeys.BODY, Fixture::class)!!

val Megaman.damageableFixture: Fixture
    get() = body.getProperty(ConstKeys.DAMAGEABLE, Fixture::class)!!

val Megaman.feetOnGround: Boolean
    get() = (body.getProperty(ConstKeys.FEET_ON_GROUND) as () -> Boolean).invoke()

internal fun Megaman.defineBodyComponent(): BodyComponent {
    val body = Body(BodyType.DYNAMIC)
    body.putProperty("${ConstKeys.ICE}_${ConstKeys.FRICTION_Y}", false)
    body.physics.applyFrictionX = true
    body.physics.applyFrictionY = true
    body.drawingColor = Color.GRAY

    val debugShapes = Array<() -> IDrawableShape?>()
    // debugShapes.add { body.getBounds() }

    val playerFixture = Fixture(body, FixtureType.PLAYER, GameRectangle())
    body.addFixture(playerFixture)

    val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle())
    body.addFixture(bodyFixture)
    body.putProperty(ConstKeys.BODY, bodyFixture)

    val onBounce: () -> Unit = {
        BEHAVIORS_TO_END_ON_BOUNCE.forEach { behaviorType ->
            if (isBehaviorActive(behaviorType)) {
                val behavior = getBehavior(behaviorType)
                if (behavior?.isActive() == true) behavior.reset()
            }
        }

        wallSlideNotAllowedTimer.resetDuration(MegamanValues.WALLSLIDE_NOT_ALLOWED_DELAY_ON_BOUNCE)
    }

    val feetFixture =
        Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.5f * ConstVals.PPM, 0.25f * ConstVals.PPM))
    feetFixture.setRunnable {
        onBounce.invoke()
        if (!body.isSensing(BodySense.IN_WATER)) aButtonTask = AButtonTask.AIR_DASH
    }
    feetFixture.setShouldStickToBlock { _, _ -> !body.isSensing(BodySense.IN_WATER) }
    body.addFixture(feetFixture)
    feetFixture.drawingColor = Color.GREEN
    // debugShapes.add { feetFixture }
    body.putProperty(ConstKeys.FEET, feetFixture)

    // The feet gravity fixture is a consumer that checks for overlap with blocks. If there is a contact with a block,
    // then Megaman's gravity should be adjusted accordingly. Note the differences in size and offset between this feet
    // fixture and the other feet fixture.
    val feetGravityFixture =
        Fixture(body, FixtureType.CONSUMER, GameRectangle().setSize(0.9f * ConstVals.PPM, 0.1f * ConstVals.PPM))
    feetGravityFixture.setFilter { it.getType() == FixtureType.BLOCK }
    val feetGravitySet = ObjectSet<IFixture>()
    feetGravityFixture.putProperty(ConstKeys.SET, feetGravitySet)
    feetGravityFixture.setConsumer consumer@{ processState, fixture ->
        when (processState) {
            ProcessState.BEGIN, ProcessState.CONTINUE -> {
                val block = fixture.getEntity() as Block

                if (block.body.hasBodyLabel(BodyLabel.COLLIDE_DOWN_ONLY) && body.physics.velocity.y > 0f) {
                    feetGravitySet.remove(fixture)
                    return@consumer
                }

                feetGravitySet.add(fixture)
            }

            ProcessState.END -> feetGravitySet.remove(fixture)
        }
    }
    val isFeetOnGround: () -> Boolean = { !feetGravitySet.isEmpty }
    body.putProperty(ConstKeys.FEET_ON_GROUND, isFeetOnGround)
    body.onReset.put("${ConstKeys.FEET}_${ConstKeys.GRAVITY}") { feetGravitySet.clear() }
    body.addFixture(feetGravityFixture)

    val headFixture =
        Fixture(body, FixtureType.HEAD, GameRectangle().setSize(0.75f * ConstVals.PPM, 0.25f * ConstVals.PPM))
    headFixture.setRunnable(onBounce)
    body.addFixture(headFixture)
    headFixture.drawingColor = Color.ORANGE
    // debugShapes.add { headFixture }
    body.putProperty(ConstKeys.HEAD, headFixture)

    val leftFixture =
        Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.2f * ConstVals.PPM, ConstVals.PPM.toFloat()))
    leftFixture.offsetFromBodyAttachment.y = 0.1f * ConstVals.PPM
    leftFixture.setRunnable(onBounce)
    leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
    leftFixture.setShouldStickToBlock shouldStick@{ _, blockFixture ->
        val owner = blockFixture.getEntity().getProperty(ConstKeys.OWNER) as IGameEntity?
        return@shouldStick owner !is GutsTank
    }
    body.addFixture(leftFixture)
    // debugShapes.add { leftFixture }
    body.putProperty("${ConstKeys.LEFT}_${ConstKeys.SIDE}", leftFixture)

    val rightFixture =
        Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.2f * ConstVals.PPM, ConstVals.PPM.toFloat()))
    rightFixture.offsetFromBodyAttachment.y = 0.1f * ConstVals.PPM
    rightFixture.setRunnable(onBounce)
    rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
    rightFixture.setShouldStickToBlock shouldStick@{ _, blockFixture ->
        val owner = blockFixture.getEntity().getProperty(ConstKeys.OWNER) as IGameEntity?
        return@shouldStick owner !is GutsTank
    }
    body.addFixture(rightFixture)
    // debugShapes.add { rightFixture }
    body.putProperty("${ConstKeys.RIGHT}_${ConstKeys.SIDE}", rightFixture)

    val damagableRect = GameRectangle()
    val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, damagableRect)
    damageableFixture.attachedToBody = false
    body.addFixture(damageableFixture)
    body.putProperty(ConstKeys.DAMAGEABLE, damageableFixture)
    damageableFixture.drawingColor = Color.PURPLE
    debugShapes.add { damageableFixture }

    val waterListenerFixture = Fixture(body, FixtureType.WATER_LISTENER, GameRectangle())
    body.addFixture(waterListenerFixture)

    val teleporterListenerFixture = Fixture(body, FixtureType.TELEPORTER_LISTENER, GameRectangle())
    body.addFixture(teleporterListenerFixture)

    val needleSpinDamagerFixture = Fixture(body, FixtureType.DAMAGER, GameCircle().setRadius(ConstVals.PPM.toFloat()))
    body.addFixture(needleSpinDamagerFixture)
    needleSpinDamagerFixture.drawingColor = Color.ORANGE
    // debugShapes.add { needleSpinDamagerFixture }

    val needleSpinShieldFixture = Fixture(body, FixtureType.SHIELD, GameCircle().setRadius(ConstVals.PPM.toFloat()))
    body.addFixture(needleSpinShieldFixture)
    needleSpinShieldFixture.drawingColor = Color.PURPLE
    // debugShapes.add { needleSpinShieldFixture }

    val axeShieldFixture = Fixture(
        body,
        FixtureType.SHIELD,
        GameRectangle().setHeight(ConstVals.PPM.toFloat())
    )
    axeShieldFixture.offsetFromBodyAttachment.y = -0.1f * ConstVals.PPM
    body.addFixture(axeShieldFixture)
    axeShieldFixture.drawingColor = Color.GREEN
    debugShapes.add { if (axeShieldFixture.isActive()) axeShieldFixture else null }

    val fixturesToSizeToBody = gdxArrayOf(
        bodyFixture, playerFixture, waterListenerFixture, teleporterListenerFixture
    )

    body.preProcess.put(ConstKeys.DEFAULT) {
        if (abs(body.physics.velocity.x) < 0.025f * ConstVals.PPM) body.physics.velocity.x = 0f
        if (abs(body.physics.velocity.y) < 0.025f * ConstVals.PPM) body.physics.velocity.y = 0f

        val needleSpinning = currentWeapon == MegamanWeapon.NEEDLE_SPIN && shooting
        needleSpinDamagerFixture.setActive(needleSpinning)
        needleSpinShieldFixture.setActive(needleSpinning)

        val height = when {
            isAnyBehaviorActive(BehaviorType.GROUND_SLIDING, BehaviorType.CROUCHING) -> GROUNDSLIDE_CROUCH_HEIGHT
            else -> MEGAMAN_BODY_HEIGHT
        }
        body.setSize(MEGAMAN_BODY_WIDTH * ConstVals.PPM, height * ConstVals.PPM)

        fixturesToSizeToBody.forEach { fixture ->
            val bounds = fixture.rawShape as GameRectangle
            bounds.set(body)
        }

        axeShieldFixture.setActive(currentWeapon == MegamanWeapon.AXE_SWINGER)
        val axeShieldWidth = if (isBehaviorActive(BehaviorType.CLIMBING)) 0.75f else 0.5f
        (axeShieldFixture.rawShape as GameRectangle).setWidth(axeShieldWidth * ConstVals.PPM)
        val axeShieldOffsetX = when {
            isBehaviorActive(BehaviorType.CLIMBING) -> 0f
            slipSliding -> 0.5f
            else -> 0.75f
        }
        axeShieldFixture.offsetFromBodyAttachment.x = axeShieldOffsetX * ConstVals.PPM * facing.value

        feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        feetGravityFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f

        headFixture.offsetFromBodyAttachment.y = body.getHeight() / 2f
        leftFixture.offsetFromBodyAttachment.x = -body.getWidth() / 2f
        rightFixture.offsetFromBodyAttachment.x = body.getWidth() / 2f

        if (!ready) {
            body.physics.velocity.setZero()
            body.physics.gravity.setZero()
            return@put
        }

        val wallSlidingOnIce = isBehaviorActive(BehaviorType.WALL_SLIDING) &&
            (body.isSensingAny(BodySense.SIDE_TOUCHING_ICE_LEFT, BodySense.SIDE_TOUCHING_ICE_RIGHT))

        var gravityValue = when {
            body.isSensing(BodySense.IN_WATER) -> when {
                wallSlidingOnIce || frozen -> waterIceGravity
                else -> waterGravity
            }
            !feetGravitySet.isEmpty -> groundGravity
            wallSlidingOnIce || frozen -> iceGravity
            isBehaviorActive(BehaviorType.JUMPING) -> jumpGravity
            else -> fallGravity
        }

        gravityValue *= gravityScalar

        when (direction) {
            Direction.UP,
            Direction.DOWN -> {
                body.physics.gravity.set(0f, gravityValue * ConstVals.PPM)
                body.physics.defaultFrictionOnSelf.set(ConstVals.STANDARD_RESISTANCE_X, ConstVals.STANDARD_RESISTANCE_Y)
                body.physics.velocityClamp.set(MegamanValues.CLAMP_X, MegamanValues.CLAMP_Y)
                    .scl(ConstVals.PPM.toFloat())

                damagableRect.let {
                    val size = GameObjectPools.fetch(Vector2::class)
                    when {
                        isAnyBehaviorActive(BehaviorType.GROUND_SLIDING, BehaviorType.CROUCHING) ->
                            size.set(MEGAMAN_BODY_HEIGHT, MEGAMAN_BODY_WIDTH)
                        else -> size.set(MEGAMAN_BODY_WIDTH, MEGAMAN_BODY_HEIGHT)
                    }
                    it.setSize(size.scl(ConstVals.PPM.toFloat()))

                    val position = DirectionPositionMapper.getInvertedPosition(direction)
                    it.positionOnPoint(body.getBounds().getPositionPoint(position), position)
                }
            }
            else -> {
                body.physics.gravity.set(gravityValue * ConstVals.PPM, 0f)
                body.physics.defaultFrictionOnSelf.set(ConstVals.STANDARD_RESISTANCE_Y, ConstVals.STANDARD_RESISTANCE_X)
                body.physics.velocityClamp.set(MegamanValues.CLAMP_Y, MegamanValues.CLAMP_X)
                    .scl(ConstVals.PPM.toFloat())

                damagableRect.let {
                    val size = GameObjectPools.fetch(Vector2::class)
                    when {
                        isAnyBehaviorActive(BehaviorType.GROUND_SLIDING, BehaviorType.CROUCHING) ->
                            size.set(MEGAMAN_BODY_WIDTH, MEGAMAN_BODY_HEIGHT)
                        else -> size.set(MEGAMAN_BODY_HEIGHT, MEGAMAN_BODY_WIDTH)
                    }
                    it.setSize(size.scl(ConstVals.PPM.toFloat()))

                    val position = DirectionPositionMapper.getInvertedPosition(direction)
                    it.positionOnPoint(body.getBounds().getPositionPoint(position), position)
                }
            }
        }
    }

    addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

    return BodyComponentCreator.create(this, body)
}

fun Megaman.getMaxRunSpeed(): Float {
    var threshold = MegamanValues.RUN_MAX_SPEED * ConstVals.PPM
    if (body.isSensing(BodySense.IN_WATER)) threshold *= MegamanValues.WATER_RUN_MAX_SPEED_SCALAR
    else if (game.getCurrentLevel() == LevelDefinition.MOON_MAN) threshold *= MegamanValues.MOON_RUN_MAX_SPEED_SCALAR
    return threshold
}
