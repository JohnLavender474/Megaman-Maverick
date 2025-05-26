package com.megaman.maverick.game.utils.misc

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.common.extensions.toInt
import com.mega.game.engine.common.extensions.toObjectSet
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.events.Event
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.contracts.ILightSource
import com.megaman.maverick.game.events.EventType

object LightSourceUtils {

    fun loadLightSourceFromProps(
        lightSource: ILightSource,
        props: Properties
    ) {
        loadLightSourceKeysFromProps(lightSource, props)
        lightSource.lightSourceRadius = props.get(ConstKeys.RADIUS, Int::class)!!
        lightSource.lightSourceRadiance = props.get(ConstKeys.RADIANCE, Float::class)!!
    }

    fun loadLightSourceKeysFromProps(
        lightSource: ILightSource,
        props: Properties
    ) {
        lightSource.lightSourceKeys.addAll(
            props.get(ConstKeys.KEYS, String::class)!!
                .replace("\\s+", "")
                .split(",")
                .map { it.toInt() }
                .toObjectSet()
        )
    }

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
