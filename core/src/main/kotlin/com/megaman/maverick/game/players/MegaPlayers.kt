package com.megaman.maverick.game.players

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.players.IPlayers
import com.maverick.game.engine.players.Players
import com.maverick.game.engine.players.minByOrNull
import com.megaman.maverick.game.world.body.getCenter

class MegaPlayers(capacity: Int = 1) : Players<MegaPlayer>(capacity, ::MegaPlayer)

fun IPlayers<MegaPlayer>.nearestTo(point: Vector2) = minByOrNull { it.body.getCenter().dst2(point) }
