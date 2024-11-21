package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.shapes.IGameShape2D
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import kotlin.reflect.KClass

class JoeBall(game: MegamanMaverickGame) : AbstractProjectile(game), IAnimatedEntity {

    companion object {
        const val TAG = "JoeBall"
        const val SNOW_TYPE = "Snow"

        private const val CLAMP = 15f
        private const val REFLECT_VEL = 5f

        private var joeBallReg: TextureRegion? = null
        private var snowJoeBallReg: TextureRegion? = null
    }

    private val trajectory = Vector2()
    private var type = ""

    override fun init() {
        if (joeBallReg == null)
            joeBallReg = game.assMan.getTextureRegion(TextureAsset.PROJECTILES_1.source, "Joeball")
        if (snowJoeBallReg == null)
            snowJoeBallReg =
                game.assMan.getTextureRegion(TextureAsset.PROJECTILES_1.source, "SnowJoeball")
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        type = spawnProps.get(ConstKeys.TYPE, String::class)!!

        trajectory.set(spawnProps.get(ConstKeys.TRAJECTORY, Vector2::class)!!)
        body.physics.velocity.set(trajectory)
    }

    override fun onDamageInflictedTo(damageable: IDamageable) = explodeAndDie()

    override fun hitBlock(blockFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) = explodeAndDie()

    override fun hitShield(shieldFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        super.hitShield(shieldFixture, thisShape, otherShape)
        // owner = shieldFixture.getEntity()
        trajectory.x *= -1f

        val deflection =
            if (shieldFixture.hasProperty(ConstKeys.DIRECTION))
                shieldFixture.getProperty(ConstKeys.DIRECTION, Direction::class)!!
            else Direction.UP
        when (deflection) {
            Direction.UP -> trajectory.y = REFLECT_VEL * ConstVals.PPM
            Direction.DOWN -> trajectory.y = -REFLECT_VEL * ConstVals.PPM
            Direction.LEFT,
            Direction.RIGHT -> trajectory.y = 0f
        }
        body.physics.velocity.set(trajectory)

        requestToPlaySound(SoundAsset.DINK_SOUND, false)
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(0.15f * ConstVals.PPM)
        body.physics.applyFrictionX = false
body.physics.applyFrictionY = false
        body.physics.velocityClamp.set(CLAMP * ConstVals.PPM, CLAMP * ConstVals.PPM)

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle().set(body))
        body.addFixture(bodyFixture)

        val projectileFixture = Fixture(body, FixtureType.PROJECTILE, GameRectangle().setSize(0.2f * ConstVals.PPM))
        body.addFixture(projectileFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(0.2f * ConstVals.PPM))
        body.addFixture(damagerFixture)

        val shieldFixture = Fixture(body, FixtureType.SHIELD, GameRectangle().setSize(0.2f * ConstVals.PPM))
        body.addFixture(shieldFixture)

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.25f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setFlip(trajectory.x < 0f, false)
            _sprite.setCenter(body.getCenter())
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String = { type }
        val animations =
            objectMapOf<String, IAnimation>(
                "" pairTo Animation(joeBallReg!!, 1, 4, 0.1f, true),
                SNOW_TYPE pairTo Animation(snowJoeBallReg!!, 1, 4, 0.1f, true)
            )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    override fun explodeAndDie(vararg params: Any?) {
        destroy()

        val explosionType: String
        val soundAsset: SoundAsset
        when (type) {
            SNOW_TYPE -> {
                soundAsset = SoundAsset.THUMP_SOUND
                explosionType = ExplosionsFactory.SNOWBALL_EXPLOSION
            }

            else -> {
                soundAsset = SoundAsset.EXPLOSION_2_SOUND
                explosionType = ExplosionsFactory.EXPLOSION
            }
        }

        val explosion = EntityFactories.fetch(EntityType.EXPLOSION, explosionType)!!
        explosion.spawn(
            props(
                ConstKeys.POSITION pairTo body.getCenter(),
                ConstKeys.SOUND pairTo soundAsset,
                ConstKeys.MASK pairTo
                        objectSetOf<KClass<out IDamageable>>(
                            if (owner is Megaman) AbstractEnemy::class else Megaman::class
                        )
            )
        )
    }
}
