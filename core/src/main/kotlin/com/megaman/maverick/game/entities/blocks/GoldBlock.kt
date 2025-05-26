package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.contracts.ILightSource
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.misc.LightSourceUtils

class GoldBlock(game: MegamanMaverickGame) : AnimatedBlock(game), ILightSource {

    companion object {
        const val TAG = "GoldBlock"
        private const val LIGHT_RADIUS = 5
        private const val LIGHT_RADIANCE = 1.025f
    }

    override val lightSourceKeys = ObjectSet<Int>()
    override val lightSourceCenter: Vector2
        get() = body.getCenter()
    override var lightSourceRadius = LIGHT_RADIUS
    override var lightSourceRadiance = LIGHT_RADIANCE

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.ANIMATION, TAG)
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)
        LightSourceUtils.loadLightSourceKeysFromProps(this, spawnProps)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        lightSourceKeys.clear()
    }

    override fun defineUpdateablesComponent(updateablesComponent: UpdatablesComponent) {
        super.defineUpdateablesComponent(updateablesComponent)
        updateablesComponent.add { delta -> LightSourceUtils.sendLightSourceEvent(game, this) }
    }
}
