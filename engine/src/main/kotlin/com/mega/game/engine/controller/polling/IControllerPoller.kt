package com.mega.game.engine.controller.polling

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.common.interfaces.IActivatable
import com.mega.game.engine.common.interfaces.Initializable
import com.mega.game.engine.controller.buttons.ButtonStatus

interface IControllerPoller : IActivatable, Runnable, Initializable {

    override fun init() {
        // default implementation is a no-op
    }

    fun getStatus(key: Any): ButtonStatus?

    fun isPressed(key: Any): Boolean {
        val status = getStatus(key)
        return status == ButtonStatus.PRESSED || status == ButtonStatus.JUST_PRESSED
    }

    fun areAllPressed(keys: Array<Any>) = keys.all { isPressed(it) }

    fun isAnyPressed(keys: Array<Any>) = keys.any { isPressed(it) }

    fun isJustPressed(key: Any): Boolean {
        val status = getStatus(key)
        return status == ButtonStatus.JUST_PRESSED
    }

    fun areAllJustPressed(keys: Array<Any>) = keys.all { isJustPressed(it) }

    fun isAnyJustPressed(keys: Array<Any>) = keys.any { isJustPressed(it) }

    fun isJustReleased(key: Any): Boolean {
        val status = getStatus(key)
        return status == ButtonStatus.JUST_RELEASED
    }

    fun areAllJustReleased(keys: Array<Any>) = keys.all { isJustReleased(it) }

    fun isAnyJustReleased(keys: Array<Any>) = keys.any { isJustReleased(it) }

    fun isReleased(key: Any): Boolean {
        val status = getStatus(key)
        return status == ButtonStatus.RELEASED || status == ButtonStatus.JUST_RELEASED
    }

    fun areAllReleased(keys: Array<Any>) = keys.all { isReleased(it) }

    fun isAnyReleased(keys: Array<Any>) = keys.any { isReleased(it) }

    fun allMatch(map: ObjectMap<Any, ButtonStatus>) = map.all { getStatus(it.key) == it.value }
}
