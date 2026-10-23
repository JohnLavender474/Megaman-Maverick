package com.megaman.maverick.game.events

import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.common.objects.PropertyKey
import com.maverick.game.engine.players.PlayerSlot
import com.megaman.maverick.game.ConstKeys

object PlayerEventKeys {

    val SLOT = PropertyKey<PlayerSlot>(ConstKeys.SLOT)

    val SLOTS = PropertyKey<Array<PlayerSlot>>(ConstKeys.SLOTS)
}
