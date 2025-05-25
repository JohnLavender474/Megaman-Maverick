package com.megaman.maverick.game.utils.misc

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.events.Event
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.contracts.ILightSource
import com.megaman.maverick.game.events.EventType

object LightSourceUtils {

    fun sendLightSourceEvent(
        game: MegamanMaverickGame, lightSource: ILightSource
    ) = sendLightSourceEvent(
        game,
        lightSource.lightSourceKeys,
        lightSource.lightSourceCenter,
        lightSource.lightSourceRadiance,
        lightSource.lightSourceRadius
    )

    fun sendLightSourceEvent(
        game: MegamanMaverickGame,
        keys: ObjectSet<Int>,
        center: Vector2,
        radiance: Float,
        radius: Int
    ) = game.eventsMan.submitEvent(
        Event(
            EventType.ADD_LIGHT_SOURCE, props(
                ConstKeys.KEYS pairTo keys,
                ConstKeys.CENTER pairTo center,
                ConstKeys.RADIANCE pairTo radiance,
                ConstKeys.RADIUS pairTo radius * ConstVals.PPM
            )
        )
    )
}
