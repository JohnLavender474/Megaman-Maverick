package com.mega.game.engine.events

import com.mega.game.engine.common.interfaces.IPropertizable
import com.mega.game.engine.common.objects.Properties

data class Event(val key: Any, override val properties: Properties = Properties()) : IPropertizable
