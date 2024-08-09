package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.common.enums.Direction
import com.engine.common.enums.Position
import com.engine.common.extensions.getTextureRegion
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.cullables.CullablesComponent
import com.engine.damage.IDamager
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.drawables.sprites.setSize
import com.engine.entities.IGameEntity
import com.engine.entities.contracts.*
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.IDirectionRotatable
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType

class Bolt(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, IHazard, IDamager, ISpritesEntity,
    IAnimatedEntity, ICullableEntity, IChildEntity, IDirectionRotatable {

    companion object {
        const val TAG = "Bolt"
        private var region: TextureRegion? = null
        private const val CULL_DUR = 1f
        private val BODY_SIZE = Vector2(0.15f * ConstVals.PPM, ConstVals.PPM.toFloat())
    }

    override var parent: IGameEntity? = null

    override var directionRotation: Direction?
        get() = body.cardinalRotation
        set(value) {
            body.cardinalRotation = value
        }

    var scale = 1f

    override fun init() {
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.HAZARDS_1.source, "Bolt")
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
        addComponent(CullablesComponent(this))
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        parent = spawnProps.get(ConstKeys.ENTITY) as IGameEntity?
        scale = spawnProps.getOrDefault(ConstKeys.SCALE, 1f, Float::class)
        body.setSize(BODY_SIZE.cpy().scl(scale))
        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        directionRotation = spawnProps.getOrDefault(ConstKeys.DIRECTION, Direction.UP, Direction::class)
        when (directionRotation!!) {
            Direction.UP -> body.setBottomCenterToPoint(spawn)
            Direction.DOWN -> body.setTopCenterToPoint(spawn)
            Direction.LEFT -> body.setCenterRightToPoint(spawn)
            Direction.RIGHT -> body.setCenterLeftToPoint(spawn)
        }
        body.physics.velocity = spawnProps.getOrDefault(ConstKeys.TRAJECTORY, Vector2(), Vector2::class)
        val cullOnOutOfBounds = spawnProps.getOrDefault(ConstKeys.CULL_OUT_OF_BOUNDS, true, Boolean::class)
        if (cullOnOutOfBounds) {
            val cull = getGameCameraCullingLogic(this, CULL_DUR)
            putCullable(ConstKeys.CULL_OUT_OF_BOUNDS, cull)
        } else removeCullable(ConstKeys.CULL_OUT_OF_BOUNDS)
    }

    override fun onDestroy() {
        super<MegaGameEntity>.onDestroy()
        parent = null
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)

        val debugShapes = Array<() -> IDrawableShape?>()

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle())
        body.addFixture(damagerFixture)
        damagerFixture.getShape().color = Color.RED
        debugShapes.add { damagerFixture.getShape() }

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.setSize(BODY_SIZE.cpy().scl(scale))
            (damagerFixture.rawShape as GameRectangle).set(body)
        }

        addComponent(DrawableShapesComponent(this, debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        val spritesComponent = SpritesComponent(this, sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            sprite.setSize(ConstVals.PPM.toFloat() * scale)
            _sprite.setOriginCenter()
            _sprite.rotation = directionRotation?.rotation ?: 0f
            val position = when (directionRotation!!) {
                Direction.UP -> Position.BOTTOM_CENTER
                Direction.DOWN -> Position.TOP_CENTER
                Direction.LEFT -> Position.CENTER_RIGHT
                Direction.RIGHT -> Position.CENTER_LEFT
                else -> Position.BOTTOM_CENTER
            }
            val bodyPosition = body.getPositionPoint(position)
            _sprite.setPosition(bodyPosition, position)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(region!!, 1, 2, 0.1f, true)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }
}