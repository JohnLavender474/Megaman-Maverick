package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.animations.Animation
import com.maverick.game.engine.animations.AnimationsComponentBuilder
import com.maverick.game.engine.animations.AnimatorBuilder
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.extensions.objectSetOf
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameCircle
import com.maverick.game.engine.common.shapes.IGameShape2D
import com.maverick.game.engine.damage.IDamageableEntity
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponentBuilder
import com.maverick.game.engine.drawables.sprites.setCenter
import com.maverick.game.engine.drawables.sprites.setSize
import com.maverick.game.engine.entities.contracts.IAnimatedEntity
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.contracts.IFreezerEntity
import com.megaman.maverick.game.entities.contracts.IProjectileEntity
import com.megaman.maverick.game.entities.explosions.IceBombExplosion
import com.megaman.maverick.game.entities.utils.SpriteSkin
import com.megaman.maverick.game.players.asPlayer
import com.megaman.maverick.game.world.body.*

class IceBomb(services: MegaServices) : AbstractProjectile(services), IFreezerEntity, IAnimatedEntity {

    companion object {
        const val TAG = "IceBomb"
        private val SKINS = objectSetOf(SpriteSkin.DEFAULT, SpriteSkin.LAB)
        private const val GRAVITY = -22.5f
        private val regions = ObjectMap<SpriteSkin, TextureRegion>()

        private fun regionName(skin: SpriteSkin) = if (skin == SpriteSkin.DEFAULT) TAG else skin.regionName(TAG, TAG)
    }

    private var skin = SpriteSkin.DEFAULT

    override fun init(vararg params: Any) {
        if (regions.isEmpty) {
            val atlas = services.assets.getTextureAtlas(TextureAsset.PROJECTILES_1.source)
            SKINS.forEach { skin -> regions.put(skin, atlas.findRegion(regionName(skin))) }
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        skin = SpriteSkin.of(TAG, spawnProps, SKINS)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        val impulse = spawnProps.get(ConstKeys.IMPULSE, Vector2::class)!!
        body.physics.velocity.set(impulse)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
    }

    override fun onDamageInflictedTo(damageable: IDamageableEntity) {
        super.onDamageInflictedTo(damageable)
        explodeAndDie()
    }

    override fun hitBlock(blockFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) = explodeAndDie()

    override fun hitShield(shieldFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        if (shieldFixture.getEntity() != owner) explodeAndDie()
    }

    override fun hitWater(waterFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) = explodeAndDie()

    override fun hitProjectile(projectileFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        val projectile = projectileFixture.getEntity() as IProjectileEntity
        if (projectile.owner.asPlayer != null) explodeAndDie()
    }

    override fun hitExplosion(explosionFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) =
        explodeAndDie()

    override fun explodeAndDie(vararg params: Any?) {
        destroy()
        val explosion = MegaEntityFactory.fetch(IceBombExplosion::class)!!
        explosion.spawn(props(ConstKeys.OWNER pairTo this, ConstKeys.POSITION pairTo body.getCenter()))
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.5f * ConstVals.PPM)
        body.physics.gravity.y = GRAVITY * ConstVals.PPM
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false

        val circle = GameCircle().setRadius(body.getWidth() / 2f)

        return BodyComponentCreator.create(
            this, body, BodyFixtureDef.of(
                FixtureType.PROJECTILE pairTo circle.copy(),
                FixtureType.DAMAGER pairTo circle.copy()
            )
        )
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().also { sprite -> sprite.setSize(0.5f * ConstVals.PPM) })
        .preProcess { _, sprite -> sprite.setCenter(body.getCenter()) }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier { skin.name }
                .applyToAnimations { animations ->
                    SKINS.forEach { skin -> animations.put(skin.name, Animation(regions[skin], 2, 1, 0.1f, true)) }
                }
                .build()
        )
        .build()
}
