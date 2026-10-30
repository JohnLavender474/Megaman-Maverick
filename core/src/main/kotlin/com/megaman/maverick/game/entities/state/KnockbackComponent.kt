package com.megaman.maverick.game.entities.state

import com.badlogic.gdx.utils.ObjectSet
import com.maverick.game.engine.components.IGameComponent

class KnockbackComponent(val x: Float, val y: Float, val exempt: ObjectSet<Any>) : IGameComponent
