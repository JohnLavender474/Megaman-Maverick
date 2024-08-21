package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.enums.Position
import com.engine.common.extensions.gdxArrayOf
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.objects.Loop
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.damage.IDamager
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.drawables.sprites.setSize
import com.engine.entities.contracts.IAnimatedEntity
import com.engine.updatables.UpdatablesComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType
import kotlin.math.abs
import kotlin.reflect.KClass

class Shotman(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity {

    companion object {
        const val TAG = "Shotman"
        private const val SHOOT_TIME = 0.6f
        private const val CROUCH_TIME = 0.2f
        private const val LAUNCH_IMPULSE_Y = 10f
        private const val LAUNCH_GRAVITY = -0.15f
        private const val SHOOT_SPEED_X = 5f
        private var shootRegion: TextureRegion? = null
        private var launchRegion: TextureRegion? = null
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class to dmgNeg(10),
        Fireball::class to dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShot::class to dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) 15 else 10
        },
        ChargedShotExplosion::class to dmgNeg {
            it as ChargedShotExplosion
            if (it.fullyCharged) 10 else 5
        }
    )

    private val shootTimer = Timer(SHOOT_TIME)
    private val crouchTimer = Timer(CROUCH_TIME)
    private val shootLoop = Loop(gdxArrayOf(true, true, false), true)

    override fun init() {
        if (shootRegion == null || launchRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            shootRegion = atlas.findRegion("Shotman/Shoot")
            launchRegion = atlas.findRegion("Shotman/Launch")
        }
        super<AbstractEnemy>.init()
        addComponent(defineAnimationsComponent())
        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body }), debug = true))
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomCenterPoint()
        body.setBottomCenterToPoint(spawn)
        shootLoop.reset()
        shootTimer.reset()
        crouchTimer.setToEnd()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            crouchTimer.update(delta)
            shootTimer.update(delta)
            if (shootTimer.isFinished()) {
                val shoot = shootLoop.next()
                if (shoot) shootBullet() else {
                    lauchBullet()
                    crouchTimer.reset()
                }
                shootTimer.reset()
            }
        }
    }

    private fun lauchBullet() {
        val spawn = body.getCenter()
        spawn.y += 0.25f * ConstVals.PPM

        val bullet = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.BULLET)!!
        val xFactor = 1f - ((abs(getMegaman().body.y - body.y) / ConstVals.PPM) / 10f) + 0.2f
        val impulseX = (getMegaman().body.x - body.x) * xFactor

        game.engine.spawn(
            bullet, props(
                ConstKeys.OWNER to this,
                ConstKeys.POSITION to spawn,
                ConstKeys.GRAVITY to Vector2(0f, LAUNCH_GRAVITY * ConstVals.PPM),
                ConstKeys.TRAJECTORY to Vector2(impulseX, LAUNCH_IMPULSE_Y * ConstVals.PPM)
            )
        )
    }

    private fun shootBullet() {
        val offsetX = if (getMegaman().body.x > body.x) 0.5f else -0.5f
        val spawn = body.getCenter().add(offsetX * ConstVals.PPM, 0.2f * ConstVals.PPM)
        val bullet = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.BULLET)!!
        val impulseX = if (getMegaman().body.x > body.x) SHOOT_SPEED_X * ConstVals.PPM else -SHOOT_SPEED_X * ConstVals.PPM
        game.engine.spawn(
            bullet, props(
                ConstKeys.OWNER to this,
                ConstKeys.POSITION to spawn,
                ConstKeys.TRAJECTORY to Vector2(impulseX, 0f)
            )
        )
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.6875f * ConstVals.PPM, 0.75f * ConstVals.PPM)

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle().set(body))
        body.addFixture(bodyFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().set(body))
        body.addFixture(damagerFixture)

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().set(body))
        body.addFixture(damageableFixture)

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.25f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.hidden = damageBlink
            val bodyPosition = body.getBottomCenterPoint()
            _sprite.setPosition(bodyPosition, Position.BOTTOM_CENTER)
            _sprite.hidden = damageBlink
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = {
            if (crouchTimer.isFinished()) "shoot" else "launch"
        }
        val animations = objectMapOf<String, IAnimation>(
            "shoot" to Animation(shootRegion!!, 1, 2, 0.1f, true),
            "launch" to Animation(launchRegion!!)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}