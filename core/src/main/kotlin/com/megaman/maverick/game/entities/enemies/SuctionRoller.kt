package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
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
import com.megaman.maverick.game.com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*

class SuctionRoller(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.SMALL), IFaceable {

    companion object {
        const val TAG = "SuctionRoller"
        private var textureRegion: TextureRegion? = null
        private const val GRAVITY = -0.1f
        private const val VEL_X = 2.5f
        private const val VEL_Y = 2.5f
    }

    override lateinit var facing: Facing

    private var onWall = false
    private var wasOnWall = false

    override fun init() {
        if (textureRegion == null)
            textureRegion = game.assMan.getTextureRegion(TextureAsset.ENEMIES_1.source, "SuctionRoller")
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        onWall = false
        wasOnWall = false
        facing = if (game.megaman.body.getX() > body.getX()) Facing.RIGHT else Facing.LEFT
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.positionOnPoint(spawn, Position.BOTTOM_CENTER)
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add {
            val megaman = game.megaman
            if (megaman.dead) return@add
            wasOnWall = onWall
            onWall =
                (facing == Facing.LEFT && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT)) ||
                    (facing == Facing.RIGHT && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT))

            if (body.isSensing(BodySense.FEET_ON_GROUND)) {
                if (megaman.body.getBounds().getPositionPoint(Position.BOTTOM_RIGHT).x < body.getX())
                    facing = Facing.LEFT
                else if (megaman.body.getX() > body.getBounds().getPositionPoint(Position.BOTTOM_RIGHT).x)
                    facing = Facing.RIGHT
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(0.75f * ConstVals.PPM, ConstVals.PPM.toFloat())

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val bodyFixture =
            Fixture(
                body,
                FixtureType.BODY,
                GameRectangle().setSize(0.75f * ConstVals.PPM, ConstVals.PPM.toFloat()),
            )
        bodyFixture.putProperty(ConstKeys.GRAVITY_ROTATABLE, false)
        body.addFixture(bodyFixture)
        bodyFixture.drawingColor = Color.BLUE
        debugShapes.add { bodyFixture }

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(ConstVals.PPM / 4f, ConstVals.PPM / 32f))
        feetFixture.offsetFromBodyAttachment.y = -0.6f * ConstVals.PPM
        body.addFixture(feetFixture)
        feetFixture.drawingColor = Color.GREEN
        debugShapes.add { feetFixture }

        val leftFixture =
            Fixture(
                body,
                FixtureType.SIDE,
                GameRectangle().setSize(ConstVals.PPM / 32f, ConstVals.PPM.toFloat())
            )
        leftFixture.offsetFromBodyAttachment.x = -0.375f * ConstVals.PPM
        leftFixture.offsetFromBodyAttachment.y = ConstVals.PPM / 5f
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        body.addFixture(leftFixture)
        leftFixture.drawingColor = Color.ORANGE
        debugShapes.add { leftFixture }

        val rightFixture =
            Fixture(
                body,
                FixtureType.SIDE,
                GameRectangle().setSize(ConstVals.PPM / 32f, ConstVals.PPM.toFloat())
            )
        rightFixture.offsetFromBodyAttachment.x = 0.375f * ConstVals.PPM
        rightFixture.offsetFromBodyAttachment.y = ConstVals.PPM / 5f
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        body.addFixture(rightFixture)
        rightFixture.drawingColor = Color.ORANGE
        debugShapes.add { rightFixture }

        val damageableFixture =
            Fixture(
                body,
                FixtureType.DAMAGEABLE,
                GameRectangle().setSize(0.75f * ConstVals.PPM, ConstVals.PPM.toFloat()),
            )
        body.addFixture(damageableFixture)

        val damagerFixture =
            Fixture(
                body,
                FixtureType.DAMAGER,
                GameRectangle().setSize(0.75f * ConstVals.PPM, ConstVals.PPM.toFloat()),
            )
        body.addFixture(damagerFixture)
        debugShapes.add { damagerFixture }

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.physics.gravity.y =
                if (body.isSensing(BodySense.FEET_ON_GROUND)) 0f else GRAVITY * ConstVals.PPM
            if (onWall) {
                if (!wasOnWall) body.physics.velocity.x = 0f
                body.physics.velocity.y = VEL_Y * ConstVals.PPM
            } else {
                if (wasOnWall) body.translate(0f, ConstVals.PPM / 10f)
                body.physics.velocity.x = VEL_X * ConstVals.PPM * facing.value
            }
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.5f * ConstVals.PPM)
        sprite.setOriginCenter()
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setFlip(facing == Facing.RIGHT, false)
            _sprite.hidden = damageBlink

            val position =
                if (onWall) {
                    if (facing == Facing.LEFT) Position.CENTER_LEFT else Position.CENTER_RIGHT
                } else Position.BOTTOM_CENTER

            val bodyPosition =
                if (onWall) {
                    if (position == Position.CENTER_LEFT) body.getPositionPoint(Position.CENTER_LEFT)
                    else body.getPositionPoint(Position.CENTER_RIGHT)
                } else body.getPositionPoint(Position.BOTTOM_CENTER)

            _sprite.setPosition(bodyPosition, position)

            _sprite.rotation =
                if (onWall) {
                    if (facing == Facing.LEFT) -90f else 90f
                } else 0f
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(textureRegion!!, 1, 5, 0.1f, true)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }
}
