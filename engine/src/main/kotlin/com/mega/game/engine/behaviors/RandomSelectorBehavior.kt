package com.mega.game.engine.behaviors

import com.badlogic.gdx.utils.Array

class RandomSelectorBehavior(childBehaviors: Array<IBehavior>) : SelectorBehavior(childBehaviors) {

    override fun evaluate(delta: Float): Boolean {
        if (!isBehaviorSelected()) childBehaviors.shuffle()
        return super.evaluate(delta)
    }
}
