package com.megaman.maverick.game.screens.levels.stats

import com.badlogic.gdx.graphics.g2d.Batch
import com.engine.common.interfaces.Updatable
import com.engine.drawables.IDrawable
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.drawables.sprites.BitsBar
import com.megaman.maverick.game.entities.contracts.IHealthEntity

class EntityStatsHandler(private val game: MegamanMaverickGame): IDrawable<Batch> {

    companion object {
        private const val BAR_X: Float = (ConstVals.VIEW_WIDTH - 0.75f) * ConstVals.PPM
        private const val BAR_Y: Float = 7f * ConstVals.PPM
    }

    private var entity: IHealthEntity? = null
    private var bar: BitsBar? = null

    override fun draw(drawer: Batch) {
        bar?.draw(drawer)
    }

    fun set(entity: IHealthEntity) {
        bar = BitsBar(game.assMan,
            "Bit",
            BAR_X,
            BAR_Y,
            { entity.getHealthPoints().current },
            { entity.getHealthPoints().max })
        bar!!.init()
    }

    fun unset() {
        entity = null
        bar = null
    }

}