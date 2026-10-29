package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Size
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.extensions.getTextureRegion
import com.maverick.game.engine.common.extensions.objectMapOf
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.IGameShape2D
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.damage.IDamageableEntity
import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.drawables.shapes.DrawableShapesComponent
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponentBuilder
import com.maverick.game.engine.drawables.sprites.setCenter
import com.maverick.game.engine.drawables.sprites.setSize
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.entities.contracts.ISpritesEntity
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.CustomMapDamageNegotiator
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractHealthEntity
import com.megaman.maverick.game.entities.contracts.IProjectileEntity
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.explosions.Disintegration
import com.megaman.maverick.game.entities.hazards.DeadlyLeaf
import com.megaman.maverick.game.world.body.*

class GroundPebble(game: MegaServices) : AbstractHealthEntity(game, dmgDuration = DAMAGE_DUR), IProjectileEntity,
    IBodyEntity, ISpritesEntity {

    companion object {
        const val TAG = "GroundPebble"

        private const val DAMAGE_DUR = 0.1f

        private const val BODY_SIZE = 0.5f
        private const val GRAVITY = -22.5f

        private const val SPRITE_SIZE = 0.5f
        private const val SPRITE_ROTATION = 90f
        private const val SPRITE_ROTATE_DELAY = 0.1f

        private var region: TextureRegion? = null
    }

    override var size = Size.SMALL
    override var owner: IGameEntity? = null
    override val damageNegotiator = CustomMapDamageNegotiator(
        objectMapOf(
            Bullet::class pairTo dmgNeg(15),
            ChargedShot::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
            ChargedShotExplosion::class pairTo dmgNeg {
                it as ChargedShotExplosion
                if (it.fullyCharged) ConstVals.MAX_HEALTH else 15
            },
            MoonScythe::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
            Fireball::class pairTo null
        )
    )

    private val spriteRotDelay = Timer(SPRITE_ROTATE_DELAY)

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.PROJECTILES_2.source, TAG)
        super.init()
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponents(defineProjectileComponents())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val owner = spawnProps.get(ConstKeys.OWNER, IGameEntity::class)
        this.owner = owner

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        val impulse = spawnProps.get(ConstKeys.IMPULSE, Vector2::class)!!
        body.physics.velocity.set(impulse)

        spriteRotDelay.reset()
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
    }

    override fun takeDamageFrom(damager: IDamagerEntity): Boolean {
        GameLogger.debug(TAG, "takeDamageFrom(): damager=$damager")
        requestToPlaySound(SoundAsset.ENEMY_DAMAGE_SOUND, false)
        return super.takeDamageFrom(damager)
    }

    override fun hitBlock(blockFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) = explodeAndDie()

    override fun onDamageInflictedTo(damageable: IDamageableEntity) {
        if (damageable is DeadlyLeaf) return
        explodeAndDie()
    }

    override fun explodeAndDie(vararg params: Any?) {
        GameLogger.debug(TAG, "explodeAndDie()")
        destroy()
        val disintegration = MegaEntityFactory.fetch(Disintegration::class)!!
        disintegration.spawn(props(ConstKeys.POSITION pairTo body.getCenter(), ConstKeys.SOUND pairTo false))
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(BODY_SIZE * ConstVals.PPM)
        body.physics.gravity.y = GRAVITY * ConstVals.PPM

        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body.getBounds() }), debug = true))

        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.DAMAGER, FixtureType.DAMAGEABLE, FixtureType.PROJECTILE, FixtureType.SHIELD)
        )
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(
            GameSprite(region!!, DrawingPriority(DrawingSection.PLAYGROUND, 5))
                .also { sprite ->
                    sprite.setSize(SPRITE_SIZE * ConstVals.PPM)
                    sprite.setOriginCenter()
                }
        )
        .preProcess { delta, sprite ->
            sprite.setCenter(body.getCenter())

            sprite.hidden = damageBlink

            spriteRotDelay.update(delta)
            if (spriteRotDelay.isFinished()) {
                sprite.rotation += SPRITE_ROTATION
                spriteRotDelay.reset()
            }
        }
        .build()
}
