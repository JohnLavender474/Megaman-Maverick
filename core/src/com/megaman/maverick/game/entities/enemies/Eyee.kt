package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.GameLogger
import com.engine.common.extensions.equalsAny
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.extensions.toGdxArray
import com.engine.common.interpolate
import com.engine.common.objects.Loop
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
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
import com.megaman.maverick.game.world.FixtureType
import kotlin.reflect.KClass

class Eyee(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity {

    enum class EyeeState {
        MOVING_TO_END, WAITING_AT_END, MOVING_TO_START, WAITING_AT_START
    }

    companion object {
        const val TAG = "Eyee"
        private const val WAIT_DURATION = 0.6f
        private const val SPEED = 10f
        private var openRegion: TextureRegion? = null
        private var blinkRegion: TextureRegion? = null
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class to dmgNeg(10),
        Fireball::class to dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShot::class to dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) ConstVals.MAX_HEALTH else 15
        },
        ChargedShotExplosion::class to dmgNeg {
            it as ChargedShotExplosion
            if (it.fullyCharged) ConstVals.MAX_HEALTH else 10
        }
    )

    private val loop = Loop(EyeeState.values().toGdxArray())
    private val currentState: EyeeState
        get() = loop.getCurrent()
    private val waitTimer = Timer(WAIT_DURATION)

    private lateinit var start: Vector2
    private lateinit var end: Vector2

    private var progress = 0f

    override fun init() {
        super<AbstractEnemy>.init()
        if (openRegion == null || blinkRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            openRegion = atlas.findRegion("Eyee/Open")
            blinkRegion = atlas.findRegion("Eyee/Blink")
        }
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.CULL_TIME, 2f)
        super.spawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)

        start = spawn.cpy()
        val endX = spawnProps.getOrDefault(ConstKeys.X, 0f, Float::class)
        val endY = spawnProps.getOrDefault(ConstKeys.Y, 0f, Float::class)
        end = start.cpy().add(endX * ConstVals.PPM, endY * ConstVals.PPM)

        loop.reset()
        waitTimer.reset()
        progress = 0f

        GameLogger.debug(TAG, "Movement scalar = $movementScalar")
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            when (currentState) {
                EyeeState.MOVING_TO_END -> {
                    progress += SPEED * movementScalar * ConstVals.PPM * (delta / start.dst(end))
                    if (progress >= 1f) {
                        progress = 1f
                        loop.next()
                        waitTimer.reset()
                    }
                }

                EyeeState.WAITING_AT_END -> {
                    waitTimer.update(delta)
                    if (waitTimer.isFinished()) {
                        loop.next()
                        progress = 1f
                    }
                }

                EyeeState.MOVING_TO_START -> {
                    progress -= SPEED * movementScalar * ConstVals.PPM * (delta / start.dst(end))
                    if (progress <= 0f) {
                        progress = 0f
                        loop.next()
                        waitTimer.reset()
                    }
                }

                EyeeState.WAITING_AT_START -> {
                    waitTimer.update(delta)
                    if (waitTimer.isFinished()) {
                        loop.next()
                        progress = 0f
                    }
                }
            }

            val center = interpolate(start, end, progress)
            body.setCenter(center)
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.75f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle().setSize(0.75f * ConstVals.PPM))
        body.addFixture(bodyFixture)
        bodyFixture.getShape().color = Color.GOLD
        debugShapes.add { bodyFixture.getShape() }

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(0.75f * ConstVals.PPM))
        body.addFixture(damagerFixture)
        damagerFixture.getShape().color = Color.RED
        debugShapes.add { damagerFixture.getShape() }

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().setSize(0.75f * ConstVals.PPM))
        body.addFixture(damageableFixture)
        damageableFixture.getShape().color = Color.PURPLE
        debugShapes.add { damageableFixture.getShape() }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(0.85f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.hidden = damageBlink
            _sprite.setCenter(body.getCenter())
            _sprite.hidden = if (invincible) damageBlink else false
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? =
            { if (currentState.equalsAny(EyeeState.MOVING_TO_START, EyeeState.MOVING_TO_END)) "open" else "blink" }
        val animations = objectMapOf<String, IAnimation>(
            "open" to Animation(openRegion!!),
            "blink" to Animation(blinkRegion!!, 1, 5, 0.1f, false)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}