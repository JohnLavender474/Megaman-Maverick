package com.megaman.maverick.game.entities.megaman

import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.entities.megaman.constants.MegaAbility
import com.megaman.maverick.game.entities.megaman.constants.MegaArmorPiece
import com.megaman.maverick.game.entities.megaman.constants.MegaHealthTank
import com.megaman.maverick.game.entities.megaman.constants.MegaHeartTank

/**
 * An interface representing Megaman's inventory, which includes heart tanks, abilities, armor
 * pieces, and health tanks.
 */
interface IMegaUpgradable {

  /** The handler for Megaman's upgrades. */
  val upgradeHandler: MegamanUpgradeHandler

  /**
   * Checks if the inventory contains a specific heart tank.
   *
   * @param heartTank The heart tank to check for.
   * @return `true` if the heart tank is in the inventory, `false` otherwise.
   */
  fun has(heartTank: MegaHeartTank) = upgradeHandler.has(heartTank)

  /**
   * Checks if the inventory contains a specific ability.
   *
   * @param ability The ability to check for.
   * @return `true` if the ability is in the inventory, `false` otherwise.
   */
  fun has(ability: MegaAbility) = upgradeHandler.has(ability)

  /**
   * Checks if the inventory contains a specific armor piece.
   *
   * @param armorPiece The armor piece to check for.
   * @return `true` if the armor piece is in the inventory, `false` otherwise.
   */
  fun has(armorPiece: MegaArmorPiece) = upgradeHandler.has(armorPiece)

  /**
   * Adds a heart tank to the inventory and increases Megaman's maximum health if the heart tank is
   * not already in the inventory.
   *
   * @param heartTank The heart tank to add.
   */
  fun add(heartTank: MegaHeartTank) = upgradeHandler.add(heartTank)

  /**
   * Adds an ability to the inventory.
   *
   * @param ability The ability to add.
   */
  fun add(ability: MegaAbility) = upgradeHandler.add(ability)

  /**
   * Adds an armor piece to the inventory if it's not already in the inventory.
   *
   * @param armorPiece The armor piece to add.
   */
  fun add(armorPiece: MegaArmorPiece) = upgradeHandler.add(armorPiece)

  /**
   * Adds health to the inventory. The added health may be distributed among health tanks.
   *
   * @param health The amount of health to add.
   */
  fun addToHealthTank(health: Int) = upgradeHandler.add(health)

  /**
   * Puts a health tank into the inventory with a specified health value.
   *
   * @param healthTank The health tank to put into the inventory.
   */
  fun put(healthTank: MegaHealthTank) = upgradeHandler.put(healthTank)

  /**
   * Puts a health tank into the inventory with a specified health value.
   *
   * @param healthTank The health tank to put into the inventory.
   * @param health The health value for the tank.
   */
  fun put(healthTank: MegaHealthTank, health: Int) = upgradeHandler.put(healthTank, health)

  /**
   * Checks if the inventory contains a specific health tank.
   *
   * @param healthTank The health tank to check for.
   * @return `true` if the health tank is in the inventory, `false` otherwise.
   */
  fun has(healthTank: MegaHealthTank) = upgradeHandler.has(healthTank)

  /**
   * Gets the health value associated with a specific health tank.
   *
   * @param healthTank The health tank to retrieve the health value for.
   * @return The health value of the specified health tank, or 0 if the tank is not in the
   *   inventory.
   */
  operator fun get(healthTank: MegaHealthTank) = upgradeHandler[healthTank]
}

/**
 * Handler for Megaman's upgrades.
 *
 * @param megaman The Megaman that this handler belongs to.
 */
class MegamanUpgradeHandler(private val megaman: Megaman) {

  private val abilities = ObjectSet<MegaAbility>()
  private val heartTanks = ObjectSet<MegaHeartTank>()
  private val armorPieces = ObjectSet<MegaArmorPiece>()
  private val healthTanks = ObjectMap<MegaHealthTank, Int>()

  /**
   * Checks if the inventory contains a specific heart tank.
   *
   * @param heartTank The heart tank to check for.
   * @return `true` if the heart tank is in the inventory, `false` otherwise.
   */
  fun has(heartTank: MegaHeartTank) = heartTanks.contains(heartTank)

  /**
   * Checks if the inventory contains a specific ability.
   *
   * @param ability The ability to check for.
   * @return `true` if the ability is in the inventory, `false` otherwise.
   */
  fun has(ability: MegaAbility) = abilities.contains(ability)

  /**
   * Checks if the inventory contains a specific armor piece.
   *
   * @param armorPiece The armor piece to check for.
   * @return `true` if the armor piece is in the inventory, `false` otherwise.
   */
  fun has(armorPiece: MegaArmorPiece) = armorPieces.contains(armorPiece)

  /**
   * Adds a heart tank to the inventory and increases Megaman's maximum health if the heart tank is
   * not already in the inventory.
   *
   * @param heartTank The heart tank to add.
   */
  fun add(heartTank: MegaHeartTank) {
    if (has(heartTank)) return

    heartTanks.add(heartTank)
    megaman.getHealthPoints().max += MegaHeartTank.HEALTH_BUMP
  }

  /**
   * Adds an ability to the inventory.
   *
   * @param ability The ability to add.
   */
  fun add(ability: MegaAbility) = abilities.add(ability)

  /**
   * Adds an armor piece to the inventory if it's not already in the inventory.
   *
   * @param armorPiece The armor piece to add.
   */
  fun add(armorPiece: MegaArmorPiece) {
    if (has(armorPiece)) return
    armorPieces.add(armorPiece)

    // TODO: event on add armor piece
  }

  /**
   * Adds health to the inventory. The added health may be distributed among health tanks.
   *
   * @param health The amount of health to add.
   */
  fun add(health: Int): Boolean {
    check(health >= 0) { "Cannot add negative amount of health" }
    if (health == 0 || megaman.getHealthPoints().current == megaman.getHealthPoints().max)
        return false

    var _health = health

    healthTanks.entries().forEach { e ->
      val t = ConstVals.MAX_HEALTH - e.value
      if (t >= _health) e.value += _health
      else {
        e.value += t
        _health -= t
      }
    }

    return true
  }

  /**
   * Puts a health tank into the inventory with a specified health value.
   *
   * @param healthTank The health tank to put into the inventory.
   */
  fun put(healthTank: MegaHealthTank) = put(healthTank, 0)

  /**
   * Puts a health tank into the inventory with a specified health value.
   *
   * @param healthTank The health tank to put into the inventory.
   * @param health The health value for the tank.
   */
  fun put(healthTank: MegaHealthTank, health: Int) {
    var _health = health
    if (_health > ConstVals.MAX_HEALTH) _health = ConstVals.MAX_HEALTH
    else if (health < 0) _health = 0

    healthTanks.put(healthTank, _health)
  }

  /**
   * Checks if the inventory contains a specific health tank.
   *
   * @param healthTank The health tank to check for.
   * @return `true` if the health tank is in the inventory, `false` otherwise.
   */
  fun has(healthTank: MegaHealthTank) = healthTanks.containsKey(healthTank)

  /**
   * Gets the health value associated with a specific health tank.
   *
   * @param healthTank The health tank to retrieve the health value for.
   * @return The health value of the specified health tank, or 0 if the tank is not in the
   *   inventory.
   */
  operator fun get(healthTank: MegaHealthTank): Int =
      if (!has(healthTank)) 0 else healthTanks[healthTank]
}
