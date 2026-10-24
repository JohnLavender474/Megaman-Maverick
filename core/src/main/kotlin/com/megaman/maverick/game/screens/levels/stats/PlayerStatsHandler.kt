package com.megaman.maverick.game.screens.levels.stats

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.Queue
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.extensions.toOrderedSet
import com.maverick.game.engine.common.interfaces.Initializable
import com.maverick.game.engine.common.interfaces.Updatable
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.time.TimeMarkedRunnable
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.drawables.IDrawable
import com.maverick.game.engine.drawables.sprites.SpritesSystem
import com.maverick.game.engine.events.Event
import com.maverick.game.engine.players.PlayerSlot
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.drawables.ui.BitsBar
import com.megaman.maverick.game.entities.megaman.constants.MegaHealthTank
import com.megaman.maverick.game.entities.megaman.constants.MegaHeartTank
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.megaman.constants.MegamanWeapon
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.events.PlayerEventKeys
import com.megaman.maverick.game.players.MegaPlayer
import com.megaman.maverick.game.players.megaman

class PlayerStatsHandler(private val game: MegamanMaverickGame) : Initializable, Updatable, IDrawable<Batch> {

    companion object {
        const val TAG = "PlayerStatsHandler"
        private const val RUSH_JET = "rush_jet"
        private const val PRECIOUS = "precious"
        private const val AXE = "axe"
        private const val NEEDLE = "needle"
        private const val RAT = "rat"
        private const val REACTOR = "reactor"
        private const val SPECIAL_ITEM_DUR = 0.5f
    }

    private val state = game.state
    private val engine = game.engine
    private val assMan = game.assMan
    private val eventsMan = game.eventsMan
    private val audioMan = game.audioMan

    // TODO: the HUD acts on the lead player in slot 0; which player the HUD shows is a session policy (item 047)
    private val player: MegaPlayer
        get() = game.players.get(PlayerSlot(0))!!

    private val timerQueue = Queue<Timer>()

    private lateinit var healthBar: BitsBar
    private lateinit var weaponBarSupplier: () -> BitsBar?

    private var initialized = false

    val finished: Boolean
        get() = timerQueue.isEmpty

    override fun init(vararg params: Any) {
        if (initialized) return

        GameLogger.debug(TAG, "init()")

        healthBar = BitsBar(
            assMan,
            ConstKeys.STANDARD,
            ConstVals.HEALTH_BAR_X * ConstVals.PPM,
            ConstVals.STATS_BAR_Y * ConstVals.PPM,
            { player.currentHealth },
            { player.maxHealth })
        healthBar.init()

        val weaponBars = ObjectMap<MegamanWeapon, BitsBar>()
        MegamanWeapon.entries.forEach {
            if (it == MegamanWeapon.MEGA_BUSTER) return@forEach

            val bitSource = when (it) {
                MegamanWeapon.MOON_SCYTHES -> ConstKeys.MOON
                MegamanWeapon.INFERNAL_BARRAGE -> ConstKeys.FIRE
                MegamanWeapon.FRIGID_SHOT -> ConstKeys.ICE
                MegamanWeapon.RUSH_JET -> RUSH_JET
                MegamanWeapon.PRECIOUS_GUARD -> PRECIOUS
                MegamanWeapon.AXE_SWINGER -> AXE
                MegamanWeapon.NEEDLE_SPIN -> NEEDLE
                MegamanWeapon.RODENT_CLAWS -> RAT
                MegamanWeapon.REACTOR_SHOT -> REACTOR
                else -> throw IllegalStateException("No bit source for weapon $it")
            }

            val weaponBar = BitsBar(
                assMan,
                bitSource,
                ConstVals.WEAPON_BAR_X * ConstVals.PPM,
                ConstVals.STATS_BAR_Y * ConstVals.PPM,
                { player.megaman.ammo },
                { MegamanValues.MAX_WEAPON_AMMO }
            )

            weaponBar.init()
            weaponBars.put(it, weaponBar)
        }

        weaponBarSupplier = {
            val current = player.megaman.currentWeapon
            if (current == MegamanWeapon.MEGA_BUSTER) null else weaponBars.get(current)
        }
    }

    fun attain(heartTank: MegaHeartTank) {
        if (state.containsHeartTank(heartTank)) {
            GameLogger.error(TAG, "attain(): already has heart tank: $heartTank")
            audioMan.playSound(SoundAsset.ERROR_SOUND, false)
            return
        }

        if (player.maxHealth >= ConstVals.MAX_HEALTH) {
            GameLogger.error(TAG, "attain(): cannot attain heart because max health already reached: $heartTank")
            audioMan.playSound(SoundAsset.ERROR_SOUND, false)
            return
        }

        GameLogger.debug(TAG, "attain(): heartTank=$heartTank")

        val timer = Timer(SPECIAL_ITEM_DUR)
        timer
            .setRunOnFirstupdate {
                audioMan.playSound(SoundAsset.LIFE_SOUND, false)

                engine.systems.forEach { if (it !is SpritesSystem) it.on = false }
            }
            .setRunOnJustFinished {
                state.addHeartTank(heartTank)

                eventsMan.submitEvent(
                    Event(
                        EventType.ADD_PLAYER_HEALTH, props(
                            ConstKeys.VALUE pairTo MegaHeartTank.HEALTH_BUMP,
                            PlayerEventKeys.SLOT pairTo player.slot
                        )
                    )
                )

                engine.systems.forEach { it.on = true }
            }
        timerQueue.addLast(timer)
    }

    fun attain(healthTank: MegaHealthTank) {
        if (state.containsHealthTank(healthTank)) {
            GameLogger.error(TAG, "attain(): already has health tank: $healthTank")
            audioMan.playSound(SoundAsset.ERROR_SOUND, false)
            return
        }

        GameLogger.debug(TAG, "attain(): healthTank=$healthTank")

        val timer = Timer(SPECIAL_ITEM_DUR)
        timer
            .setRunOnFirstupdate {
                audioMan.playSound(SoundAsset.LIFE_SOUND, false)

                engine.systems.forEach { if (it !is SpritesSystem) it.on = false }
            }
            .setRunOnJustFinished {
                state.putHealthTank(healthTank, 0)

                engine.systems.forEach { it.on = true }
            }
        timerQueue.addLast(timer)
    }

    fun addWeaponEnergy(ammo: Int) {
        GameLogger.debug(TAG, "addWeaponEnergy(): ammo=$ammo")

        if (!MegamanWeapon.entries.any { canAddWeaponEnergyTo(it) }) {
            GameLogger.debug(TAG, "addWeaponEnergy(): no weapons to add energy to")
            return
        }

        val currentWeapon = player.megaman.currentWeapon
        var remainingAmmo = ammo

        if (currentWeapon != MegamanWeapon.MEGA_BUSTER) {
            remainingAmmo = addWeaponEnergyTo(currentWeapon, ammo)

            GameLogger.debug(
                TAG, "addWeaponEnergy(): " +
                    "added ammo to current weapon $currentWeapon: remainingAmmo=$remainingAmmo"
            )
        }

        val weapons = MegamanWeapon.entries.toOrderedSet()
        weapons.remove(MegamanWeapon.MEGA_BUSTER)
        weapons.remove(player.megaman.currentWeapon)

        for (weapon in weapons) {
            if (remainingAmmo <= 0) {
                GameLogger.debug(TAG, "addWeaponEnergy(): loop: remaining ammo depleted")
                break
            }

            if (!state.containsWeapon(weapon)) {
                GameLogger.debug(TAG, "addWeaponEnergy(): loop: megaman does not have weapon $weapon")
                continue
            }

            remainingAmmo = addWeaponEnergyTo(weapon, remainingAmmo)

            GameLogger.debug(
                TAG, "addWeaponEnergy(): loop: added ammo to weapon $weapon: remainingAmmo=$remainingAmmo"
            )
        }

        GameLogger.debug(TAG, "addWeaponEnergy(): finish processing: remainingAmmo=$remainingAmmo")

        audioMan.playSound(SoundAsset.ENERGY_FILL_SOUND, false)
    }

    private fun canAddWeaponEnergyTo(weapon: MegamanWeapon) =
        weapon != MegamanWeapon.MEGA_BUSTER &&
            state.containsWeapon(weapon) &&
            player.megaman.weaponsHandler.getAmmo(weapon) < MegamanValues.MAX_WEAPON_AMMO

    private fun addWeaponEnergyTo(weapon: MegamanWeapon, ammoToAdd: Int): Int {
        val currentAmmo = player.megaman.weaponsHandler.getAmmo(weapon)
        val maxAmmo = MegamanValues.MAX_WEAPON_AMMO

        GameLogger.debug(
            TAG,
            "addWeaponEnergyTo(): " +
                "weapon=$weapon, ammoToAdd=$ammoToAdd, currentAmmo=$currentAmmo, maxAmmo=$maxAmmo"
        )

        if (currentAmmo >= maxAmmo) {
            GameLogger.debug(TAG, "addWeaponEnergyTo(): current ammo is greater than max-allowed ammo")
            return ammoToAdd
        }

        val remaining = player.megaman.translateAmmo(ammoToAdd, weapon)
        GameLogger.debug(
            TAG, "addWeaponEnergyTo(): " +
                "weapon energy now: ${player.megaman.weaponsHandler.getAmmo(weapon)}, remaining=$remaining"
        )
        return remaining
    }

    fun addHealth(value: Int) {
        val healthMegamanNeeds = player.maxHealth - player.currentHealth

        GameLogger.debug(TAG, "addHealth(): healthMegamanNeeds=$healthMegamanNeeds, value=$value")

        val addToTanks: Boolean
        val healthToAddNow: Int

        when {
            value <= healthMegamanNeeds -> {
                GameLogger.debug(TAG, "addHealth(): value less than what Megaman needs, don't add any to health tanks")

                healthToAddNow = value
                addToTanks = false
            }
            else -> {
                GameLogger.debug(TAG, "addHealth(): value greater than what Megaman needs, try to add to health tanks")

                healthToAddNow = healthMegamanNeeds

                when {
                    state.hasAnyHealthTanks() -> {
                        val valueToAddToTanks = value - healthMegamanNeeds
                        val hasAddedToTanks = state.addToHealthTanks(valueToAddToTanks)
                        addToTanks = hasAddedToTanks
                        GameLogger.debug(TAG, "addHealth(): hasAddedToTanks=$hasAddedToTanks")
                    }

                    else -> {
                        GameLogger.debug(TAG, "addHealth(): megaman has no health tanks")
                        addToTanks = false
                    }
                }
            }
        }

        val timeMarkedRunnables = Array<TimeMarkedRunnable>()
        for (i in 0 until healthToAddNow) {
            val time = i * ConstVals.DUR_PER_BIT
            GameLogger.debug(TAG, "addHealth(): add runnable to timer: time=$time")

            timeMarkedRunnables.add(TimeMarkedRunnable(time) {
                GameLogger.debug(TAG, "addHealth(): timer: translate health by one")
                player.translateHealth(1)
                audioMan.playSound(SoundAsset.ENERGY_FILL_SOUND)
            })
        }

        val dur = healthToAddNow * ConstVals.DUR_PER_BIT
        val timer = Timer(dur, timeMarkedRunnables)

        if (addToTanks) timer.setRunOnFinished {
            GameLogger.debug(TAG, "addHealth(): timer: play life sound for adding health to tanks")
            audioMan.playSound(SoundAsset.ENERGY_FILL_SOUND)
        }

        timerQueue.addLast(timer)
        GameLogger.debug(TAG, "addHealth(): add timer to queue: queue.size=${timerQueue.size}")

        engine.systems.forEach { if (it !is SpritesSystem) it.on = false }
        GameLogger.debug(TAG, "addHealth(): shut off all systems expect sprites system")
    }

    override fun update(delta: Float) {
        if (timerQueue.isEmpty) return

        val timer = timerQueue.first()
        timer.update(delta)

        if (timer.isFinished()) {
            timerQueue.removeFirst()
            GameLogger.debug(TAG, "update(): timer just finished: pop timer from queue: queue.size=${timerQueue.size}")
        }

        if (timerQueue.isEmpty) {
            GameLogger.debug(TAG, "update(): timer queue just emptied: turn on all systems")
            engine.systems.forEach { it.on = true }
        }
    }

    override fun draw(drawer: Batch) {
        healthBar.draw(drawer)
        weaponBarSupplier()?.draw(drawer)
    }
}
