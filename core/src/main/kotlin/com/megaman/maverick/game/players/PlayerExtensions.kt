package com.megaman.maverick.game.players

import com.maverick.game.engine.common.interfaces.IPropertizable

val Any?.asPlayer: MegaPlayer?
    get() = (this as? IPropertizable)?.getProperty(PlayerKeys.VIEW)
