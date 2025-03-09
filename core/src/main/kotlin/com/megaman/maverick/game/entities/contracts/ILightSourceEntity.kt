package com.megaman.maverick.game.entities.contracts

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.events.Event
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.events.EventType

interface ILightSourceEntity : IGameEntity {

    var radius: Int
    var radiance: Float

    val center: Vector2
    val keys: ObjectSet<Int>
}

fun sendLightEvent(game: MegamanMaverickGame, lightSource: ILightSourceEntity) {
    game.eventsMan.submitEvent(
        Event(
            EventType.ADD_LIGHT_SOURCE, props(
                ConstKeys.KEYS pairTo lightSource.keys,
                ConstKeys.CENTER pairTo lightSource.center,
                ConstKeys.RADIANCE pairTo lightSource.radiance,
                ConstKeys.RADIUS pairTo lightSource.radius * ConstVals.PPM
            )
        )
    )
}
