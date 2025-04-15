package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.world.body.BodyComponent
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.contracts.AbstractBoss

/**
 * State machine:
 * - stand
 * --
 */
class ReactorManV2(game: MegamanMaverickGame): AbstractBoss(game) {

    companion object {
        const val TAG = "ReactorMan"
        private val regions = ObjectMap<String, TextureRegion>()
    }

    override fun init() {
        super.init()
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun isReady(delta: Float): Boolean {
        TODO("Not yet implemented")
    }

    override fun defineBodyComponent(): BodyComponent {
        TODO("Not yet implemented")
    }

    override fun defineSpritesComponent(): SpritesComponent {
        TODO("Not yet implemented")
    }
}
