package com.megaman.maverick.game.entities.blocks

import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.contracts.IFreezableEntity
import com.megaman.maverick.game.entities.contracts.MegaGameEntity

class FrozenEntityBlock(game: MegamanMaverickGame) : IceBlock(game) {

    companion object {
        const val TAG = "FrozenEntityBlock"
    }

    private var entity: IFreezableEntity? = null

    override fun init() {
        GameLogger.debug(TAG, "init()")
        super.init()
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        spawnProps.put(ConstKeys.CULL_OUT_OF_BOUNDS, false)
        spawnProps.put(ConstKeys.BLOCK_FILTERS, { dynamic: MegaGameEntity, _: MegaGameEntity -> filter(dynamic) })
        super.onSpawn(spawnProps)
        entity = spawnProps.get(ConstKeys.ENTITY, IFreezableEntity::class)!!
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        entity = null
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({
        if (entity == null || (entity as MegaGameEntity).dead) destroy()
    })

    private fun filter(dynamic: MegaGameEntity) = dynamic == entity
}
