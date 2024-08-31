package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.GameLogger
import com.engine.common.enums.Position
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.damage.IDamager
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sorting.DrawingPriority
import com.engine.drawables.sorting.DrawingSection
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.entities.contracts.IAnimatedEntity
import com.engine.entities.contracts.IDrawableShapesEntity
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
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.entities.projectiles.SealionBall
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType
import kotlin.reflect.KClass

class Sealion(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IDrawableShapesEntity {

    companion object {
        const val TAG = "Sealion"
        private const val WAIT_DUR = 0.25f
        private const val TAUNT_DUR = 0.5f
        private const val BEFORE_THROW_BALL_DELAY = 0.25f
        private const val POUT_SINK_DELAY = 1f
        private const val POUT_FADE_OUT_DUR = 2.5f
        private const val SINK_VELOCITY_Y = -0.75f
        private const val BALL_CATCH_BOUNDS_OFFSET_X = -0.125f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class SealionState {
        WAIT, THROW, TAUNT, POUT
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class to dmgNeg(15),
        Fireball::class to dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShot::class to dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) ConstVals.MAX_HEALTH else 20
        },
        ChargedShotExplosion::class to dmgNeg {
            it as ChargedShotExplosion
            if (it.fullyCharged) ConstVals.MAX_HEALTH else 10
        }
    )

    private val timers = objectMapOf(
        "wait" to Timer(WAIT_DUR),
        "taunt" to Timer(TAUNT_DUR),
        "before_throw_ball_delay" to Timer(BEFORE_THROW_BALL_DELAY),
        "pout_sink_delay" to Timer(POUT_SINK_DELAY),
        "pout_fade_out" to Timer(POUT_FADE_OUT_DUR)
    )
    private val ballCatchBounds = GameRectangle().setSize(0.5f * ConstVals.PPM)
    private lateinit var state: SealionState
    private var sealionBall: SealionBall? = null
    private var ballInHands = true
    private var fadingOut = false

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            regions.put("wait_with_ball", atlas.findRegion("$TAG/wait_with_ball"))
            regions.put("wait_no_ball", atlas.findRegion("$TAG/wait_no_ball"))
            regions.put("taunt_with_ball", atlas.findRegion("$TAG/taunt_with_ball"))
            regions.put("taunt_no_ball", atlas.findRegion("$TAG/taunt_no_ball"))
            regions.put("pout", atlas.findRegion("$TAG/pout"))
            regions.put("before_throw_ball", atlas.findRegion("$TAG/before_throw_ball"))
            regions.put("after_throw_ball", atlas.findRegion("$TAG/after_throw_ball"))
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomCenterPoint()
        body.setBottomCenterToPoint(spawn)

        ballCatchBounds.setCenter(body.getCenter().add(BALL_CATCH_BOUNDS_OFFSET_X * ConstVals.PPM, 0f))
        sealionBall = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.SEALION_BALL)!! as SealionBall
        game.engine.spawn(
            sealionBall!!,
            props(ConstKeys.OWNER to this, ConstKeys.POSITION to ballCatchBounds.getCenter())
        )

        fadingOut = false
        state = SealionState.WAIT

        timers.values().forEach { it.reset() }
    }

    fun onBallDamagedInflicted() {
        GameLogger.debug(TAG, "On ball damaged inflicted")
        state = SealionState.TAUNT
    }

    fun onBallDestroyed() {
        GameLogger.debug(TAG, "On ball destroyed")
        sealionBall = null
        state = SealionState.POUT
    }

    private fun throwBall() {
        GameLogger.debug(TAG, "Throw ball")
        sealionBall!!.body.setBottomCenterToPoint(ballCatchBounds.getTopCenterPoint())
        sealionBall!!.throwBall()
        ballInHands = false
    }

    private fun catchBall() {
        GameLogger.debug(TAG, "Catch ball")
        sealionBall!!.body.setBottomCenterToPoint(ballCatchBounds.getTopCenterPoint())
        sealionBall!!.catchBall()
        ballInHands = true
    }

    private fun canCatchBall() = sealionBall!!.body.contains(ballCatchBounds.getBottomCenterPoint()) ||
            sealionBall!!.body.getMaxY() < ballCatchBounds.y // in case ball falls below sealion before it's caught

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            when (state) {
                SealionState.WAIT -> {
                    val timer = timers["wait"]
                    timer.update(delta)
                    if (timer.isFinished()) {
                        timer.reset()
                        state = SealionState.THROW
                        GameLogger.debug(TAG, "Wait timer finished, set state to THROW")
                    }
                }

                SealionState.THROW -> {
                    if (ballInHands) {
                        val beforeThrowBallDelayTimer = timers["before_throw_ball_delay"]
                        beforeThrowBallDelayTimer.update(delta)
                        if (!beforeThrowBallDelayTimer.isFinished()) return@add
                        if (beforeThrowBallDelayTimer.isJustFinished()) {
                            beforeThrowBallDelayTimer.reset()
                            throwBall()
                        }
                    } else if (canCatchBall()) {
                        catchBall()
                        state = SealionState.WAIT
                        GameLogger.debug(TAG, "Catch ball, set state to WAIT")
                    }
                }

                SealionState.TAUNT -> {
                    val timer = timers["taunt"]
                    timer.update(delta)
                    if (!ballInHands && canCatchBall()) catchBall()
                    if (timer.isFinished()) {
                        timer.reset()
                        state = if (ballInHands) SealionState.WAIT else SealionState.THROW
                        GameLogger.debug(TAG, "Taunt finished, setting state to $state")
                    }
                }

                SealionState.POUT -> {
                    val delayTimer = timers["pout_sink_delay"]
                    delayTimer.update(delta)
                    if (!delayTimer.isFinished()) return@add
                    if (delayTimer.isJustFinished()) {
                        body.physics.velocity.y = SINK_VELOCITY_Y * ConstVals.PPM
                        fadingOut = true
                        GameLogger.debug(TAG, "Delay timer finished, start fading out")
                    }

                    val fadeOutTimer = timers["pout_fade_out"]
                    fadeOutTimer.update(delta)
                    if (fadeOutTimer.isFinished()) {
                        kill()
                        GameLogger.debug(TAG, "Fade out timer finished, killing this sea lion :(")
                    }
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(1.5f * ConstVals.PPM, 0.9175f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBodyBounds() }

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle(body))
        body.addFixture(bodyFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle(body))
        body.addFixture(damagerFixture)

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle(body))
        body.addFixture(damageableFixture)

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 1))
        sprite.setSize(2f * ConstVals.PPM, 1.25f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setPosition(body.getBottomCenterPoint(), Position.BOTTOM_CENTER)
            _sprite.hidden = damageBlink
            if (fadingOut) {
                val fadeOutTimer = timers["pout_fade_out"]
                _sprite.setAlpha(1f - fadeOutTimer.getRatio())
            } else _sprite.setAlpha(1f)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = {
            if (state == SealionState.THROW) {
                if (ballInHands) "before_throw_ball" else "after_throw_ball"
            } else when (state) {
                SealionState.WAIT -> "wait_${if (ballInHands) "with_ball" else "no_ball"}"
                SealionState.TAUNT -> "taunt_${if (ballInHands) "with_ball" else "no_ball"}"
                else -> "pout"
            }
        }
        val animations = objectMapOf<String, IAnimation>(
            "wait_with_ball" to Animation(regions["wait_with_ball"], 2, 1, 0.1f, true),
            "wait_no_ball" to Animation(regions["wait_no_ball"], 2, 1, 0.1f, true),
            "taunt_with_ball" to Animation(regions["taunt_with_ball"], 2, 1, 0.1f, true),
            "taunt_no_ball" to Animation(regions["taunt_no_ball"], 2, 2, 0.1f, true),
            "before_throw_ball" to Animation(regions["before_throw_ball"], 2, 1, 0.1f, true),
            "after_throw_ball" to Animation(regions["after_throw_ball"], 2, 1, 0.1f, true),
            "pout" to Animation(regions["pout"], 2, 2, 0.2f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}