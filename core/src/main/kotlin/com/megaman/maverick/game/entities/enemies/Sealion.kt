package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IDrawableShapesEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.projectiles.SealionBall
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*

class Sealion(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.MEDIUM), IAnimatedEntity,
    IDrawableShapesEntity {

    companion object {
        const val TAG = "Sealion"
        private const val WAIT_DUR = 0.25f
        private const val TAUNT_DUR = 1f
        private const val BEFORE_THROW_BALL_DELAY = 0.25f
        private const val POUT_SINK_DELAY = 1f
        private const val POUT_FADE_OUT_DUR = 2.5f
        private const val SINK_VELOCITY_Y = -0.75f
        private const val BALL_CATCH_BOUNDS_OFFSET_X = -0.25f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class SealionState { WAIT, THROW, TAUNT, POUT }

    private val timers = objectMapOf(
        "wait" pairTo Timer(WAIT_DUR),
        "taunt" pairTo Timer(TAUNT_DUR),
        "before_throw_ball_delay" pairTo Timer(BEFORE_THROW_BALL_DELAY),
        "pout_sink_delay" pairTo Timer(POUT_SINK_DELAY),
        "pout_fade_out" pairTo Timer(POUT_FADE_OUT_DUR)
    )
    private val ballCatchBounds = GameRectangle().setSize(0.5f * ConstVals.PPM)
    private lateinit var sealionState: SealionState
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

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)

        ballCatchBounds.setCenter(body.getCenter().add(BALL_CATCH_BOUNDS_OFFSET_X * ConstVals.PPM, 0f))
        sealionBall = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.SEALION_BALL)!! as SealionBall
        sealionBall!!.spawn(
            props(ConstKeys.OWNER pairTo this, ConstKeys.POSITION pairTo ballCatchBounds.getCenter())
        )

        fadingOut = false
        sealionState = SealionState.WAIT

        timers.values().forEach { it.reset() }
    }

    override fun onDestroy() {
        super.onDestroy()
        sealionBall?.explodeAndDie()
        sealionBall = null
    }

    override fun canDamage(damageable: IDamageable) = sealionState != SealionState.POUT

    fun onBallDamagedInflicted() {
        GameLogger.debug(TAG, "OnBallDamagedInflicted()")
        sealionState = SealionState.TAUNT
    }

    fun onBallDestroyed() {
        GameLogger.debug(TAG, "onBallDestroyed()")
        sealionBall = null
        sealionState = SealionState.POUT
    }

    private fun throwBall() {
        GameLogger.debug(TAG, "throwBall()")
        sealionBall!!.body.setBottomCenterToPoint(ballCatchBounds.getPositionPoint(Position.TOP_CENTER))
        sealionBall!!.throwBall()
        ballInHands = false
    }

    private fun catchBall() {
        GameLogger.debug(TAG, "catchBall()")
        sealionBall!!.body.setBottomCenterToPoint(ballCatchBounds.getPositionPoint(Position.TOP_CENTER))
        sealionBall!!.catchBall()
        ballInHands = true
    }

    private fun canCatchBall() =
        sealionBall!!.body.getBounds().contains(ballCatchBounds.getPositionPoint(Position.BOTTOM_CENTER)) ||
            sealionBall!!.body.getMaxY() < ballCatchBounds.getY() // in case ball falls below sealion before it's caught

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            when (sealionState) {
                SealionState.WAIT -> {
                    val timer = timers["wait"]
                    timer.update(delta)
                    if (timer.isFinished()) {
                        timer.reset()
                        sealionState = SealionState.THROW
                        GameLogger.debug(TAG, "update(): wait timer finished, set state pairTo THROW")
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
                        sealionState = SealionState.WAIT
                        GameLogger.debug(TAG, "update(): catch ball, set state pairTo WAIT")
                    }
                }

                SealionState.TAUNT -> {
                    val timer = timers["taunt"]
                    timer.update(delta)
                    if (!ballInHands && canCatchBall()) catchBall()
                    if (timer.isFinished()) {
                        timer.reset()
                        sealionState = if (ballInHands) SealionState.WAIT else SealionState.THROW
                        GameLogger.debug(TAG, "update(); taunt finished, setting state pairTo $sealionState")
                    }
                }

                SealionState.POUT -> {
                    val delayTimer = timers["pout_sink_delay"]
                    delayTimer.update(delta)
                    if (!delayTimer.isFinished()) return@add
                    if (delayTimer.isJustFinished()) {
                        body.physics.velocity.y = SINK_VELOCITY_Y * ConstVals.PPM
                        fadingOut = true
                        GameLogger.debug(TAG, "update(): delay timer finished, start fading out")
                    }

                    val fadeOutTimer = timers["pout_fade_out"]
                    fadeOutTimer.update(delta)
                    if (fadeOutTimer.isFinished()) {
                        destroy()
                        GameLogger.debug(TAG, "update(): fade out timer finished, killing this sea lion :(")
                    }
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(2f * ConstVals.PPM, 1.5f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }
        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE)
        )
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 1))
        sprite.setSize(3f * ConstVals.PPM, 1.875f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.setPosition(body.getPositionPoint(Position.BOTTOM_CENTER), Position.BOTTOM_CENTER)
            sprite.hidden = damageBlink
            when {
                fadingOut -> {
                    val fadeOutTimer = timers["pout_fade_out"]
                    sprite.setAlpha(1f - fadeOutTimer.getRatio())
                }

                else -> sprite.setAlpha(1f)
            }
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = {
            when (sealionState) {
                SealionState.THROW -> if (ballInHands) "before_throw_ball" else "after_throw_ball"
                else -> when (sealionState) {
                    SealionState.WAIT -> "wait_${if (ballInHands) "with_ball" else "no_ball"}"
                    SealionState.TAUNT -> "taunt_${if (ballInHands) "with_ball" else "no_ball"}"
                    else -> "pout"
                }
            }
        }
        val animations = objectMapOf<String, IAnimation>(
            "wait_with_ball" pairTo Animation(regions["wait_with_ball"], 2, 1, 0.1f, true),
            "wait_no_ball" pairTo Animation(regions["wait_no_ball"], 2, 1, 0.1f, true),
            "taunt_with_ball" pairTo Animation(regions["taunt_with_ball"], 2, 1, 0.1f, true),
            "taunt_no_ball" pairTo Animation(regions["taunt_no_ball"], 2, 2, 0.05f, true),
            "before_throw_ball" pairTo Animation(regions["before_throw_ball"], 2, 1, 0.1f, true),
            "after_throw_ball" pairTo Animation(regions["after_throw_ball"], 2, 1, 0.1f, true),
            "pout" pairTo Animation(regions["pout"], 2, 2, 0.2f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}
