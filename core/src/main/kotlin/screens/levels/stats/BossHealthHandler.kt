package com.megaman.maverick.game.screens.levels.stats

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.time.TimeMarkedRunnable
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.IDrawable
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.drawables.ui.BitsBar
import com.megaman.maverick.game.entities.contracts.IHealthEntity

class BossHealthHandler(private val game: MegamanMaverickGame) : IDrawable<Batch>, Updatable {

    val finished: Boolean
        get() = timer == null || timer!!.isFinished()

    private val health: Int
        get() = if (entity != null && timer?.isFinished() != false) entity!!.getCurrentHealth() else temp

    private var entity: IHealthEntity? = null
    private var timer: Timer? = null
    private var bar: BitsBar? = null
    private var temp = 0

    override fun draw(drawer: Batch) {
        bar?.draw(drawer)
    }

    override fun update(delta: Float) {
        timer?.update(delta)
    }

    fun set(entity: IHealthEntity, runOnFirstUpdate: (() -> Unit)? = null, runOnFinished: (() -> Unit)? = null) {
        this.entity = entity
        temp = 0

        val bits = entity.getMaxHealth()
        val timer = Timer(ConstVals.DUR_PER_BIT * bits)

        val runnables = Array<TimeMarkedRunnable>()
        for (i in 0 until bits) {
            val timeToRun = i * ConstVals.DUR_PER_BIT
            runnables.add(TimeMarkedRunnable(timeToRun) {
                temp = i + 1
                game.audioMan.playSound(SoundAsset.ENERGY_FILL_SOUND)
            })
        }
        timer.setRunnables(runnables)
        timer.runOnFirstUpdate = runOnFirstUpdate
        timer.runOnFinished = runOnFinished

        this.timer = timer

        bar = BitsBar(
            game.assMan,
            "Bit",
            (ConstVals.VIEW_WIDTH - (ConstVals.HEALTH_BAR_X + ConstVals.STAT_BIT_WIDTH)) * ConstVals.PPM,
            ConstVals.STATS_BAR_Y * ConstVals.PPM,
            { health },
            { entity.getMaxHealth() })
        bar!!.init()
    }

    fun unset() {
        entity = null
        timer = null
        bar = null
    }
}
