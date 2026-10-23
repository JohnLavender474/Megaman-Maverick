package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.animations.Animation
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.enums.ProcessState
import com.maverick.game.engine.common.enums.Size
import com.maverick.game.engine.common.extensions.getTextureRegion
import com.maverick.game.engine.common.interfaces.IFaceable
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.drawables.shapes.DrawableShapesComponent
import com.maverick.game.engine.drawables.shapes.IDrawableShape
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.drawables.sprites.setCenter
import com.maverick.game.engine.drawables.sprites.setSize
import com.maverick.game.engine.players.viewOf
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.contracts.players
import com.megaman.maverick.game.entities.contracts.targetPlayer
import com.megaman.maverick.game.utils.VelocityAlteration
import com.megaman.maverick.game.utils.VelocityAlterationType
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.*
import kotlin.math.exp
import kotlin.math.max

class MagFly(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.SMALL), IFaceable {

    companion object {
        const val TAG = "MagFly"

        private const val FORCE_FLASH_DURATION = 0.1f

        private const val X_VEL_NORMAL = 3f
        private const val X_VEL_SLOW = 1f

        private const val PULL_FORCE_X = 6f
        private const val PULL_FORCE_Y = 68f

        private const val CARRY_VEL_Y = 3f
        private const val MEGAMAN_MOVEMENT_X_SCALAR = 0.5f

        private var region: TextureRegion? = null
    }

    override lateinit var facing: Facing

    private val forceFlashTimer = Timer(FORCE_FLASH_DURATION)
    private var flash = false
    private lateinit var forceFixture: Fixture

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        super.init()
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.ENEMIES_1.source, TAG)
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)

        facing = if (targetPlayer.body.getX() > body.getX()) Facing.RIGHT else Facing.LEFT
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(ConstVals.PPM.toFloat())

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle().setSize(ConstVals.PPM.toFloat()))
        body.addFixture(bodyFixture)
        debugShapes.add { bodyFixture }

        val leftFixture = Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM))
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        leftFixture.offsetFromBodyAttachment.x = -0.6f * ConstVals.PPM
        leftFixture.offsetFromBodyAttachment.y = 0.4f * ConstVals.PPM
        body.addFixture(leftFixture)
        debugShapes.add { leftFixture }

        val rightFixture = Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM))
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        rightFixture.offsetFromBodyAttachment.x = 0.6f * ConstVals.PPM
        rightFixture.offsetFromBodyAttachment.y = 0.4f * ConstVals.PPM
        body.addFixture(rightFixture)
        debugShapes.add { rightFixture }

        forceFixture = Fixture(
            body,
            FixtureType.FORCE,
            GameRectangle().setSize(ConstVals.PPM / 2f, ConstVals.VIEW_HEIGHT * ConstVals.PPM)
        )
        forceFixture.offsetFromBodyAttachment.y = -ConstVals.VIEW_HEIGHT * ConstVals.PPM / 2f
        forceFixture.setReceiver(Receivers.VELOCITY_ALTERATION) { fixture, delta, processState ->
            val entity = fixture.getEntity()
            val player = players.viewOf(entity)

            if (entity is AbstractEnemy ||
                (player != null && (player.damaged || player.isBehaviorActive(BehaviorType.CLIMBING)))
            ) return@setReceiver VelocityAlteration.addNone()

            if (entity is AbstractProjectile) entity.owner = null

            if (player != null) {
                if (processState == ProcessState.END) return@setReceiver VelocityAlteration.addNone()

                val physics = player.body.physics

                val ownMovementX = physics.velocity.x * delta
                val carryX = body.getPositionDelta().x - (1f - MEGAMAN_MOVEMENT_X_SCALAR) * ownMovementX

                val minVelY = getCarryVelocity(
                    CARRY_VEL_Y * ConstVals.PPM,
                    physics.gravity.y,
                    if (physics.applyFrictionY) physics.defaultFrictionOnSelf.y else 0f,
                    delta
                )

                return@setReceiver VelocityAlteration(
                    carryX,
                    max(0f, minVelY - physics.velocity.y),
                    VelocityAlterationType.TRANSLATE,
                    VelocityAlterationType.ADD
                )
            }

            // TODO: multiplying by delta makes this pull about 60 times weaker than before; check if PULL_FORCE_X needs to increase
            val x = PULL_FORCE_X * ConstVals.PPM * delta * facing.value
            val y = PULL_FORCE_Y * ConstVals.PPM * delta

            return@setReceiver VelocityAlteration.add(x, y)
        }
        body.addFixture(forceFixture)
        debugShapes.add { forceFixture }

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().setSize(ConstVals.PPM.toFloat()))
        body.addFixture(damageableFixture)
        debugShapes.add { damageableFixture }

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(0.95f * ConstVals.PPM))
        body.addFixture(damagerFixture)
        debugShapes.add { damagerFixture }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(2f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putPreProcess { _, _ ->
            sprite.hidden = damageBlink
            sprite.setCenter(body.getCenter())
            sprite.setFlip(facing == Facing.LEFT, false)
        }
        return spritesComponent
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add {
            forceFlashTimer.update(it)
            if (forceFlashTimer.isFinished()) {
                flash = !flash
                forceFlashTimer.reset()
            }

            val slow = targetPlayer.body.getBounds().overlaps(forceFixture.getShape())

            if (!slow && targetPlayer.body.getY() < body.getY() && !facingAndMMDirMatch())
                facing = if (targetPlayer.body.getX() > body.getX()) Facing.RIGHT else Facing.LEFT

            when {
                (facing == Facing.LEFT && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT)) ||
                    (facing == Facing.RIGHT && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT)) ->
                    body.physics.velocity.x = 0f

                else -> body.physics.velocity.x =
                    (if (slow) X_VEL_SLOW else X_VEL_NORMAL) * ConstVals.PPM * facing.value
            }
        }
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(region!!, 1, 2, 0.1f, true)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }

    private fun getCarryVelocity(target: Float, gravity: Float, friction: Float, delta: Float) =
        (target - gravity * delta) / exp(-friction * delta)

    private fun facingAndMMDirMatch() =
        (targetPlayer.body.getX() > body.getX() && facing == Facing.RIGHT) ||
            (targetPlayer.body.getX() < body.getX() && facing == Facing.LEFT)
}
