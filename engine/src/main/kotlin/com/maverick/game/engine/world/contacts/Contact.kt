package com.maverick.game.engine.world.contacts

import com.maverick.game.engine.world.body.IFixture

data class Contact(var fixture1: IFixture, var fixture2: IFixture) {

    fun set(fixture1: IFixture, fixture2: IFixture) {
        this.fixture1 = fixture1
        this.fixture2 = fixture2
    }

    override fun equals(other: Any?) =
        other is Contact && ((fixture1 == other.fixture1 && fixture2 == other.fixture2) ||
            (fixture1 == other.fixture2 && fixture2 == other.fixture1))

    override fun hashCode() = 49 + 7 * fixture1.hashCode() + 7 * fixture2.hashCode()

    override fun toString() = "Contact(fixture1=$fixture1, fixture2=$fixture2)"
}
