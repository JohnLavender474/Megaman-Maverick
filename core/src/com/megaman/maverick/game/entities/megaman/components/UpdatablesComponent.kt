package com.megaman.maverick.game.entities.megaman.components

import com.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.entities.megaman.Megaman

/**
 * Returns the [UpdatablesComponent] of this [Megaman], or creates a new one if it doesn't have one.
 */
internal fun Megaman.defineUpdatablesComponent() = UpdatablesComponent(this, { delta -> })
