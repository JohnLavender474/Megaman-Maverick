package com.megaman.maverick.game.entities.megaman.handlers

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.interfaces.Resettable
import com.maverick.game.engine.common.interfaces.Updatable
import com.maverick.game.engine.entities.isAnyBehaviorActive
import com.maverick.game.engine.entities.isBehaviorActive
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.components.GROUND_SLIDE_SPRITE_OFFSET_Y
import com.megaman.maverick.game.entities.megaman.constants.MegaChargeStatus
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.megaman.weapons.MegamanWeapons
import com.megaman.maverick.game.entities.state.frozen
import com.megaman.maverick.game.entities.state.inPlay
import com.megaman.maverick.game.entities.state.running
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.weapons.WeaponDef
import com.megaman.maverick.game.weapons.WeaponState
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.getCenter
import com.megaman.maverick.game.world.body.isSensing

class MegamanWeaponsHandler(private val megaman: Megaman) : Updatable, Resettable {

    companion object {
        const val TAG = "MegamanWeaponsHandler"
    }

    private val services: MegaServices
        get() = megaman.services

    private val states = ObjectMap<WeaponDef, WeaponState>()

    init {
        MegamanWeapons.all.forEach { weapon -> states.put(weapon, weapon.newState(megaman)) }
    }

    override fun reset() {
        states.values().forEach { state ->
            state.clearAllSpawned()
            state.cooldown.setToEnd()
        }

        GameLogger.debug(TAG, "reset()")
    }

    override fun update(delta: Float) = states.values().forEach { state -> state.update(delta) }

    fun stateOf(weapon: WeaponDef): WeaponState = states[weapon]

    fun getSpawnPosition(): Vector2 {
        val out = GameObjectPools.fetch(Vector2::class).set(megaman.body.getCenter())

        when (megaman.direction) {
            Direction.UP -> {
                val xOffset = when {
                    megaman.isBehaviorActive(BehaviorType.AIR_DASHING) -> 1f
                    megaman.isBehaviorActive(BehaviorType.WALL_SLIDING) -> 0.75f
                    megaman.isBehaviorActive(BehaviorType.GROUND_SLIDING) -> 0.5f
                    megaman.isBehaviorActive(BehaviorType.CROUCHING) -> 0.75f
                    !megaman.body.isSensing(BodySense.FEET_ON_GROUND) -> 1f
                    megaman.slipSliding -> 0.75f
                    megaman.running -> 1.75f
                    else -> 1f
                }

                val yOffset = when {
                    megaman.isBehaviorActive(BehaviorType.AIR_DASHING) -> 0f
                    megaman.isBehaviorActive(BehaviorType.WALL_SLIDING) -> 0.25f
                    megaman.isBehaviorActive(BehaviorType.JETPACKING) -> 0.1f
                    megaman.isBehaviorActive(BehaviorType.GROUND_SLIDING) -> 0.15f
                    megaman.isBehaviorActive(BehaviorType.CROUCHING) -> 0.15f
                    megaman.isBehaviorActive(BehaviorType.CLIMBING) -> 0.15f
                    !megaman.body.isSensing(BodySense.FEET_ON_GROUND) -> -0.05f
                    else -> 0f
                }

                out.x += xOffset * ConstVals.PPM * megaman.facing.value
                out.y += yOffset * ConstVals.PPM

                if (megaman.isBehaviorActive(BehaviorType.GROUND_SLIDING))
                    out.y -= GROUND_SLIDE_SPRITE_OFFSET_Y * ConstVals.PPM
            }

            Direction.DOWN -> {
                val xOffset = when {
                    megaman.isBehaviorActive(BehaviorType.AIR_DASHING) -> 1f
                    megaman.isBehaviorActive(BehaviorType.WALL_SLIDING) -> 0.75f
                    megaman.isBehaviorActive(BehaviorType.GROUND_SLIDING) -> 0.5f
                    megaman.isBehaviorActive(BehaviorType.CROUCHING) -> 0.75f
                    !megaman.body.isSensing(BodySense.FEET_ON_GROUND) -> 1f
                    megaman.slipSliding -> 0.75f
                    megaman.running -> 1.75f
                    else -> 1f
                }

                val yOffset = when {
                    megaman.isBehaviorActive(BehaviorType.AIR_DASHING) -> 0f
                    megaman.isBehaviorActive(BehaviorType.WALL_SLIDING) -> 0.25f
                    megaman.isBehaviorActive(BehaviorType.JETPACKING) -> 0.35f
                    megaman.isBehaviorActive(BehaviorType.GROUND_SLIDING) -> 0.15f
                    megaman.isBehaviorActive(BehaviorType.CROUCHING) -> 0.15f
                    megaman.isBehaviorActive(BehaviorType.CLIMBING) -> 0.15f
                    !megaman.body.isSensing(BodySense.FEET_ON_GROUND) -> 0.1f
                    else -> 0f
                }

                out.x += xOffset * ConstVals.PPM * -megaman.facing.value
                out.y -= yOffset * ConstVals.PPM

                if (megaman.isBehaviorActive(BehaviorType.GROUND_SLIDING))
                    out.y += GROUND_SLIDE_SPRITE_OFFSET_Y * ConstVals.PPM
            }

            Direction.LEFT -> {
                val yOffset = when {
                    megaman.isBehaviorActive(BehaviorType.AIR_DASHING) -> 1f
                    megaman.isBehaviorActive(BehaviorType.WALL_SLIDING) -> 0.75f
                    megaman.isBehaviorActive(BehaviorType.GROUND_SLIDING) -> 0.5f
                    megaman.isBehaviorActive(BehaviorType.CROUCHING) -> 0.75f
                    !megaman.body.isSensing(BodySense.FEET_ON_GROUND) -> 1f
                    megaman.slipSliding -> 0.75f
                    megaman.running -> 1.75f
                    else -> 1f
                }

                val xOffset = when {
                    megaman.isBehaviorActive(BehaviorType.AIR_DASHING) -> 0f
                    megaman.isBehaviorActive(BehaviorType.WALL_SLIDING) -> -0.25f
                    megaman.isBehaviorActive(BehaviorType.JETPACKING) -> 0.1f
                    megaman.isBehaviorActive(BehaviorType.GROUND_SLIDING) -> 0f
                    megaman.isBehaviorActive(BehaviorType.CROUCHING) -> -0.1f
                    megaman.isBehaviorActive(BehaviorType.CLIMBING) -> 0.15f
                    !megaman.body.isSensing(BodySense.FEET_ON_GROUND) ->
                        if (megaman.body.physics.velocity.x > 0f) -0.2f else -0.3f

                    else -> 0f
                }

                out.x += xOffset * ConstVals.PPM
                out.y += yOffset * ConstVals.PPM * megaman.facing.value

                if (megaman.isAnyBehaviorActive(BehaviorType.CROUCHING, BehaviorType.GROUND_SLIDING)) when {
                    megaman.isFacing(Facing.LEFT) -> out.y += 0.15f * ConstVals.PPM
                    else -> out.y += 0.2f * ConstVals.PPM
                }
            }

            Direction.RIGHT -> {
                val yOffset = when {
                    megaman.isBehaviorActive(BehaviorType.AIR_DASHING) -> 1f
                    megaman.isBehaviorActive(BehaviorType.WALL_SLIDING) -> 0.75f
                    megaman.isBehaviorActive(BehaviorType.GROUND_SLIDING) -> 0.5f
                    megaman.isBehaviorActive(BehaviorType.CROUCHING) -> 1f
                    !megaman.body.isSensing(BodySense.FEET_ON_GROUND) -> 1f
                    megaman.slipSliding -> 0.75f
                    megaman.running -> 1.75f
                    else -> 1f
                }

                val xOffset = when {
                    megaman.isBehaviorActive(BehaviorType.AIR_DASHING) -> 0f
                    megaman.isBehaviorActive(BehaviorType.WALL_SLIDING) -> 0.25f
                    megaman.isBehaviorActive(BehaviorType.JETPACKING) -> 0.1f
                    megaman.isBehaviorActive(BehaviorType.GROUND_SLIDING) -> 0f
                    megaman.isBehaviorActive(BehaviorType.CROUCHING) -> 0.1f
                    megaman.isBehaviorActive(BehaviorType.CLIMBING) -> 0.15f
                    !megaman.body.isSensing(BodySense.FEET_ON_GROUND) ->
                        if (megaman.body.physics.velocity.x < 0f) 0.05f else 0.2f

                    else -> 0f
                }

                out.x += xOffset * ConstVals.PPM
                out.y += yOffset * ConstVals.PPM * -megaman.facing.value

                if (megaman.isAnyBehaviorActive(BehaviorType.CROUCHING, BehaviorType.GROUND_SLIDING)) when {
                    megaman.isFacing(Facing.LEFT) -> out.y += 0.1f * ConstVals.PPM
                    else -> out.y += 0.25f * ConstVals.PPM
                }
            }
        }

        return out
    }

    fun hasWeapon(weapon: WeaponDef) = services.gameState.containsWeapon(weapon)

    fun onChangeWeapon(current: WeaponDef, previous: WeaponDef?) {
        GameLogger.debug(TAG, "onChangeWeapon(): current=$current, previous=$previous")

        megaman.shooting = false

        if (previous != null) previous.onDeselect(megaman, states[previous])
    }

    fun isChargeable(weapon: WeaponDef) = hasWeapon(weapon) && weapon.chargeable

    fun translateAmmo(weapon: WeaponDef, delta: Int): Int {
        if (!hasWeapon(weapon)) return delta

        val state = states[weapon]
        state.ammo += delta

        when {
            state.ammo > MegamanValues.MAX_WEAPON_AMMO -> {
                val diff = state.ammo - MegamanValues.MAX_WEAPON_AMMO
                state.ammo = MegamanValues.MAX_WEAPON_AMMO
                return diff
            }

            state.ammo < 0 -> {
                state.ammo = 0
                return 0
            }
        }

        return 0
    }

    fun setAllToMaxAmmo() = states.keys().forEach { setToMaxAmmo(it) }

    fun setToMaxAmmo(weapon: WeaponDef) {
        states[weapon]?.ammo = MegamanValues.MAX_WEAPON_AMMO
    }

    fun getAmmo(weapon: WeaponDef) = when {
        !hasWeapon(weapon) -> 0
        weapon.infiniteAmmo -> MegamanValues.MAX_WEAPON_AMMO
        else -> states[weapon].ammo
    }

    fun isAtMaxAmmo(weapon: WeaponDef) = getAmmo(weapon) >= MegamanValues.MAX_WEAPON_AMMO

    fun isDepleted(weapon: WeaponDef) = getAmmo(weapon) == 0

    fun canFireWeapon(weapon: WeaponDef, stat: MegaChargeStatus): Boolean {
        if (!megaman.inPlay) {
            GameLogger.debug(TAG, "canFireWeapon(): cannot fire $weapon: megaman is not ready")
            return false
        }
        if (!hasWeapon(weapon)) {
            GameLogger.debug(TAG, "canFireWeapon(): cannot fire $weapon: megaman doesn't have weapon")
            return false
        }
        if (megaman.frozen) {
            GameLogger.debug(TAG, "canFireWeapon(): cannot fire $weapon: megaman is frozen")
            return false
        }

        val state = states[weapon]
        if (!state.cooldown.isFinished()) {
            GameLogger.debug(TAG, "canFireWeapon(): cannot fire $weapon: cooldown is not finished")
            return false
        }
        if (!state.canFire(stat)) {
            GameLogger.debug(TAG, "canFireWeapon(): cannot fire $weapon: custom predicate failed")
            return false
        }

        return weapon.cost(state) <= state.ammo
    }

    fun fireWeapon(weapon: WeaponDef, statToTry: MegaChargeStatus): Boolean {
        GameLogger.debug(TAG, "fireWeapon(): weapon=$weapon, statToTry=$statToTry")

        var stat = statToTry

        if (!canFireWeapon(weapon, stat)) {
            GameLogger.debug(TAG, "fireWeapon(): cannot fire weapon")
            return false
        }

        if (!isChargeable(weapon)) {
            GameLogger.debug(TAG, "fireWeapon(): weapon not chargeable")
            stat = MegaChargeStatus.NOT_CHARGED
        }

        val state = states[weapon]
        val cost = weapon.cost(state)

        val ammo = getAmmo(weapon)
        if (cost > ammo) {
            GameLogger.debug(TAG, "fireWeapon(): ammo is less than cost: ammo=$ammo, cost=$cost")
            return false
        }

        state.cooldown.reset()
        weapon.fire(megaman, state, stat)
        translateAmmo(weapon, -cost)

        GameLogger.debug(TAG, "fireWeapon(): weapon fired: state=$state")
        return true
    }
}
