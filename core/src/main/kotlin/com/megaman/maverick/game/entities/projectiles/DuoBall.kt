package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.IGameShape2D
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
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
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.bosses.GutsTank
import com.megaman.maverick.game.entities.bosses.GutsTankFist
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.explosions.Explosion
import com.megaman.maverick.game.world.body.*

class DuoBall(game: MegamanMaverickGame) : AbstractProjectile(game), IAnimatedEntity {

    companion object {
        const val TAG = "DuoBall"
        private const val GRAVITY = -0.15f
        private const val BOUNCE_MAX = 2
        private var region: TextureRegion? = null
    }

    private var bounced = 0

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.PROJECTILES_1.source, TAG)
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        val impulse = spawnProps.get(ConstKeys.IMPULSE, Vector2::class)!!
        body.physics.velocity.set(impulse)

        bounced = 0
    }

    override fun hitBlock(blockFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) = explodeAndDie()

    override fun hitShield(shieldFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        if (owner == shieldFixture.getEntity()) return

        if (shieldFixture.getEntity() is GutsTankFist && owner is GutsTank) return

        bounced++
        if (bounced > BOUNCE_MAX) {
            explodeAndDie()
            return
        }

        body.physics.velocity.x *= -1f
    }

    override fun onDamageInflictedTo(damageable: IDamageable) = explodeAndDie()

    override fun explodeAndDie(vararg params: Any?) {
        GameLogger.debug(TAG, "explodeAndDie()")

        destroy()

        val explosion = MegaEntityFactory.fetch(Explosion::class)!!
        explosion.spawn(
            props(
                ConstKeys.POSITION pairTo body.getCenter(),
                ConstKeys.SOUND pairTo SoundAsset.EXPLOSION_2_SOUND,
                ConstKeys.OWNER pairTo owner
            )
        )
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.5f * ConstVals.PPM)
        body.physics.gravity.y = GRAVITY * ConstVals.PPM
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.PROJECTILE, FixtureType.DAMAGER))
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().also { sprite -> sprite.setSize(0.5f * ConstVals.PPM) })
        .updatable { _, sprite -> sprite.setCenter(body.getCenter()) }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG).animator(Animator(Animation(region!!, 2, 1, 0.1f, true))).build()
}
