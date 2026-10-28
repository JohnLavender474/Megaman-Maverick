package com.maverick.game.engine.groups

import com.badlogic.gdx.utils.OrderedSet
import com.maverick.game.engine.components.IGameComponent

class GroupsComponent : IGameComponent {

    val groups = OrderedSet<String>()

    override fun reset() = groups.clear()
}
