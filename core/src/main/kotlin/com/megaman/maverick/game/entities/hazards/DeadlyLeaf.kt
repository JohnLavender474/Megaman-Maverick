package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.SmoothOscillationTimer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IAudioEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.IDamageNegotiator
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.*
import com.megaman.maverick.game.entities.decorations.FallingLeaf
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.world.body.*

class DeadlyLeaf(game: MegamanMaverickGame) : AbstractHealthEntity(game), IBodyEntity, ISpritesEntity, IAnimatedEntity,
    IAudioEntity, IHazard, IDamager {

    companion object {
        const val TAG = "DeadlyLeaf"
        private const val OSCILLATION_AMPLITUDE = 2f
        private const val OSCILLATION_FREQUENCY = 1f
        private const val X_VEL = 1f
        private const val Y_VEL = -3f
        private const val DEFAULT_MIN_Y_OFFSET = 15f
        private var region: TextureRegion? = null
    }

    override val damageNegotiator = object : IDamageNegotiator {

        override fun get(damager: IDamager): Int {
            val tag = (damager as MegaGameEntity).getTag()
            return when (tag) {
                ChargedShot.TAG, ChargedShotExplosion.TAG, Fireball.TAG -> ConstVals.MAX_HEALTH
                else -> (ConstVals.MAX_HEALTH / 2).toInt()
            }
        }
    }

    private val oscillationTimer = SmoothOscillationTimer(
        OSCILLATION_FREQUENCY,
        start = -OSCILLATION_AMPLITUDE,
        end = OSCILLATION_AMPLITUDE
    )
    private var minY = 0f

    override fun init() {
        if (region == null)
            region = game.assMan.getTextureRegion(TextureAsset.ENVIRONS_1.source, "Wood/${FallingLeaf.Companion.TAG}")

        super.init()

        addComponent(AudioComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        oscillationTimer.reset()

        minY = spawnProps.getOrDefault(
            "${ConstKeys.MIN}_${ConstKeys.Y}",
            spawn.y - DEFAULT_MIN_Y_OFFSET * ConstVals.PPM,
            Float::class
        )
    }

    override fun canBeDamagedBy(damager: IDamager) = damager is IProjectileEntity && damager.owner == megaman

    override fun takeDamageFrom(damager: IDamager): Boolean {
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
        .updatable { _, sprite ->
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

    override fun getEntityType() = EntityType.HAZARD
}
