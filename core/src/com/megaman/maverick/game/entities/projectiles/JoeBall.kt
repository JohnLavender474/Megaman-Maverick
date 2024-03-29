package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.enums.Direction
import com.engine.common.extensions.getTextureRegion
import com.engine.common.extensions.objectMapOf
import com.engine.common.extensions.objectSetOf
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.damage.IDamageable
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setSize
import com.engine.entities.IGameEntity
import com.engine.world.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.contracts.defineProjectileComponents
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.utils.getMegamanMaverickGame
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType
import com.megaman.maverick.game.world.getEntity
import kotlin.reflect.KClass

class JoeBall(game: MegamanMaverickGame) : AbstractProjectile(game) {

    companion object {
        const val SNOW_TYPE = "Snow"

        private const val CLAMP = 15f
        private const val REFLECT_VEL = 5f

        private var joeBallReg: TextureRegion? = null
        private var snowJoeBallReg: TextureRegion? = null
    }

    override var owner: IGameEntity? = null

    private val trajectory = Vector2()
    private var type = ""

    override fun init() {
        if (joeBallReg == null)
            joeBallReg = game.assMan.getTextureRegion(TextureAsset.PROJECTILES_1.source, "Joeball")
        if (snowJoeBallReg == null)
            snowJoeBallReg =
                game.assMan.getTextureRegion(TextureAsset.PROJECTILES_1.source, "SnowJoeball")
        addComponents(defineProjectileComponents())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesCompoent())
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        type = spawnProps.get(ConstKeys.TYPE, String::class)!!

        trajectory.set(spawnProps.get(ConstKeys.TRAJECTORY, Vector2::class)!!)
        body.physics.velocity.set(trajectory)
    }

    override fun onDamageInflictedTo(damageable: IDamageable) {
        super.onDamageInflictedTo(damageable)
        explodeAndDie()
    }

    override fun hitBlock(blockFixture: IFixture) {
        super.hitBlock(blockFixture)
        explodeAndDie()
    }

    override fun hitShield(shieldFixture: IFixture) {
        super.hitShield(shieldFixture)
        owner = shieldFixture.getEntity()
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

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(0.15f * ConstVals.PPM)
        body.physics.velocityClamp.set(CLAMP * ConstVals.PPM, CLAMP * ConstVals.PPM)

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle().set(body))
        body.addFixture(bodyFixture)

        val projectileFixture =
            Fixture(body, FixtureType.PROJECTILE, GameRectangle().setSize(0.2f * ConstVals.PPM))
        body.addFixture(projectileFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(0.2f * ConstVals.PPM))
        body.addFixture(damagerFixture)

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesCompoent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.25f * ConstVals.PPM)
        val SpritesComponent = SpritesComponent(this, "joeBall" to sprite)
        SpritesComponent.putUpdateFunction("joeBall") { _, _sprite ->
            _sprite as GameSprite
            _sprite.setFlip(trajectory.x < 0f, false)
            val center = body.getCenter()
            _sprite.setCenter(center.x, center.y)
        }
        return SpritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String = { type }
        val animations =
            objectMapOf<String, IAnimation>(
                "" to Animation(joeBallReg!!, 1, 4, 0.1f, true),
                SNOW_TYPE to Animation(snowJoeBallReg!!, 1, 4, 0.1f, true)
            )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    override fun explodeAndDie() {
        kill()

        val explosionType: String
        val soundAsset: SoundAsset
        when (type) {
            SNOW_TYPE -> {
                soundAsset = SoundAsset.THUMP_SOUND
                explosionType = ExplosionsFactory.SNOWBALL_EXPLOSION
            }

            else -> {
                soundAsset = SoundAsset.EXPLOSION_1_SOUND
                explosionType = ExplosionsFactory.EXPLOSION
            }
        }

        val explosion = EntityFactories.fetch(EntityType.EXPLOSION, explosionType)!!
        game.gameEngine.spawn(
            explosion,
            props(
                ConstKeys.POSITION to body.getCenter(),
                ConstKeys.MASK to
                        objectSetOf<KClass<out IDamageable>>(
                            if (owner is Megaman) AbstractEnemy::class else Megaman::class
                        )
            )
        )

        getMegamanMaverickGame().audioMan.playSound(soundAsset, false)
    }
}
