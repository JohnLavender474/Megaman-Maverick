package com.maverick.game.engine.world.contacts

import com.maverick.game.engine.world.body.IFixture

interface IContactRule {

    fun onBegin(a: IFixture, b: IFixture, delta: Float) {}

    fun onContinue(a: IFixture, b: IFixture, delta: Float) {}

    fun onEnd(a: IFixture, b: IFixture, delta: Float) {}
}
