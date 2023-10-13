package com.test.game.utils

import com.badlogic.gdx.utils.OrderedSet
import com.engine.world.Body
import com.test.game.ConstKeys

@Suppress("UNCHECKED_CAST")
fun Body.getLabels() =
    properties.putIfAbsentAndGet(ConstKeys.BODY_LABELS, OrderedSet<Any>()) as OrderedSet<Any>

fun Body.addLabel(label: Any) = getLabels().add(label)

fun Body.hasLabel(label: Any) = getLabels().contains(label)

fun Body.removeLabel(label: Any) = getLabels().remove(label)
