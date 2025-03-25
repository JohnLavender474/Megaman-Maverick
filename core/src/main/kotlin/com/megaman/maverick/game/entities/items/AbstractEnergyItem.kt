package com.megaman.maverick.game.entities.items

import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.time.TimeMarkedRunnable
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.utils.misc.DirectionPositionMapper
import com.megaman.maverick.game.world.body.getPositionPoint

abstract class AbstractEnergyItem(game: MegamanMaverickGame) : AbstractItem(game), ISpritesEntity {

    companion object {
        const val TAG = "AbstractItem"

        const val SMALL_AMOUNT = 4
        const val LARGE_AMOUNT = 8

        private const val SMALL_WIDTH = 0.5f
        private const val SMALL_HEIGHT = 0.5f
        private const val LARGE_WIDTH = 1f
        private const val LARGE_HEIGHT = 0.75f

        private const val TIME_TO_BLINK = 2f
        private const val BLINK_DUR = 0.01f
        private const val CULL_DUR = 3.5f
    }

    protected var large = false

    private val blinkTimer = Timer(BLINK_DUR)
    private val cullTimer = Timer(CULL_DUR)

    private var blink = false
    private var warning = false
    private var timeCull = false

    override fun init() {
        GameLogger.debug(TAG, "init()")
        super.init()
        addComponent(defineSpritesCompoent())
        cullTimer.addRunnables(TimeMarkedRunnable(TIME_TO_BLINK) { warning = true })
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")

        large = spawnProps.getOrDefault(ConstKeys.LARGE, true, Boolean::class)

        val width: Float
        val height: Float
        if (large) {
            width = LARGE_WIDTH
            height = LARGE_HEIGHT
        } else {
            width = SMALL_WIDTH
            height = SMALL_HEIGHT
        }
        body.setSize(width * ConstVals.PPM, height * ConstVals.PPM)

        super.onSpawn(spawnProps)

        timeCull = spawnProps.getOrDefault(ConstKeys.TIMED, true, Boolean::class)

        warning = false
        blink = false

        blinkTimer.setToEnd()
        cullTimer.reset()
    }

    private fun defineSpritesCompoent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 1))
        sprite.setSize(2f * ConstVals.PPM)
        val component = SpritesComponent(sprite)
        component.putUpdateFunction { _, _ ->
            sprite.setOriginCenter()
            sprite.rotation = direction.rotation

            val position = DirectionPositionMapper.getInvertedPosition(direction)
            sprite.setPosition(body.getPositionPoint(position), position)

            sprite.hidden = blink
        }
        return component
    }

    override fun defineUpdatablesComponent(component: UpdatablesComponent) {
        super.defineUpdatablesComponent(component)
        component.add { delta ->
            if (timeCull) {
                if (warning) {
                    blinkTimer.update(delta)
                    if (blinkTimer.isFinished()) {
                        blinkTimer.reset()
                        blink = !blink
                    }
                }

                cullTimer.update(delta)
                if (cullTimer.isFinished()) destroy()
            }
        }
    }
}
