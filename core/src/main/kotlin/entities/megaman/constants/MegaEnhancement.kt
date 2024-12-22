package com.megaman.maverick.game.entities.megaman.constants

import com.megaman.maverick.game.entities.megaman.constants.MegaEnhancement.Companion.BOSS_DAMAGE_INCREASE_SCALAR
import com.megaman.maverick.game.entities.megaman.constants.MegaEnhancement.Companion.ENEMY_DAMAGE_INCREASE_SCALAR
import com.megaman.maverick.game.entities.megaman.constants.MegaEnhancement.Companion.MEGAMAN_DAMAGE_INCREASE_SCALAR
import kotlin.math.ceil


enum class MegaEnhancement {
    /**
     * When [DAMAGE_INCREASE] is equipped, the damaged applied to enemies and bosses from Megaman's weapons will be
     * scaled by [ENEMY_DAMAGE_INCREASE_SCALAR] and [BOSS_DAMAGE_INCREASE_SCALAR] respectively. Additionally, the
     * damage applied to Megaman will be scaled by [MEGAMAN_DAMAGE_INCREASE_SCALAR].
     */
    DAMAGE_INCREASE,

    /**
     * Megaman is able to jump higher with this enhancement. His jump velocity is scaled by [JUMP_BOOST_SCALAR].
     */
    JUMP_BOOST,

    /**
     * Megaman can ground slide faster.
     */
    GROUND_SLIDE_BOOST,

    /**
     * Megaman can air dash faster.
     */
    AIR_DASH_BOOST,

    /**
     * Megaman's buster charges up faster.
     */
    FASTER_BUSTER_CHARGING;

    companion object {
        const val ENEMY_DAMAGE_INCREASE_SCALAR = 2f
        const val BOSS_DAMAGE_INCREASE_SCALAR = 1.5f
        const val MEGAMAN_DAMAGE_INCREASE_SCALAR = 2f

        fun scaleDamage(damage: Int, scalar: Float) = ceil(damage * scalar).toInt()

        const val JUMP_BOOST_SCALAR = 1.25f
        const val GROUND_SLIDE_BOOST_SCALAR = 1.5f
        const val AIR_DASH_BOOST_SCALAR = 1.5f

        const val FASTER_BUSTER_CHARGING_SCALAR = 1.5f
    }
}
