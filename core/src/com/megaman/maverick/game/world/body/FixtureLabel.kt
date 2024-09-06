package com.megaman.maverick.game.world.body

import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.world.body.IFixture
import com.megaman.maverick.game.ConstKeys

enum class FixtureLabel {
    NO_PROJECTILE_COLLISION,
    NO_BODY_TOUCHIE,
    NO_SIDE_TOUCHIE
}

fun IFixture.hasFixtureLabel(fixtureLabel: FixtureLabel): Boolean {
    if (!hasProperty(ConstKeys.FIXTURE_LABELS)) return false
    val labels = getProperty(ConstKeys.FIXTURE_LABELS) as ObjectSet<FixtureLabel>
    return labels.contains(fixtureLabel)
}

fun IFixture.addFixtureLabel(fixtureLabel: FixtureLabel) {
    val labels = getOrDefaultProperty(ConstKeys.FIXTURE_LABELS, ObjectSet<FixtureLabel>()) as ObjectSet<FixtureLabel>
    labels.add(fixtureLabel)
    putProperty(ConstKeys.FIXTURE_LABELS, labels)
}

fun IFixture.addFixtureLabels(fixtureLabels: ObjectSet<FixtureLabel>) {
    val labels = getOrDefaultProperty(ConstKeys.FIXTURE_LABELS, ObjectSet<FixtureLabel>()) as ObjectSet<FixtureLabel>
    labels.addAll(fixtureLabels)
    putProperty(ConstKeys.FIXTURE_LABELS, labels)
}

fun IFixture.clearFixtureLabels() {
    removeProperty(ConstKeys.FIXTURE_LABELS)
}

fun IFixture.removeFixtureLabel(fixtureLabel: FixtureLabel) {
    if (!hasProperty(ConstKeys.FIXTURE_LABELS)) return
    val labels = getProperty(ConstKeys.FIXTURE_LABELS) as ObjectSet<FixtureLabel>
    labels.remove(fixtureLabel)
}

