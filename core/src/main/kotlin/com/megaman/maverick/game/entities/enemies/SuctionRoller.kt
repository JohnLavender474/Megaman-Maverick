package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.GameLogger
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
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*

class SuctionRoller(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.SMALL), IFaceable {

    companion object {
        const val TAG = "SuctionRoller"

        private const val GRAVITY = -0.1f
        private const val VEL_X = 2.5f
        private const val VEL_Y = 2.5f

        private var region: TextureRegion? = null
    }

    override lateinit var facing: Facing

    private var onWall = false
    private var wasOnWall = false

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.ENEMIES_1.source, TAG)
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.positionOnPoint(spawn, Position.BOTTOM_CENTER)

        facing = if (game.megaman.body.getX() > body.getX()) Facing.RIGHT else Facing.LEFT

        onWall = false
        wasOnWall = false
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add {
            wasOnWall = onWall
            onWall = (facing == Facing.LEFT && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT)) ||
                (facing == Facing.RIGHT && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT))

            if (!megaman.dead && body.isSensing(BodySense.FEET_ON_GROUND)) when {
                megaman.body.getBounds().getPositionPoint(Position.BOTTOM_RIGHT).x < body.getX() ->
                    facing = Facing.LEFT

                megaman.body.getX() > body.getBounds().getPositionPoint(Position.BOTTOM_RIGHT).x ->
                    facing = Facing.RIGHT
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(ConstVals.PPM.toFloat())

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val bodyFixture = Fixture(
            body,
            FixtureType.BODY,
            GameRectangle().setSize(0.75f * ConstVals.PPM, ConstVals.PPM.toFloat()),
        )
        bodyFixture.putProperty(ConstKeys.GRAVITY_ROTATABLE, false)
        body.addFixture(bodyFixture)
        bodyFixture.drawingColor = Color.BLUE
        debugShapes.add { bodyFixture }

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.25f * ConstVals.PPM, 0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        body.addFixture(feetFixture)
        feetFixture.drawingColor = Color.GREEN
        debugShapes.add { feetFixture }

        val leftFixture =
            Fixture(
                body,
                FixtureType.SIDE,
                GameRectangle().setSize(ConstVals.PPM / 32f, ConstVals.PPM.toFloat())
            )
        leftFixture.offsetFromBodyAttachment.x = -body.getWidth() / 2f
        leftFixture.offsetFromBodyAttachment.y = 0.25f * ConstVals.PPM
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        body.addFixture(leftFixture)
        leftFixture.drawingColor = Color.ORANGE
        debugShapes.add { leftFixture }

        val rightFixture = Fixture(
            body,
            FixtureType.SIDE,
            GameRectangle().setSize(0.1f * ConstVals.PPM, ConstVals.PPM.toFloat())
        )
        rightFixture.offsetFromBodyAttachment.x = body.getWidth() / 2f
        rightFixture.offsetFromBodyAttachment.y = 0.25f * ConstVals.PPM
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        body.addFixture(rightFixture)
        rightFixture.drawingColor = Color.ORANGE
        debugShapes.add { rightFixture }

        val damageableFixture = Fixture(
            body,
            FixtureType.DAMAGEABLE,
            GameRectangle().setSize(0.75f * ConstVals.PPM, ConstVals.PPM.toFloat()),
        )
        body.addFixture(damageableFixture)

        val damagerFixture = Fixture(
            body,
            FixtureType.DAMAGER,
            GameRectangle().setSize(0.75f * ConstVals.PPM, ConstVals.PPM.toFloat()),
        )
        body.addFixture(damagerFixture)
        debugShapes.add { damagerFixture }

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.physics.gravity.y =
                if (body.isSensing(BodySense.FEET_ON_GROUND)) 0f else GRAVITY * ConstVals.PPM

            when {
                onWall -> {
                    if (!wasOnWall) body.physics.velocity.x = 0f
                    body.physics.velocity.y = VEL_Y * ConstVals.PPM
                }

                else -> {
                    if (wasOnWall) body.translate(0f, ConstVals.PPM / 10f)
                    body.physics.velocity.x = VEL_X * ConstVals.PPM * facing.value
                }
            }
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(2f * ConstVals.PPM)
        val component = SpritesComponent(sprite)
        component.putUpdateFunction { _, _ ->
            sprite.setFlip(facing == Facing.RIGHT, false)
            sprite.hidden = damageBlink

            val position = when {
                onWall -> if (facing == Facing.LEFT) Position.CENTER_LEFT else Position.CENTER_RIGHT
                else -> Position.BOTTOM_CENTER
            }

            val bodyPosition = when {
                onWall -> when (position) {
                    Position.CENTER_LEFT -> body.getPositionPoint(Position.CENTER_LEFT)
                    else -> body.getPositionPoint(Position.CENTER_RIGHT)
                }

                else -> body.getPositionPoint(Position.BOTTOM_CENTER)
            }

            sprite.setPosition(bodyPosition, position)

            sprite.setOriginCenter()
            sprite.rotation = when {
                onWall -> if (facing == Facing.LEFT) -90f else 90f
                else -> 0f
            }
        }
        return component
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(region!!, 1, 5, 0.1f, true)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }
}
