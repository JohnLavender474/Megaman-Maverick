package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.UtilMethods.getRandom
import com.maverick.game.engine.common.enums.Size
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.extensions.objectMapOf
import com.maverick.game.engine.common.interfaces.ISizable
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.damage.IDamageableEntity
import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.entities.contracts.ISpritesEntity
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.SelfSizeDamageNegotiator
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.decorations.FallingPiece
import com.megaman.maverick.game.entities.explosions.Explosion
import com.megaman.maverick.game.entities.megamanx.MegamanXSabreSlash
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter
import com.megaman.maverick.game.world.body.setEntity

class SabreBlock(services: MegaServices) : Block(services), ISpritesEntity, IDamageableEntity, ISizable {

    companion object {
        const val TAG = "SabreBlock"


        private const val DAMAGE_DUR = 0.1f
        private const val EXPLOSIONS = 5
        private const val EXPLOSION_SPREAD = 0.5f

        private val DEBRIS = arrayOf("$TAG/debris_large", "$TAG/debris_medium", "$TAG/debris_small")

        private var region: TextureRegion? = null
    }

    override var size = Size.LARGE
    override val invincible: Boolean
        get() = !damageTimer.isFinished()

    private val negotiator = SelfSizeDamageNegotiator(this, objectMapOf(MegamanXSabreSlash::class pairTo dmgNeg(9)))
    private val damageTimer = Timer(DAMAGE_DUR)
    private lateinit var damageableFixture: Fixture
    private var health = ConstVals.MAX_HEALTH

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        if (region == null)
            region = services.assets.getTextureAtlas(TextureAsset.ENEMIES_2.source).findRegion("$TAG/block")
                ?: throw IllegalStateException("Missing region $TAG/block")
        FallingPiece.load(services, DEBRIS)
        super.init()
        addComponent(defineSpritesComponent())
        addComponent(UpdatablesComponent({ delta -> damageTimer.update(delta) }))
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)
        (damageableFixture.rawShape as GameRectangle).set(body.getBounds())
        health = ConstVals.MAX_HEALTH
        damageTimer.setToEnd()
    }

    override fun canBeDamagedBy(damager: IDamagerEntity) =
        !invincible && damager is MegamanXSabreSlash

    override fun takeDamageFrom(damager: IDamagerEntity): Boolean {
        if (!canBeDamagedBy(damager)) return false
        val damage = negotiator.get(damager)
        if (damage <= 0) return false
        health -= damage
        damageTimer.reset()
        services.audio.playSound(SoundAsset.ENEMY_DAMAGE_SOUND, false)
        if (health <= 0) breakApart()
        return true
    }

    private fun breakApart() {
        GameLogger.debug(TAG, "breakApart()")
        destroy()
        val center = body.getCenter()
        val spread = EXPLOSION_SPREAD * ConstVals.PPM
        repeat(EXPLOSIONS) {
            spawnExplosion(Vector2(center.x + getRandom(-1, 1) * spread, center.y + getRandom(-1, 1) * spread))
        }
        spawnExplosion(center)
        FallingPiece.throwDebris(DEBRIS, center)
        services.audio.playSound(SoundAsset.EXPLOSION_2_SOUND, false)
    }

    private fun spawnExplosion(position: Vector2) {
        val explosion = MegaEntityFactory.fetch(Explosion::class)!!
        explosion.spawn(props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.POSITION pairTo position,
                ConstKeys.DAMAGER pairTo false
            ))
    }

    override fun defineBodyComponent(): BodyComponent {
        val component = super.defineBodyComponent()
        damageableFixture = Fixture(component.body, FixtureType.DAMAGEABLE, GameRectangle())
        damageableFixture.setEntity(this)
        component.body.addFixture(damageableFixture)
        return component
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(region!!, DrawingPriority(DrawingSection.PLAYGROUND, 0))
        val component = SpritesComponent(sprite)
        component.putPreProcess { _, _ ->
            val bounds = body.getBounds()
            sprite.setBounds(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight())
            sprite.hidden = invincible
        }
        return component
    }
}
