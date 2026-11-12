package com.megaman.maverick.game.entities.bosses.d1000

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.UtilMethods
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.entities.body
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.decorations.D1000RubblePile
import com.megaman.maverick.game.entities.decorations.DebrisPiece
import com.megaman.maverick.game.entities.explosions.Explosion
import com.megaman.maverick.game.entities.explosions.SmokePuff
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter
import com.megaman.maverick.game.entities.bosses.D1000

class D1000Defeat(private val boss: D1000) {

    companion object {
        const val TAG = "D1000Defeat"
        const val DEFEAT_DUR = 7f
        const val DEFEAT_SHAKE_MIN = 1.5f
        const val DEFEAT_SHAKE_MAX = 4f
        const val BREAK_BURST_COUNT = 4
        const val BREAK_CHAIN_COUNT = 10
        const val BREAK_CHAIN_STEP = 0.1f
        const val BREAK_PUFF_EVERY = 2
        const val PILE_SHOW_AT = 0.7f
        const val PILE_HOLD = 1.5f
        const val DEBRIS_EXTRA_MIN = 4
        const val DEBRIS_EXTRA_MAX = 6
        const val DEBRIS_SPEED_MIN = 5f * ConstVals.PPM
        const val DEBRIS_SPEED_MAX = 12f * ConstVals.PPM
        private const val DEBRIS_ANGLE_MIN = 30f
        private const val DEBRIS_ANGLE_MAX = 150f
        private const val BREAK_SHAKE = 0.5f
        private const val PILE_SHAKE = 0.33f
        val DEBRIS = Array(8) { "D1000Debris/chunk${it + 1}" }
    }

    private enum class Step { SHAKE, BREAK, PILE, DONE }

    private var step = Step.SHAKE
    private var time = 0f
    private var breakTime = 0f
    private var chainSpawned = 0
    var pile: D1000RubblePile? = null
        private set

    val broken: Boolean
        get() = step != Step.SHAKE

    fun reset() {
        step = Step.SHAKE
        time = 0f
        breakTime = 0f
        chainSpawned = 0
        pile = null
    }

    fun begin() {
        reset()
        boss.shake(DEFEAT_SHAKE_MIN)
    }

    fun update(delta: Float, coreGone: Boolean): Boolean {
        time += delta
        when (step) {
            Step.SHAKE -> {
                boss.shakeOffset = if (flickerOn(time)) D1000.SHAKE_STEP else 0f
                if (time >= DEFEAT_SHAKE_MIN && (coreGone || time >= DEFEAT_SHAKE_MAX)) breakApart()
            }

            Step.BREAK -> {
                updateChain(delta)
                if (time >= PILE_SHOW_AT) showPile()
            }

            Step.PILE -> {
                updateChain(delta)
                if (time >= PILE_HOLD) {
                    goTo(Step.DONE)
                    return true
                }
            }

            Step.DONE -> Unit
        }
        return false
    }

    private fun goTo(next: Step) {
        step = next
        time = 0f
    }

    private fun randomIn(bounds: GameRectangle) = Vector2(
        UtilMethods.getRandom(bounds.getX(), bounds.getMaxX()),
        UtilMethods.getRandom(bounds.getY(), bounds.getMaxY())
    )

    private fun breakApart() {
        GameLogger.debug(TAG, "breakApart()")
        boss.shakeOffset = 0f
        breakTime = 0f
        chainSpawned = 0
        val bounds = boss.body.getBounds()
        explosionAt(boss.body.getCenter())
        repeat(BREAK_BURST_COUNT - 1) { explosionAt(randomIn(bounds)) }
        val count = DEBRIS.size + UtilMethods.getRandom(DEBRIS_EXTRA_MIN, DEBRIS_EXTRA_MAX)
        for (i in 0 until count) {
            val key = if (i < DEBRIS.size) DEBRIS[i] else DEBRIS[UtilMethods.getRandom(0, DEBRIS.size - 1)]
            val velocity = Vector2(UtilMethods.getRandom(DEBRIS_SPEED_MIN, DEBRIS_SPEED_MAX), 0f)
                .setAngleDeg(UtilMethods.getRandom(DEBRIS_ANGLE_MIN, DEBRIS_ANGLE_MAX))
            DebrisPiece.spawnPiece(key, randomIn(bounds), velocity)
        }
        boss.requestToPlaySound(SoundAsset.EXPLOSION_1_SOUND, false)
        boss.land(BREAK_SHAKE)
        goTo(Step.BREAK)
    }

    private fun updateChain(delta: Float) {
        breakTime += delta
        while (chainSpawned < BREAK_CHAIN_COUNT && breakTime >= (chainSpawned + 1) * BREAK_CHAIN_STEP) {
            chainSpawned++
            val point = randomIn(boss.body.getBounds())
            explosionAt(point)
            if (chainSpawned % BREAK_PUFF_EVERY == 0) {
                val puff = MegaEntityFactory.fetch(SmokePuff::class)!!
                puff.spawn(props(ConstKeys.POSITION pairTo point))
                boss.requestToPlaySound(SoundAsset.EXPLOSION_2_SOUND, false)
            }
        }
    }

    private fun showPile() {
        GameLogger.debug(TAG, "showPile()")
        val heap = MegaEntityFactory.fetch(D1000RubblePile::class)!!
        heap.spawn(props(ConstKeys.POSITION pairTo Vector2(boss.origin.x, boss.floor), ConstKeys.FACING pairTo boss.facing))
        pile = heap
        boss.land(PILE_SHAKE)
        goTo(Step.PILE)
    }

    fun pileCenter(): Vector2 {
        val heap = pile
        return if (heap == null) Vector2(boss.origin.x, boss.floor) else Vector2(boss.origin.x, boss.floor + heap.height() / 2f)
    }

    fun explosionAt(position: Vector2) {
        val explosion = MegaEntityFactory.fetch(Explosion::class)!!
        explosion.spawn(props(ConstKeys.OWNER pairTo boss, ConstKeys.POSITION pairTo position, ConstKeys.DAMAGER pairTo false))
    }

    fun explosionInBody() {
        explosionAt(randomIn(boss.body.getBounds()))
        boss.requestToPlaySound(SoundAsset.EXPLOSION_2_SOUND, false)
    }
}
