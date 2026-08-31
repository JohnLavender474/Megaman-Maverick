package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.utils.ObjectSet

/**
 * The rules deciding whether a [DarknessV2] is currently darkening, split out from the entity's event handling so
 * they can be exercised directly. They take room *names* rather than map objects, so nothing here needs a
 * [com.badlogic.gdx.maps.objects.RectangleMapObject].
 *
 * The pair of transition rules is deliberately asymmetric, and that asymmetry is load-bearing. Both fire during a
 * room transition, while the camera shows part of the room being left and part of the room being entered:
 *
 * - going *out* of darkness the flag drops at the **begin** of the transition, so the darkness has the whole
 *   transition to fade out and is already clear by the time the new room fills the screen;
 * - going *into* darkness it raises at the **end**, so the room being left is never darkened on the way out.
 *
 * Each rule only ever moves the flag in its own direction; anything else leaves it as it was.
 */

/** Whether the darkness starts out darkening for the room the player spawns into. */
internal fun darkModeOnPlayerReady(currentRoom: String?, rooms: ObjectSet<String>) =
    currentRoom != null && rooms.contains(currentRoom)

/** Leaving a darkened room for an undarkened one clears the flag as the transition starts. */
internal fun darkModeOnRoomTransBegin(
    priorRoom: String?,
    newRoom: String?,
    rooms: ObjectSet<String>,
    darkMode: Boolean
) = when {
    (priorRoom == null || rooms.contains(priorRoom)) && newRoom != null && !rooms.contains(newRoom) -> false
    else -> darkMode
}

/** Arriving in a darkened room from an undarkened one raises the flag as the transition finishes. */
internal fun darkModeOnRoomTransEnd(
    priorRoom: String?,
    newRoom: String?,
    rooms: ObjectSet<String>,
    darkMode: Boolean
) = when {
    (priorRoom == null || !rooms.contains(priorRoom)) && newRoom != null && rooms.contains(newRoom) -> true
    else -> darkMode
}
