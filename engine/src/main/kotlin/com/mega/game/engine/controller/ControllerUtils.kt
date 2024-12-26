package com.mega.game.engine.controller

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.controllers.Controller
import com.badlogic.gdx.controllers.Controllers


object ControllerUtils {


    fun isKeyboardKeyPressed(key: Int) = Gdx.input.isKeyPressed(key)


    fun isControllerKeyPressed(key: Int) = isControllerKeyPressed(0, key)


    fun isControllerKeyPressed(index: Int, key: Int) = getController(index)?.getButton(key) == true


    fun getController(index: Int): Controller? =
        try {
            Controllers.getControllers().get(index)
        } catch (_: Exception) {
            null
        }


    fun isControllerConnected(index: Int) = getController(index) != null


    fun getController() = if (!Controllers.getControllers().isEmpty) Controllers.getControllers().get(0) else null


    fun isControllerConnected() = getController() != null
}
