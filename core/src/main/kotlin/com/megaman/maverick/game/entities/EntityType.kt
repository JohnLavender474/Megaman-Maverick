package com.megaman.maverick.game.entities

enum class EntityType(private val packageSuffix: String) {
    BLOCK("blocks"),
    DECORATION("decorations"),
    EXPLOSION("explosions"),
    ENEMY("enemies"),
    BOSS("bosses"),
    HAZARD("hazards"),
    ITEM("items"),
    MEGAMAN("megaman"),
    PROJECTILE("projectiles"),
    SENSOR("sensors"),
    SPECIAL("special"),
    ANIMATED_MOCK("animated_mocks");

    companion object {
        private const val PACKAGE_PREFIX = "com.megaman.maverick.game.entities."
    }

    fun getFullyQualifiedName(entityName: String) = "${getPackageName()}.$entityName"

    fun getPackageName() = "$PACKAGE_PREFIX$packageSuffix"
}
