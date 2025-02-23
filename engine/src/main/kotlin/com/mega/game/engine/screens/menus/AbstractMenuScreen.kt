package com.mega.game.engine.screens.menus

import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.screens.BaseScreen

abstract class AbstractMenuScreen(protected var buttons: ObjectMap<String, IMenuButton> = ObjectMap()) : BaseScreen() {

    var selectionMade = false
        protected set

    abstract fun setCurrentButtonKey(key: String?)

    abstract fun getCurrentButtonKey(): String?

    protected open fun isInteractionAllowed() = !selectionMade

    protected abstract fun getNavigationDirection(): Direction?

    protected abstract fun selectionRequested(): Boolean

    protected open fun onAnyMovement(direction: Direction) {}

    protected open fun onAnySelection() {}

    override fun show() {
        super.show()
        selectionMade = false
    }

    override fun reset() {
        super.reset()
        selectionMade = false
    }

    override fun render(delta: Float) {
        super.render(delta)

        val key = getCurrentButtonKey()
        if (isInteractionAllowed() && key != null) buttons[key]?.let { button ->
            getNavigationDirection()?.let {
                val key = button.onNavigate(it, delta)
                setCurrentButtonKey(key)
                onAnyMovement(it)
            }

            if (selectionRequested()) {
                selectionMade = button.onSelect(delta)
                if (selectionMade) onAnySelection()
            }
        }
    }
}
