package com.mega.game.engine.screens.menus

import com.badlogic.gdx.utils.ObjectMap

abstract class StandardMenuScreen(
    buttons: ObjectMap<String, IMenuButton> = ObjectMap(),
    pauseSupplier: () -> Boolean = { false },
    protected var firstButtonKey: String? = null
) : AbstractMenuScreen(buttons, pauseSupplier) {

    protected var buttonKey: String? = null

    override fun getCurrentButtonKey() = buttonKey

    override fun setCurrentButtonKey(key: String?) {
        this.buttonKey = key
    }

    override fun show() {
        super.show()
        setCurrentButtonKey(firstButtonKey)
    }

    override fun reset() {
        super.reset()
        setCurrentButtonKey(firstButtonKey)
    }
}
