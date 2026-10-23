package com.maverick.game.engine.world.contacts

import com.maverick.game.engine.world.body.IFixture

interface IContactListener {

    fun beginContact(fixture1: IFixture, fixture2: IFixture, delta: Float)

    fun continueContact(fixture1: IFixture, fixture2: IFixture, delta: Float)

    fun endContact(fixture1: IFixture, fixture2: IFixture, delta: Float)
}
