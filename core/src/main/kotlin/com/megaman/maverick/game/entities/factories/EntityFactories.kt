package com.megaman.maverick.game.entities.factories

import com.mega.game.engine.common.interfaces.IClearable
import com.mega.game.engine.common.interfaces.Initializable
import com.mega.game.engine.factories.Factories
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.factories.impl.*

object EntityFactories : Factories<Any?, MegaGameEntity>(), Initializable, IClearable {

    fun initialize(game: MegamanMaverickGame) {
        factories.put(EntityType.BLOCK, BlocksFactory(game))
        factories.put(EntityType.PROJECTILE, ProjectilesFactory(game))
        factories.put(EntityType.EXPLOSION, ExplosionsFactory(game))
        factories.put(EntityType.DECORATION, DecorationsFactory(game))
        factories.put(EntityType.ENEMY, EnemiesFactory(game))
        factories.put(EntityType.BOSS, BossesFactory(game))
        factories.put(EntityType.ITEM, ItemsFactory(game))
        factories.put(EntityType.SPECIAL, SpecialsFactory(game))
        factories.put(EntityType.HAZARD, HazardsFactory(game))
        factories.put(EntityType.SENSOR, SensorsFactory(game))
    }

    override fun init() = factories.forEach { (it.value as EntityFactory).init() }

    override fun clear() = factories.forEach { (it.value as EntityFactory).clear() }
}
