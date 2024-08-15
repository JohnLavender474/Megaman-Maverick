package com.megaman.maverick.game.screens.levels.stats

import com.badlogic.gdx.graphics.g2d.Batch
import com.engine.drawables.IDrawable
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.drawables.sprites.BitsBar
import com.megaman.maverick.game.entities.contracts.IHealthEntity

class EntityStatsHandler(private val game: MegamanMaverickGame) : IDrawable<Batch> {

    private var entity: IHealthEntity? = null
    private var bar: BitsBar? = null

    override fun draw(drawer: Batch) {
        bar?.draw(drawer)
    }

    fun set(entity: IHealthEntity) {
        bar = BitsBar(game.assMan,
            "Bit",
            (ConstVals.VIEW_WIDTH - (ConstVals.HEALTH_BAR_X + ConstVals.STAT_BIT_WIDTH)) * ConstVals.PPM,
            ConstVals.STATS_BAR_Y * ConstVals.PPM,
            { entity.getCurrentHealth() },
            { entity.getMaxHealth() })
        bar!!.init()
    }

    fun unset() {
        entity = null
        bar = null
    }

}