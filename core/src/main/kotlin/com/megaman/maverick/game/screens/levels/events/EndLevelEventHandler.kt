package com.megaman.maverick.game.screens.levels.events

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.animations.Animation
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.interfaces.Initializable
import com.maverick.game.engine.common.interfaces.Resettable
import com.maverick.game.engine.common.interfaces.Updatable
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.setPosition
import com.maverick.game.engine.drawables.sprites.setSize
import com.maverick.game.engine.events.Event
import com.maverick.game.engine.locks.lock
import com.maverick.game.engine.players.PlayerSlot
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.components.MEGAMAN_SPRITE_SIZE
import com.megaman.maverick.game.entities.megaman.components.getSpritePriority
import com.megaman.maverick.game.entities.megaman.components.shouldFlipSpriteX
import com.megaman.maverick.game.entities.megaman.components.shouldFlipSpriteY
import com.megaman.maverick.game.entities.megaman.constants.MegamanWeapon
import com.megaman.maverick.game.entities.megaman.sprites.MegamanAnimations
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.locks.MegaLockGroup
import com.megaman.maverick.game.locks.MegaLockReason
import com.megaman.maverick.game.players.MegaPlayer
import com.megaman.maverick.game.players.body
import com.megaman.maverick.game.players.direction
import com.megaman.maverick.game.players.facing
import com.megaman.maverick.game.players.megaman
import com.megaman.maverick.game.players.ready
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.misc.DirectionPositionMapper
import com.megaman.maverick.game.world.body.getPositionPoint

class EndLevelEventHandler(private val game: MegamanMaverickGame) : Initializable, Updatable, Resettable {

    companion object {
        const val TAG = "PlayerSpawnEventHandler"
        private const val START_DELAY_DUR = 2f
        private const val PRE_BEAM_DUR = 6f
        private const val BEAM_UP_DUR = 0.5f
        private const val BEAM_TRANS_DUR = 0.2f
        private const val BEAM_END_DUR = 0.5f

        private val BEAM_REGIONS = ObjectMap<String, TextureRegion>()
        private val BEAM_TRANS_ANIMS = ObjectMap<String, Animation>()
    }

    val finished: Boolean
        get() = startDelayTimer.isFinished() &&
            preBeamTimer.isFinished() &&
            beamUpTimer.isFinished() &&
            beamTransitionTimer.isFinished() &&
            beamEndTimer.isFinished()

    // TODO: the beam-out acts on the lead player in slot 0; with more players, each player needs its own (item 047)
    private val player: MegaPlayer
        get() = game.players.get(PlayerSlot(0))!!

    private val startDelayTimer = Timer(START_DELAY_DUR).setToEnd()
    private val preBeamTimer = Timer(PRE_BEAM_DUR).setToEnd()
    private val beamUpTimer = Timer(BEAM_UP_DUR).setToEnd()
    private val beamTransitionTimer = Timer(BEAM_TRANS_DUR).setToEnd()
    private val beamEndTimer = Timer(BEAM_END_DUR).setToEnd()

    private lateinit var beamSprite: GameSprite
    private lateinit var beamTransAnim: Animation
    private lateinit var beamRegion: TextureRegion

    private val beamCenter = Vector2()

    private var initialized = false

    override fun init(vararg params: Any) {
        GameLogger.debug(PlayerSpawnEventHandler.TAG, "init()")
        if (!initialized) {
            MegamanWeapon.entries.forEach { weapon ->
                val atlas = game.assMan.getTextureAtlas(MegamanAnimations.getAssetSourceForWeapon(weapon))
                val key = weapon.name
                atlas.findRegion(ConstKeys.BEAM)?.let { BEAM_REGIONS.put(key, it) }
                atlas.findRegion(ConstKeys.SPAWN)?.let {
                    BEAM_TRANS_ANIMS.put(key, Animation(it, 1, 7, 0.05f, false).reversed())
                }
            }

            beamRegion = BEAM_REGIONS.get(MegamanWeapon.MEGA_BUSTER.name)
            beamTransAnim = BEAM_TRANS_ANIMS.get(MegamanWeapon.MEGA_BUSTER.name)

            val priority = DrawingPriority()
            beamSprite = GameSprite(beamRegion, player.megaman.getSpritePriority(priority))
            beamSprite.setSize(MEGAMAN_SPRITE_SIZE * ConstVals.PPM)

            initialized = true
        }

        startDelayTimer.reset()

        beamTransAnim.reset()
        beamSprite.hidden = true
        beamSprite.setPosition(-100f * ConstVals.PPM, -100f * -ConstVals.PPM)

        player.entity.lock(MegaLockGroup.DAMAGE, MegaLockReason.LEVEL_END)
        player.entity.lock(MegaLockGroup.BEHAVIORS, MegaLockReason.LEVEL_END)

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
                    game.audioMan.playMusic(MusicAsset.VINNYZ_VICTORY_MUSIC, false)
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

            player.ready = false

            val weaponKey = player.megaman.currentWeapon.name
            beamRegion = BEAM_REGIONS.get(weaponKey) ?: BEAM_REGIONS.get(MegamanWeapon.MEGA_BUSTER.name)
            beamTransAnim = (BEAM_TRANS_ANIMS.get(weaponKey) ?: BEAM_TRANS_ANIMS.get(MegamanWeapon.MEGA_BUSTER.name))
                .also { it.reset() }

            beamSprite.setOriginCenter()
            beamSprite.rotation = player.direction.rotation
            val position = DirectionPositionMapper.getInvertedPosition(player.direction)
            beamSprite.setPosition(player.body.getPositionPoint(position), position)
            beamSprite.setFlip(player.megaman.shouldFlipSpriteX(), player.megaman.shouldFlipSpriteY())

            beamTransitionTimer.reset()

            game.audioMan.playSound(SoundAsset.BEAM_SOUND, false)
        }
    }

    private fun beamTrans(delta: Float) {
        beamTransitionTimer.update(delta)
        beamTransAnim.update(delta)
        beamSprite.setRegion(beamTransAnim.getCurrentRegion())
        beamSprite.setFlip(player.facing == Facing.LEFT, false)

        if (beamTransitionTimer.isFinished()) {
            GameLogger.debug(TAG, "beam transition timer just finished")
            beamUpTimer.reset()
        }
    }

    private fun beamUp(delta: Float) {
        beamUpTimer.update(delta)

        val position = DirectionPositionMapper.getInvertedPosition(player.direction)
        beamSprite.setPosition(player.body.getPositionPoint(position), position)
        beamSprite.setFlip(player.facing == Facing.LEFT, false)

        when (player.direction) {
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
            game.audioMan.stopMusic()
            val level = game.getCurrentLevel()
            game.eventsMan.submitEvent(Event(EventType.END_LEVEL, props(ConstKeys.LEVEL pairTo level)))
            game.putProperty("${Megaman.TAG}_${ConstKeys.BEAM}", true)
        }
    }
}
