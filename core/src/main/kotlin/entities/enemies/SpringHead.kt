package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IFaceable
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
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.utils.VelocityAlteration
import com.megaman.maverick.game.utils.VelocityAlterationType
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*
import kotlin.reflect.KClass

class SpringHead(game: MegamanMaverickGame) : AbstractEnemy(game), IFaceable {

    companion object {
        const val TAG = "SpringHead"

        private var textureAtlas: TextureAtlas? = null

        private const val SPEED_NORMAL = 2f
        private const val SPEED_SUPER = 5f

        private const val BOUNCE_DUR = 2f
        private const val TURN_DELAY = 0.35f

        private const val X_BOUNCE = 10f
        private const val Y_BOUNCE = 30f
    }

    override var facing = Facing.RIGHT
    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>()
    val bouncing: Boolean
        get() = !bounceTimer.isFinished()

    private val turnTimer = Timer(TURN_DELAY)
    private val bounceTimer = Timer(BOUNCE_DUR)
    private val speedUpScanner = GameRectangle().setSize(ConstVals.VIEW_WIDTH * ConstVals.PPM, ConstVals.PPM / 4f)
    private val facingWrongDirection: Boolean
        get() {
            val megamanBody = game.megaman.body
            return (body.getX() < megamanBody.getX() && isFacing(Facing.LEFT)) ||
                (body.getX() > megamanBody.getX() && isFacing(Facing.RIGHT))
        }

    override fun init() {
        if (textureAtlas == null) textureAtlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        bounceTimer.setToEnd()
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)
        body.physics.velocity.setZero()
        facing = if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(0.5f * ConstVals.PPM)

        val leftFixture = Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM))
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        leftFixture.offsetFromBodyAttachment.set(-0.4f * ConstVals.PPM, -0.25f * ConstVals.PPM)
        body.addFixture(leftFixture)

        val rightFixture = Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM))
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        rightFixture.offsetFromBodyAttachment.set(0.4f * ConstVals.PPM, -0.25f * ConstVals.PPM)
        body.addFixture(rightFixture)

        val c1 = GameRectangle().setSize(ConstVals.PPM.toFloat())

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, c1.copy())
        body.addFixture(damagerFixture)

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, c1.copy())
        body.addFixture(damageableFixture)

        val shieldFixture = Fixture(
            body,
            FixtureType.SHIELD,
            GameRectangle().setSize(0.85f * ConstVals.PPM, 0.6f * ConstVals.PPM),
        )
        shieldFixture.putProperty(ConstKeys.DIRECTION, Direction.UP)
        shieldFixture.offsetFromBodyAttachment.y = 0.1f * ConstVals.PPM
        body.addFixture(shieldFixture)

        val bouncerFixture = Fixture(
            body, FixtureType.BOUNCER, GameRectangle().setSize(0.5f * ConstVals.PPM)
        )
        bouncerFixture.offsetFromBodyAttachment.y = 0.1f * ConstVals.PPM
        bouncerFixture.putProperty(
            ConstKeys.VELOCITY_ALTERATION,
            { bounceable: Fixture, _: Float -> velocityAlteration(bounceable) })
        body.addFixture(bouncerFixture)

        return BodyComponentCreator.create(this, body)
    }

    private fun velocityAlteration(bounceable: Fixture): VelocityAlteration {
        if (bouncing) return VelocityAlteration.addNone()

        requestToPlaySound(SoundAsset.DINK_SOUND, false)

        val bounceableBody = bounceable.getBody()
        bounceTimer.reset()
        val x = (if (body.getX() > bounceableBody.getX()) -X_BOUNCE else X_BOUNCE) * ConstVals.PPM
        return VelocityAlteration(
            x, Y_BOUNCE * ConstVals.PPM, VelocityAlterationType.ADD, VelocityAlterationType.SET
        )
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add {
            speedUpScanner.setCenter(body.getCenter())
            turnTimer.update(it)

            if (turnTimer.isJustFinished()) facing =
                if (megaman.body.getX() > body.getX()) Facing.RIGHT else Facing.LEFT
            if (turnTimer.isFinished() && facingWrongDirection) turnTimer.reset()

            bounceTimer.update(it)
            if (bouncing) {
                body.physics.velocity.x = 0f
                return@add
            }

            body.physics.velocity.x =
                if ((isFacing(Facing.LEFT) && !body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT)) ||
                    (isFacing(Facing.RIGHT) && !body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT))
                ) 0f
                else (if (megaman.body.getBounds().overlaps(speedUpScanner)
                ) SPEED_SUPER else SPEED_NORMAL) * ConstVals.PPM * facing.value
        }
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.5f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.hidden = damageBlink
            _sprite.setPosition(body.getPositionPoint(Position.BOTTOM_CENTER), Position.BOTTOM_CENTER)
            _sprite.setFlip(facing == Facing.LEFT, false)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String = { if (bouncing) "unleashed" else "compressed" }
        val animations = objectMapOf<String, IAnimation>(
            "unleashed" pairTo Animation(textureAtlas!!.findRegion("SpringHead/Unleashed"), 1, 6, 0.1f, true),
            "compressed" pairTo Animation(textureAtlas!!.findRegion("SpringHead/Compressed"))
        )
        return AnimationsComponent(this, Animator(keySupplier, animations))
    }
}
