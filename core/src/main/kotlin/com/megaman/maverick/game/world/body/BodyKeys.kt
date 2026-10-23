package com.megaman.maverick.game.world.body

import com.maverick.game.engine.common.objects.PropertyKey

object BodyKeys {

    val PASS_THROUGH_ONE_WAY = PropertyKey<() -> Boolean>("pass_through_one_way")

    val FALL_THROUGH_PRESS_UP = PropertyKey<() -> Boolean>("fall_through_press_up")
}
