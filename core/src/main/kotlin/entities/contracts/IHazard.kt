package com.megaman.maverick.game.entities.contracts

/**
 * Marker interface for hazards. Since Megaman can only be damaged by enemies and hazards, this interface
 * marks hazardous entities that (1) are not enemies and (2) can damage Megaman. Entities that implement
 * [com.mega.game.engine.damage.IDamager] but which do not inherit [AbstractEnemy] or [IHazard] cannot
 * hurt Megaman.
 */
interface IHazard
