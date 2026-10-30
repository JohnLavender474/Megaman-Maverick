package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.animations.Animation
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.animations.IAnimation
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.enums.Position
import com.maverick.game.engine.common.enums.Size
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.extensions.objectMapOf
import com.maverick.game.engine.common.interfaces.IFaceable
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.drawables.sprites.setPosition
import com.maverick.game.engine.drawables.sprites.setSize
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.targetPlayer
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*
import com.megaman.maverick.game.world.contacts.ContactValues
import kotlin.math.abs

class Penguin(services: MegaServices) : AbstractEnemy(services, size = Size.MEDIUM), IFaceable {

    companion object {
        const val TAG = "Penguin"
        private var atlas: TextureAtlas? = null
        private const val STAND_DUR = 1f
        private const val SLIDE_DUR = 0.2f
        private const val G_GRAV = -0.15f
        private const val GRAV = -56.25f
        private const val JUMP_X = 6f
        private const val JUMP_Y = 14f
        private const val SLIDE_X = 8f
    }

    override lateinit var facing: Facing

    val sliding: Boolean
        get() = !slideTimer.isFinished() && body.isSensing(BodySense.FEET_ON_GROUND)
    val jumping: Boolean
        get() = !slideTimer.isFinished() && !body.isSensing(BodySense.FEET_ON_GROUND)
    val standing: Boolean
        get() = slideTimer.isFinished()

    private val standTimer = Timer(STAND_DUR)
    private val slideTimer = Timer(SLIDE_DUR)

    override fun init(vararg params: Any) {
        super.init()
        if (atlas == null) atlas = services.assets.getTextureAtlas(TextureAsset.ENEMIES_1.source)
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        val spawn = spawnProps
            .get(ConstKeys.BOUNDS, GameRectangle::class)!!
            .getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)
        slideTimer.setToEnd()
        standTimer.reset()
        facing = if (targetPlayer.body.getX() > body.getX()) Facing.RIGHT else Facing.LEFT
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.physics.defaultFrictionOnSelf.set(
            ContactValues.ICE_FRICTION,
            ContactValues.ICE_FRICTION
        ).scl(ConstVals.PPM.toFloat())
        body.physics.applyFrictionX = true
        body.physics.applyFrictionY = false

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle())
        body.addFixture(bodyFixture)

        val feetFixture = Fixture(body, FixtureType.FEET, GameRectangle().setHeight(0.1f * ConstVals.PPM))
        body.addFixture(feetFixture)

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle())
        body.addFixture(damageableFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle())
        body.addFixture(damagerFixture)

        body.preProcess.put(ConstKeys.DEFAULT) {
            val feetBounds = feetFixture.rawShape as GameRectangle
            if (standing || jumping) {
                body.setSize(0.9f * ConstVals.PPM, 1.25f * ConstVals.PPM)
                feetBounds.setWidth(0.65f * ConstVals.PPM)
                feetFixture.offsetFromBodyAttachment.y = -0.625f * ConstVals.PPM
            } else {
                body.setSize(ConstVals.PPM.toFloat(), 0.75f * ConstVals.PPM)
                feetBounds.setWidth(0.9f * ConstVals.PPM.toFloat())
                feetFixture.offsetFromBodyAttachment.y = -0.375f * ConstVals.PPM
            }

            (damageableFixture.rawShape as GameRectangle).set(body)
            (damagerFixture.rawShape as GameRectangle).set(body)

            body.physics.gravity.y =
                (if (body.isSensing(BodySense.FEET_ON_GROUND)) G_GRAV else GRAV) * ConstVals.PPM

            if (sliding) body.physics.velocity.x = SLIDE_X * ConstVals.PPM * facing.value
        }

        return BodyComponentCreator.create(this, body)
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { if (standing) stand(it) else if (sliding) slide(it) }
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.75f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putPreProcess { _, _ ->
            sprite.hidden = damageBlink
            sprite.setFlip(facing == Facing.LEFT, false)
            sprite.setPosition(body.getPositionPoint(Position.BOTTOM_CENTER), Position.BOTTOM_CENTER)
            if (sliding) sprite.translateY(-0.25f * ConstVals.PPM)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = {
            if (standing) {
                if (abs(body.physics.velocity.x) > 0.25f * ConstVals.PPM) "slippin" else "stand"
            } else if (jumping) "jump" else "slide"
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
        facing = if (targetPlayer.body.getX() > body.getX()) Facing.RIGHT else Facing.LEFT
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
