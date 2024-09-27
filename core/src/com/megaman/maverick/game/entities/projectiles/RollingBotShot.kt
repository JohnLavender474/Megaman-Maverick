package com.megaman.maverick.game.entities.projectiles


import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.GameEntity
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType

class RollingBotShot(game: MegamanMaverickGame) : AbstractProjectile(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "RollingBotShot"
        private const val X_VEL = 8f
        private const val EXPLOSION_DURATION = 0.1f
        private var region: TextureRegion? = null
    }

    override lateinit var facing: Facing

    override fun init() {
        if (region == null)
            region = game.assMan.getTextureRegion(TextureAsset.PROJECTILES_1.source, "RollingBotShot")
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun explodeAndDie(vararg params: Any?) {
        destroy()

        if (overlapsGameCamera()) playSoundNow(SoundAsset.ENEMY_DAMAGE_SOUND, false)

        val explosion = EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.CHARGED_SHOT_EXPLOSION)!!
        explosion.spawn(
            props(
                ConstKeys.OWNER pairTo owner,
                ConstKeys.BOOLEAN pairTo false,
                ConstKeys.DURATION pairTo EXPLOSION_DURATION,
                ConstKeys.POSITION pairTo body.getCenter(),
                ConstKeys.DIRECTION pairTo if (isFacing(Facing.LEFT)) Direction.LEFT else Direction.RIGHT
            )
        )
    }

    override fun onDamageInflictedTo(damageable: IDamageable) = explodeAndDie()

    override fun hitBlock(blockFixture: IFixture) = explodeAndDie()

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        owner = spawnProps.get(ConstKeys.OWNER, GameEntity::class)
        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)
        val left = spawnProps.getOrDefault(ConstKeys.LEFT, false, Boolean::class)
        facing = if (left) Facing.LEFT else Facing.RIGHT
        body.physics.velocity.x = X_VEL * ConstVals.PPM * facing.value
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.75f * ConstVals.PPM, 0.35f * ConstVals.PPM)
        body.physics.takeFrictionFromOthers = false

        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body }), debug = true))

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle().set(body))
        body.addFixture(bodyFixture)

        val projectileFixture = Fixture(body, FixtureType.PROJECTILE, GameRectangle().set(body))
        body.addFixture(projectileFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().set(body))
        body.addFixture(damagerFixture)

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(0.75f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setCenter(body.getCenter())
            _sprite.setFlip(isFacing(Facing.LEFT), false)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(region!!, 1, 2, 0.1f, true)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }
}