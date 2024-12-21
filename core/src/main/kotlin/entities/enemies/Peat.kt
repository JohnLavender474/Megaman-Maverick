package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IFaceable

import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
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
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter
import kotlin.math.max
import kotlin.reflect.KClass

class Peat(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "Peat"
        private const val DELAY_DUR = 0.5f
        private const val SPEED = 8f
        private const val MIN_SPEED = 2f
        private var region: TextureRegion? = null
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class pairTo dmgNeg(10),
        Fireball::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShot::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShotExplosion::class pairTo dmgNeg(ConstVals.MAX_HEALTH)
    )
    override lateinit var facing: Facing

    private val delayTimer = Timer(DELAY_DUR)
    private var startPosition: Vector2? = null
    private var targetPosition: Vector2? = null
    private var midPoint: Vector2? = null
    private var moving = false

    override fun init() {
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.ENEMIES_2.source, TAG)
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)

        val gravityX = spawnProps.getOrDefault("${ConstKeys.GRAVITY}_${ConstKeys.X}", 0f, Float::class)
        val gravityY = spawnProps.getOrDefault("${ConstKeys.GRAVITY}_${ConstKeys.Y}", 0f, Float::class)
        body.physics.gravity.set(gravityX * ConstVals.PPM, gravityY * ConstVals.PPM)

        delayTimer.reset()

        startPosition = null
        targetPosition = null
        midPoint = null

        moving = false
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            facing = if (megaman().body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT

            if (!moving) {
                body.physics.velocity.setZero()

                delayTimer.update(delta)
                if (delayTimer.isFinished()) {
                    moving = true

                    startPosition = body.getCenter(false)
                    targetPosition = megaman().body.getCenter(false)

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

        if (body.getBounds().contains(targetPosition!!)) {
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
        body.setSize(0.5f * ConstVals.PPM)

        addComponent(
            DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body.getBounds() }), debug = true)
        )

        val bodyFixture = Fixture(body, FixtureType.BODY, GameCircle().setRadius(0.25f * ConstVals.PPM))
        body.addFixture(bodyFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameCircle().setRadius(0.25f * ConstVals.PPM))
        body.addFixture(damagerFixture)

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameCircle().setRadius(0.25f * ConstVals.PPM))
        body.addFixture(damageableFixture)

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(2f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.hidden = damageBlink
            sprite.setCenter(body.getCenter())
            sprite.setFlip(isFacing(Facing.LEFT), false)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(region!!, 1, 2, 0.1f, true)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }
}
