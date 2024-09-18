package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.*
import com.mega.game.engine.common.interpolate
import com.mega.game.engine.common.objects.Loop
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.events.Event
import com.mega.game.engine.events.IEventListener
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
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import kotlin.reflect.KClass

class Eyee(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IEventListener {

    enum class EyeeState {
        MOVING_TO_END, WAITING_AT_END, MOVING_TO_START, WAITING_AT_START
    }

    companion object {
        const val TAG = "Eyee"
        private const val WAIT_DURATION = 0.6f
        private const val SPEED = 10f
        private const val CULL_TIME = 2f
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
    override val eventKeyMask = objectSetOf<Any>(EventType.SET_GAME_CAM_ROTATION, EventType.END_GAME_CAM_ROTATION)

    private val loop = Loop(EyeeState.values().toGdxArray())
    private val currentState: EyeeState
        get() = loop.getCurrent()
    private val waitTimer = Timer(WAIT_DURATION)
    private lateinit var start: Vector2
    private lateinit var end: Vector2
    private var progress = 0f
    private var canMove = true

    override fun init() {
        super.init()
        if (openRegion == null || blinkRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            openRegion = atlas.findRegion("Eyee/Open")
            blinkRegion = atlas.findRegion("Eyee/Blink")
        }
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.CULL_TIME, CULL_TIME)
        super.onSpawn(spawnProps)

        game.eventsMan.addListener(this)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)

        start = spawn.cpy()
        val endX = spawnProps.getOrDefault(ConstKeys.X, 0f, Float::class)
        val endY = spawnProps.getOrDefault(ConstKeys.Y, 0f, Float::class)
        end = start.cpy().add(endX * ConstVals.PPM, endY * ConstVals.PPM)

        loop.reset()
        waitTimer.reset()
        progress = 0f

        canMove = true

        GameLogger.debug(TAG, "Movement scalar = $movementScalar")
    }

    override fun onDestroy() {
        super.onDestroy()
        game.eventsMan.removeListener(this)
    }

    override fun onEvent(event: Event) {
        when (event.key) {
            EventType.SET_GAME_CAM_ROTATION -> canMove = false
            EventType.END_GAME_CAM_ROTATION -> canMove = true
        }
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (!canMove) {
                body.physics.velocity.setZero()
                return@add
            }

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
        body.setSize(0.5f * ConstVals.PPM)

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
        sprite.setSize(0.75f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setOriginCenter()
            _sprite.rotation = getMegaman().directionRotation!!.rotation
            _sprite.setCenter(body.getCenter())
            _sprite.hidden = damageBlink
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