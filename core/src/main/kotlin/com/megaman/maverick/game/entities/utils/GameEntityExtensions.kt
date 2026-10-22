package com.megaman.maverick.game.entities.utils

import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.entities.GameEntity

fun GameEntity.spawn() = this.spawn(props())
