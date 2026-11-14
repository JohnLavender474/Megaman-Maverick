package com.megaman.maverick.game.screens.levels.stats

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.common.interfaces.Updatable
import com.maverick.game.engine.common.time.TimeMarkedRunnable
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.drawables.IDrawable
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.drawables.ui.BossBitsBar
import com.megaman.maverick.game.entities.contracts.AbstractBoss

class BossHealthHandler(private val game: MegamanMaverickGame) : IDrawable<Batch>, Updatable {

    companion object {
        fun phaseAtFillBit(bit: Int, max: Int, phases: Int) = phases - bit / max

        fun countAtFillBit(bit: Int, max: Int) = bit % max + 1
    }

    val finished: Boolean
        get() = timer == null || timer!!.isFinished()

    private val health: Int
        get() = if (entity != null && timer?.isFinished() != false) entity!!.getCurrentHealth() else temp

    private var entity: AbstractBoss? = null
    private var timer: Timer? = null
    private var bar: BossBitsBar? = null
    private var temp = 0
    private var fillPhase = 1

    override fun draw(drawer: Batch) {
        bar?.draw(drawer)
    }

    override fun update(delta: Float) {
        timer?.update(delta)
    }

    fun set(entity: AbstractBoss, runOnFirstUpdate: () -> Unit, runOnFinished: () -> Unit) {
        this.entity = entity
        temp = 0
        fillPhase = entity.healthPhases

        bar = BossBitsBar(
            game.assets,
            entity.healthBarRegions,
            (ConstVals.VIEW_WIDTH - (ConstVals.HEALTH_BAR_X + ConstVals.STAT_BIT_WIDTH)) * ConstVals.PPM,
            ConstVals.STATS_BAR_Y * ConstVals.PPM,
            { if (timer?.isFinished() != false) entity.healthPhase else fillPhase },
            { health },
            { entity.getMaxHealth() }
        )
        bar!!.init()

        val max = entity.getMaxHealth()
        val phases = entity.healthPhases
        val bits = max * phases
        val timer = Timer(ConstVals.DUR_PER_BIT * bits)

        val runnables = Array<TimeMarkedRunnable>()
        for (i in 0 until bits) {
            val timeToRun = i * ConstVals.DUR_PER_BIT
            runnables.add(TimeMarkedRunnable(timeToRun) {
                fillPhase = phaseAtFillBit(i, max, phases)
                temp = countAtFillBit(i, max)
                game.audio.playSound(SoundAsset.ENERGY_FILL_SOUND)
            })
        }
        timer.addRunnables(runnables).setRunOnFirstupdate(runOnFirstUpdate).setRunOnJustFinished(runOnFinished)

        this.timer = timer
    }

    fun unset() {
        entity = null
        timer = null
        bar = null
    }
}
