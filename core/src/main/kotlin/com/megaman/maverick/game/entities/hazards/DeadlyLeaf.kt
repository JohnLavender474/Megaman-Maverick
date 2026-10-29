package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.animations.Animation
import com.maverick.game.engine.animations.AnimationsComponentBuilder
import com.maverick.game.engine.animations.AnimatorBuilder
import com.maverick.game.engine.audio.AudioComponent
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.extensions.getTextureRegion
import com.maverick.game.engine.common.extensions.objectSetOf
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.SmoothOscillationTimer
import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.drawables.shapes.DrawableShapesComponent
import com.maverick.game.engine.drawables.shapes.IDrawableShape
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponentBuilder
import com.maverick.game.engine.drawables.sprites.setCenter
import com.maverick.game.engine.drawables.sprites.setSize
import com.maverick.game.engine.entities.contracts.IAnimatedEntity
import com.maverick.game.engine.entities.contracts.IAudioEntity
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.entities.contracts.ISpritesEntity
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.IDamageNegotiator
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.*
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.projectiles.*
import com.megaman.maverick.game.players.asPlayer
import com.megaman.maverick.game.world.body.*
import kotlin.reflect.KClass

class DeadlyLeaf(game: MegaServices) : AbstractHealthEntity(game), IBodyEntity, ISpritesEntity, IAnimatedEntity,
    IAudioEntity, IHazard, IDamagerEntity {

    companion object {
        const val TAG = "DeadlyLeaf"

        private const val X_VEL = 2f
        private const val Y_VEL = -4f

        private const val OSCILLATION_AMPLITUDE = 2f
        private const val OSCILLATION_FREQUENCY = 1f

        private const val DEFAULT_MIN_Y_OFFSET = 15f

        private val FULL_DMG_NEGS = objectSetOf<KClass<out IDamagerEntity>>(
            ChargedShot::class,
            ChargedShotExplosion::class,

            MagmaFlame::class,
            MagmaGoop::class,
            MagmaWave::class,
            MagmaPellet::class,

            GroundPebble::class,
        )

        private var region: TextureRegion? = null
    }

    override val damageNegotiator = object : IDamageNegotiator {

        override fun get(damager: IDamagerEntity) =
            if (FULL_DMG_NEGS.contains(damager::class)) ConstVals.MAX_HEALTH
            else ConstVals.MAX_HEALTH / 2
    }

    private val oscillationTimer = SmoothOscillationTimer(
        duration = OSCILLATION_FREQUENCY,
        start = -OSCILLATION_AMPLITUDE,
        end = OSCILLATION_AMPLITUDE
    )
    private var minY = 0f

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.HAZARDS_1.source, TAG)
        super.init()
        addComponent(AudioComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        body.forEachFixture { fixture -> fixture.putProperty(ConstKeys.DEATH_LISTENER, false) }

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        oscillationTimer.reset()

        minY = spawnProps.getOrDefault(
            "${ConstKeys.MIN}_${ConstKeys.Y}",
            spawn.y - DEFAULT_MIN_Y_OFFSET * ConstVals.PPM,
            Float::class
        )
    }

    override fun canBeDamagedBy(damager: IDamagerEntity) =
        (damager is IProjectileEntity && damager.owner.asPlayer != null) ||
            FULL_DMG_NEGS.contains(damager::class)

    override fun takeDamageFrom(damager: IDamagerEntity): Boolean {
        val damaged = super.takeDamageFrom(damager)
        if (damaged && overlapsGameCamera()) requestToPlaySound(SoundAsset.ENEMY_DAMAGE_SOUND, false)
        return damaged
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (body.getBounds().getY() < minY) {
                destroy()
                return@add
            }

            oscillationTimer.update(delta)

            val velX = X_VEL * ConstVals.PPM * oscillationTimer.getValue()
            body.physics.velocity.x = velX

            body.physics.velocity.y = Y_VEL * ConstVals.PPM
        }
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ConstVals.PPM.toFloat())
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE)
        )
    }

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(
            TAG, GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 10))
                .also { sprite -> sprite.setSize(5f * ConstVals.PPM) }
        )
        .preProcess { _, sprite ->
            sprite.setCenter(body.getCenter())
            sprite.hidden = damageBlink
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .addAnimation(Animation(region!!, 1, 10, 0.1f, true))
                .build()
        )
        .build()

    override fun getType() = EntityType.HAZARD

    override fun getDamageToMegaman() = 2
}
