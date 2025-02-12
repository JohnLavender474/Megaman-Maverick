package com.mega.game.engine.controller.polling

import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.common.extensions.putIfAbsentAndGet
import com.mega.game.engine.controller.buttons.ButtonStatus
import com.mega.game.engine.controller.buttons.ControllerButtons

open class ControllerPoller(val controllerButtons: ControllerButtons) : IControllerPoller {

    override var on = true

    private val statusMap = ObjectMap<Any, ButtonStatus>()
    private var initialized = false

    override fun init() = controllerButtons.keys().forEach { statusMap.put(it, ButtonStatus.RELEASED) }

    override fun getStatus(key: Any): ButtonStatus? = statusMap[key]

    override fun run() {
        if (!initialized) {
            init()
            initialized = true
        }

        controllerButtons.forEach { e ->
            val key = e.key
            val button = e.value
            val oldStatus = statusMap.putIfAbsentAndGet(key) { ButtonStatus.RELEASED }
            val newStatus = if (on) {
                if (button.isEnabled()) {
                    val pressed = button.isPressed()
                    when (oldStatus) {
                        ButtonStatus.RELEASED,
                        ButtonStatus.JUST_RELEASED -> if (pressed) ButtonStatus.JUST_PRESSED else ButtonStatus.RELEASED

                        else -> if (pressed) ButtonStatus.PRESSED else ButtonStatus.JUST_RELEASED
                    }
                } else if (oldStatus == ButtonStatus.JUST_RELEASED) ButtonStatus.RELEASED
                else ButtonStatus.JUST_RELEASED
            } else when (oldStatus) {
                ButtonStatus.PRESSED, ButtonStatus.JUST_PRESSED -> ButtonStatus.JUST_RELEASED
                ButtonStatus.RELEASED, ButtonStatus.JUST_RELEASED -> ButtonStatus.RELEASED
            }
            statusMap.put(key, newStatus)
        }
    }
}
