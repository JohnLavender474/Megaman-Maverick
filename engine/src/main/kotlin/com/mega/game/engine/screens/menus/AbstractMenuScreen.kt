package com.mega.game.engine.screens.menus

import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.screens.BaseScreen


abstract class AbstractMenuScreen(
    protected var buttons: ObjectMap<String, IMenuButton> = ObjectMap(),
    protected var pauseSupplier: () -> Boolean = { false },
    protected var firstButtonKey: String? = null
) : BaseScreen() {


    var currentButtonKey: String? = firstButtonKey


    var selectionMade = false
        protected set


    protected abstract fun getNavigationDirection(): Direction?


    protected abstract fun selectionRequested(): Boolean


    protected open fun onAnyMovement(direction: Direction) {}


    protected open fun onAnySelection() {}


    override fun show() {
        selectionMade = false
        currentButtonKey = firstButtonKey
    }


    override fun reset() {
        selectionMade = false
        currentButtonKey = firstButtonKey
    }


    override fun render(delta: Float) {
        if (selectionMade || pauseSupplier.invoke()) return

        buttons[currentButtonKey]?.let { button ->
            getNavigationDirection()?.let {
                currentButtonKey = button.onNavigate(it, delta)
                onAnyMovement(it)
            }

            if (selectionRequested()) {
                selectionMade = button.onSelect(delta)
                if (selectionMade) onAnySelection()
            }
        }
    }
}
