package com.mega.game.engine.cullables

import java.util.function.Predicate


class Cullable(
    private val shouldBeCulledFunction: (Float) -> Boolean,
    private val resetFunction: () -> Unit
) : ICullable {


    constructor(
        shouldBeCulledFunction: Predicate<Float>,
        resetFunction: Runnable
    ) : this(
        shouldBeCulledFunction::test,
        resetFunction::run
    )

    override fun shouldBeCulled(delta: Float) = shouldBeCulledFunction(delta)

    override fun reset() = resetFunction()
}
