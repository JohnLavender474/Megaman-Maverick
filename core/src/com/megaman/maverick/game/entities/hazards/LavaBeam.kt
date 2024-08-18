package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.common.enums.Direction
import com.engine.common.extensions.gdxArrayOf
import com.engine.common.extensions.getTextureRegion
import com.engine.common.extensions.objectMapOf
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.cullables.CullablesComponent
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.sorting.DrawingPriority
import com.engine.drawables.sorting.DrawingSection
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setCenter
import com.engine.entities.contracts.IAnimatedEntity
import com.engine.entities.contracts.IBodyEntity
import com.engine.entities.contracts.ICullableEntity
import com.engine.entities.contracts.ISpritesEntity
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.IDirectionRotatable
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType

class LavaBeam(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity, IAnimatedEntity,
    ICullableEntity, IHazard, IDirectionRotatable {

    companion object {
        const val TAG = "LavaBeam"
        private var region: TextureRegion? = null
    }

    override var directionRotation: Direction?
        get() = body.cardinalRotation
        set(value) {
            body.cardinalRotation = value
        }

    override fun getEntityType() = EntityType.HAZARD

    override fun init() {
        super<MegaGameEntity>.init()
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.HAZARDS_1.source, "LavaBeam")
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
        addComponent(defineCullablesComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        directionRotation = spawnProps.get(ConstKeys.DIRECTION, Direction::class)!!
        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        when (directionRotation!!) {
            Direction.UP -> body.setTopCenterToPoint(spawn)
            Direction.DOWN -> body.setBottomCenterToPoint(spawn)
            Direction.LEFT -> body.setCenterLeftToPoint(spawn)
            Direction.RIGHT -> body.setCenterRightToPoint(spawn)
        }
        val speed = spawnProps.get(ConstKeys.SPEED, Float::class)!!
        val trajectory = when (directionRotation!!) {
            Direction.UP -> Vector2(0f, speed)
            Direction.DOWN -> Vector2(0f, -speed)
            Direction.LEFT -> Vector2(-speed, 0f)
            Direction.RIGHT -> Vector2(speed, 0f)
        }
        body.physics.velocity.set(trajectory)
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(2f * ConstVals.PPM, 5f * ConstVals.PPM)

        val deathFixture = Fixture(body, FixtureType.DEATH, GameRectangle(body))
        body.addFixture(deathFixture)

        addComponent(
            DrawableShapesComponent(
                debugShapeSuppliers = gdxArrayOf({ body.getBodyBounds() }), debug = true
            )
        )

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.BACKGROUND, 0))
        sprite.setSize(2f * ConstVals.PPM, 5f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setCenter(body.getCenter())
            _sprite.setOriginCenter()
            _sprite.rotation = directionRotation!!.rotation
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(region!!, 1, 3, 0.1f, true)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }

    private fun defineCullablesComponent(): CullablesComponent {
        val cullOutOfBounds = getGameCameraCullingLogic(this)
        return CullablesComponent(objectMapOf(ConstKeys.CULL_OUT_OF_BOUNDS to cullOutOfBounds))
    }
}