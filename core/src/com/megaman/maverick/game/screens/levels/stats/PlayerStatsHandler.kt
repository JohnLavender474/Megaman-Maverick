package com.megaman.maverick.game.screens.levels.stats

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.engine.common.interfaces.Initializable
import com.engine.common.interfaces.Updatable
import com.engine.common.objects.props
import com.engine.common.time.TimeMarkedRunnable
import com.engine.common.time.Timer
import com.engine.drawables.IDrawable
import com.engine.drawables.sprites.SpriteSystem
import com.engine.events.Event
import com.engine.systems.IGameSystem
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.drawables.sprites.BitsBar
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.*
import com.megaman.maverick.game.events.EventType

/**
 * A class that handles the player's stats, such as health, weapon ammo, and so on. It implements
 * [Initializable], [Updatable], and [IDrawable].
 */
class PlayerStatsHandler(private val megaman: Megaman) :
    Initializable, Updatable, IDrawable<Batch> {

  companion object {
    private const val BAR_X: Float = .4f * ConstVals.PPM
    private const val BAR_Y: Float = 9f * ConstVals.PPM
    private const val SPECIAL_ITEM_DUR = .35f
    private const val DUR_PER_BIT = .1f
  }

  private val engine = megaman.game.gameEngine
  private val assMan = megaman.game.assMan
  private val eventsMan = megaman.game.eventsMan
  private val audioMan = (megaman.game as MegamanMaverickGame).audioMan

  private lateinit var healthBar: BitsBar
  private lateinit var weaponBarSupplier: () -> BitsBar?

  private var timer: Timer? = null
  private var initialized = false
  private var systemStates: ObjectMap<IGameSystem, Boolean>? = null

  val finished: Boolean
    get() = timer?.isFinished() ?: false

  /** Initializes this handler. Must be called before [update] and [draw]. */
  override fun init() {
    if (initialized) return

    healthBar =
        BitsBar(
            assMan,
            "Bit",
            BAR_X,
            BAR_Y,
            { megaman.getHealthPoints().current },
            { megaman.getHealthPoints().max })
    healthBar.init()

    val weaponBars = ObjectMap<MegamanWeapon, BitsBar>()
    MegamanWeapon.values().forEach {
      if (it == MegamanWeapon.BUSTER) return@forEach

      val bitSource =
          when (it) {
            MegamanWeapon.FLAME_TOSS -> "RedBit"
            else -> throw IllegalStateException("No bit source for weapon $it")
          }

      val weaponBar =
          BitsBar(
              assMan,
              bitSource,
              BAR_X + ConstVals.PPM,
              BAR_Y,
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

  /**
   * Should be called when megaman attains the given armor piece.
   *
   * @param armorPiece the armor piece
   */
  fun attain(armorPiece: MegaArmorPiece) {
    if (!finished) throw IllegalStateException("Cannot call attain if handler is not finished")

    if (megaman.has(armorPiece)) return

    audioMan.playMusic(SoundAsset.LIFE_SOUND, false)
    timer = Timer(SPECIAL_ITEM_DUR)
    timer?.runOnFinished = { megaman.add(armorPiece) }
  }

  /**
   * Shuld be called when megaman attains the given heart tank.
   *
   * @param heartTank the heart tank
   */
  fun attain(heartTank: MegaHeartTank) {
    check(finished) { "Cannot call attain if handler is not finished" }

    if (megaman.has(heartTank)) return

    audioMan.playMusic(SoundAsset.LIFE_SOUND)
    timer = Timer(SPECIAL_ITEM_DUR)
    timer!!.runOnFinished = {
      megaman.add(heartTank)
      eventsMan.submitEvent(
          Event(EventType.ADD_PLAYER_HEALTH, props(ConstKeys.VALUE to MegaHeartTank.HEALTH_BUMP)))
    }

    systemStates = ObjectMap()
    engine.systems.forEach { systemStates!!.put(it, it.on) }
    engine.systems.forEach { if (it !is SpriteSystem) it.on = false }
  }

  /**
   * Should be called when megaman attains the given health tank.
   *
   * @param healthTank the health tank
   */
  fun attain(healthTank: MegaHealthTank) {
    check(finished) { "Cannot call attain if handler is not finished" }

    if (megaman.has(healthTank)) return

    timer = Timer(SPECIAL_ITEM_DUR)
    timer!!.runOnFinished = { megaman.put(healthTank) }
  }

  /**
   * Should be called when health is added to Megaman. This method begins by adding health to
   * Megaman, and any leftover health is added to the health tanks.
   *
   * @param health the amount of health to add
   */
  fun addHealth(health: Int) {
    check(finished) { "Cannot call add health if handler is not finished" }

    val healthNeeded: Int = megaman.getHealthPoints().max - megaman.getHealthPoints().current
    if (healthNeeded <= 0) return

    val addToTanks: Boolean
    val healthToAdd: Int
    if (healthNeeded >= health) {
      healthToAdd = health
      addToTanks = false
    } else {
      healthToAdd = healthNeeded
      addToTanks = megaman.addToHealthTank(health)
    }

    var dur = healthToAdd * DUR_PER_BIT
    if (addToTanks) dur += DUR_PER_BIT

    val timeMarkedRunnables = Array<TimeMarkedRunnable>()
    for (i in 0 until healthToAdd) {
      val time = i * DUR_PER_BIT
      timeMarkedRunnables.add(
          TimeMarkedRunnable(time) {
            megaman.addHealth(1)
            audioMan.playSound(SoundAsset.ENERGY_FILL_SOUND)
          })
    }
    timer = Timer(dur, timeMarkedRunnables)
    if (addToTanks) timer?.runOnFinished = { audioMan.playSound(SoundAsset.LIFE_SOUND) }

    systemStates = ObjectMap()
    engine.systems.forEach { systemStates!!.put(it, it.on) }
    engine.systems.forEach { if (it !is SpriteSystem) it.on = false }
  }

  /**
   * Updates the player stats handler.
   *
   * @param delta the time since the last update
   */
  override fun update(delta: Float) {
    timer?.let {
      it.update(delta)
      if (it.justFinished) {
        timer = null
        systemStates?.forEach { e -> e.key.on = e.value }
        systemStates = null
      }
    }
  }

  /**
   * Draws the player stats handler.
   *
   * @param drawer the batch to draw with
   */
  override fun draw(drawer: Batch) {
    healthBar.draw(drawer)
    weaponBarSupplier()?.draw(drawer)
  }
}
