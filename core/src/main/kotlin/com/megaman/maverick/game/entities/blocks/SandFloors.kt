package com.megaman.maverick.game.entities.blocks

import com.maverick.game.engine.world.body.IFixture
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.world.body.getBody
import com.megaman.maverick.game.world.body.getFeetBlocks

class PrivateSandFloor(services: MegaServices) : FeetRiseSinkBlock(services) {

    override fun shouldListenToFeet(feetFixture: IFixture): Boolean {
        for (block in feetFixture.getBody().getFeetBlocks())
            if (block !is PrivateSandFloor && block.body.getMaxY() >= body.getMaxY()) return false
        return true
    }
}

class SharedSandFloor(services: MegaServices) : FeetRiseSinkBlock(services) {

    override fun shouldListenToFeet(feetFixture: IFixture) = false
}
