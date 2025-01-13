package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.TimeMarkedRunnable
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.BodyFixtureDef
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getPositionPoint
import kotlin.reflect.KClass

class Arigock(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "Arigock"
        private const val SHOOTING_DUR = 1.25f
        private const val CLOSED_DUR = 1.5f
        private const val BALL_Y_FORCE = 12f
        private val shotImpulses = gdxArrayOf(2.5f, -2.5f, 4f, -4f)
        private val regions = ObjectMap<String, TextureRegion>()
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class pairTo dmgNeg(10),
        Fireball::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShot::class pairTo dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) 15 else 10
        },
        ChargedShotExplosion::class pairTo dmgNeg {
            it as ChargedShotExplosion
            if (it.fullyCharged) 10 else 5
        }
    )
    override lateinit var facing: Facing

    private val shootingTimer = Timer(
        SHOOTING_DUR, gdxArrayOf(
            TimeMarkedRunnable(0.25f) { shoot(0) },
            TimeMarkedRunnable(0.5f) { shoot(1) },
            TimeMarkedRunnable(0.75f) { shoot(2) },
            TimeMarkedRunnable(1f) { shoot(3) }
        ))
    private val closedTimer = Timer(CLOSED_DUR)

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            regions.put("closed", atlas.findRegion("$TAG/closed"))
            regions.put("shooting", atlas.findRegion("$TAG/shooting"))
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)
        shootingTimer.setToEnd()
        closedTimer.reset()
    }

    private fun shoot(xImpulseIndex: Int) {
        val ball = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.ARIGOCK_BALL)!!
        val impulse = Vector2(shotImpulses[xImpulseIndex], BALL_Y_FORCE).scl(ConstVals.PPM.toFloat())
        val position = body.getPositionPoint(Position.TOP_CENTER).sub(0f, 0.2f * ConstVals.PPM)
        ball.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.POSITION pairTo position,
                ConstKeys.IMPULSE pairTo impulse
            )
        )
        if (overlapsGameCamera()) requestToPlaySound(SoundAsset.CHILL_SHOOT_SOUND, false)
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (!closedTimer.isFinished()) {
                closedTimer.update(delta)
                if (closedTimer.isFinished()) shootingTimer.reset()
                return@add
            }

            shootingTimer.update(delta)
            if (shootingTimer.isFinished()) closedTimer.reset()
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ConstVals.PPM.toFloat())
        return BodyComponentCreator.create(
            this, body, BodyFixtureDef.of(
                FixtureType.BODY, FixtureType.DAMAGER,
                FixtureType.DAMAGEABLE
            )
        )
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.25f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.setPosition(body.getPositionPoint(Position.BOTTOM_CENTER), Position.BOTTOM_CENTER)
            sprite.hidden = damageBlink
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = { if (!shootingTimer.isFinished()) "shooting" else "closed" }
        val animations = objectMapOf<String, IAnimation>(
            "shooting" pairTo Animation(regions.get("shooting"), 2, 1, 0.1f, true),
            "closed" pairTo Animation(regions.get("closed"), 2, 1, gdxArrayOf(1f, 0.15f), true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}