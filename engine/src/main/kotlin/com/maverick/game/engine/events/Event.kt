package com.maverick.game.engine.events

import com.maverick.game.engine.common.interfaces.IPropertizable
import com.maverick.game.engine.common.objects.Properties

data class Event(val key: Any, override val properties: Properties = Properties()) : IPropertizable
