package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.engine.common.enums.Direction
import com.engine.common.enums.Position
import com.engine.common.extensions.gdxArrayOf
import com.engine.common.extensions.getTextureRegion
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.damage.IDamageable
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.sorting.DrawingPriority
import com.engine.drawables.sorting.DrawingSection
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.drawables.sprites.setSize
import com.engine.entities.IGameEntity
import com.engine.world.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.contracts.IDirectionRotatable
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType
import com.megaman.maverick.game.world.getEntity

class Bullet(game: MegamanMaverickGame) : AbstractProjectile(game), IDirectionRotatable {

    companion object {
        private const val CLAMP = 10f
        private const val BOUNCE_LIMIT = 3
        private var bulletRegion: TextureRegion? = null
    }

    override var directionRotation: Direction? = null

    private var bounced = 0

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)

        owner = spawnProps.get(ConstKeys.OWNER) as IGameEntity?

        val spawn = spawnProps.get(ConstKeys.POSITION) as Vector2
        body.setCenter(spawn)

        directionRotation = spawnProps.getOrDefault(ConstKeys.DIRECTION, Direction.UP, Direction::class)

        val trajectory = spawnProps.get(ConstKeys.TRAJECTORY, Vector2::class)!!
        body.physics.velocity.set(trajectory.scl(movementScalar))

        val gravity = spawnProps.getOrDefault(ConstKeys.GRAVITY, Vector2(), Vector2::class)
        body.physics.gravity.set(gravity)

        bounced = 0
    }

    override fun onDamageInflictedTo(damageable: IDamageable) = explodeAndDie()

    override fun hitBody(bodyFixture: IFixture) {
        val entity = bodyFixture.getEntity()
        if (entity != owner && entity is IDamageable && !entity.canBeDamagedBy(this)) explodeAndDie()
    }

    override fun hitBlock(blockFixture: IFixture) = explodeAndDie()

    override fun hitShield(shieldFixture: IFixture) {
        if (owner == shieldFixture.getEntity()) return

        bounced++
        if (bounced >= BOUNCE_LIMIT) {
            kill()
            return
        }

        owner = shieldFixture.getEntity()

        val trajectory = body.physics.velocity.cpy()
        if (isDirectionRotatedVertically()) trajectory.x *= -1f else trajectory.y *= -1f
        val deflection = shieldFixture.getOrDefaultProperty(ConstKeys.DIRECTION, Direction.UP, Direction::class)
        when (deflection) {
            Direction.UP -> {
                when (directionRotation!!) {
                    Direction.UP, null -> trajectory.y = 5f * ConstVals.PPM
                    Direction.DOWN -> trajectory.y = -5f * ConstVals.PPM
                    Direction.LEFT -> trajectory.x = -5f * ConstVals.PPM
                    Direction.RIGHT -> trajectory.x = 5f * ConstVals.PPM
                }
            }

            Direction.DOWN -> {
                when (directionRotation!!) {
                    Direction.UP, null -> trajectory.y = -5f * ConstVals.PPM
                    Direction.DOWN -> trajectory.y = 5f * ConstVals.PPM
                    Direction.LEFT -> trajectory.x = 5f * ConstVals.PPM
                    Direction.RIGHT -> trajectory.x = -5f * ConstVals.PPM
                }
            }

            else -> {}
        }
        body.physics.velocity.set(trajectory)

        requestToPlaySound(SoundAsset.DINK_SOUND, false)
    }

    override fun explodeAndDie(vararg params: Any?) {
        kill()
        val disintegration = EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.DISINTEGRATION)
        game.engine.spawn(disintegration!!, props(ConstKeys.POSITION to body.getCenter()))
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(.15f * ConstVals.PPM)
        body.physics.velocityClamp.set(CLAMP * ConstVals.PPM, CLAMP * ConstVals.PPM)

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle().set(body))
        bodyFixture.putProperty(ConstKeys.GRAVITY_ROTATABLE, false)
        body.addFixture(bodyFixture)

        val projectileFixture = Fixture(body, FixtureType.PROJECTILE, GameRectangle().setSize(.2f * ConstVals.PPM))
        body.addFixture(projectileFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(.2f * ConstVals.PPM))
        body.addFixture(damagerFixture)

        addComponent(
            DrawableShapesComponent(this, debugShapeSuppliers = gdxArrayOf({ body }), debug = true)
        )

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        if (bulletRegion == null) bulletRegion =
            game.assMan.getTextureRegion(TextureAsset.PROJECTILES_1.source, "Bullet")
        val sprite = GameSprite(bulletRegion!!, DrawingPriority(DrawingSection.FOREGROUND, 5))
        sprite.setSize(1.25f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(this, sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setPosition(body.getCenter(), Position.CENTER)
            val rotation = if (directionRotation?.isVertical() == true) 0f else 90f
            _sprite.setOriginCenter()
            _sprite.rotation = rotation
        }
        return spritesComponent
    }
}
