package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.ObjectMap
import com.engine.animations.AnimationsComponent
import com.engine.common.enums.Facing
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.interfaces.IFaceable
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.damage.IDamager
import com.engine.drawables.sprites.SpritesComponent
import com.engine.entities.contracts.IAnimatedEntity
import com.engine.updatables.UpdatablesComponent
import com.engine.world.BodyComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.world.BodySense
import com.megaman.maverick.game.world.isSensing
import kotlin.reflect.KClass

class ReactMan(game: MegamanMaverickGame) : AbstractBoss(game), IAnimatedEntity, IFaceable {

    enum class ReactManState(val regionName: String) {
        DANCE("Dance"), JUMP("Jump"), RUN("Run"), STAND("Stand"), THROW("Throw")
    }

    companion object {
        const val TAG = "ReactMan"
        private const val STAND_DUR = 1.5f
        private const val DANCE_DUR = 0.5f
        private const val FIRST_JUMP_IMPULSE = 20f
        private const val SECOND_JUMP_IMPULSE = 12f
        private const val THIRD_JUMP_IMPULSE = 8f
        private const val THROW_DELAY = 1f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>()
    override lateinit var facing: Facing

    private val standTimer = Timer(STAND_DUR)
    private val danceTimer = Timer(DANCE_DUR)
    private val throwTimer = Timer(THROW_DELAY)
    private lateinit var state: ReactManState
    private var jumps = 0

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES.source)
            ReactManState.values().forEach {
                val region = atlas.findRegion("ReactMan/${it.regionName}")
                regions.put(it.regionName, region)
            }
        }
        super<AbstractBoss>.init()
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomCenterPoint()
        body.setBottomCenterToPoint(spawn)
        standTimer.reset()
        danceTimer.reset()
        throwTimer.reset()
        jumps = 0
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (!ready) return@add
            if (defeated) {
                explodeOnDefeat(delta)
                return@add
            }

            when (state) {
                ReactManState.DANCE -> {
                    danceTimer.update(delta)
                    if (danceTimer.isFinished()) {
                        danceTimer.reset()
                        state = ReactManState.STAND
                    }
                }

                ReactManState.STAND -> {
                    standTimer.update(delta)
                    if (standTimer.isFinished()) {
                        standTimer.reset()
                        state =
                            if ((megaman.body.getMaxX() >= body.getMaxX() && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT)) ||
                                (megaman.body.getX() <= body.getX() && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT))
                            ) ReactManState.JUMP else ReactManState.RUN
                    }
                }

                ReactManState.JUMP -> {
                    if (jumps == 0) {

                    }
                }
                ReactManState.RUN -> TODO()
                ReactManState.THROW -> TODO()
            }
        }
    }

    private fun jump(yImpulse: Float) {
        /*
         monkeyBall!!.body.physics.gravityOn = true

        val horizontalDistance = megaman.body.x - body.x
        val verticalDistance = megaman.body.y - body.y

        val adjustedImpulseX = horizontalDistance * 1.1f

        val baseImpulseY = BALL_IMPULSE_Y * ConstVals.PPM
        val adjustedImpulseY = baseImpulseY + (verticalDistance * 0.75f)

        val impulse = Vector2(adjustedImpulseX, adjustedImpulseY)

        monkeyBall!!.body.physics.velocity.set(impulse)
        monkeyBall!!.firstSprite!!.hidden = false
         */
    }

    override fun onReady() {
        super.onReady()
        state = ReactManState.DANCE
    }

    override fun defineBodyComponent(): BodyComponent {
        TODO("Not yet implemented")
    }

    override fun defineSpritesComponent(): SpritesComponent {
        TODO("Not yet implemented")
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        TODO()
    }
}