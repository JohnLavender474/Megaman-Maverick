package com.mega.game.engine.behaviors

import com.badlogic.gdx.utils.Array


open class SelectorBehavior(val childBehaviors: Array<IBehavior>) : AbstractBehaviorImpl() {

    private var selectedBehavior: IBehavior? = null

    fun isBehaviorSelected() = selectedBehavior != null

    override fun evaluate(delta: Float): Boolean {
        if (selectedBehavior == null) {
            for (childBehavior in childBehaviors) {
                if (childBehavior.evaluate(delta)) {
                    selectedBehavior = childBehavior
                    return true
                }
            }
            return false
        } else return selectedBehavior!!.evaluate(delta)
    }

    override fun init() {
        selectedBehavior?.init()
    }

    override fun act(delta: Float) {
        selectedBehavior?.act(delta)
    }

    override fun end() {
        selectedBehavior = null
    }
}