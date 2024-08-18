package com.megaman.maverick.game.entities

import com.engine.common.extensions.overlaps
import com.engine.common.objects.Properties
import com.engine.entities.GameEntity
import com.engine.entities.contracts.IBodyEntity
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame

abstract class MegaGameEntity(val game: MegamanMaverickGame) : GameEntity() {

    abstract fun getEntityType(): EntityType

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        MegaGameEntitiesMap.add(getEntityType(), this)
    }

    override fun onDestroy() {
        super.onDestroy()
        MegaGameEntitiesMap.remove(getEntityType(), this)
    }

    fun getGameCamera() = game.getGameCamera()

    fun getMegaman() = game.megaman

    fun playSoundNow(soundKey: Any, loop: Boolean) = game.audioMan.playSound(soundKey, loop)

    fun stopSoundNow(soundKey: Any) = game.audioMan.stopSound(soundKey)

    fun isLoggingLifecyle() =
        properties.getOrDefault("${ConstKeys.LOG}_${ConstKeys.LIFECYCLE}", false, Boolean::class)
}

fun IBodyEntity.overlapsGameCamera() = (this as MegaGameEntity).game.getGameCamera().overlaps(body)