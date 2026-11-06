package com.megaman.maverick.game.screens.levels.events

import com.maverick.game.engine.common.objects.Properties
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractBoss

class BossSpawnEventHandler(private val game: MegamanMaverickGame) {

    companion object {
        const val TAG = "BossSpawnEventHandler"
    }

    fun init(bossName: String, bossSpawnProps: Properties): AbstractBoss {
        val boss = MegaEntityFactory.fetch(EntityType.BOSS.getFullyQualifiedName(bossName), AbstractBoss::class)!!

        boss.spawn(bossSpawnProps)

        return boss
    }
}
