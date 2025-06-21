package com.megaman.maverick.game.world.body

import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.world.body.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.contracts.IProjectileEntity

enum class FixtureLabel {
    NO_PROJECTILE_COLLISION,
    NO_BODY_TOUCHIE,
    NO_SIDE_TOUCHIE,
    NO_FEET_TOUCHIE
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

fun IFixture.addFixtureLabels(vararg fixtureLabels: FixtureLabel) = fixtureLabels.forEach { addFixtureLabel(it) }

fun IFixture.clearFixtureLabels() {
    removeProperty(ConstKeys.FIXTURE_LABELS)
}

fun IFixture.removeFixtureLabel(fixtureLabel: FixtureLabel) {
    if (!hasProperty(ConstKeys.FIXTURE_LABELS)) return
    val labels = getProperty(ConstKeys.FIXTURE_LABELS) as ObjectSet<FixtureLabel>
    labels.remove(fixtureLabel)
}

fun IFixture.setExceptionForNoProjectileCollision(exception: (IProjectileEntity, IFixture) -> Boolean) {
    putProperty("${FixtureLabel.NO_PROJECTILE_COLLISION}_${ConstKeys.EXCEPTION}", exception)
}

fun IFixture.isExceptionForNoProjectileCollision(entity: IProjectileEntity, fixture: IFixture): Boolean {
    val exception = getProperty("${FixtureLabel.NO_PROJECTILE_COLLISION}_${ConstKeys.EXCEPTION}")
    if (exception == null) return false
    return (exception as (IProjectileEntity, IFixture) -> Boolean).invoke(entity, fixture)
}

