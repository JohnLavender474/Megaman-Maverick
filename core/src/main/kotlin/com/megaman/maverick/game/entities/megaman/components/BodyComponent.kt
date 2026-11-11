package com.megaman.maverick.game.entities.megaman.components

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.enums.ProcessState
import com.maverick.game.engine.common.shapes.GameCircle
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.drawables.shapes.DrawableShapesComponent
import com.maverick.game.engine.drawables.shapes.IDrawableShape
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.entities.isBehaviorActive
import com.maverick.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.bosses.GutsTank
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.AButtonTask
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.megaman.constants.MegamanWeapon
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.levels.LevelType
import com.megaman.maverick.game.world.body.*
import com.megaman.maverick.game.players.bodyOverGround
import com.megaman.maverick.game.players.playerMaxRunSpeed
import com.megaman.maverick.game.players.PlayerBodyParams
import com.megaman.maverick.game.players.playerBody

const val MEGAMAN_BODY_WIDTH = 1f
const val MEGAMAN_BODY_HEIGHT = 1.5f

// slightly less than 1 so that Megaman can slide under spaces that are 1 tile in height
const val GROUNDSLIDE_CROUCH_HEIGHT = 0.9f

internal fun Megaman.defineBodyComponent(): BodyComponent {
    lateinit var weaponPreStep: () -> Unit
    val body = playerBody(
        PlayerBodyParams(
            width = MEGAMAN_BODY_WIDTH,
            height = MEGAMAN_BODY_HEIGHT,
            crouchHeight = GROUNDSLIDE_CROUCH_HEIGHT,
            gravity = playerGravity,
            waterGravityScalar = MegamanValues.WATER_GRAVITY_SCALAR,
            clampX = MegamanValues.CLAMP_X,
            clampY = MegamanValues.CLAMP_Y,
            onBounce = { wallSlideNotAllowedTimer.reset() },
            onFeetBounce = { if (!body.isSensing(BodySense.IN_WATER)) aButtonTask = AButtonTask.AIR_DASH },
            onFeetBlockBegin = { makesSound ->
                aButtonTask = AButtonTask.JUMP
                if (makesSound && canMakeLandSound) {
                    requestToPlaySound(SoundAsset.MEGAMAN_LAND_SOUND, false)
                    canMakeLandSound = false
                }
            },
            onFeetBlockContinue = { aButtonTask = AButtonTask.JUMP },
            onFeetBlockEnd = {
                if (!bodyOverGround) {
                    aButtonTask = when {
                        body.isSensing(BodySense.IN_WATER) -> AButtonTask.SWIM
                        else -> AButtonTask.AIR_DASH
                    }
                }
                if (!body.hasAnyFeetBlock()) canMakeLandSound = true
            },
            onHeadHitBlock = {
                if (services.levelState.currentLevel!!.type == LevelType.MARIO_LEVEL) {
                    val shouldMakeBumpSound = when (direction) {
                        Direction.UP -> body.physics.velocity.y >= 0f
                        Direction.DOWN -> body.physics.velocity.y <= 0f
                        Direction.LEFT -> body.physics.velocity.x <= 0f
                        Direction.RIGHT -> body.physics.velocity.x >= 0f
                    }
                    if (shouldMakeBumpSound) requestToPlaySound(SoundAsset.SMB3_BUMP_SOUND, false)
                }
            },
            sideSticksTo = { blockFixture ->
                val owner = blockFixture.getEntity().getProperty(ConstKeys.OWNER) as IGameEntity?
                owner !is GutsTank
            },
            onWater = { state ->
                when (state) {
                    ProcessState.BEGIN, ProcessState.CONTINUE -> if (
                        !body.isSensing(BodySense.FEET_ON_GROUND) && !isBehaviorActive(BehaviorType.WALL_SLIDING)
                    ) aButtonTask = AButtonTask.SWIM

                    ProcessState.END -> aButtonTask = AButtonTask.AIR_DASH
                }
            },
            onPreStep = { weaponPreStep() }
        )
    )

    val debugShapes = Array<() -> IDrawableShape?>()
    debugShapes.add { body.getProperty(ConstKeys.DAMAGEABLE) as Fixture }

    val needleSpinDamagerFixture = Fixture(body, FixtureType.DAMAGER, GameCircle().setRadius(ConstVals.PPM.toFloat()))
    body.addFixture(needleSpinDamagerFixture)
    needleSpinDamagerFixture.drawingColor = Color.ORANGE

    val needleSpinShieldFixture = Fixture(body, FixtureType.SHIELD, GameCircle().setRadius(ConstVals.PPM.toFloat()))
    body.addFixture(needleSpinShieldFixture)
    needleSpinShieldFixture.drawingColor = Color.PURPLE

    val axeShieldFixture = Fixture(
        body,
        FixtureType.SHIELD,
        GameRectangle().setHeight(MEGAMAN_BODY_HEIGHT * ConstVals.PPM)
    )
    axeShieldFixture.putProperty(ConstKeys.AXE, true)
    body.addFixture(axeShieldFixture)
    axeShieldFixture.drawingColor = Color.GREEN
    debugShapes.add { if (axeShieldFixture.isActive()) axeShieldFixture else null }

    weaponPreStep = {
        val needleSpinning = currentWeapon == MegamanWeapon.NEEDLE_SPIN && shooting
        needleSpinDamagerFixture.setActive(needleSpinning)
        needleSpinShieldFixture.setActive(needleSpinning)

        axeShieldFixture.setActive(currentWeapon == MegamanWeapon.AXE_SWINGER)
        val axeShieldWidth = if (isBehaviorActive(BehaviorType.CLIMBING)) 0.75f else 0.5f
        (axeShieldFixture.rawShape as GameRectangle).setWidth(axeShieldWidth * ConstVals.PPM)
        val axeShieldOffsetX = when {
            isBehaviorActive(BehaviorType.CLIMBING) -> 0f
            slipSliding -> 0.5f
            else -> 0.75f
        }
        axeShieldFixture.offsetFromBodyAttachment.x = axeShieldOffsetX * ConstVals.PPM * facing.value
        val axeShieldReflectDir = when (direction) {
            Direction.UP -> if (isFacing(Facing.LEFT)) Direction.LEFT else Direction.RIGHT
            Direction.DOWN -> if (isFacing(Facing.LEFT)) Direction.RIGHT else Direction.LEFT
            Direction.LEFT -> if (isFacing(Facing.LEFT)) Direction.DOWN else Direction.UP
            Direction.RIGHT -> if (isFacing(Facing.LEFT)) Direction.UP else Direction.DOWN
        }
        axeShieldFixture.putProperty("${ConstKeys.REFLECT}_${ConstKeys.DIRECTION}", axeShieldReflectDir)
    }

    addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

    return BodyComponentCreator.create(this, body)
}

fun Megaman.getMaxRunSpeed(): Float {
    var threshold = playerMaxRunSpeed(MegamanValues.RUN_MAX_SPEED, MegamanValues.WATER_RUN_MAX_SPEED_SCALAR)
    if (!body.isSensing(BodySense.IN_WATER) && services.levelState.currentLevel!! == LevelDefinition.MOON_MAN)
        threshold *= MegamanValues.MOON_RUN_MAX_SPEED_SCALAR
    return threshold
}
