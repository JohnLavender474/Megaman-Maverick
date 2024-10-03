package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball

import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.isSensing
import kotlin.math.abs
import kotlin.reflect.KClass

class Penguin(game: MegamanMaverickGame) : AbstractEnemy(game), IFaceable {

    companion object {
        private var atlas: TextureAtlas? = null
        private const val STAND_DUR = 1f
        private const val SLIDE_DUR = 0.25f
        private const val G_GRAV = -0.0015f
        private const val GRAV = -0.375f
        private const val JUMP_X = 5f
        private const val JUMP_Y = 10f
        private const val SLIDE_X = 8f
    }

    override var facing = Facing.RIGHT
    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class pairTo dmgNeg(10),
        Fireball::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShot::class pairTo dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) ConstVals.MAX_HEALTH else 15
        },
        ChargedShotExplosion::class pairTo dmgNeg {
            it as ChargedShotExplosion
            if (it.fullyCharged) 10 else 5
        }
    )

    val sliding: Boolean
        get() = !slideTimer.isFinished() && body.isSensing(BodySense.FEET_ON_GROUND)
    val jumping: Boolean
        get() = !slideTimer.isFinished() && !body.isSensing(BodySense.FEET_ON_GROUND)
    val standing: Boolean
        get() = slideTimer.isFinished()

    private val standTimer = Timer(STAND_DUR)
    private val slideTimer = Timer(SLIDE_DUR)

    override fun init() {
        super.init()
        if (atlas == null) atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomCenterPoint()
        body.setBottomCenterToPoint(spawn)
        slideTimer.setToEnd()
        standTimer.reset()
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle())
        body.addFixture(bodyFixture)

        val feetFixture = Fixture(body, FixtureType.FEET, GameRectangle().setHeight(0.1f * ConstVals.PPM))
        body.addFixture(feetFixture)

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle())
        body.addFixture(damageableFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle())
        body.addFixture(damagerFixture)

        body.preProcess.put(ConstKeys.DEFAULT, Updatable {
            val feetBounds = feetFixture.rawShape as GameRectangle
            if (standing || jumping) {
                body.setSize(0.9f * ConstVals.PPM, 1.25f * ConstVals.PPM)
                feetBounds.width = 0.65f * ConstVals.PPM
                feetFixture.offsetFromBodyCenter.y = -0.625f * ConstVals.PPM
            } else {
                body.setSize(ConstVals.PPM.toFloat(), 0.75f * ConstVals.PPM)
                feetBounds.width = 0.9f * ConstVals.PPM.toFloat()
                feetFixture.offsetFromBodyCenter.y = -0.375f * ConstVals.PPM
            }

            (damageableFixture.rawShape as GameRectangle).set(body)
            (damagerFixture.rawShape as GameRectangle).set(body)

            body.physics.gravity.y =
                (if (body.isSensing(BodySense.FEET_ON_GROUND)) G_GRAV else GRAV) * ConstVals.PPM

            if (sliding) body.physics.velocity.x = SLIDE_X * ConstVals.PPM * facing.value
        })

        return BodyComponentCreator.create(this, body)
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { if (standing) stand(it) else if (sliding) slide(it) }
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.75f * ConstVals.PPM)
        val SpritesComponent = SpritesComponent(sprite)
        SpritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.hidden = damageBlink
            _sprite.setFlip(facing == Facing.LEFT, false)
            _sprite.setPosition(body.getBottomCenterPoint(), Position.BOTTOM_CENTER)
            if (sliding) sprite.translateY(-0.25f * ConstVals.PPM)
        }
        return SpritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String = {
            if (standing) if (abs(body.physics.velocity.x) > ConstVals.PPM / 4f) "slippin" else "stand"
            else if (jumping) "jump" else "slide"
        }
        val animations =
            objectMapOf<String, IAnimation>(
                "slippin" pairTo Animation(atlas!!.findRegion("Penguin/Slippin")),
                "stand" pairTo Animation(atlas!!.findRegion("Penguin/Stand"), 1, 2, 0.1f, true),
                "jump" pairTo Animation(atlas!!.findRegion("Penguin/Jump")),
                "slide" pairTo Animation(atlas!!.findRegion("Penguin/Slide"))
            )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    private fun stand(delta: Float) {
        facing = if (getMegaman().body.x > body.x) Facing.RIGHT else Facing.LEFT
        standTimer.update(delta)
        if (body.isSensing(BodySense.FEET_ON_GROUND) && standTimer.isFinished()) jump()
    }

    private fun jump() {
        standTimer.setToEnd()
        slideTimer.reset()

        val impulse = Vector2()
        impulse.x = JUMP_X * ConstVals.PPM * facing.value
        impulse.y = JUMP_Y * ConstVals.PPM
        body.physics.velocity.add(impulse)
    }

    private fun slide(delta: Float) {
        slideTimer.update(delta)
        if (slideTimer.isFinished()) standTimer.reset()
    }
}
