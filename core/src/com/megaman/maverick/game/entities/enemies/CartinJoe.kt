package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.CAUSE_OF_DEATH_MESSAGE
import com.engine.common.GameLogger
import com.engine.common.enums.Direction
import com.engine.common.enums.Facing
import com.engine.common.enums.Position
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.interfaces.IFaceable
import com.engine.common.interfaces.Updatable
import com.engine.common.interfaces.isFacing
import com.engine.common.interfaces.swapFacing
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.damage.IDamager
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.drawables.sprites.setSize
import com.engine.entities.IGameEntity
import com.engine.entities.contracts.IAnimatedEntity
import com.engine.entities.contracts.ISpritesEntity
import com.engine.updatables.UpdatablesComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.IDirectionRotatable
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.world.*
import kotlin.reflect.KClass

class CartinJoe(game: MegamanMaverickGame) : AbstractEnemy(game), ISpritesEntity, IAnimatedEntity, IFaceable,
    IDirectionRotatable {

    companion object {
        const val TAG = "CartinJoe"
        private var moveRegion: TextureRegion? = null
        private var shootRegion: TextureRegion? = null
        private const val VEL_X = 5f
        private const val GROUND_GRAVITY = -0.0015f
        private const val GRAVITY = -0.5f
        private const val WAIT_DURATION = 1f
        private const val SHOOT_DURATION = 0.25f
        private const val BULLET_SPEED = 10f
    }

    override var facing = Facing.RIGHT
    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class to dmgNeg(10), Fireball::class to dmgNeg(ConstVals.MAX_HEALTH), ChargedShot::class to dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) ConstVals.MAX_HEALTH else 15
        }, ChargedShotExplosion::class to dmgNeg(ConstVals.MAX_HEALTH)
    )

    val shooting: Boolean get() = !shootTimer.isFinished()

    override var directionRotation = Direction.UP

    private val waitTimer = Timer(WAIT_DURATION)
    private val shootTimer = Timer(SHOOT_DURATION)

    override fun init() {
        super<AbstractEnemy>.init()
        if (moveRegion == null || shootRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            moveRegion = atlas.findRegion("CartinJoe/Move")
            shootRegion = atlas.findRegion("CartinJoe/Shoot")
        }
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomCenterPoint()
        body.setBottomCenterToPoint(spawn)
        val left = spawnProps.getOrDefault(ConstKeys.LEFT, true, Boolean::class)
        facing = if (left) Facing.LEFT else Facing.RIGHT
        waitTimer.reset()
        shootTimer.setToEnd()
    }

    override fun onDestroy() {
        super<AbstractEnemy>.onDestroy()
        if (hasDepletedHealth()) explode()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add {
            shootTimer.update(it)
            if (shootTimer.isJustFinished()) waitTimer.reset()

            waitTimer.update(it)
            if (waitTimer.isJustFinished()) {
                shoot()
                shootTimer.reset()
            }

            if (body.isSensingAny(BodySense.SIDE_TOUCHING_BLOCK_LEFT, BodySense.SIDE_TOUCHING_BLOCK_RIGHT)) {
                kill(props(CAUSE_OF_DEATH_MESSAGE to "Side touching block"))
                explode()
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(ConstVals.PPM * 1.25f)

        val debugShapes = Array<() -> IDrawableShape?>()

        val bodyFixture = Fixture(
            body, FixtureType.BODY, GameRectangle().setSize(1.25f * ConstVals.PPM, 1.85f * ConstVals.PPM)
        )
        body.addFixture(bodyFixture)
        bodyFixture.getShape().color = Color.GRAY

        val shieldFixture =
            Fixture(body, FixtureType.SHIELD, GameRectangle().setSize(1.25f * ConstVals.PPM, 0.75f * ConstVals.PPM))
        shieldFixture.offsetFromBodyCenter.y = -0.25f * ConstVals.PPM
        body.addFixture(shieldFixture)
        shieldFixture.getShape().color = Color.BLUE
        debugShapes.add { shieldFixture.getShape() }

        val damagerFixture =
            Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(ConstVals.PPM.toFloat(), 1.25f * ConstVals.PPM))
        body.addFixture(damagerFixture)
        damagerFixture.getShape().color = Color.RED
        debugShapes.add { damagerFixture.getShape() }

        val damageableFixture = Fixture(
            body, FixtureType.DAMAGEABLE, GameRectangle().setSize(
                ConstVals.PPM.toFloat(), 0.75f * ConstVals.PPM
            )
        )
        damageableFixture.offsetFromBodyCenter.y = 0.45f * ConstVals.PPM
        body.addFixture(damageableFixture)
        damageableFixture.getShape().color = Color.PURPLE
        debugShapes.add { damageableFixture.getShape() }

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(ConstVals.PPM.toFloat(), 0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyCenter.y = -0.6f * ConstVals.PPM
        body.addFixture(feetFixture)
        feetFixture.getShape().color = Color.GREEN
        debugShapes.add { feetFixture.getShape() }

        val onBounce: () -> Unit = {
            swapFacing()
            GameLogger.debug(TAG, "onBounce: swap facing to $facing")
        }

        val leftFixture = Fixture(
            body, FixtureType.SIDE, GameRectangle().setSize(
                0.1f * ConstVals.PPM, 0.5f * ConstVals.PPM
            )
        )
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        leftFixture.setRunnable(onBounce)
        leftFixture.offsetFromBodyCenter.x = -0.75f * ConstVals.PPM
        body.addFixture(leftFixture)
        leftFixture.getShape().color = Color.YELLOW
        debugShapes.add { leftFixture.getShape() }

        val rightFixture =
            Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM, 0.5f * ConstVals.PPM))
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        rightFixture.setRunnable(onBounce)
        rightFixture.offsetFromBodyCenter.x = 0.75f * ConstVals.PPM
        body.addFixture(rightFixture)
        rightFixture.getShape().color = Color.YELLOW
        debugShapes.add { rightFixture.getShape() }

        body.preProcess.put(ConstKeys.DEFAULT, Updatable {
            body.physics.gravity.y =
                ConstVals.PPM * if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_GRAVITY else GRAVITY
            body.physics.velocity.x = VEL_X * ConstVals.PPM * facing.value
        })

        addComponent(DrawableShapesComponent(this, debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(2f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(this, sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.hidden = damageBlink
            val position = body.getBottomCenterPoint()
            _sprite.setPosition(position, Position.BOTTOM_CENTER)
            _sprite.setFlip(isFacing(Facing.RIGHT), false)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = {
            if (shooting) "shoot" else "move"
        }
        val animations = objectMapOf<String, IAnimation>(
            "shoot" to Animation(shootRegion!!, 1, 2, 0.1f, true), "move" to Animation(moveRegion!!, 1, 2, 0.1f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    private fun shoot() {
        @Suppress("DuplicatedCode") val spawn = (when (directionRotation) {
            Direction.UP -> Vector2(0.25f * facing.value, 0.15f)
            Direction.DOWN -> Vector2(0.25f * facing.value, -0.15f)
            Direction.LEFT -> Vector2(-0.2f, 0.25f * facing.value)
            Direction.RIGHT -> Vector2(0.2f, 0.25f * facing.value)
        }).scl(ConstVals.PPM.toFloat()).add(body.getCenter())

        val trajectory = Vector2()
        if (isDirectionRotatedVertically()) trajectory.set(BULLET_SPEED * ConstVals.PPM * facing.value, 0f)
        else trajectory.set(0f, BULLET_SPEED * ConstVals.PPM * facing.value)

        val props = props(
            ConstKeys.OWNER to this,
            ConstKeys.POSITION to spawn,
            ConstKeys.TRAJECTORY to trajectory,
            ConstKeys.DIRECTION to directionRotation
        )

        val entity: IGameEntity = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.BULLET)!!

        requestToPlaySound(SoundAsset.ENEMY_BULLET_SOUND, false)

        game.engine.spawn(entity, props)
    }
}