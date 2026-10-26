package com.megaman.maverick.game.locks

import com.maverick.game.engine.behaviors.BehaviorsComponent
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.locks.LockGroup

fun behaviorsLockHook(entity: IGameEntity): (LockGroup, Boolean) -> Unit = { group, locked ->
    if (group == MegaLockGroup.BEHAVIORS) entity.getComponent(BehaviorsComponent::class)?.setAllBehaviorsAllowed(!locked)
}
