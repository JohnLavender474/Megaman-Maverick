package com.megaman.maverick.game.players

import com.maverick.game.engine.common.objects.PropertyKey

object PlayerKeys {

    val GRAVITY_DEVICE_SWITCH = PropertyKey<() -> Unit>("gravity_device_switch")

    val LIGHT = PropertyKey<() -> PlayerLight>("player_light")

    val VIEW = PropertyKey<MegaPlayer>("player_view")
}
