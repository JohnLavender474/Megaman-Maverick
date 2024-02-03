@file:Suppress("DuplicatedCode")

package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.CAUSE_OF_DEATH_MESSAGE
import com.engine.common.enums.Position
import com.engine.common.extensions.getTextureRegion
import com.engine.common.extensions.objectMapOf
import com.engine.common.interfaces.Updatable
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.entities.GameEntity
import com.engine.entities.IGameEntity
import com.engine.entities.contracts.IAnimatedEntity
import com.engine.entities.contracts.IBodyEntity
import com.engine.entities.contracts.ISpriteEntity
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.contracts.IOwnable
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.BodySense
import com.megaman.maverick.game.world.FixtureType
import com.megaman.maverick.game.world.isSensing
import kotlin.math.abs

class Cart(game: MegamanMaverickGame) : GameEntity(game), IOwnable, IBodyEntity, ISpriteEntity, IAnimatedEntity {

    companion object {
        const val TAG = "Cart"
        private var region: TextureRegion? = null
        private const val GROUND_GRAVITY = -0.0015f
        private const val GRAVITY = -0.5f
    }

    override var owner: IGameEntity? = null

    lateinit var childBlock: Block

    override fun init() {
        if (region == null)
            region = game.assMan.getTextureRegion(TextureAsset.SPECIALS_1.source, "Cart")
        childBlock = Block(game)
        addComponent(defineBodyComponent())
        addComponent(defineSpriteComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomCenterPoint()
        body.setBottomCenterToPoint(spawn)
        game.gameEngine.spawn(
            childBlock, props(
                ConstKeys.BOUNDS to GameRectangle().setSize(0.9f * ConstVals.PPM, 0.75f * ConstVals.PPM),
                ConstKeys.PARENT to this
            )
        )
    }

    override fun onDestroy() {
        super<GameEntity>.onDestroy()
        childBlock.kill(props(CAUSE_OF_DEATH_MESSAGE to "Parent entity cart destroyed"))
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(1.25f * ConstVals.PPM, 0.75f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()

        // cart fixture
        val cartFixture =
            Fixture(GameRectangle().setSize(0.75f * ConstVals.PPM, 1.5f * ConstVals.PPM), FixtureType.CART)
        cartFixture.offsetFromBodyCenter.y = -0.25f * ConstVals.PPM
        cartFixture.putProperty(ConstKeys.ENTITY, this)
        body.addFixture(cartFixture)
        cartFixture.shape.color = Color.GOLD
        debugShapes.add { cartFixture.shape }

        // body fixture
        val bodyFixture =
            Fixture(GameRectangle().setSize(1.25f * ConstVals.PPM, 0.75f * ConstVals.PPM), FixtureType.BODY)
        body.addFixture(bodyFixture)
        bodyFixture.shape.color = Color.GRAY
        // debugShapes.add { bodyFixture.shape }

        // shield fixture
        val shieldFixture =
            Fixture(GameRectangle().setSize(1.25f * ConstVals.PPM, 0.75f * ConstVals.PPM), FixtureType.SHIELD)
        body.addFixture(shieldFixture)
        shieldFixture.shape.color = Color.BLUE
        // debugShapes.add { shieldFixture.shape }

        // left fixture
        val leftFixture =
            Fixture(GameRectangle().setSize(0.1f * ConstVals.PPM, 0.5f * ConstVals.PPM), FixtureType.SIDE)
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        leftFixture.offsetFromBodyCenter.x = -0.75f * ConstVals.PPM
        body.addFixture(leftFixture)
        leftFixture.shape.color = Color.YELLOW
        // debugShapes.add { leftFixture.shape }

        // right fixture
        val rightFixture =
            Fixture(GameRectangle().setSize(0.1f * ConstVals.PPM, 0.5f * ConstVals.PPM), FixtureType.SIDE)
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        rightFixture.offsetFromBodyCenter.x = 0.75f * ConstVals.PPM
        body.addFixture(rightFixture)
        rightFixture.shape.color = Color.YELLOW
        // debugShapes.add { rightFixture.shape }

        // feet fixture
        val feetFixture =
            Fixture(GameRectangle().setSize(ConstVals.PPM.toFloat(), 0.1f * ConstVals.PPM), FixtureType.FEET)
        feetFixture.offsetFromBodyCenter.y = -0.6f * ConstVals.PPM
        body.addFixture(feetFixture)
        feetFixture.shape.color = Color.GREEN
        // debugShapes.add { feetFixture.shape }

        body.preProcess = Updatable {
            childBlock.body.setBottomCenterToPoint(body.getBottomCenterPoint())
            body.physics.gravity.y =
                ConstVals.PPM * if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_GRAVITY else GRAVITY
        }

        addComponent(DrawableShapesComponent(this, debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpriteComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(2.45f * ConstVals.PPM, 1.85f * ConstVals.PPM)

        val spritesComponent = SpritesComponent(this, "cart" to sprite)
        spritesComponent.putUpdateFunction("cart") { _, _sprite ->
            _sprite as GameSprite
            val position = body.getBottomCenterPoint()
            _sprite.setPosition(position, Position.BOTTOM_CENTER)
        }

        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = {
            val vel = abs(body.physics.velocity.x)
            if (vel > 0.5f * ConstVals.PPM) "moving_fast"
            else if (vel > 0.1f * ConstVals.PPM) "moving_slow"
            else if (vel > 0.05f * ConstVals.PPM) "moving_slowest"
            else "idle"
        }
        val animations = objectMapOf<String, IAnimation>(
            "moving_fast" to Animation(region!!, 1, 2, 0.1f, true),
            "moving_slow" to Animation(region!!, 1, 2, 0.25f, true),
            "moving_slowest" to Animation(region!!, 1, 2, 0.35f, true),
            "idle" to Animation(region!!, 1, 2, 0.5f, false)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

}