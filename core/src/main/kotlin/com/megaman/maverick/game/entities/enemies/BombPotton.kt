package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.animations.Animation
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.enums.Position
import com.maverick.game.engine.common.enums.Size
import com.maverick.game.engine.common.extensions.getTextureRegion
import com.maverick.game.engine.common.interfaces.IFaceable
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.drawables.shapes.DrawableShapesComponent
import com.maverick.game.engine.drawables.shapes.IDrawableShape
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.drawables.sprites.setCenter
import com.maverick.game.engine.drawables.sprites.setSize
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.entities.contracts.IAnimatedEntity
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.targetPlayer
import com.megaman.maverick.game.entities.projectiles.SmallGreenMissile
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.*

class BombPotton(services: MegaServices) : AbstractEnemy(services, size = Size.SMALL), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "BombPotton"
        private const val MAX_SPEED = 8f
        private const val ACCELERATION = 15f
        private var region: TextureRegion? = null
    }

    override lateinit var facing: Facing

    private val target = Vector2()
    private var targetReached = false
    private var launchedBomb = false
    private var speed = 0f

    override fun init(vararg params: Any) {
        if (region == null) region = services.assets.getTextureRegion(TextureAsset.ENEMIES_2.source, TAG)
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val position = spawnProps.get(ConstKeys.START, RectangleMapObject::class)!!.rectangle.getCenter()
        body.setCenter(position)

        target.set(spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter())
        targetReached = false

        speed = 0f

        launchedBomb = false

        facing = if (body.getX() > targetPlayer.body.getX()) Facing.LEFT else Facing.RIGHT
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            speed = minOf(MAX_SPEED, speed + ACCELERATION * delta)

            if (!targetReached) {
                val trajectory = GameObjectPools.fetch(Vector2::class)
                    .set(target)
                    .sub(body.getCenter())
                    .nor()
                    .scl(speed * ConstVals.PPM)
                body.physics.velocity.set(trajectory)

                if (body.getCenter().epsilonEquals(target, 0.1f * ConstVals.PPM)) {
                    speed = 0f
                    targetReached = true
                    facing = if (body.getX() > targetPlayer.body.getX()) Facing.LEFT else Facing.RIGHT
                }
            } else {
                val trajectory = GameObjectPools.fetch(Vector2::class)
                    .set(speed * facing.value * ConstVals.PPM, 0f)
                body.physics.velocity.set(trajectory)

                if (!launchedBomb && body.getX() < targetPlayer.body.getMaxX() && body.getMaxX() > targetPlayer.body.getX()) {
                    launchBomb()
                    launchedBomb = true
                }
            }
        }
    }

    private fun launchBomb() {
        val bomb = MegaEntityFactory.fetch(SmallGreenMissile::class)!!
        bomb.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.DIRECTION pairTo Direction.UP,
                ConstKeys.EXPLOSION pairTo SmallGreenMissile.WAVE_EXPLOSION,
                ConstKeys.POSITION pairTo body.getPositionPoint(Position.BOTTOM_CENTER)
            )
        )
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(1.5f * ConstVals.PPM)
        body.preProcess.put(ConstKeys.DEFAULT) {
            if (body.isSensing(BodySense.BODY_TOUCHING_BLOCK)) {
                explode()
                destroy()
            }
        }

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGER)
        )
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(2f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putPreProcess { _, _ ->
            sprite.setCenter(body.getCenter())
            sprite.setFlip(isFacing(Facing.RIGHT), false)
            sprite.hidden = damageBlink
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(region!!, 2, 1, 0.1f, true)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }
}
