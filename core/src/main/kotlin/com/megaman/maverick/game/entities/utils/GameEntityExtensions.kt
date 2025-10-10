package com.megaman.maverick.game.entities.utils

import com.mega.game.engine.common.objects.props
import com.mega.game.engine.entities.GameEntity

fun GameEntity.spawn() = this.spawn(props())
