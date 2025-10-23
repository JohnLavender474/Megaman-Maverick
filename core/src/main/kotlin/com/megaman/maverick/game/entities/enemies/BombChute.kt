package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.UtilMethods.interpolate
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
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
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.*

class BombChute(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.SMALL), IAnimatedEntity {

    companion object {
        const val TAG = "BombChute"
        private const val RISE_SPEED = 8f
        private const val FALL_SPEED = -3f
        private const val TURN_DUR = 0.85f
        private const val X_MAX_SPEED = 3f
        private const val X_ACCELERATION = 5f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private val turnTimer = Timer(TURN_DUR)

    private var targetReached = false
    private var targetY = 0f

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            gdxArrayOf("up", "turn", "down").forEach { key -> regions.put(key, atlas.findRegion("$TAG/$key")) }
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.START, RectangleMapObject::class)!!.rectangle.getCenter()
        body.setCenter(spawn)

        body.physics.velocity.set(0f, RISE_SPEED * ConstVals.PPM)

        turnTimer.reset()

        targetY = spawnProps.get(ConstKeys.TARGET, RectangleMapObject::class)!!.rectangle.getCenter().y
        targetReached = false
    }

    private fun explodeAndDie() {
        explode()
        destroy()
    }

    override fun onDamageInflictedTo(damageable: IDamageable) = explodeAndDie()

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (body.isSensing(BodySense.BODY_TOUCHING_BLOCK)) explodeAndDie()

            if (!targetReached) when {
                body.getCenter().y >= targetY -> targetReached = true
                else -> return@add
            }

            if (!turnTimer.isFinished()) {
                turnTimer.update(delta)
                body.physics.velocity.y = interpolate(RISE_SPEED, FALL_SPEED, turnTimer.getRatio()) * ConstVals.PPM
                return@add
            }

            body.physics.velocity.y = FALL_SPEED * ConstVals.PPM

            var xSpeed = body.physics.velocity.x

            when {
                body.getX() < megaman.body.getX() -> xSpeed += X_ACCELERATION * delta * ConstVals.PPM
                else -> xSpeed -= X_ACCELERATION * delta * ConstVals.PPM
            }

            when {
                xSpeed > X_MAX_SPEED * ConstVals.PPM -> xSpeed = X_MAX_SPEED * ConstVals.PPM
                xSpeed < -X_MAX_SPEED * ConstVals.PPM -> xSpeed = -X_MAX_SPEED * ConstVals.PPM
            }

            body.physics.velocity.x = xSpeed
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ConstVals.PPM.toFloat())
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE)
        )
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().also { sprite -> sprite.setSize(2f * ConstVals.PPM) })
        .preProcess { _, sprite ->
            sprite.hidden = damageBlink
            sprite.setCenter(body.getCenter())
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier {
                    when {
                        !targetReached -> "up"
                        !turnTimer.isFinished() -> "turn"
                        else -> "down"
                    }
                }
                .addAnimations(
                    "up" pairTo Animation(regions["up"], 2, 1, 0.1f, true),
                    "turn" pairTo Animation(regions["turn"], 2, 2, 0.2f, false),
                    "down" pairTo Animation(regions["down"], 2, 2, 0.25f, true)
                )
                .build()
        )
        .build()
}
