package com.megaman.maverick.game.entities.factories

import com.maverick.game.engine.common.interfaces.IClearable
import com.maverick.game.engine.common.interfaces.Initializable
import com.maverick.game.engine.factories.Factories
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.factories.impl.*

// TODO: This is deprecated by the MegaEntityFactory object.
object EntityFactories : Factories<Any?, MegaGameEntity>(), Initializable, IClearable {

    fun initialize(services: MegaServices) {
        factories.put(EntityType.BLOCK, BlocksFactory(services))
        factories.put(EntityType.PROJECTILE, ProjectilesFactory(services))
        factories.put(EntityType.EXPLOSION, ExplosionsFactory(services))
        factories.put(EntityType.DECORATION, DecorationsFactory(services))
        factories.put(EntityType.ENEMY, EnemiesFactory(services))
        factories.put(EntityType.BOSS, BossesFactory(services))
        factories.put(EntityType.SPECIAL, SpecialsFactory(services))
        factories.put(EntityType.HAZARD, HazardsFactory(services))
        factories.put(EntityType.SENSOR, SensorsFactory(services))
    }

    override fun init(vararg params: Any) = factories.forEach { (it.value as EntityFactory).init() }

    override fun clear() = factories.forEach { (it.value as EntityFactory).clear() }
}
