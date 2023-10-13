package com.test.game.world

import com.badlogic.gdx.utils.ObjectSet
import com.engine.common.extensions.objectSetOf
import com.engine.world.Body
import com.test.game.ConstKeys

enum class BodyLabel {
  PLAYER_BODY,
  NO_SIDE_TOUCHIE,
  COLLIDE_DOWN_ONLY,
  COLLIDE_UP_ONLY,
  PRESS_UP_FALL_THRU,
  NO_PROJECTILE_COLLISION
}

fun Body.addBodyLabel(bodyLabel: BodyLabel) {
  getProperty(ConstKeys.BODY_LABELS)?.let {
    @Suppress("UNCHECKED_CAST") val labels = it as ObjectSet<BodyLabel>
    labels.add(bodyLabel)
  } ?: putProperty(ConstKeys.BODY_LABELS, objectSetOf(bodyLabel))
}

fun Body.removeBodyLabel(bodyLabel: BodyLabel) {
  getProperty(ConstKeys.BODY_LABELS)?.let {
    @Suppress("UNCHECKED_CAST") val labels = it as ObjectSet<BodyLabel>
    labels.remove(bodyLabel)
  }
}

fun Body.hasBodyLabel(bodyLabel: BodyLabel) =
    getProperty(ConstKeys.BODY_LABELS)?.let {
      @Suppress("UNCHECKED_CAST") val labels = it as ObjectSet<BodyLabel>
      labels.contains(bodyLabel)
    } ?: false
