package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
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
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*

class Imorm(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.SMALL), IAnimatedEntity, IFaceable, IDirectional {

    companion object {
        const val TAG = "Imorm"
        private const val SLITHER_DISTANCE = 0.3f * ConstVals.PPM
        private const val SLITHER_DURATION = 0.6f
        private var region: TextureRegion? = null
    }

    override lateinit var facing: Facing
    override lateinit var direction: Direction

    private val slitherTimer = Timer(SLITHER_DURATION)
    override fun init() {
        if (region == null)
            region = game.assMan.getTextureRegion(TextureAsset.ENEMIES_2.source, "Imorm")
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val position = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(position)

        slitherTimer.reset()

        facing = if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT

        direction = Direction.valueOf(
            spawnProps.getOrDefault(ConstKeys.DIRECTION, "up", String::class)
                .uppercase()
        )
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add {
            slitherTimer.update(it)
            if (slitherTimer.isFinished()) {
                val x = SLITHER_DISTANCE * facing.value
                body.translate(x, 0f)
                slitherTimer.reset()
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(1f * ConstVals.PPM, 0.375f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle().set(body))
        body.addFixture(bodyFixture)
        debugShapes.add { bodyFixture}

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().setHeight(0.5f * ConstVals.PPM))
        body.addFixture(damagerFixture)
        debugShapes.add { damagerFixture}

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().setHeight(0.5f * ConstVals.PPM))
        body.addFixture(damageableFixture)
        debugShapes.add { damageableFixture}

        val shieldFixture = Fixture(body, FixtureType.SHIELD, GameRectangle().setHeight(0.5f * ConstVals.PPM))
        body.addFixture(shieldFixture)
        debugShapes.add { shieldFixture}

        val leftFixture = Fixture(
            body, FixtureType.SIDE, GameRectangle().setSize(
                0.1f * ConstVals.PPM, 0.5f * ConstVals.PPM
            )
        )
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        leftFixture.offsetFromBodyAttachment.x = -SLITHER_DISTANCE * 1.5f
        body.addFixture(leftFixture)
        debugShapes.add { leftFixture}

        val rightFixture =
            Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM, 0.5f * ConstVals.PPM))
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        rightFixture.offsetFromBodyAttachment.x = SLITHER_DISTANCE * 1.5f
        body.addFixture(rightFixture)
        debugShapes.add { rightFixture}

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        body.preProcess.put(ConstKeys.DEFAULT) {
            val width = when {
                slitherTimer.time <= 0.15f -> 0.375f * ConstVals.PPM
                slitherTimer.time <= 0.3f || slitherTimer.time >= 0.45f -> 0.5f * ConstVals.PPM
                else -> 0.95f * ConstVals.PPM
            }
            (damagerFixture.rawShape as GameRectangle).setWidth(width)
            (damageableFixture.rawShape as GameRectangle).setWidth(width)
            (shieldFixture.rawShape as GameRectangle).setWidth(0.9f * ConstVals.PPM)

            if (isFacing(Facing.LEFT) && !body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT))
                facing = Facing.RIGHT
            else if (isFacing(Facing.RIGHT) && !body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT))
                facing = Facing.LEFT
        }

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.5f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.hidden = damageBlink
            sprite.setFlip(isFacing(Facing.LEFT), false)
            sprite.setPosition(body.getPositionPoint(Position.BOTTOM_CENTER), Position.BOTTOM_CENTER)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(region!!, 1, 4, 0.15f, true)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }
}
