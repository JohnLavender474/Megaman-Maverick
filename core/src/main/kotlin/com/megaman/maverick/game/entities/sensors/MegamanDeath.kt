package com.megaman.maverick.game.entities.sensors

import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.world.body.getBounds

// Unlike `Death`, the `MegamanDeath` entity is NOT added to the world system and applies ONLY to Mega Man.
// Use this entity if it is guaranteed that ONLY Mega Man will ever make contact with this entity.
open class MegamanDeath(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, Updatable {

    companion object {
        const val TAG = "MegamanDeath"
    }

    private val bounds = GameRectangle()
    private var instant = false

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        addComponent(UpdatablesComponent({ delta -> update(delta) }))
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        this.bounds.set(bounds)

        instant = spawnProps.getOrDefault(ConstKeys.INSTANT, false, Boolean::class)
    }

    override fun update(delta: Float) {
        if (megaman.dead) return
        if (!bounds.overlaps(megaman.body.getBounds())) return
        if (!instant && megaman.invincible) return
        megaman.depleteHealth()
    }

    override fun getType() = EntityType.SENSOR

    override fun getTag() = TAG
}
