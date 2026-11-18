package com.megaman.maverick.game.entities.blocks

import com.maverick.game.engine.common.objects.Properties
import com.megaman.maverick.game.MegaServices

class GateBlock(services: MegaServices) : Block(services) {

    companion object {
        const val TAG = "GateBlock"
    }

    // TODO: a GateBlock spawns only when the session rules spawn gate blocks (co-op); a single-player use widens this (item 170)
    override fun canSpawn(spawnProps: Properties) = services.rules.spawnGateBlocks && super.canSpawn(spawnProps)
}
