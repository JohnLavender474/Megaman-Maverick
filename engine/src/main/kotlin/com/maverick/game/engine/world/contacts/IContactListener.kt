package com.maverick.game.engine.world.contacts

interface IContactListener {

    fun beginContact(contact: Contact, delta: Float)

    fun continueContact(contact: Contact, delta: Float)

    fun endContact(contact: Contact, delta: Float)
}
