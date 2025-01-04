package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.IGameShape2D
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.contracts.IProjectileEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.world.body.*

class GroundPebble(game: MegamanMaverickGame) : AbstractProjectile(game), IBodyEntity, ISpritesEntity {

    companion object {
        const val TAG = "GroundPebble"
        private const val BODY_SIZE = 0.5f
        private const val SPRITE_SIZE = 0.5f
        private const val SPRITE_ROTATE_DELAY = 0.1f
        private const val SPRITE_ROTATION = 90f
        private const val GRAVITY = -0.15f
        private var region: TextureRegion? = null
    }

    private val rotationDelay = Timer(SPRITE_ROTATE_DELAY)

    override fun init() {
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.PROJECTILES_2.source, TAG)
        super.init()
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        val impulse = spawnProps.get(ConstKeys.IMPULSE, Vector2::class)!!
        body.physics.velocity.set(impulse)

        rotationDelay.reset()
    }

    override fun hitBlock(
        blockFixture: IFixture,
        thisShape: IGameShape2D,
        otherShape: IGameShape2D
    ) = explodeAndDie()

    override fun hitProjectile(projectileFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        val entity = projectileFixture.getEntity() as IProjectileEntity
        if (entity.owner == megaman) explodeAndDie()
    }

    override fun onDamageInflictedTo(damageable: IDamageable) = explodeAndDie()

    override fun explodeAndDie(vararg params: Any?) {
        destroy()

        val disintegration = EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.DISINTEGRATION)
        disintegration!!.spawn(props(ConstKeys.POSITION pairTo body.getCenter(), ConstKeys.SOUND pairTo false))
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(BODY_SIZE * ConstVals.PPM)
        body.physics.gravity.y = GRAVITY * ConstVals.PPM
        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body.getBounds() }), debug = true))
        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.DAMAGER, FixtureType.PROJECTILE, FixtureType.SHIELD)
        )
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(GameSprite(region!!, DrawingPriority(DrawingSection.PLAYGROUND, 5))
            .also { sprite ->
                sprite.setSize(SPRITE_SIZE * ConstVals.PPM)
                sprite.setOriginCenter()
            }
        )
        .updatable { delta, sprite ->
            sprite.setCenter(body.getCenter())

            rotationDelay.update(delta)

            if (rotationDelay.isFinished()) {
                sprite.rotation += SPRITE_ROTATION

                rotationDelay.reset()
            }
        }
        .build()

}
