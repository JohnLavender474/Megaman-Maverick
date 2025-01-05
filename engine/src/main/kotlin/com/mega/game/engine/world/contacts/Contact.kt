package com.mega.game.engine.world.contacts

import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.common.objects.GamePair
import com.mega.game.engine.world.body.IFixture

data class Contact(var fixture1: IFixture, var fixture2: IFixture) {

    fun set(fixture1: IFixture, fixture2: IFixture) {
        this.fixture1 = fixture1
        this.fixture2 = fixture2
    }

    fun fixturesMatch(fixtureType1: Any, fixtureType2: Any) =
        (fixture1.getType() == fixtureType1 && fixture2.getType() == fixtureType2) ||
            (fixture2.getType() == fixtureType1 && fixture1.getType() == fixtureType2)

    fun fixtureSetsMatch(fixtureLabels1: ObjectSet<Any>, fixtureLabels2: ObjectSet<Any>) =
        (fixtureLabels1.contains(fixture1.getType()) && fixtureLabels2.contains(fixture2.getType())) ||
            (fixtureLabels1.contains(fixture2.getType()) && fixtureLabels2.contains(fixture1.getType()))

    fun oneFixtureMatches(fixtureType: Any) =
        fixture1.getType() == fixtureType || fixture2.getType() == fixtureType

    fun getFixturesIfOneMatches(fixtureType: Any, out: GamePair<IFixture, IFixture>) = when (fixtureType) {
        fixture1.getType() -> out.set(fixture1, fixture2)
        fixture2.getType() -> out.set(fixture2, fixture1)
        else -> null
    }

    fun getFixturesInOrder(fixtureType1: Any, fixtureType2: Any, out: GamePair<IFixture, IFixture>) = when {
        fixture1.getType() == fixtureType1 && fixture2.getType() == fixtureType2 -> out.set(fixture1, fixture2)
        fixture2.getType() == fixtureType1 && fixture1.getType() == fixtureType2 -> out.set(fixture2, fixture1)
        else -> null
    }

    fun getFixtureSetsInOrder(
        fixtureLabels1: ObjectSet<Any>,
        fixtureLabels2: ObjectSet<Any>,
        out: GamePair<IFixture, IFixture>
    ) = when {
        fixtureLabels1.contains(fixture1.getType()) &&
            fixtureLabels2.contains(fixture2.getType())
            -> out.set(fixture1, fixture2)

        fixtureLabels1.contains(fixture2.getType()) &&
            fixtureLabels2.contains(fixture1.getType())
            -> out.set(fixture2, fixture1)

        else -> null
    }


    override fun equals(other: Any?) =
        other is Contact && ((fixture1 == other.fixture1 && fixture2 == other.fixture2) ||
            (fixture1 == other.fixture2 && fixture2 == other.fixture1))

    override fun hashCode() = 49 + 7 * fixture1.hashCode() + 7 * fixture2.hashCode()

    override fun toString() = "Contact(fixture1=$fixture1, fixture2=$fixture2)"
}
