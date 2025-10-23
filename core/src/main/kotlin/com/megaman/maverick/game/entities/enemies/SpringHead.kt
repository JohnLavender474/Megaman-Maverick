package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.enums.*
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.utils.VelocityAlteration
import com.megaman.maverick.game.utils.VelocityAlterationType
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*

class SpringHead(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.SMALL), IFaceable {

    companion object {
        const val TAG = "SpringHead"

        private const val SPEED_NORMAL = 2f
        private const val SPEED_SUPER = 5f

        private const val BOUNCE_DUR = 2f
        private const val TURN_DELAY = 0.35f

        private const val X_BOUNCE = 10f
        private const val Y_BOUNCE = 30f

        private val regions = ObjectMap<String, TextureRegion>()
    }

    override lateinit var facing: Facing
    // spring head cannot be damaged
    override var invincible = true

    val bouncing: Boolean
        get() = !bounceTimer.isFinished()

    private val turnTimer = Timer(TURN_DELAY)
    private val bounceTimer = Timer(BOUNCE_DUR)

    private val speedUpScanner = GameRectangle().setSize(ConstVals.VIEW_WIDTH * ConstVals.PPM, ConstVals.PPM / 4f)

    private val facingWrongDirection: Boolean
        get() = (body.getX() < megaman.body.getX() && isFacing(Facing.LEFT)) ||
            (body.getX() > megaman.body.getX() && isFacing(Facing.RIGHT))

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            gdxArrayOf("unleashed", "compressed").forEach { key -> regions.put(key, atlas.findRegion("$TAG/$key")) }
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)

        facing = if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT

        bounceTimer.setToEnd()
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(ConstVals.PPM.toFloat())
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val leftFixture = Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM))
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        leftFixture.offsetFromBodyAttachment.set(-0.75f * ConstVals.PPM, -0.5f * ConstVals.PPM)
        body.addFixture(leftFixture)
        leftFixture.drawingColor = Color.YELLOW
        debugShapes.add { leftFixture }

        val rightFixture = Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM))
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        rightFixture.offsetFromBodyAttachment.set(0.75f * ConstVals.PPM, -0.5f * ConstVals.PPM)
        body.addFixture(rightFixture)
        rightFixture.drawingColor = Color.YELLOW
        debugShapes.add { rightFixture }

        val c1 = GameRectangle().setSize(ConstVals.PPM.toFloat())

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, c1.copy())
        body.addFixture(damagerFixture)

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, c1.copy())
        body.addFixture(damageableFixture)

        val shieldFixture = Fixture(
            body,
            FixtureType.SHIELD,
            GameRectangle().setSize(ConstVals.PPM.toFloat())
        )
        shieldFixture.putProperty(ConstKeys.DIRECTION, Direction.UP)
        shieldFixture.offsetFromBodyAttachment.y = 0.1f * ConstVals.PPM
        body.addFixture(shieldFixture)

        val bouncerFixture = Fixture(
            body, FixtureType.BOUNCER, GameRectangle().setSize(0.5f * ConstVals.PPM)
        )
        bouncerFixture.offsetFromBodyAttachment.y = 0.1f * ConstVals.PPM
        bouncerFixture.setVelocityAlteration alter@{ bounceable, _, state ->
            if (state == ProcessState.BEGIN) requestToPlaySound(SoundAsset.DINK_SOUND, false)

            return@alter velocityAlteration(bounceable)
        }
        body.addFixture(bouncerFixture)

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun velocityAlteration(bounceable: IFixture): VelocityAlteration {
        if (bouncing) return VelocityAlteration.addNone()

        bounceTimer.reset()

        val bounceableBody = bounceable.getBody()
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

            if (turnTimer.isJustFinished())
                facing = if (megaman.body.getX() > body.getX()) Facing.RIGHT else Facing.LEFT

            if (turnTimer.isFinished() && facingWrongDirection) turnTimer.reset()

            bounceTimer.update(it)

            if (bouncing) {
                body.physics.velocity.x = 0f
                return@add
            }

            body.physics.velocity.x = when {
                (isFacing(Facing.LEFT) && !body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT)) ||
                    (isFacing(Facing.RIGHT) && !body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT)) -> 0f

                else -> when {
                    megaman.body.getBounds().overlaps(speedUpScanner) -> SPEED_SUPER
                    else -> SPEED_NORMAL
                } * ConstVals.PPM * facing.value
            }
        }
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(2f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putPreProcess { _, _ ->
            sprite.setPosition(body.getPositionPoint(Position.BOTTOM_CENTER), Position.BOTTOM_CENTER)
            sprite.setFlip(facing == Facing.LEFT, false)
            sprite.hidden = damageBlink
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = { if (bouncing) "unleashed" else "compressed" }
        val animations = objectMapOf<String, IAnimation>(
            "unleashed" pairTo Animation(regions["unleashed"], 1, 6, 0.1f, true),
            "compressed" pairTo Animation(regions["compressed"])
        )
        return AnimationsComponent(this, Animator(keySupplier, animations))
    }
}
