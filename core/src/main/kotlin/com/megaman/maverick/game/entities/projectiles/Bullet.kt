package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.ProcessState
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.set
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.shapes.IGameShape2D
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.bosses.GutsTank
import com.megaman.maverick.game.entities.bosses.GutsTankFist
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.decorations.BulletResidual
import com.megaman.maverick.game.entities.explosions.Disintegration
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.VelocityAlterator
import com.megaman.maverick.game.world.body.*

class Bullet(game: MegamanMaverickGame) : AbstractProjectile(game), IDirectional {

    companion object {
        const val TAG = "Bullet"
        private const val CLAMP = 10f
        private const val BOUNCE_MAX = 1
        private var region: TextureRegion? = null
    }

    override lateinit var direction: Direction

    private val trajectory = Vector2()
    private var followTraj = true
    private var bounced = 0

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.PROJECTILES_1.source, TAG)
        super.init()
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        direction = spawnProps.getOrDefault(ConstKeys.DIRECTION, Direction.UP, Direction::class)

        val impulse = spawnProps.get(ConstKeys.IMPULSE, Vector2::class)
        impulse?.let { body.physics.velocity.set(it) }

        val residualRotation: Float

        followTraj = spawnProps.containsKey(ConstKeys.TRAJECTORY)
        if (followTraj) {
            trajectory.set(spawnProps.get(ConstKeys.TRAJECTORY, Vector2::class))
            residualRotation = trajectory.angleDeg()
        } else {
            trajectory.setZero()
            residualRotation = body.physics.velocity.angleDeg()
        }

        val gravity = spawnProps.getOrDefault(ConstKeys.GRAVITY, Vector2.Zero, Vector2::class)
        body.physics.gravity.set(gravity)

        bounced = 0

        val shouldSpawnResidual =
            spawnProps.getOrDefault("${ConstKeys.SPAWN}_${ConstKeys.RESIDUAL}", true, Boolean::class)
        if (shouldSpawnResidual) spawnResidual(residualRotation)
    }

    private fun spawnResidual(residualRotation: Float) {
        val spawn = GameObjectPools.fetch(Vector2::class)
            .set(1f, 0f)
            .rotateDeg(residualRotation)
            .nor()
            .scl(body.getWidth() / 2f)
            .add(body.getCenter())

        val residual = MegaEntityFactory.fetch(BulletResidual::class)!!
        residual.spawn(props(ConstKeys.POSITION pairTo spawn, ConstKeys.ROTATION pairTo residualRotation))
    }

    override fun onDamageInflictedTo(damageable: IDamageable) = explodeAndDie()

    override fun hitBody(bodyFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        val entity = bodyFixture.getEntity()
        if (entity is AbstractEnemy && owner is AbstractEnemy) return
        if (entity != owner && entity is IDamageable && !entity.canBeDamagedBy(this)) explodeAndDie()
    }

    override fun hitSand(sandFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) = explodeAndDie()

    override fun hitBlock(blockFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) = explodeAndDie()

    override fun hitShield(shieldFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        if (owner == shieldFixture.getEntity()) return

        if (shieldFixture.getEntity() is GutsTankFist && owner is GutsTank) return

        bounced++
        if (bounced > BOUNCE_MAX) {
            explodeAndDie()
            return
        }

        bounce(shieldFixture.getProperty(ConstKeys.DIRECTION, Direction::class))
    }

    private fun bounce(deflectNullable: Direction? = null) {
        val velocity = if (followTraj) trajectory else body.physics.velocity
        if (direction.isVertical()) velocity.x *= -1f else velocity.y *= -1f

        val deflection = deflectNullable ?: Direction.UP
        when (deflection) {
            Direction.UP -> {
                when (direction) {
                    Direction.UP -> velocity.y = 5f * ConstVals.PPM
                    Direction.DOWN -> velocity.y = -5f * ConstVals.PPM
                    Direction.LEFT -> velocity.x = -5f * ConstVals.PPM
                    Direction.RIGHT -> velocity.x = 5f * ConstVals.PPM
                }
            }

            Direction.DOWN -> {
                when (direction) {
                    Direction.UP -> velocity.y = -5f * ConstVals.PPM
                    Direction.DOWN -> velocity.y = 5f * ConstVals.PPM
                    Direction.LEFT -> velocity.x = 5f * ConstVals.PPM
                    Direction.RIGHT -> velocity.x = -5f * ConstVals.PPM
                }
            }

            else -> {}
        }

        requestToPlaySound(SoundAsset.DINK_SOUND, false)
    }

    override fun explodeAndDie(vararg params: Any?) {
        destroy()

        val disintegration = MegaEntityFactory.fetch(Disintegration::class)!!
        disintegration.spawn(props(ConstKeys.POSITION pairTo body.getCenter()))
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.5f * ConstVals.PPM, 0.35f * ConstVals.PPM)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.physics.velocityClamp.set(CLAMP * ConstVals.PPM)

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle().set(body))
        bodyFixture.putProperty(ConstKeys.GRAVITY_ROTATABLE, false)
        bodyFixture.setForceAlterationForState(ProcessState.BEGIN) { alteration ->
            val startVelocity = body.physics.velocity.cpy()
            GameLogger.debug(TAG, "start force alteration: startVel=$startVelocity")
            body.putProperty("${ConstKeys.START}_${ConstKeys.VELOCITY}", startVelocity)
            VelocityAlterator.alterate(body, alteration)
        }
        bodyFixture.setForceAlterationForState(ProcessState.END) { _ ->
            val newVelocity = body.removeProperty("${ConstKeys.START}_${ConstKeys.VELOCITY}", Vector2::class)
            GameLogger.debug(TAG, "end force alteration: newVel=$newVelocity")
            newVelocity?.let { body.physics.velocity.set(it) }
        }
        body.addFixture(bodyFixture)

        body.preProcess.put(ConstKeys.DEFAULT) {
            when {
                canMove -> if (followTraj) body.physics.velocity.set(trajectory).scl(movementScalar)
                else -> body.physics.velocity.setZero()
            }
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body.getBounds() }), debug = true))

        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.PROJECTILE, FixtureType.DAMAGER))
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(region!!, DrawingPriority(DrawingSection.PLAYGROUND, 10))
        sprite.setSize(2f * ConstVals.PPM)
        val component = SpritesComponent(sprite)
        component.putUpdateFunction { _, _ ->
            sprite.setCenter(body.getCenter())
            sprite.setOriginCenter()
            sprite.rotation = body.physics.velocity.angleDeg()
        }
        return component
    }
}
