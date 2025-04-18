package com.megaman.maverick.game.screens.levels.events

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.interfaces.Initializable
import com.mega.game.engine.common.interfaces.Resettable
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.events.Event
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.components.MEGAMAN_SPRITE_SIZE
import com.megaman.maverick.game.entities.megaman.components.getSpritePriority
import com.megaman.maverick.game.entities.megaman.components.shouldFlipSpriteX
import com.megaman.maverick.game.entities.megaman.components.shouldFlipSpriteY
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.misc.DirectionPositionMapper
import com.megaman.maverick.game.world.body.getPositionPoint

class EndLevelEventHandler(private val game: MegamanMaverickGame) : Initializable, Updatable, Resettable {

    companion object {
        const val TAG = "PlayerSpawnEventHandler"
        private const val START_DELAY_DUR = 3f
        private const val PRE_BEAM_DUR = 7f
        private const val BEAM_UP_DUR = 0.5f
        private const val BEAM_TRANS_DUR = 0.2f
        private const val BEAM_END_DUR = 0.5f
    }

    val finished: Boolean
        get() = startDelayTimer.isFinished() &&
            preBeamTimer.isFinished() &&
            beamUpTimer.isFinished() &&
            beamTransitionTimer.isFinished() &&
            beamEndTimer.isFinished()

    private val megaman = game.megaman

    private val startDelayTimer = Timer(START_DELAY_DUR).setToEnd()
    private val preBeamTimer = Timer(PRE_BEAM_DUR).setToEnd()
    private val beamUpTimer = Timer(BEAM_UP_DUR).setToEnd()
    private val beamTransitionTimer = Timer(BEAM_TRANS_DUR).setToEnd()
    private val beamEndTimer = Timer(BEAM_END_DUR).setToEnd()

    private lateinit var beamRegion: TextureRegion
    private lateinit var beamSprite: GameSprite
    private lateinit var beamTransAnim: Animation

    private val beamCenter = Vector2()

    private var initialized = false

    override fun init() {
        GameLogger.debug(PlayerSpawnEventHandler.TAG, "init()")

        if (!initialized) {
            initialized = true

            val atlas = game.assMan.getTextureAtlas(TextureAsset.MEGAMAN_BUSTER.source)

            beamRegion = atlas.findRegion(ConstKeys.BEAM)

            val priority = DrawingPriority()
            beamSprite = GameSprite(beamRegion, megaman.getSpritePriority(priority))
            beamSprite.setSize(MEGAMAN_SPRITE_SIZE * ConstVals.PPM)

            beamTransAnim = Animation(atlas.findRegion(ConstKeys.SPAWN), 1, 7, 0.05f, false).reversed()
        }

        startDelayTimer.reset()

        beamTransAnim.reset()
        beamSprite.hidden = true
        beamSprite.setPosition(-100f * ConstVals.PPM, -100f * -ConstVals.PPM)

        megaman.canBeDamaged = false
        megaman.setAllBehaviorsAllowed(false)

        game.eventsMan.submitEvent(Event(EventType.TURN_CONTROLLER_OFF))

        game.putProperty("${Megaman.TAG}_${ConstKeys.BEAM}", true)

        game.audioMan.stopMusic()
    }

    override fun update(delta: Float) {
        beamCenter.set(beamSprite.boundingRectangle.getCenter())
        game.putProperty("${Megaman.TAG}_${ConstKeys.BEAM}_${ConstKeys.CENTER}", beamCenter)

        game.addDrawable(beamSprite)

        when {
            !startDelayTimer.isFinished() -> {
                startDelayTimer.update(delta)
                if (startDelayTimer.isJustFinished()) {
                    GameLogger.debug(TAG, "start delay timer just finished")
                    preBeamTimer.reset()
                    game.audioMan.playSound(SoundAsset.MM2_VICTORY_SOUND, false)
                }
            }

            !preBeamTimer.isFinished() -> preBeam(delta)
            !beamTransitionTimer.isFinished() -> {
                beamSprite.hidden = false
                beamTrans(delta)
            }

            !beamUpTimer.isFinished() -> {
                beamSprite.setRegion(beamRegion)
                beamUp(delta)
            }

            !beamEndTimer.isFinished() -> beamEnd(delta)
        }
    }

    override fun reset() {
        startDelayTimer.setToEnd()
        preBeamTimer.setToEnd()
        beamUpTimer.setToEnd()
        beamTransitionTimer.setToEnd()
        beamEndTimer.setToEnd()
    }

    private fun preBeam(delta: Float) {
        preBeamTimer.update(delta)
        if (preBeamTimer.isFinished()) {
            GameLogger.debug(TAG, "pre-beam timer just finished")

            megaman.ready = false

            beamSprite.setOriginCenter()
            beamSprite.rotation = megaman.direction.rotation
            val position = DirectionPositionMapper.getInvertedPosition(megaman.direction)
            beamSprite.setPosition(megaman.body.getPositionPoint(position), position)
            beamSprite.setFlip(megaman.shouldFlipSpriteX(), megaman.shouldFlipSpriteY())

            beamTransitionTimer.reset()

            game.audioMan.playSound(SoundAsset.BEAM_SOUND, false)
        }
    }

    private fun beamTrans(delta: Float) {
        beamTransitionTimer.update(delta)
        beamTransAnim.update(delta)
        beamSprite.setRegion(beamTransAnim.getCurrentRegion())
        beamSprite.setFlip(megaman.isFacing(Facing.LEFT), false)

        if (beamTransitionTimer.isFinished()) {
            GameLogger.debug(TAG, "beam transition timer just finished")
            beamUpTimer.reset()
        }
    }

    private fun beamUp(delta: Float) {
        beamUpTimer.update(delta)

        val position = DirectionPositionMapper.getInvertedPosition(megaman.direction)
        beamSprite.setPosition(megaman.body.getPositionPoint(position), position)
        beamSprite.setFlip(megaman.isFacing(Facing.LEFT), false)

        when (megaman.direction) {
            Direction.UP -> beamSprite.y += ConstVals.VIEW_HEIGHT * ConstVals.PPM * beamUpTimer.getRatio()
            Direction.DOWN -> beamSprite.y -= ConstVals.VIEW_HEIGHT * ConstVals.PPM * beamUpTimer.getRatio()
            Direction.LEFT -> beamSprite.x -= ConstVals.VIEW_HEIGHT * ConstVals.PPM * beamUpTimer.getRatio()
            Direction.RIGHT -> beamSprite.x += ConstVals.VIEW_HEIGHT * ConstVals.PPM * beamUpTimer.getRatio()
        }

        if (beamUpTimer.isFinished()) {
            GameLogger.debug(TAG, "beam up timer just finished")
            beamSprite.hidden = true
            beamEndTimer.reset()
        }
    }

    private fun beamEnd(delta: Float) {
        beamEndTimer.update(delta)

        if (beamEndTimer.isFinished()) {
            GameLogger.debug(TAG, "beam end timer just finished")

            val level = game.getCurrentLevel()
            game.eventsMan.submitEvent(Event(EventType.END_LEVEL, props(ConstKeys.LEVEL pairTo level)))

            game.putProperty("${Megaman.TAG}_${ConstKeys.BEAM}", true)
        }
    }
}
