package com.megaman.maverick.game.entities.mocks

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.IAnimator
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.orderedMapOf
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.drawables.sprites.GameSprite
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.bosses.TimberWoman
import com.megaman.maverick.game.utils.AnimationUtils

class MockTimberWoman(private val game: MegamanMaverickGame): MockRobotMaster {

    companion object {
        const val TAG = "MockTimberWoman"

        private val animDefs = orderedMapOf(
            "init" pairTo AnimationDef(7, 1, 0.1f, true),
            "jump_spin" pairTo AnimationDef(4, 2, 0.025f, true),
            "stand_swing" pairTo AnimationDef(
                2, 4, gdxArrayOf(0.1f, 0.1f, 0.1f, 0.1f, 0.1f, 0.25f, 0.1f, 0.1f), false
            )
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private val sprite = GameSprite()
    private lateinit var animator: IAnimator

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES_2.source)
            AnimationUtils.loadRegions(TimberWoman.TAG, atlas, animDefs.keys(), regions)
        }
    }

    override fun update(delta: Float) {
        TODO("Not yet implemented")
    }

    override fun draw(drawer: Batch) {
        TODO("Not yet implemented")
    }
}
