package com.mega.game.engine.controller.buttons

import com.mega.game.engine.controller.polling.IControllerPoller


interface IButtonActuator {


    fun onJustPressed(poller: IControllerPoller)


    fun onPressContinued(poller: IControllerPoller, delta: Float)


    fun onJustReleased(poller: IControllerPoller)


    fun onReleaseContinued(poller: IControllerPoller, delta: Float)
}
