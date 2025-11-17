package com.mega.game.engine.world.body

import com.mega.game.engine.components.IGameComponent

class BodyComponent(var body: Body, var doUpdate: () -> Boolean = { true }) : IGameComponent {

    override fun reset() = body.reset()
}
