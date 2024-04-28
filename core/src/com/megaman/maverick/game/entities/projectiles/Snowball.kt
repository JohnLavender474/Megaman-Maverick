package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.engine.common.extensions.getTextureRegion
import com.engine.common.extensions.objectSetOf
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.damage.IDamageable
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setCenter
import com.engine.drawables.sprites.setSize
import com.engine.entities.IGameEntity
import com.engine.world.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.contracts.IProjectileEntity
import com.megaman.maverick.game.entities.contracts.defineProjectileComponents
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType
import com.megaman.maverick.game.world.getEntity
import kotlin.reflect.KClass

class Snowball(game: MegamanMaverickGame) : AbstractProjectile(game) {

    companion object {
        const val TAG = "Snowball"
        private const val CLAMP = 10f
        private var region: TextureRegion? = null
    }

    override var owner: IGameEntity? = null

    override fun init() {
        if (region == null)
            region = game.assMan.getTextureRegion(TextureAsset.PROJECTILES_1.source, "Snowball")
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponents(defineProjectileComponents())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        owner = spawnProps.get(ConstKeys.OWNER, IGameEntity::class)

        body.physics.velocity = spawnProps.getOrDefault(ConstKeys.TRAJECTORY, Vector2(), Vector2::class)
        body.physics.gravityOn = spawnProps.getOrDefault(ConstKeys.GRAVITY_ON, false, Boolean::class)
        body.physics.gravity = spawnProps.getOrDefault(ConstKeys.GRAVITY, Vector2(), Vector2::class)
    }

    override fun hitBody(bodyFixture: IFixture) {
        if (bodyFixture.getEntity() !is AbstractEnemy && bodyFixture.getEntity() !is IProjectileEntity)
            explodeAndDie()
    }

    override fun hitBlock(blockFixture: IFixture) = explodeAndDie()

    override fun hitShield(shieldFixture: IFixture) {
        if (shieldFixture.getEntity() != owner) explodeAndDie()
    }

    override fun hitWater(waterFixture: IFixture) = explodeAndDie()

    override fun explodeAndDie() {
        kill()
        val explosion =
            EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.SNOWBALL_EXPLOSION)!!
        game.engine.spawn(
            explosion,
            props(
                ConstKeys.POSITION to body.getCenter(),
                ConstKeys.MASK to
                        objectSetOf<KClass<out IDamageable>>(
                            if (owner is Megaman) AbstractEnemy::class else Megaman::class
                        )
            )
        )
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.15f * ConstVals.PPM)
        body.physics.velocityClamp.set(CLAMP * ConstVals.PPM.toFloat(), CLAMP * ConstVals.PPM.toFloat())

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle(body))
        body.addFixture(bodyFixture)

        val projectileFixture =
            Fixture(body, FixtureType.PROJECTILE, GameRectangle().setSize(0.2f * ConstVals.PPM))
        body.addFixture(projectileFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(0.2f * ConstVals.PPM))
        body.addFixture(damagerFixture)

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(0.85f * ConstVals.PPM)
        sprite.setRegion(region!!)
        val spritesComponent = SpritesComponent(this, sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setCenter(body.getCenter())
        }
        return spritesComponent
    }
}
