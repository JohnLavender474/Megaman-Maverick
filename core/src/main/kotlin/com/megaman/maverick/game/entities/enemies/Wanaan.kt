package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
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
import com.megaman.maverick.game.world.body.*

class Wanaan(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.MEDIUM), IAnimatedEntity, IDirectional {

    companion object {
        const val TAG = "Wanaan"
        private var region: TextureRegion? = null
        private const val GRAVITY = 0.15f
    }

    override var direction: Direction
        get() = body.direction
        set(value) {
            body.direction = value
        }
    // wanaan cannot be damaged
    override var invincible = true

    val comingDown: Boolean
        get() {
            val velocity = body.physics.velocity
            return when (direction) {
                Direction.UP -> velocity.y < 0f
                Direction.DOWN -> velocity.y > 0f
                Direction.LEFT -> velocity.x > 0f
                Direction.RIGHT -> velocity.x < 0f
            }
        }
    val cullPoint: Vector2
        get() = body.getCenter() // body.getPositionPoint(DirectionPositionMapper.getPosition(direction))

    override fun init() {
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.ENEMIES_2.source, TAG)
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setBottomCenterToPoint(spawn)

        val gravityOn = spawnProps.getOrDefault(ConstKeys.GRAVITY_ON, false, Boolean::class)
        body.physics.gravityOn = gravityOn

        direction = spawnProps.get(ConstKeys.DIRECTION, Direction::class)!!
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ConstVals.PPM.toFloat())
        body.physics.applyFrictionY = false

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val headFixture =
            Fixture(body, FixtureType.HEAD, GameRectangle().setSize(0.75f * ConstVals.PPM, 0.1f * ConstVals.PPM))
        headFixture.offsetFromBodyAttachment.y = body.getHeight() / 2f
        body.addFixture(headFixture)
        headFixture.drawingColor = Color.YELLOW
        debugShapes.add { headFixture}

        body.preProcess.put(ConstKeys.DEFAULT) {
            val velocity = body.physics.velocity
            if (body.isSensing(BodySense.HEAD_TOUCHING_BLOCK)) when (direction) {
                Direction.UP -> if (velocity.y > 0f) velocity.y = 0f
                Direction.DOWN -> if (velocity.y < 0f) velocity.y = 0f
                Direction.RIGHT -> if (velocity.x > 0f) velocity.x = 0f
                Direction.LEFT -> if (velocity.x < 0f) velocity.x = 0f
            }

            val gravity = body.physics.gravity
            when (direction) {
                Direction.UP -> gravity.y = -GRAVITY * ConstVals.PPM
                Direction.DOWN -> gravity.y = GRAVITY * ConstVals.PPM
                Direction.RIGHT -> gravity.x = -GRAVITY * ConstVals.PPM
                Direction.LEFT -> gravity.x = GRAVITY * ConstVals.PPM
            }
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.SHIELD, FixtureType.DAMAGER))
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(2f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.hidden = damageBlink
            sprite.setCenter(body.getCenter())
            sprite.setFlip(false, comingDown)
            when (direction) {
                Direction.UP -> sprite.rotation = 0f
                Direction.DOWN -> sprite.rotation = 180f
                Direction.LEFT -> sprite.rotation = 90f
                Direction.RIGHT -> sprite.rotation = 270f
            }
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(region!!, 2, 1, 0.1f, true)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }

    override fun getTag() = TAG
}
