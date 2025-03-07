package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
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
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*
import com.megaman.maverick.game.world.contacts.MegaContactListener
import kotlin.math.abs

class Penguin(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.MEDIUM), IFaceable {

    companion object {
        const val TAG = "Penguin"
        private var atlas: TextureAtlas? = null
        private const val STAND_DUR = 1f
        private const val SLIDE_DUR = 0.2f
        private const val G_GRAV = -0.001f
        private const val GRAV = -0.375f
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

    override fun init() {
        super.init()
        if (atlas == null) atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
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
        facing = if (megaman.body.getX() > body.getX()) Facing.RIGHT else Facing.LEFT
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.physics.defaultFrictionOnSelf.set(
            MegaContactListener.ICE_FRICTION,
            MegaContactListener.ICE_FRICTION
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
        spritesComponent.putUpdateFunction { _, _ ->
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
        facing = if (megaman.body.getX() > body.getX()) Facing.RIGHT else Facing.LEFT
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
