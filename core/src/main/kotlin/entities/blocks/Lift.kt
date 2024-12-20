package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.equalsAny
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.interfaces.IDirectional
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
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.*

class Lift(game: MegamanMaverickGame) : Block(game), ISpritesEntity, IDirectional {

    enum class LiftState {
        LIFTING, FALLING, STOPPED
    }

    companion object {
        const val TAG = "Lift"
        private var region: TextureRegion? = null
        private const val LIFT_SPEED = 5f
        private const val FALL_SPEED = 2f
    }

    override var direction: Direction
        get() = body.direction
        set(value) {
            body.direction = value
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
            direction = direction as Direction
        } else direction = Direction.UP
    }

    override fun defineBodyComponent(): BodyComponent {
        val bodyComponent = super.defineBodyComponent()
        val body = bodyComponent.body
        debugShapeSuppliers.add { body.getBounds() }

        val headFixture =
            Fixture(body, FixtureType.HEAD, GameRectangle().setSize(ConstVals.PPM.toFloat(), 0.1f * ConstVals.PPM))
        headFixture.setEntity(this)
        headFixture.offsetFromBodyAttachment.y = 0.5f * ConstVals.PPM
        body.addFixture(headFixture)
        debugShapeSuppliers.add { headFixture }

        /*
        body.preProcess.put(ConstKeys.HEAD) {
            (headFixture.rawShape as GameRectangle).setSize(
                when (direction) {
                    Direction.UP, Direction.DOWN -> Vector2(
                        ConstVals.PPM.toFloat(), 0.1f * ConstVals.PPM
                    )

                    Direction.LEFT, Direction.RIGHT -> Vector2(
                        0.1f * ConstVals.PPM, ConstVals.PPM.toFloat()
                    )
                }
            )

            headFixture.offsetFromBodyAttachment = when (direction) {
                Direction.UP -> Vector2(0f, 0.5f * ConstVals.PPM)
                Direction.DOWN -> Vector2(0f, -0.5f * ConstVals.PPM)
                Direction.LEFT -> Vector2(-0.5f * ConstVals.PPM, 0f)
                Direction.RIGHT -> Vector2(0.5f * ConstVals.PPM, 0f)
            }
        }
        */

        return bodyComponent
    }

    private fun aboveStopPoint() = when (direction) {
        Direction.UP -> body.getCenter().y > stopPoint.y
        Direction.DOWN -> body.getCenter().y < stopPoint.y
        Direction.LEFT -> body.getCenter().x < stopPoint.x
        Direction.RIGHT -> body.getCenter().x > stopPoint.x
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({
        var megamanOverlapping = !megaman().dead && megaman().body.fixtures.values().any { set ->
            set.any { fixture ->
                fixture.getType().equalsAny(FixtureType.SIDE, FixtureType.FEET) &&
                    fixture.getShape().overlaps(body.getBounds())
            }
        }

        currentState = if (megamanOverlapping && !body.isSensing(BodySense.HEAD_TOUCHING_BLOCK)) LiftState.LIFTING
        else if (body.isSensing(BodySense.HEAD_TOUCHING_BLOCK) || aboveStopPoint()) LiftState.FALLING
        else LiftState.STOPPED

        when (currentState) {
            LiftState.LIFTING -> {
                val velocity = GameObjectPools.fetch(Vector2::class)
                when (direction) {
                    Direction.UP -> velocity.set(0f, LIFT_SPEED * ConstVals.PPM)
                    Direction.DOWN -> velocity.set(0f, -LIFT_SPEED * ConstVals.PPM)
                    Direction.LEFT -> velocity.set(-LIFT_SPEED * ConstVals.PPM, 0f)
                    Direction.RIGHT -> velocity.set(LIFT_SPEED * ConstVals.PPM, 0f)
                }
                body.physics.velocity.set(velocity)
            }

            LiftState.FALLING -> {
                val velocity = GameObjectPools.fetch(Vector2::class)
                when (direction) {
                    Direction.UP -> velocity.set(0f, -FALL_SPEED * ConstVals.PPM)
                    Direction.DOWN -> velocity.set(0f, FALL_SPEED * ConstVals.PPM)
                    Direction.LEFT -> velocity.set(FALL_SPEED * ConstVals.PPM, 0f)
                    Direction.RIGHT -> velocity.set(-FALL_SPEED * ConstVals.PPM, 0f)
                }
                body.physics.velocity.set(velocity)
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
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.setOriginCenter()
            sprite.rotation = direction.rotation
            sprite.setCenter(body.getCenter())
        }
        return spritesComponent
    }
}
