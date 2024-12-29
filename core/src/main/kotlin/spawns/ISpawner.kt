package com.megaman.maverick.game.spawns

import com.mega.game.engine.common.interfaces.UpdatePredicate
import com.mega.game.engine.cullables.ICullable
import java.util.function.Supplier

interface ISpawner : Supplier<Spawn?>, UpdatePredicate, ICullable {

    var respawnable: Boolean

    override fun shouldBeCulled(delta: Float) = false

    override fun reset() {}
}
