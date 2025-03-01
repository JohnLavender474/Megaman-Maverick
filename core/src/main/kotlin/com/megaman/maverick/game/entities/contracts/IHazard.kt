package com.megaman.maverick.game.entities.contracts

interface IHazard : IBossListener {

    fun getDamageToMegaman(): Int = 3
}
