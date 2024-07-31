package com.megaman.maverick.game.entities.blocks

import com.engine.common.objects.Properties
import com.megaman.maverick.game.MegamanMaverickGame

class BreakableBlock(game: MegamanMaverickGame): Block(game) {

    companion object {
        const val TAG = "BreakableBlock"
    }

    override fun init() {
        super<Block>.init()
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
    }

    // TODO: make breakable
}