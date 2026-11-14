package com.megaman.maverick.game.entities.protoman

enum class ProtomanWeapon(val hudRegion: String) {

    SABRE("protoman_sabre"), BUSTER("protoman_buster");

    fun next() = entries[(ordinal + 1) % entries.size]
}
