package com.megaman.maverick.game.entities.megaman

import com.engine.updatables.UpdatablesComponent

/**
 * Returns the [UpdatablesComponent] of this [Megaman], or creates a new one if it doesn't have one.
 */
fun Megaman.updatablesComponent(): UpdatablesComponent {
  if (hasComponent(UpdatablesComponent::class)) return getComponent(UpdatablesComponent::class)!!

  return UpdatablesComponent(this, { delta -> })
}
