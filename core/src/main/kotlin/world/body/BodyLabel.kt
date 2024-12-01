package com.megaman.maverick.game.world.body

import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.IBody
import com.megaman.maverick.game.ConstKeys

enum class BodyLabel {
    COLLIDE_DOWN_ONLY,
    COLLIDE_UP_ONLY,
    PRESS_UP_FALL_THRU
}

fun IBody.addBodyLabel(bodyLabel: BodyLabel) {
    val labels = getOrDefaultProperty(ConstKeys.BODY_LABELS, ObjectSet<BodyLabel>()) as ObjectSet<BodyLabel>
    labels.add(bodyLabel)
    putProperty(ConstKeys.BODY_LABELS, labels)
}

fun IBody.addBodyLabels(bodyLabels: ObjectSet<BodyLabel>) {
    val labels = getOrDefaultProperty(ConstKeys.BODY_LABELS, ObjectSet<BodyLabel>()) as ObjectSet<BodyLabel>
    labels.addAll(bodyLabels)
    putProperty(ConstKeys.BODY_LABELS, labels)
}

fun IBody.clearBodyLabels() {
    removeProperty(ConstKeys.BODY_LABELS)
}

fun IBody.removeBodyLabel(bodyLabel: BodyLabel) {
    if (!hasProperty(ConstKeys.BODY_LABELS)) return
    val labels = getProperty(ConstKeys.BODY_LABELS) as ObjectSet<BodyLabel>
    labels.remove(bodyLabel)
}

fun IBody.hasBodyLabel(bodyLabel: BodyLabel): Boolean {
    if (!hasProperty(ConstKeys.BODY_LABELS)) return false
    val labels = getProperty(ConstKeys.BODY_LABELS) as ObjectSet<BodyLabel>
    return labels.contains(bodyLabel)
}
