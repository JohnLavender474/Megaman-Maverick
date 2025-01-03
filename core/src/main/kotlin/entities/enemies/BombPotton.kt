package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.entities.projectiles.SmallMissile
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.*
import kotlin.reflect.KClass

class BombPotton(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "BombPotton"
        private const val MAX_SPEED = 8f
        private const val ACCELERATION = 15f
        private var region: TextureRegion? = null
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class pairTo dmgNeg(10),
        Fireball::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShot::class pairTo dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) 15 else 10
        },
        ChargedShotExplosion::class pairTo dmgNeg {
            it as ChargedShotExplosion
            if (it.fullyCharged) 10 else 5
        }
    )
    override lateinit var facing: Facing

    private val target = Vector2()

    private var speed = 0f
    private var targetReached = false
    private var launchedBomb = false

    override fun init() {
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.ENEMIES_2.source, TAG)
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
        facing = if (body.getX() > megaman.body.getX()) Facing.LEFT else Facing.RIGHT
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
                    facing = if (body.getX() > megaman.body.getX()) Facing.LEFT else Facing.RIGHT
                }
            } else {
                val trajectory = GameObjectPools.fetch(Vector2::class)
                    .set(speed * facing.value * ConstVals.PPM, 0f)
                body.physics.velocity.set(trajectory)

                if (!launchedBomb && body.getX() < megaman.body.getMaxX() && body.getMaxX() > megaman.body.getX()) {
                    launchBomb()
                    launchedBomb = true
                }
            }
        }
    }

    private fun launchBomb() {
        val greenBomb = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.SMALL_MISSILE)!!
        greenBomb.spawn(
            props(
                ConstKeys.POSITION pairTo body.getPositionPoint(Position.BOTTOM_CENTER),
                ConstKeys.OWNER pairTo this,
                ConstKeys.DIRECTION pairTo Direction.UP,
                ConstKeys.EXPLOSION pairTo SmallMissile.WAVE_EXPLOSION
            )
        )
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(1.25f * ConstVals.PPM)
        body.preProcess.put(ConstKeys.DEFAULT) {
            if (body.isSensing(BodySense.BODY_TOUCHING_BLOCK)) {
                explode()
                destroy()
            }
        }

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body }
        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGER)
        )
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.5f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
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
