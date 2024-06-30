package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.common.enums.Direction
import com.engine.common.extensions.gdxArrayOf
import com.engine.common.extensions.getTextureRegion
import com.engine.common.getOverlapPushDirection
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.damage.IDamageable
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setCenter
import com.engine.drawables.sprites.setSize
import com.engine.entities.GameEntity
import com.engine.entities.IGameEntity
import com.engine.entities.contracts.IAnimatedEntity
import com.engine.entities.contracts.IBodyEntity
import com.engine.world.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.IProjectileEntity
import com.megaman.maverick.game.entities.contracts.defineProjectileComponents
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType

class ToxicGoopShot(game: MegamanMaverickGame) : GameEntity(game), IProjectileEntity, IAnimatedEntity {

    companion object {
        const val TAG = "ToxicGoopShot"
        private const val GRAVITY = -0.05f
        private var region: TextureRegion? = null
    }

    override var owner: IGameEntity? = null

    override fun init() {
        if (region == null)
            region = game.assMan.getTextureRegion(TextureAsset.PROJECTILES_1.source, "ToxicGoopShot")
        addComponents(defineProjectileComponents())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        owner = spawnProps.get(ConstKeys.OWNER, IGameEntity::class)
        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)
        val impulse = spawnProps.getOrDefault(ConstKeys.IMPULSE, Vector2(), Vector2::class)!!
        body.physics.velocity.set(impulse)
    }

    override fun onDamageInflictedTo(damageable: IDamageable) = explodeAndDie(
        getOverlapPushDirection(body.rotatedBounds, (damageable as IBodyEntity).body.rotatedBounds)
    )

    override fun hitBlock(blockFixture: IFixture) = explodeAndDie(
        getOverlapPushDirection(body.rotatedBounds, blockFixture.getBody().rotatedBounds)
    )

    override fun explodeAndDie(vararg params: Any?) {
        kill()
        val goopSplash = EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.TOXIC_GOOP_SPLASH)!!
        game.engine.spawn(
            goopSplash, props(
                ConstKeys.OWNER to this,
                ConstKeys.DIRECTION to params[0] as Direction?,
                ConstKeys.POSITION to body.getBottomCenterPoint()
            )
        )
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.5f * ConstVals.PPM)
        body.physics.gravity.y = GRAVITY * ConstVals.PPM

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle().set(body))
        body.addFixture(bodyFixture)

        val projectileFixture = Fixture(body, FixtureType.PROJECTILE, GameRectangle().set(body))
        body.addFixture(projectileFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().set(body))
        body.addFixture(damagerFixture)

        addComponent(DrawableShapesComponent(this, debugShapeSuppliers = gdxArrayOf({ body }), debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(0.75f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(this, sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setCenter(body.getCenter())
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(region!!, 1, 3, 0.1f, true)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }
}