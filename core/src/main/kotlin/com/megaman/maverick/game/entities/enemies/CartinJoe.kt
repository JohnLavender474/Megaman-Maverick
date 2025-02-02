package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
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
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*

class CartinJoe(game: MegamanMaverickGame) : AbstractEnemy(game), ISpritesEntity, IAnimatedEntity, IFaceable,
    IDirectional {

    companion object {
        const val TAG = "CartinJoe"
        private var moveRegion: TextureRegion? = null
        private var shootRegion: TextureRegion? = null
        private const val VEL_X = 5f
        private const val GROUND_GRAVITY = -0.0015f
        private const val GRAVITY = -0.5f
        private const val WAIT_DURATION = 0.5f
        private const val SHOOT_DURATION = 0.25f
        private const val BULLET_SPEED = 10f
    }

    override var facing = Facing.RIGHT
    override var direction: Direction = Direction.UP

    val shooting: Boolean get() = !shootTimer.isFinished()

    private val waitTimer = Timer(WAIT_DURATION)
    private val shootTimer = Timer(SHOOT_DURATION)

    override fun init() {
        super.init()
        if (moveRegion == null || shootRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            moveRegion = atlas.findRegion("$TAG/Move")
            shootRegion = atlas.findRegion("$TAG/Shoot")
        }
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)

        val left = spawnProps.getOrDefault(ConstKeys.LEFT, true, Boolean::class)
        facing = if (left) Facing.LEFT else Facing.RIGHT

        waitTimer.reset()
        shootTimer.setToEnd()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isHealthDepleted()) explode()
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

            if ((isFacing(Facing.LEFT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT)) ||
                (isFacing(Facing.RIGHT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT))
            ) swapFacing()
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(1.25f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()

        val bodyFixture = Fixture(
            body, FixtureType.BODY, GameRectangle().setSize(1.25f * ConstVals.PPM, 1.85f * ConstVals.PPM)
        )
        body.addFixture(bodyFixture)

        val shieldFixture =
            Fixture(body, FixtureType.SHIELD, GameRectangle().setSize(0.85f, 0.65f * ConstVals.PPM))
        shieldFixture.offsetFromBodyAttachment.y = -0.275f * ConstVals.PPM
        body.addFixture(shieldFixture)
        debugShapes.add { shieldFixture}

        val damagerFixture =
            Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(ConstVals.PPM.toFloat(), 1.25f * ConstVals.PPM))
        body.addFixture(damagerFixture)
        debugShapes.add { damagerFixture}

        val damageableFixture = Fixture(
            body, FixtureType.DAMAGEABLE, GameRectangle().setSize(
                ConstVals.PPM.toFloat(), 0.75f * ConstVals.PPM
            )
        )
        damageableFixture.offsetFromBodyAttachment.y = 0.45f * ConstVals.PPM
        body.addFixture(damageableFixture)
        debugShapes.add { damageableFixture}

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(ConstVals.PPM.toFloat(), 0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = -0.6f * ConstVals.PPM
        body.addFixture(feetFixture)
        debugShapes.add { feetFixture}

        val onBounce: () -> Unit = {
            swapFacing()
            GameLogger.debug(TAG, "onBounce: swap facing pairTo $facing")
        }

        val leftFixture = Fixture(
            body, FixtureType.SIDE, GameRectangle().setSize(
                0.1f * ConstVals.PPM, 0.5f * ConstVals.PPM
            )
        )
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        leftFixture.setRunnable(onBounce)
        leftFixture.offsetFromBodyAttachment.x = -0.75f * ConstVals.PPM
        body.addFixture(leftFixture)
        debugShapes.add { leftFixture}

        val rightFixture =
            Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM, 0.5f * ConstVals.PPM))
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        rightFixture.setRunnable(onBounce)
        rightFixture.offsetFromBodyAttachment.x = 0.75f * ConstVals.PPM
        body.addFixture(rightFixture)
        debugShapes.add { rightFixture}

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.physics.gravity.y =
                ConstVals.PPM * if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_GRAVITY else GRAVITY
            body.physics.velocity.x = VEL_X * ConstVals.PPM * facing.value
        }

        body.forEachFixture { it.putProperty(ConstKeys.DEATH_LISTENER, false) }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(2.5f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.hidden = damageBlink
            val position = body.getPositionPoint(Position.BOTTOM_CENTER)
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
            "shoot" pairTo Animation(shootRegion!!, 1, 2, 0.1f, true),
            "move" pairTo Animation(moveRegion!!, 1, 2, 0.1f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    private fun shoot() {
        val spawn = (when (direction) {
            Direction.UP -> Vector2(0.25f * facing.value, 0.5f)
            Direction.DOWN -> Vector2(0.25f * facing.value, -0.5f)
            Direction.LEFT -> Vector2(-0.4f, 0.25f * facing.value)
            Direction.RIGHT -> Vector2(0.4f, 0.25f * facing.value)
        }).scl(ConstVals.PPM.toFloat()).add(body.getCenter())

        val trajectory = Vector2()
        if (direction.isVertical()) trajectory.set(BULLET_SPEED * ConstVals.PPM * facing.value, 0f)
        else trajectory.set(0f, BULLET_SPEED * ConstVals.PPM * facing.value)

        val props = props(
            ConstKeys.OWNER pairTo this,
            ConstKeys.POSITION pairTo spawn,
            ConstKeys.TRAJECTORY pairTo trajectory,
            ConstKeys.DIRECTION pairTo direction
        )

        val entity = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.BULLET)!!
        entity.spawn(props)

        if (overlapsGameCamera()) requestToPlaySound(SoundAsset.ENEMY_BULLET_SOUND, false)
    }
}
