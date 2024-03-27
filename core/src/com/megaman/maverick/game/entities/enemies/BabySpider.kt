package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.GameLogger
import com.engine.common.extensions.getTextureRegion
import com.engine.common.extensions.objectMapOf
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.damage.IDamager
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setSize
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.BodySense
import com.megaman.maverick.game.world.FixtureType
import com.megaman.maverick.game.world.isSensing
import kotlin.reflect.KClass

class BabySpider(game: MegamanMaverickGame) : AbstractEnemy(game) {

    companion object {
        const val TAG = "BabySpider"
        private const val SLOW_SPEED = 2.5f
        private const val FAST_SPEED = 5f
        private const val GRAVITY_BEFORE_LAND = -0.375f
        private var runRegion: TextureRegion? = null
        private var stillRegion: TextureRegion? = null
    }

    private enum class BabySpiderState {
        FALLING, RUNNING_ON_GROUND, RUNNING_ON_CEILING, SCALING_WALL_LEFT, SCALING_WALL_RIGHT
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class to dmgNeg(10), Fireball::class to dmgNeg(ConstVals.MAX_HEALTH), ChargedShot::class to dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) ConstVals.MAX_HEALTH else 15
        }, ChargedShotExplosion::class to dmgNeg(15)
    )

    private lateinit var state: BabySpiderState

    private var landed = false
    private var leftOnLand = false

    private var slow = false

    private var wasLeftTouchingBlock = false
    private var wasRightTouchingBlock = false
    private var wasHeadTouchingBlock = false
    private var wasFeetTouchingBlock = false

    override fun init() {
        if (runRegion == null || stillRegion == null) {
            runRegion = game.assMan.getTextureRegion(TextureAsset.ENEMIES_2.source, "BabySpider/Run")
            stillRegion = game.assMan.getTextureRegion(TextureAsset.ENEMIES_2.source, "BabySpider/Still")
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.DROP_ITEM_ON_DEATH, false)
        super.spawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)
        body.physics.gravity.y = GRAVITY_BEFORE_LAND * ConstVals.PPM

        state = BabySpiderState.FALLING
        leftOnLand = spawnProps.get(ConstKeys.LEFT, Boolean::class)!!
        slow = spawnProps.getOrDefault(ConstKeys.SLOW, false, Boolean::class)
        landed = false

        wasLeftTouchingBlock = false
        wasRightTouchingBlock = false
        wasHeadTouchingBlock = false
        wasFeetTouchingBlock = false
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy() position = ${body.getCenter()}")
        super.onDestroy()
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(0.5f * ConstVals.PPM, 0.25f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle().set(body))
        body.addFixture(bodyFixture)
        bodyFixture.getShape().color = Color.GRAY
        debugShapes.add { bodyFixture.getShape() }

        val leftFixture = Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM))
        leftFixture.offsetFromBodyCenter.x = -0.275f * ConstVals.PPM
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        body.addFixture(leftFixture)
        leftFixture.getShape().color = Color.YELLOW
        debugShapes.add { leftFixture.getShape() }

        val rightFixture = Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM))
        rightFixture.offsetFromBodyCenter.x = 0.275f * ConstVals.PPM
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        body.addFixture(rightFixture)
        rightFixture.getShape().color = Color.YELLOW
        debugShapes.add { rightFixture.getShape() }

        val feetFixture = Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyCenter.y = -0.15f * ConstVals.PPM
        body.addFixture(feetFixture)
        feetFixture.getShape().color = Color.GREEN
        debugShapes.add { feetFixture.getShape() }

        val headFixture = Fixture(body, FixtureType.HEAD, GameRectangle().setSize(0.1f * ConstVals.PPM))
        headFixture.offsetFromBodyCenter.y = 0.15f * ConstVals.PPM
        body.addFixture(headFixture)
        headFixture.getShape().color = Color.BLUE
        debugShapes.add { headFixture.getShape() }

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().set(body))
        body.addFixture(damagerFixture)
        damagerFixture.getShape().color = Color.RED
        debugShapes.add { damagerFixture.getShape() }

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().set(body))
        body.addFixture(damageableFixture)
        damageableFixture.getShape().color = Color.PURPLE
        debugShapes.add { damageableFixture.getShape() }

        addComponent(DrawableShapesComponent(this, debugShapeSuppliers = debugShapes, debug = true))

        body.preProcess.put(ConstKeys.DEFAULT) {
            val isLeftTouchingBlock = body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT)
            val isRightTouchingBlock = body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT)
            val isHeadTouchingBlock = body.isSensing(BodySense.HEAD_TOUCHING_BLOCK)
            val isFeetTouchingBlock = body.isSensing(BodySense.FEET_ON_GROUND)

            if (!wasLeftTouchingBlock && isLeftTouchingBlock) {
                state = BabySpiderState.SCALING_WALL_LEFT
                GameLogger.debug(TAG, "Change state to $state")
            } else if (!wasRightTouchingBlock && isRightTouchingBlock) {
                state = BabySpiderState.SCALING_WALL_RIGHT
                GameLogger.debug(TAG, "Change state to $state")
            } else if (!wasHeadTouchingBlock && isHeadTouchingBlock) {
                state = BabySpiderState.RUNNING_ON_CEILING
                GameLogger.debug(TAG, "Change state to $state")
            } else if (!wasFeetTouchingBlock && isFeetTouchingBlock) {
                state = BabySpiderState.RUNNING_ON_GROUND
                GameLogger.debug(TAG, "Change state to $state")
            } else if (!isLeftTouchingBlock && !isRightTouchingBlock && !isHeadTouchingBlock && !isFeetTouchingBlock) {
                state = BabySpiderState.FALLING
                GameLogger.debug(TAG, "Change state to $state")
            }

            val speed = ConstVals.PPM * if (slow) SLOW_SPEED else FAST_SPEED * if (leftOnLand) -1f else 1f
            when (state) {
                BabySpiderState.FALLING -> body.physics.velocity.x = 0f
                BabySpiderState.RUNNING_ON_GROUND -> body.physics.velocity.set(speed, 0f)
                BabySpiderState.RUNNING_ON_CEILING -> body.physics.velocity.set(-speed, 0f)
                BabySpiderState.SCALING_WALL_LEFT -> body.physics.velocity.set(0f, -speed)
                BabySpiderState.SCALING_WALL_RIGHT -> body.physics.velocity.set(0f, speed)
            }

            body.physics.gravity.y = if (state == BabySpiderState.FALLING) GRAVITY_BEFORE_LAND * ConstVals.PPM else 0f

            wasLeftTouchingBlock = isLeftTouchingBlock
            wasRightTouchingBlock = isRightTouchingBlock
            wasHeadTouchingBlock = isHeadTouchingBlock
            wasFeetTouchingBlock = isFeetTouchingBlock
        }

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(this, TAG to sprite)
        spritesComponent.putUpdateFunction(TAG) { _, _sprite ->
            _sprite as GameSprite
            /*
            val position = when (state) {
                BabySpiderState.FALLING -> Position.CENTER
                BabySpiderState.RUNNING_ON_GROUND -> Position.BOTTOM_CENTER
                BabySpiderState.RUNNING_ON_CEILING -> Position.TOP_CENTER
                BabySpiderState.SCALING_WALL_LEFT -> Position.CENTER_LEFT
                BabySpiderState.SCALING_WALL_RIGHT -> Position.CENTER_RIGHT
            }
            val bodyPosition = body.getPositionPoint(position)
            _sprite.setPosition(bodyPosition, position)
             */
            val center = body.getCenter()
            _sprite.setCenter(center.x, center.y)
            _sprite.setOriginCenter()
            val rotation = when (state) {
                BabySpiderState.FALLING, BabySpiderState.RUNNING_ON_GROUND -> 0f
                BabySpiderState.RUNNING_ON_CEILING -> 180f
                BabySpiderState.SCALING_WALL_LEFT -> 270f
                BabySpiderState.SCALING_WALL_RIGHT -> 90f
            }
            _sprite.rotation = rotation
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = {
            when (state) {
                BabySpiderState.FALLING -> "still"
                else -> "running"
            }
        }
        val animations = objectMapOf<String, IAnimation>(
            "still" to Animation(stillRegion!!),
            "running" to Animation(runRegion!!, 1, 4, 0.1f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}