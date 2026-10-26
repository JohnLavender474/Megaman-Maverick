package com.maverick.game.engine.locks

import com.maverick.game.engine.components.require
import com.maverick.game.engine.entities.IGameEntity

fun IGameEntity.lock(group: LockGroup, reason: LockReason) = require(LocksComponent::class).add(group, reason)

fun IGameEntity.unlock(group: LockGroup, reason: LockReason) = require(LocksComponent::class).remove(group, reason)

fun IGameEntity.clearLocks(group: LockGroup) = require(LocksComponent::class).clear(group)

fun IGameEntity.isLocked(group: LockGroup) = getComponent(LocksComponent::class)?.isLocked(group) == true
