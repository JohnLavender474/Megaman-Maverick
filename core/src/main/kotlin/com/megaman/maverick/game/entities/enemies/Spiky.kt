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
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
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

class Spiky(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.SMALL), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "Spiky"
        private const val X_VEL = 4f
        private const val CULL_TIME = 2.5f
        private const val GROUND_GRAVITY = -0.01f
        private const val GRAVITY = -0.15f
        private var region: TextureRegion? = null
    }

    override lateinit var facing: Facing

    override fun init() {
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.ENEMIES_1.source, TAG)
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.CULL_TIME, CULL_TIME)
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)

        facing = when {
            spawnProps.containsKey(ConstKeys.FACING) ->
                Facing.valueOf(spawnProps.get(ConstKeys.FACING, String::class)!!.uppercase())

            megaman.body.getX() < body.getX() -> Facing.LEFT

            else -> Facing.RIGHT
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(1.5f * ConstVals.PPM)
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val bodyFixture = Fixture(body, FixtureType.BODY, GameCircle().setRadius(body.getWidth() / 2f))
        body.addFixture(bodyFixture)
        debugShapes.add { bodyFixture }

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameCircle().setRadius(body.getWidth() / 2f))
        body.addFixture(damagerFixture)

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameCircle().setRadius(body.getWidth() / 2f))
        body.addFixture(damageableFixture)

        val feetFixture = Fixture(
            body, FixtureType.FEET, GameRectangle().setSize(1.25f * ConstVals.PPM, 0.1f * ConstVals.PPM)
        )
        feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        body.addFixture(feetFixture)
        feetFixture.drawingColor = Color.GREEN
        debugShapes.add { feetFixture }

        val leftFixture = Fixture(
            body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM, 0.5f * ConstVals.PPM)
        )
        leftFixture.offsetFromBodyAttachment.x = -body.getWidth() / 2f
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        body.addFixture(leftFixture)
        leftFixture.drawingColor = Color.YELLOW
        debugShapes.add { leftFixture }

        val rightFixture = Fixture(
            body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM, 0.5f * ConstVals.PPM)
        )
        rightFixture.offsetFromBodyAttachment.x = body.getWidth() / 2f
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        body.addFixture(rightFixture)
        rightFixture.drawingColor = Color.YELLOW
        debugShapes.add { rightFixture }

        body.preProcess.put(ConstKeys.DEFAULT) {
            when (facing) {
                Facing.LEFT -> if (body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT)) facing = Facing.RIGHT
                Facing.RIGHT -> if (body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT)) facing = Facing.LEFT
            }

            body.physics.velocity.x = X_VEL * ConstVals.PPM * facing.value
            body.physics.gravity.y =
                (if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_GRAVITY else GRAVITY) * ConstVals.PPM
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(2f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.hidden = damageBlink
            sprite.setFlip(isFacing(Facing.LEFT), false)
            sprite.setPosition(body.getPositionPoint(Position.BOTTOM_CENTER), Position.BOTTOM_CENTER)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(region!!, 2, 1, 0.1f, true)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }
}
