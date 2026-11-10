package com.megaman.maverick.game.entities.hazards

import com.maverick.game.engine.damage.IDamagerEntity
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.contracts.MegaGameEntity

class ViewEdgeHazard(services: MegaServices) : MegaGameEntity(services), IHazard, IDamagerEntity {

    companion object {
        const val TAG = "ViewEdgeHazard"
    }

    var damage = 0

    override fun getDamageToMegaman() = damage

    override fun onBossDefeated(boss: AbstractBoss) {}

    override fun getType() = EntityType.HAZARD
}
