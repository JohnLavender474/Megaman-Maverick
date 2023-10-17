package com.megaman.maverick.game.entities.megaman.components

import com.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.entities.megaman.Megaman

internal fun Megaman.defineUpdatablesComponent() =
    UpdatablesComponent(
        this,
        { delta ->
          // TODO: update charging and shoot anim here?
          chargingTimer.update(delta)
          shootAnimTimer.update(delta)
        })
