package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.GameEntity
import com.mega.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getEntity
import kotlin.reflect.KClass

class Snowball(game: MegamanMaverickGame) : AbstractProjectile(game) {

    companion object {
        const val TAG = "Snowball"
        private const val CLAMP = 10f
        private var region: TextureRegion? = null
    }

    override fun init() {
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.PROJECTILES_1.source, "Snowball2")
        super.init()
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        owner = spawnProps.get(ConstKeys.OWNER, GameEntity::class)

        body.physics.velocity = spawnProps.getOrDefault(ConstKeys.TRAJECTORY, Vector2(), Vector2::class)
        body.physics.gravityOn = spawnProps.getOrDefault(ConstKeys.GRAVITY_ON, false, Boolean::class)
        body.physics.gravity = spawnProps.getOrDefault(ConstKeys.GRAVITY, Vector2(), Vector2::class)
    }

    override fun onDamageInflictedTo(damageable: IDamageable) = explodeAndDie()

    override fun hitBlock(blockFixture: IFixture) = explodeAndDie()

    override fun hitShield(shieldFixture: IFixture) {
        if (shieldFixture.getEntity() != owner) explodeAndDie()
    }

    override fun hitWater(waterFixture: IFixture) = explodeAndDie()

    override fun explodeAndDie(vararg params: Any?) {
        destroy()
        val explosion = EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.SNOWBALL_EXPLOSION)!!
        explosion.spawn(
            props(
                ConstKeys.POSITION pairTo body.getCenter(),
                ConstKeys.MASK pairTo objectSetOf<KClass<out IDamageable>>(
                    if (owner is Megaman) AbstractEnemy::class else Megaman::class
                )
            )
        )
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.15f * ConstVals.PPM)
        body.physics.velocityClamp.set(CLAMP * ConstVals.PPM.toFloat(), CLAMP * ConstVals.PPM.toFloat())
        body.physics.applyFrictionX = false
body.physics.applyFrictionY = false

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle(body))
        body.addFixture(bodyFixture)

        val projectileFixture =
            Fixture(body, FixtureType.PROJECTILE, GameRectangle().setSize(0.2f * ConstVals.PPM))
        body.addFixture(projectileFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(0.2f * ConstVals.PPM))
        body.addFixture(damagerFixture)

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 10))
        sprite.setSize(0.5f * ConstVals.PPM)
        sprite.setRegion(region!!)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setCenter(body.getCenter())
        }
        return spritesComponent
    }
}
