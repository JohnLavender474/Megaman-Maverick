package com.megaman.maverick.game.world

import com.badlogic.gdx.utils.ObjectSet
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys

enum class FixtureLabel {
    NO_PROJECTILE_COLLISION,
    NO_SIDE_TOUCHIE
}

fun Fixture.hasFixtureLabel(fixtureLabel: FixtureLabel): Boolean {
    if (!hasProperty(ConstKeys.FIXTURE_LABELS)) return false
    val labels = getProperty(ConstKeys.FIXTURE_LABELS) as ObjectSet<FixtureLabel>
    return labels.contains(fixtureLabel)
}

fun Fixture.addFixtureLabel(fixtureLabel: FixtureLabel) {
    val labels = getOrDefaultProperty(ConstKeys.FIXTURE_LABELS, ObjectSet<FixtureLabel>()) as ObjectSet<FixtureLabel>
    labels.add(fixtureLabel)
    putProperty(ConstKeys.FIXTURE_LABELS, labels)
}

fun Fixture.addFixtureLabels(fixtureLabels: ObjectSet<FixtureLabel>) {
    val labels = getOrDefaultProperty(ConstKeys.FIXTURE_LABELS, ObjectSet<FixtureLabel>()) as ObjectSet<FixtureLabel>
    labels.addAll(fixtureLabels)
    putProperty(ConstKeys.FIXTURE_LABELS, labels)
}

fun Fixture.clearFixtureLabels() {
    removeProperty(ConstKeys.FIXTURE_LABELS)
}

fun Fixture.removeFixtureLabel(fixtureLabel: FixtureLabel) {
    if (!hasProperty(ConstKeys.FIXTURE_LABELS)) return
    val labels = getProperty(ConstKeys.FIXTURE_LABELS) as ObjectSet<FixtureLabel>
    labels.remove(fixtureLabel)
}

