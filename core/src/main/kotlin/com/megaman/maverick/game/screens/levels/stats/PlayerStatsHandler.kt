package com.megaman.maverick.game.screens.levels.stats

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.Queue
import com.mega.game.engine.common.interfaces.Initializable
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.time.TimeMarkedRunnable
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.IDrawable
import com.mega.game.engine.drawables.sprites.SpritesSystem
import com.mega.game.engine.events.Event
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.drawables.ui.BitsBar
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.MegaHealthTank
import com.megaman.maverick.game.entities.megaman.constants.MegaHeartTank
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.megaman.constants.MegamanWeapon
import com.megaman.maverick.game.events.EventType

class PlayerStatsHandler(private val megaman: Megaman) : Initializable, Updatable, IDrawable<Batch> {

    companion object {
        private const val SPECIAL_ITEM_DUR = 0.35f
    }

    private val engine = megaman.game.engine
    private val assMan = megaman.game.assMan
    private val eventsMan = megaman.game.eventsMan
    private val audioMan = megaman.game.audioMan

    private val timerQueue = Queue<Timer>()

    private lateinit var healthBar: BitsBar
    private lateinit var weaponBarSupplier: () -> BitsBar?

    private var initialized = false

    val finished: Boolean
        get() = timerQueue.isEmpty

    override fun init() {
        if (initialized) return

        healthBar = BitsBar(
            assMan,
            "Bit",
            ConstVals.HEALTH_BAR_X * ConstVals.PPM,
            ConstVals.STATS_BAR_Y * ConstVals.PPM,
            { megaman.getHealthPoints().current },
            { megaman.getHealthPoints().max })
        healthBar.init()

        val weaponBars = ObjectMap<MegamanWeapon, BitsBar>()
        MegamanWeapon.entries.forEach {
            if (it == MegamanWeapon.BUSTER) return@forEach

            val bitSource = when (it) {
                MegamanWeapon.RUSH_JETPACK -> "RedBit"
                // TODO: MegamanWeapon.FLAME_TOSS -> "OrangeBit"
                else -> throw IllegalStateException("No bit source for weapon $it")
            }

            val weaponBar = BitsBar(
                assMan,
                bitSource,
                ConstVals.WEAPON_BAR_X * ConstVals.PPM,
                ConstVals.STATS_BAR_Y * ConstVals.PPM,
                { megaman.ammo },
                { MegamanValues.MAX_WEAPON_AMMO })

            weaponBar.init()
            weaponBars.put(it, weaponBar)
        }

        weaponBarSupplier = {
            val current = megaman.currentWeapon
            if (current == MegamanWeapon.BUSTER) null else weaponBars.get(current)
        }
    }

    fun attain(heartTank: MegaHeartTank) {
        if (megaman.has(heartTank) || megaman.getMaxHealth() == ConstVals.MAX_HEALTH) {
            audioMan.playSound(SoundAsset.ERROR_SOUND, false)
            return
        }

        val timer = Timer(SPECIAL_ITEM_DUR)
        timer
            .setRunOnFirstupdate {
                audioMan.playSound(SoundAsset.LIFE_SOUND, false)

                engine.systems.forEach { if (it !is SpritesSystem) it.on = false }
            }
            .setRunOnFinished {
                megaman.add(heartTank)

                eventsMan.submitEvent(
                    Event(EventType.ADD_PLAYER_HEALTH, props(ConstKeys.VALUE pairTo MegaHeartTank.HEALTH_BUMP))
                )

                engine.systems.forEach { it.on = true }
            }
        timerQueue.addLast(timer)
    }

    fun attain(healthTank: MegaHealthTank) {
        check(finished) { "Cannot call attain if handler is not finished" }
        if (megaman.has(healthTank)) return
        /*
        timer = Timer(SPECIAL_ITEM_DUR)
        timer!!.runOnFinished = { megaman.put(healthTank) }
         */
    }

    fun addHealth(health: Int) {
        val healthNeeded = megaman.getHealthPoints().max - megaman.getHealthPoints().current
        if (healthNeeded <= 0) return

        val addToTanks: Boolean
        val healthToAdd: Int
        if (healthNeeded >= health) {
            healthToAdd = health
            addToTanks = false
        } else {
            healthToAdd = healthNeeded
            addToTanks = if (megaman.hasHealthTanks()) megaman.addToHealthTank(health) else false
        }

        var dur = healthToAdd * ConstVals.DUR_PER_BIT
        if (addToTanks) dur += ConstVals.DUR_PER_BIT

        val timeMarkedRunnables = Array<TimeMarkedRunnable>()
        for (i in 0 until healthToAdd) {
            val time = i * ConstVals.DUR_PER_BIT
            timeMarkedRunnables.add(TimeMarkedRunnable(time) {
                megaman.translateHealth(1)
                audioMan.playSound(SoundAsset.ENERGY_FILL_SOUND)
            })
        }
        val timer = Timer(dur, timeMarkedRunnables)
        if (addToTanks) timer.setRunOnFinished { audioMan.playSound(SoundAsset.LIFE_SOUND) }
        timerQueue.addLast(timer)

        engine.systems.forEach { if (it !is SpritesSystem) it.on = false }
    }

    override fun update(delta: Float) {
        if (timerQueue.isEmpty) return

        val timer = timerQueue.first()
        timer.update(delta)

        if (timer.isJustFinished()) timerQueue.removeFirst()
        if (timerQueue.isEmpty) engine.systems.forEach { it.on = true }
    }

    override fun draw(drawer: Batch) {
        healthBar.draw(drawer)
        weaponBarSupplier()?.draw(drawer)
    }
}
