package com.megaman.maverick.game.entities.megaman.handlers

import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.interfaces.Resettable
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.special.Rush
import com.megaman.maverick.game.weapons.WeaponDef

enum class RushPhase { ABSENT, ARRIVING, JOINING, JOINED, LEAVING }

interface RushHandle {

    fun arrive(owner: Megaman, weapon: WeaponDef<Megaman>)

    fun leave()

    fun leaveFromBack(owner: Megaman)

    fun vanish()
}

class MegamanRushHandler(
    private val megaman: Megaman,
    private val newRush: () -> RushHandle = { MegaEntityFactory.fetch(Rush::class)!! }
) : Resettable {

    companion object {
        const val TAG = "MegamanRushHandler"
    }

    var phase = RushPhase.ABSENT
        private set
    val joined: Boolean
        get() = phase == RushPhase.JOINED

    private var rush: RushHandle? = null

    fun summon(weapon: WeaponDef<Megaman>): Boolean {
        if (!weapon.summonsRush) return false
        if (megaman.weaponsHandler.isDepleted(weapon)) {
            GameLogger.debug(TAG, "summon(): do not summon: $weapon is depleted")
            return false
        }
        // TODO: call Rush in a turned room, where the gravity is not UP (item 190)
        if (megaman.direction != Direction.UP) {
            GameLogger.debug(TAG, "summon(): do not summon: direction=${megaman.direction}")
            return false
        }
        val rush = newRush()
        this.rush = rush
        phase = RushPhase.ARRIVING
        rush.arrive(megaman, weapon)
        GameLogger.debug(TAG, "summon(): weapon=$weapon")
        return true
    }

    fun dismiss() {
        GameLogger.debug(TAG, "dismiss(): phase=$phase")
        when (phase) {
            RushPhase.ARRIVING, RushPhase.JOINING -> {
                phase = RushPhase.LEAVING
                rush!!.leave()
            }

            RushPhase.JOINED -> {
                val rush = newRush()
                this.rush = rush
                phase = RushPhase.LEAVING
                rush.leaveFromBack(megaman)
            }

            RushPhase.ABSENT, RushPhase.LEAVING -> {}
        }
    }

    fun onJoinStarted(rush: RushHandle): Boolean {
        if (rush !== this.rush || phase != RushPhase.ARRIVING) {
            GameLogger.debug(TAG, "onJoinStarted(): ignore: phase=$phase, current=${rush === this.rush}")
            return false
        }
        phase = RushPhase.JOINING
        return true
    }

    fun onJoined(rush: RushHandle) {
        if (rush !== this.rush || phase != RushPhase.JOINING) {
            GameLogger.debug(TAG, "onJoined(): ignore: phase=$phase, current=${rush === this.rush}")
            return
        }
        this.rush = null
        phase = RushPhase.JOINED
    }

    fun onRushGone(rush: RushHandle) {
        GameLogger.debug(TAG, "onRushGone(): phase=$phase")
        this.rush = null
        phase = RushPhase.ABSENT
    }

    override fun reset() {
        val rush = this.rush
        this.rush = null
        phase = RushPhase.ABSENT
        rush?.vanish()
    }
}
