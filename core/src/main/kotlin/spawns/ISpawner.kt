package com.megaman.maverick.game.spawns

import com.mega.game.engine.common.interfaces.UpdatePredicate
import com.mega.game.engine.cullables.ICullable

interface ISpawner : UpdatePredicate, ICullable {

    var respawnable: Boolean

    fun get(): Spawn?

    override fun shouldBeCulled(delta: Float) = false

    override fun reset() {}
}
