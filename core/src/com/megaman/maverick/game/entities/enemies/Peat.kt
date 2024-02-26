package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.common.enums.Facing
import com.engine.common.extensions.gdxArrayOf
import com.engine.common.extensions.getTextureRegion
import com.engine.common.extensions.objectMapOf
import com.engine.common.interfaces.IFaceable
import com.engine.common.interfaces.isFacing
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.damage.IDamager
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setSize
import com.engine.entities.contracts.IAnimatedEntity
import com.engine.updatables.UpdatablesComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType
import kotlin.math.max
import kotlin.reflect.KClass

class Peat(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "Peat"
        private const val DELAY_DUR = 1f
        private const val SPEED = 8f
        private const val MIN_SPEED = 1.5f
        private var region: TextureRegion? = null
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>()

    override lateinit var facing: Facing

    private val delayTimer = Timer(DELAY_DUR)

    private var startPosition: Vector2? = null
    private var targetPosition: Vector2? = null
    private var midPoint: Vector2? = null
    private var moving = false

    override fun init() {
        if (region == null)
            region = game.assMan.getTextureRegion(TextureAsset.ENEMIES_2.source, "Peat")
        super<AbstractEnemy>.init()
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)
        delayTimer.reset()
        startPosition = null
        targetPosition = null
        midPoint = null
        moving = false
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            facing = if (megaman.body.x < body.x) Facing.LEFT else Facing.RIGHT
            if (!moving) {
                body.physics.velocity.setZero()
                delayTimer.update(delta)
                if (delayTimer.isFinished()) {
                    moving = true
                    startPosition = body.getCenter()
                    targetPosition = megaman.body.getCenter()
                    val midX = (startPosition!!.x + targetPosition!!.x) / 2f
                    val midY = (startPosition!!.y + targetPosition!!.y) / 2f
                    midPoint = Vector2(midX, midY)
                }
            } else moveTowardsTarget()
        }
    }

    private fun moveTowardsTarget() {
        if (startPosition == null || targetPosition == null)
            throw IllegalStateException("Start or target position is null")

        if (body.contains(targetPosition!!)) {
            startPosition = null
            targetPosition = null
            moving = false
            delayTimer.reset()
        } else {
            val currentPos = body.getCenter()
            val direction = targetPosition!!.cpy().sub(currentPos).nor()

            val distanceToMid = currentPos.dst(midPoint!!)
            val distanceToTarget = currentPos.dst(targetPosition!!)
            val distanceStartToMid = startPosition!!.dst(midPoint!!)

            val speedFactor = 1 - (distanceToMid / distanceStartToMid).let { it * it }
            val dynamicSpeed = (MIN_SPEED + speedFactor * (SPEED - MIN_SPEED)) * ConstVals.PPM

            val decelerationFactor = distanceToTarget / distanceStartToMid
            val finalSpeed = dynamicSpeed * max(decelerationFactor, 0.5f)

            body.physics.velocity.set(direction.scl(finalSpeed))
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.25f * ConstVals.PPM)

        addComponent(
            DrawableShapesComponent(this, debugShapeSuppliers = gdxArrayOf({ body }), debug = true)
        )

        // body fixture
        val bodyFixture = Fixture(GameRectangle().setSize(0.25f * ConstVals.PPM), FixtureType.BODY)
        body.addFixture(bodyFixture)

        // damager fixture
        val damagerFixture = Fixture(GameRectangle().setSize(0.25f * ConstVals.PPM), FixtureType.DAMAGER)
        body.addFixture(damagerFixture)

        // damageable fixture
        val damageableFixture = Fixture(GameRectangle().setSize(0.25f * ConstVals.PPM), FixtureType.DAMAGEABLE)
        body.addFixture(damageableFixture)

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(this, TAG to sprite)
        spritesComponent.putUpdateFunction(TAG) { _, _sprite ->
            _sprite as GameSprite
            val center = body.getCenter()
            _sprite.setCenter(center.x, center.y)
            _sprite.setFlip(isFacing(Facing.LEFT), false)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(region!!, 1, 2, 0.1f, true)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }

}