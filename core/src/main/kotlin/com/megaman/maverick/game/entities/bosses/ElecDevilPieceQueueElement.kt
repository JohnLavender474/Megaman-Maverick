package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.objects.IntPair

internal data class ElecDevilPieceQueueElement(
    internal val position: IntPair,
    internal val start: Vector2,
    internal val target: Vector2
)
