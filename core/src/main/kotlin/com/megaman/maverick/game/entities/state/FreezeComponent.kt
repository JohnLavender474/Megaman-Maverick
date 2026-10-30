package com.megaman.maverick.game.entities.state

import com.maverick.game.engine.components.IGameComponent

class FreezeComponent(private val thawRule: IThawRule, private val onThaw: () -> Unit) : IGameComponent {

    var frozen = false
        private set

    fun freeze() {
        if (frozen) return
        frozen = true
        thawRule.reset()
    }

    fun update(delta: Float) {
        if (frozen && thawRule.shouldThaw(delta)) {
            frozen = false
            thawRule.reset()
            onThaw()
        }
    }

    override fun reset() {
        frozen = false
        thawRule.reset()
    }
}
