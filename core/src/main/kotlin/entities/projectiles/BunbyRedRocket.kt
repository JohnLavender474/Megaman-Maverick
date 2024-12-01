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
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.IGameShape2D
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.world.body.*

class BunbyRedRocket(game: MegamanMaverickGame) : AbstractProjectile(game), IAnimatedEntity, IDirectional,
    IFaceable {

    companion object {
        const val TAG = "BunbyRedRocket"
        private var region: TextureRegion? = null
    }

    override var direction: Direction
        get() = body.direction
        set(value) {
            body.direction = value
        }
    override lateinit var facing: Facing

    override fun init() {
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.PROJECTILES_2.source, TAG)
        super.init()
        addComponent(defineAnimationsComponent())
        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body.getBounds() }), debug = true))
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        val trajectory = spawnProps.get(ConstKeys.TRAJECTORY, Vector2::class)!!
        body.physics.velocity.set(trajectory)

        facing = spawnProps.get(ConstKeys.FACING, Facing::class)!!
        direction = spawnProps.getOrDefault(ConstKeys.DIRECTION, Direction.UP, Direction::class)
    }

    override fun hitBlock(blockFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) = explodeAndDie()

    override fun onDamageInflictedTo(damageable: IDamageable) = explodeAndDie()

    override fun explodeAndDie(vararg params: Any?) {
        destroy()

        val explosion = EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.EXPLOSION)!!
        explosion.spawn(
            props(
                ConstKeys.POSITION pairTo body.getCenter(),
                ConstKeys.SOUND pairTo SoundAsset.EXPLOSION_2_SOUND,
                ConstKeys.OWNER pairTo this
            )
        )
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.setSize(0.75f * ConstVals.PPM, 0.25f * ConstVals.PPM)
        return BodyComponentCreator.create(
            this, body, BodyFixtureDef.of(FixtureType.PROJECTILE, FixtureType.DAMAGER, FixtureType.SHIELD)
        )
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 1))
        sprite.setSize(0.85f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.setFlip(isFacing(Facing.LEFT), false)
            sprite.setOriginCenter()
            sprite.rotation = direction.rotation
            sprite.setCenter(body.getCenter())
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(region!!, 2, 1, 0.1f, true)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }
}
