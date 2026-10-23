package com.maverick.game.engine.components

import kotlin.reflect.KClass

class MissingComponentException(val type: KClass<out IGameComponent>) : RuntimeException()
