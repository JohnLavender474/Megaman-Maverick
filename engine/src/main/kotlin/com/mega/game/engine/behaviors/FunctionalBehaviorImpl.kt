package com.mega.game.engine.behaviors

import java.util.function.Consumer
import java.util.function.Function


class FunctionalBehaviorImpl(
    private val evaluate: (delta: Float) -> Boolean,
    private val init: (() -> Unit)? = null,
    private val act: ((delta: Float) -> Unit)? = null,
    private val end: (() -> Unit)? = null,
    private val reset: (() -> Unit)? = null
) : AbstractBehaviorImpl() {


    constructor(
        evaluate: Function<Float, Boolean>, init: Runnable? = null, act: Consumer<Float>? = null, end: Runnable? = null
    ) : this({ delta -> evaluate.apply(delta) }, { init?.run() }, { delta -> act?.accept(delta) }, { end?.run() })

    override fun evaluate(delta: Float) = evaluate.invoke(delta)

    override fun init() {
        init?.invoke()
    }

    override fun act(delta: Float) {
        act?.invoke(delta)
    }

    override fun end() {
        end?.invoke()
    }


}
