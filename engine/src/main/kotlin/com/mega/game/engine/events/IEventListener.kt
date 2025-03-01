package com.mega.game.engine.events

import com.badlogic.gdx.utils.ObjectSet

interface IEventListener {

    val eventKeyMask: ObjectSet<Any>

    fun onEvent(event: Event)
}
