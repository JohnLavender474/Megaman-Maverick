package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.interpolate
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameCircle
import com.engine.common.shapes.GameRectangle
import com.engine.common.shapes.getCenter
import com.engine.common.time.Timer
import com.engine.damage.IDamageable
import com.engine.damage.IDamager
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setCenter
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
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.BodySense
import com.megaman.maverick.game.world.FixtureType
import com.megaman.maverick.game.world.isSensing
import kotlin.reflect.KClass

class BombChute(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity {

    companion object {
        const val TAG = "BombChute"
        private const val RISE_SPEED = 8f
        private const val FALL_SPEED = -3f
        private const val X_ACCELERATION = 5f
        private const val X_MAX_SPEED = 2f
        private const val TURN_DUR = 0.85f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class to dmgNeg(10),
        Fireball::class to dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShot::class to dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) 15 else 10
        },
        ChargedShotExplosion::class to dmgNeg {
            it as ChargedShotExplosion
            if (it.fullyCharged) 10 else 5
        }
    )

    private val turnTimer = Timer(TURN_DUR)
    private var targetY = 0f
    private var targetReached = false

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            regions.put("up", atlas.findRegion("$TAG/up"))
            regions.put("turn", atlas.findRegion("$TAG/turn"))
            regions.put("down", atlas.findRegion("$TAG/down"))
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)

        body.physics.velocity.set(0f, RISE_SPEED * ConstVals.PPM)

        turnTimer.reset()

        targetY = spawnProps.get(ConstKeys.TARGET, RectangleMapObject::class)!!.rectangle.getCenter().y
        targetReached = false
    }

    private fun explodeAndDie() {
        explode()
        kill()
    }

    override fun onDamageInflictedTo(damageable: IDamageable) = explodeAndDie()

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (body.isSensing(BodySense.BODY_TOUCHING_BLOCK)) explodeAndDie()

            if (!targetReached) {
                if (body.getCenter().y >= targetY) targetReached = true
                else return@add
            }

            if (!turnTimer.isFinished()) {
                turnTimer.update(delta)
                body.physics.velocity.y = interpolate(RISE_SPEED, FALL_SPEED, turnTimer.getRatio()) * ConstVals.PPM
                return@add
            }

            body.physics.velocity.y = FALL_SPEED * ConstVals.PPM

            var xSpeed = body.physics.velocity.x

            if (body.x < getMegaman().body.x) xSpeed += X_ACCELERATION * delta * ConstVals.PPM
            else xSpeed -= X_ACCELERATION * delta * ConstVals.PPM

            if (xSpeed > X_MAX_SPEED * ConstVals.PPM) xSpeed = X_MAX_SPEED * ConstVals.PPM
            else if (xSpeed < -X_MAX_SPEED * ConstVals.PPM) xSpeed = -X_MAX_SPEED * ConstVals.PPM

            body.physics.velocity.x = xSpeed
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.75f * ConstVals.PPM)
        body.color = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBodyBounds() }

        val bodyFixture = Fixture(body, FixtureType.BODY, GameCircle().setRadius(0.375f * ConstVals.PPM))
        body.addFixture(bodyFixture)
        debugShapes.add { bodyFixture.getShape() }

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameCircle().setRadius(0.375f * ConstVals.PPM))
        body.addFixture(damagerFixture)

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameCircle().setRadius(0.375f * ConstVals.PPM))
        body.addFixture(damageableFixture)

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.35f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setCenter(body.getCenter())
            _sprite.hidden = damageBlink
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? =
            { if (!targetReached) "up" else if (!turnTimer.isFinished()) "turn" else "down" }
        val animations = objectMapOf<String, IAnimation>(
            "up" to Animation(regions.get("up"), 2, 1, 0.1f, true),
            "turn" to Animation(regions.get("turn"), 2, 2, 0.2f, false),
            "down" to Animation(regions.get("down"), 2, 2, 0.25f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}