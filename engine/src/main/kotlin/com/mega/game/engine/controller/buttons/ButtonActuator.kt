package com.mega.game.engine.controller.buttons

import com.mega.game.engine.controller.polling.IControllerPoller
import java.util.function.BiConsumer
import java.util.function.Consumer

class ButtonActuator(
    var onJustPressed: ((IControllerPoller) -> Unit)? = null,
    var onPressContinued: ((IControllerPoller, Float) -> Unit)? = null,
    var onJustReleased: ((IControllerPoller) -> Unit)? = null,
    var onReleaseContinued: ((IControllerPoller, Float) -> Unit)? = null
) : IButtonActuator {

    constructor(
        onJustPressed: Consumer<IControllerPoller>? = null,
        onPressContinued: BiConsumer<IControllerPoller, Float>? = null,
        onJustReleased: Consumer<IControllerPoller>? = null,
        onReleaseContinued: BiConsumer<IControllerPoller, Float>? = null
    ) : this(
        onJustPressed?.let { { poller: IControllerPoller -> it.accept(poller) } },
        onPressContinued?.let { { poller: IControllerPoller, delta: Float -> it.accept(poller, delta) } },
        onJustReleased?.let { { poller: IControllerPoller -> it.accept(poller) } },
        onReleaseContinued?.let { { poller: IControllerPoller, delta: Float -> it.accept(poller, delta) } }
    )

    override fun onJustPressed(poller: IControllerPoller) {
        onJustPressed?.invoke(poller)
    }

    override fun onPressContinued(poller: IControllerPoller, delta: Float) {
        onPressContinued?.invoke(poller, delta)
    }

    override fun onJustReleased(poller: IControllerPoller) {
        onJustReleased?.invoke(poller)
    }

    override fun onReleaseContinued(poller: IControllerPoller, delta: Float) {
        onReleaseContinued?.invoke(poller, delta)
    }
}
