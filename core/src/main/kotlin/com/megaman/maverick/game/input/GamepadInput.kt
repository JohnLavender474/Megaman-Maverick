package com.megaman.maverick.game.input

import com.badlogic.gdx.controllers.Controller
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.common.GameLogger
import com.megaman.maverick.game.controllers.ControllerUtils
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.controllers.getMapping
import com.megaman.maverick.game.players.PlayerSettings
import com.megaman.maverick.game.players.PlayerSettingsStore
import com.megaman.maverick.game.players.unbindClashingDefaults

enum class PadShare { ALL, ALL_BUT_LAST, LAST_ONLY }

class GamepadInput(
    private val settings: PlayerSettings,
    private val store: PlayerSettingsStore,
    private val share: PadShare = PadShare.ALL
) {

    companion object {
        const val TAG = "GamepadInput"
        const val TRIGGER_LEFT_AXIS = 4
        const val TRIGGER_RIGHT_AXIS = 5
        const val TRIGGER_LEFT_CODE = 1004
        const val TRIGGER_RIGHT_CODE = 1005
        const val TRIGGER_THRESHOLD = 0.5f
        private const val STICK_THRESHOLD = 0.5f
    }

    var loadPrefsOnConnect = false

    private var primary: Controller? = null
    private val padsOfTick = Array<Controller>()
    private var noPadReported = false

    fun held(): Long {
        val pads = ControllerUtils.getControllers()
        // TODO: the split is read each frame, so a pad that connects or disconnects during play moves the other pad to
        //  the other player; the device claim in item 047 (branch 047f) replaces this rule
        val start = if (share == PadShare.LAST_ONLY) maxOf(0, pads.size - 1) else 0
        val count = when (share) {
            PadShare.ALL -> pads.size
            PadShare.ALL_BUT_LAST -> maxOf(0, pads.size - 1)
            PadShare.LAST_ONLY -> minOf(1, pads.size)
        }
        val first = if (count > 0) pads[start] else null

        if (share == PadShare.LAST_ONLY) {
            if (first == null && !noPadReported) GameLogger.error(TAG, "held(): no pad for this player")
            noPadReported = first == null
        }

        if (first !== primary) {
            if (first != null) {
                val defaulted = ArrayList<MegaControllerButton>()
                MegaControllerButton.entries.forEach {
                    var code: Int? = null
                    if (loadPrefsOnConnect) code = store.readGamepadCode(first.name, it)
                    if (code == null) {
                        code = first.mapping.getMapping(it)
                        defaulted.add(it)
                    }
                    settings.padCodes[it.index] = code
                }
                if (loadPrefsOnConnect) unbindClashingDefaults(settings.padCodes, defaulted)
            } else settings.padCodes.fill(PlayerSettings.NO_CODE)
            primary = first
        }

        padsOfTick.clear()
        padsOfTick.addAll(pads, start, count)

        var held = 0L
        val codes = settings.padCodes
        for (p in 0 until padsOfTick.size) {
            val pad = padsOfTick[p]
            for (i in codes.indices) {
                val code = codes[i]
                if (code == PlayerSettings.NO_CODE) continue
                val pressed = when (code) {
                    TRIGGER_LEFT_CODE -> pad.getAxis(TRIGGER_LEFT_AXIS) >= TRIGGER_THRESHOLD
                    TRIGGER_RIGHT_CODE -> pad.getAxis(TRIGGER_RIGHT_AXIS) >= TRIGGER_THRESHOLD
                    else -> pad.getButton(code)
                }
                if (pressed) held = held or (1L shl i)
            }

            val mapping = pad.mapping
            val x = pad.getAxis(mapping.axisLeftX)
            if (x <= -STICK_THRESHOLD) held = held or (1L shl MegaControllerButton.LEFT.index)
            else if (x >= STICK_THRESHOLD) held = held or (1L shl MegaControllerButton.RIGHT.index)
            val y = pad.getAxis(mapping.axisLeftY)
            if (y <= -STICK_THRESHOLD) held = held or (1L shl MegaControllerButton.UP.index)
            else if (y >= STICK_THRESHOLD) held = held or (1L shl MegaControllerButton.DOWN.index)
        }
        return held
    }
}
