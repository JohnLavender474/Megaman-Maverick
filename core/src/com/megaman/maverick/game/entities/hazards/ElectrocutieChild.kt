package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.GameEntity
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.IChildEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.world.Body
import com.mega.game.engine.world.BodyComponent
import com.mega.game.engine.world.BodyType
import com.mega.game.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType

class ElectrocutieChild(game: MegamanMaverickGame) : MegaGameEntity(game), IHazard, IDamager, IBodyEntity,
    ISpritesEntity, IAnimatedEntity, IChildEntity {

    enum class ElectrocutieState {
        MOVE, CHARGE, SHOCK
    }

    companion object {
        const val TAG = "Electrocutie"
        private var moveRegion: TextureRegion? = null
        private var chargeRegion: TextureRegion? = null
        private var shockRegion: TextureRegion? = null
    }

    override var parent: GameEntity? = null

    private lateinit var direction: Direction
    private lateinit var spawn: Vector2
    private var resetBodyPosition = true

    override fun getEntityType() = EntityType.HAZARD

    override fun init() {
        if (moveRegion == null || chargeRegion == null || shockRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.HAZARDS_1.source)
            moveRegion = atlas.findRegion("Electrocutie/Move")
            chargeRegion = atlas.findRegion("Electrocutie/Charge")
            shockRegion = atlas.findRegion("Electrocutie/Shock")
        }
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        parent = spawnProps.get(ConstKeys.PARENT) as GameEntity?
        spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        direction = spawnProps.get(ConstKeys.DIRECTION, Direction::class)!!
        resetBodyPosition = true
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)

        val debugShapes = Array<() -> IDrawableShape?>()

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle())
        body.addFixture(bodyFixture)
        bodyFixture.getShape().color = Color.GRAY
        debugShapes.add { bodyFixture.getShape() }

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle())
        body.addFixture(damagerFixture)
        damagerFixture.getShape().color = Color.RED
        debugShapes.add { damagerFixture.getShape() }

        val shieldFixture = Fixture(body, FixtureType.SHIELD, GameRectangle())
        body.addFixture(shieldFixture)
        shieldFixture.getShape().color = Color.BLUE
        debugShapes.add { shieldFixture.getShape() }

        body.preProcess.put(ConstKeys.DEFAULT) {
            val size = if (direction.isVertical()) Vector2(0.85f * ConstVals.PPM, 0.35f * ConstVals.PPM)
            else Vector2(0.35f * ConstVals.PPM, 0.85f * ConstVals.PPM)

            body.setSize(size)
            body.fixtures.forEach { (it.second.getShape() as GameRectangle).setSize(size) }

            if (resetBodyPosition) {
                when (direction) {
                    Direction.UP -> body.setBottomCenterToPoint(spawn)
                    Direction.DOWN -> body.setTopCenterToPoint(spawn)
                    Direction.LEFT -> body.setCenterRightToPoint(spawn)
                    Direction.RIGHT -> body.setCenterLeftToPoint(spawn)
                }
                resetBodyPosition = false
            }
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.25f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setOriginCenter()
            _sprite.rotation = direction.rotation

            val position = when (direction) {
                Direction.UP -> Position.BOTTOM_CENTER
                Direction.DOWN -> Position.TOP_CENTER
                Direction.LEFT -> Position.CENTER_RIGHT
                Direction.RIGHT -> Position.CENTER_LEFT
            }
            val bodyPosition = body.getPositionPoint(position)
            _sprite.setCenter(bodyPosition)

            val offset = when (direction) {
                Direction.UP -> Vector2(0f, 0.15f * ConstVals.PPM)
                Direction.DOWN -> Vector2(0f, -0.15f * ConstVals.PPM)
                Direction.LEFT -> Vector2(-0.15f * ConstVals.PPM, 0f)
                Direction.RIGHT -> Vector2(0.15f * ConstVals.PPM, 0f)
            }
            _sprite.translate(offset.x, offset.y)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = { (parent as Electrocutie).currentState.name }
        val animations = objectMapOf<String, IAnimation>(
            ElectrocutieState.MOVE.name to Animation(moveRegion!!),
            ElectrocutieState.CHARGE.name to Animation(chargeRegion!!, 1, 2, 0.1f, true),
            ElectrocutieState.SHOCK.name to Animation(shockRegion!!)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

}