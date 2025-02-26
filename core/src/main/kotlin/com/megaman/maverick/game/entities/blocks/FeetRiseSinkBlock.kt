package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.OrderedSet
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.ProcessState
import com.mega.game.engine.common.extensions.isAny
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.world.body.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getEntity
import kotlin.math.abs

open class FeetRiseSinkBlock(game: MegamanMaverickGame) : Block(game) {

    companion object {
        const val TAG = "FeetRiseSinkBlock"
    }

    protected val feetSet = OrderedSet<IFixture>()

    protected var maxY = 0f
    protected var minY = 0f

    protected var risingSpeed = 0f
    protected var fallingSpeed = 0f

    override fun init() {
        super.init()

        body.preProcess.put(ConstKeys.MOVE) {
            val iter = feetSet.iterator()
            while (iter.hasNext) {
                val feet = iter.next()

                if (!feet.getShape().overlaps(body.getBounds())) {
                    iter.remove()

                    GameLogger.debug(
                        TAG,
                        "body.preProcess(): remove feet from body: " +
                            "feetSet.size=${feetSet.size}, " +
                            "entity=${feet.getEntity().getTag()}"
                    )
                }
            }

            when {
                shouldMoveDown() -> when {
                    body.getY() > minY -> body.physics.velocity.y = fallingSpeed * ConstVals.PPM
                    else -> body.physics.velocity.y = 0f
                }

                shouldMoveUp() -> body.physics.velocity.y = risingSpeed * ConstVals.PPM
                else -> body.physics.velocity.y = 0f
            }

            when {
                body.getMaxY() > maxY -> body.setMaxY(maxY)
                body.getY() < minY -> body.setY(minY)
            }
        }

        body.drawingColor = Color.BLUE
    }

    protected open fun shouldMoveDown() = !feetSet.isEmpty

    protected open fun shouldMoveUp() = feetSet.isEmpty && body.getMaxY() < maxY

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val max = abs(spawnProps.getOrDefault(ConstKeys.MAX, 0f, Float::class))
        maxY = body.getMaxY() + (max * ConstVals.PPM)

        val min = abs(spawnProps.getOrDefault(ConstKeys.MIN, 0f, Float::class))
        minY = body.getY() - (min * ConstVals.PPM)

        fallingSpeed = -abs(spawnProps.getOrDefault(ConstKeys.FALL, 0f, Float::class))
        risingSpeed = abs(spawnProps.getOrDefault(ConstKeys.RISE, 0f, Float::class))

        val feetSound = spawnProps.getOrDefault("${ConstKeys.FEET}_${ConstKeys.SOUND}", false, Boolean::class)
        putProperty("${ConstKeys.FEET}_${ConstKeys.SOUND}", feetSound)
    }

    override fun onDestroy() {
        super.onDestroy()

        feetSet.clear()
    }

    protected open fun shouldListenToFeet(feetFixture: IFixture): Boolean {
        val entity = feetFixture.getEntity()
        val shouldListen = entity.isAny(Megaman::class, PushableBlock::class)
        GameLogger.debug(TAG, "shouldListenToFeet(): shouldListen=$shouldListen, entity=${entity.getTag()}")
        return shouldListen
    }

    override fun hitByFeet(processState: ProcessState, feetFixture: IFixture) {
        when (processState) {
            ProcessState.BEGIN, ProcessState.CONTINUE -> if (shouldListenToFeet(feetFixture)) {
                feetSet.add(feetFixture)
                GameLogger.debug(TAG, "hitByFeet(): feetSet.size=${feetSet.size}")
            }

            ProcessState.END -> feetSet.remove(feetFixture)
        }
    }
}
