package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.equalsAny
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.contracts.IDirectionRotatable
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.isSensing
import com.megaman.maverick.game.world.body.setEntity

class Lift(game: MegamanMaverickGame) : Block(game), ISpritesEntity, IDirectionRotatable {

    enum class LiftState {
        LIFTING, FALLING, STOPPED
    }

    companion object {
        const val TAG = "Lift"
        private var region: TextureRegion? = null
        private const val LIFT_SPEED = 5f
        private const val FALL_SPEED = 2f
    }

    override var directionRotation: Direction?
        get() = body.cardinalRotation
        set(value) {
            body.cardinalRotation = value
        }

    private lateinit var currentState: LiftState
    private lateinit var stopPoint: Vector2

    override fun init() {
        super.init()
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.PLATFORMS_1.source, "Lift")
        addComponent(defineSpritesComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        currentState = LiftState.STOPPED
        stopPoint = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(stopPoint)
        if (spawnProps.containsKey(ConstKeys.DIRECTION)) {
            var direction = spawnProps.get(ConstKeys.DIRECTION)
            if (direction is String) direction = Direction.valueOf(direction.uppercase())
            directionRotation = direction as Direction
        } else directionRotation = Direction.UP
    }

    override fun defineBodyComponent(): BodyComponent {
        val bodyComponent = super.defineBodyComponent()
        val body = bodyComponent.body
        debugShapeSuppliers.add { body.getBodyBounds() }

        val headFixture =
            Fixture(body, FixtureType.HEAD, GameRectangle().setSize(ConstVals.PPM.toFloat(), 0.1f * ConstVals.PPM))
        headFixture.setEntity(this)
        headFixture.offsetFromBodyCenter.y = 0.5f * ConstVals.PPM
        body.addFixture(headFixture)
        headFixture.rawShape.color = Color.BLUE
        debugShapeSuppliers.add { headFixture.getShape() }

        /*
        body.preProcess.put(ConstKeys.HEAD) {
            (headFixture.getShape() as GameRectangle).setSize(
                when (directionRotation!!) {
                    Direction.UP, Direction.DOWN -> Vector2(
                        ConstVals.PPM.toFloat(), 0.1f * ConstVals.PPM
                    )

                    Direction.LEFT, Direction.RIGHT -> Vector2(
                        0.1f * ConstVals.PPM, ConstVals.PPM.toFloat()
                    )
                }
            )

            headFixture.offsetFromBodyCenter = when (directionRotation!!) {
                Direction.UP -> Vector2(0f, 0.5f * ConstVals.PPM)
                Direction.DOWN -> Vector2(0f, -0.5f * ConstVals.PPM)
                Direction.LEFT -> Vector2(-0.5f * ConstVals.PPM, 0f)
                Direction.RIGHT -> Vector2(0.5f * ConstVals.PPM, 0f)
            }
        }
        */

        return bodyComponent
    }

    private fun aboveStopPoint() = when (directionRotation!!) {
        Direction.UP -> body.getCenter().y > stopPoint.y
        Direction.DOWN -> body.getCenter().y < stopPoint.y
        Direction.LEFT -> body.getCenter().x < stopPoint.x
        Direction.RIGHT -> body.getCenter().x > stopPoint.x
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({
        val megaman = game.megaman
        val megamanOverlapping = !megaman.dead && getMegaman().body.fixtures.any {
            it.second.getType().equalsAny(
                FixtureType.SIDE, FixtureType.FEET
            ) && it.second.getShape().overlaps(body)
        }

        currentState = if (megamanOverlapping && !body.isSensing(BodySense.HEAD_TOUCHING_BLOCK)) LiftState.LIFTING
        else if (body.isSensing(BodySense.HEAD_TOUCHING_BLOCK) || aboveStopPoint()) LiftState.FALLING
        else LiftState.STOPPED

        when (currentState) {
            LiftState.LIFTING -> {
                body.physics.velocity = when (directionRotation!!) {
                    Direction.UP -> Vector2(0f, LIFT_SPEED * ConstVals.PPM)
                    Direction.DOWN -> Vector2(0f, -LIFT_SPEED * ConstVals.PPM)
                    Direction.LEFT -> Vector2(-LIFT_SPEED * ConstVals.PPM, 0f)
                    Direction.RIGHT -> Vector2(LIFT_SPEED * ConstVals.PPM, 0f)
                }
            }

            LiftState.FALLING -> {
                body.physics.velocity = when (directionRotation!!) {
                    Direction.UP -> Vector2(0f, -FALL_SPEED * ConstVals.PPM)
                    Direction.DOWN -> Vector2(0f, FALL_SPEED * ConstVals.PPM)
                    Direction.LEFT -> Vector2(FALL_SPEED * ConstVals.PPM, 0f)
                    Direction.RIGHT -> Vector2(-FALL_SPEED * ConstVals.PPM, 0f)
                }
            }

            else -> {
                body.physics.velocity.setZero()
                body.setCenter(stopPoint)
            }
        }
    })

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, -1))
        sprite.setSize(0.75f * ConstVals.PPM)
        sprite.setRegion(region!!)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setOriginCenter()
            _sprite.rotation = directionRotation?.rotation ?: 0f
            _sprite.setCenter(body.getCenter())
        }
        return spritesComponent
    }
}