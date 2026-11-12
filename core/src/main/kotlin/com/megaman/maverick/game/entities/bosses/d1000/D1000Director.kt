package com.megaman.maverick.game.entities.bosses.d1000

import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.maverick.game.engine.common.extensions.objectMapOf
import com.maverick.game.engine.common.extensions.objectSetOf
import com.maverick.game.engine.common.extensions.orderedMapOf
import com.maverick.game.engine.common.objects.pairTo

// TODO: the D-1000 fight (its behaviors, mechanics, tuning and anti-trap rules) was AI-generated from owner play tests; it needs more scrutiny and refinement (see the branch NOTES of 081f, section "AI-generated fight")
enum class D1000Tier { NORMAL, OVERDRIVE }

enum class D1000Distance { NEAR, MID, FAR }

enum class D1000Action(val normal: Int, val overdrive: Int, val family: D1000Tell) {
    WALK(10, 5, D1000Tell.OFF),
    CROUCH(0, 0, D1000Tell.SLAM),
    SUMMON(14, 8, D1000Tell.EYE),
    RAM(22, 18, D1000Tell.RAM),
    BOMB(0, 12, D1000Tell.BOMB),
    BRACKET(19, 5, D1000Tell.BOMB),
    SWEEP(0, 16, D1000Tell.BOMB),
    FENCE(19, 7, D1000Tell.BOMB),
    CEILING(12, 12, D1000Tell.SLAM),
    WALL(0, 0, D1000Tell.BOMB),
    SPREAD(8, 10, D1000Tell.EYE),
    DIVE(0, 0, D1000Tell.EYE),
    FLANK(8, 10, D1000Tell.EYE);

    fun weight(tier: D1000Tier) = if (tier == D1000Tier.NORMAL) normal else overdrive
}

enum class D1000Cue(val active: (D1000Context) -> Boolean) {
    FAR({ it.distance == D1000Distance.FAR }),
    NEAR({ it.distance == D1000Distance.NEAR }),
    ON_WALL({ it.playerOnWall }),
    ABOVE({ it.playerAbove }),
    AIRBORNE({ it.playerAirborne })
}

class D1000CueRule(
    val bonus: ObjectMap<D1000Action, Int> = objectMapOf(),
    val excluded: ObjectSet<D1000Action> = objectSetOf()
)

data class D1000Context(
    val r: Int,
    val tier: D1000Tier,
    val last: D1000Action?,
    val beforeLast: D1000Action?,
    val distance: D1000Distance,
    val playerOnWall: Boolean,
    val playerAbove: Boolean,
    val playerNearEye: Boolean,
    val cyclesSinceCombo: Int,
    val hard: Boolean,
    val playerAirborne: Boolean = false,
    val refused: Set<D1000Action> = emptySet()
)

data class D1000Plan(val action: D1000Action, val followUp: D1000Action? = null)

object D1000Director {

    const val ROLLS = 1000
    const val DIVE_WEIGHT = 43
    const val WALL_WEIGHT = 25

    private val CUES = orderedMapOf(
        D1000Cue.FAR pairTo D1000CueRule(
            bonus = objectMapOf(D1000Action.RAM pairTo 10, D1000Action.WALK pairTo 5, D1000Action.BOMB pairTo 5)
        ),
        D1000Cue.NEAR pairTo D1000CueRule(
            bonus = objectMapOf(D1000Action.BRACKET pairTo 10),
            excluded = objectSetOf(D1000Action.WALK)
        ),
        D1000Cue.ON_WALL pairTo D1000CueRule(
            bonus = objectMapOf(D1000Action.RAM pairTo 15, D1000Action.BRACKET pairTo 10),
            excluded = objectSetOf(
                D1000Action.WALK, D1000Action.FENCE, D1000Action.SWEEP, D1000Action.CEILING, D1000Action.FLANK
            )
        ),
        D1000Cue.ABOVE pairTo D1000CueRule(
            bonus = objectMapOf(D1000Action.CEILING pairTo 15, D1000Action.SPREAD pairTo 10)
        ),
        D1000Cue.AIRBORNE pairTo D1000CueRule(bonus = objectMapOf(D1000Action.WALL pairTo WALL_WEIGHT))
    )

    private val CUE_ONLY = objectSetOf(D1000Action.WALL)

    private val BIG_MOVES = objectSetOf(D1000Action.FENCE, D1000Action.CEILING)
    private val SLAMS = objectSetOf(D1000Action.CROUCH, D1000Action.CEILING)
    private val ORBS = objectSetOf(
        D1000Action.BOMB, D1000Action.BRACKET, D1000Action.SWEEP, D1000Action.FENCE, D1000Action.WALL
    )

    fun guided(action: D1000Action?) = action != null && ORBS.contains(action)

    fun comboGap(tier: D1000Tier, hard: Boolean) = when {
        hard -> if (tier == D1000Tier.NORMAL) 3 else 2
        else -> if (tier == D1000Tier.NORMAL) 4 else 3
    }

    fun comboOf(action: D1000Action): D1000Action? = when (action) {
        D1000Action.RAM -> D1000Action.CROUCH
        D1000Action.SUMMON -> D1000Action.RAM
        D1000Action.CROUCH -> D1000Action.BRACKET
        D1000Action.FENCE -> D1000Action.RAM
        D1000Action.SWEEP -> D1000Action.RAM
        else -> null
    }

    fun choose(context: D1000Context): D1000Plan? {
        val slam = context.distance == D1000Distance.NEAR && context.last != D1000Action.CROUCH &&
            !context.refused.contains(D1000Action.CROUCH)
        val action = if (slam) D1000Action.CROUCH
        else pick(context.copy(refused = emptySet()))?.takeUnless { context.refused.contains(it) } ?: pick(context) ?: return null
        val followUp = if (context.cyclesSinceCombo >= comboGap(context.tier, context.hard)) comboOf(action) else null
        return D1000Plan(action, followUp)
    }

    fun weight(action: D1000Action, context: D1000Context): Int {
        if (context.refused.contains(action)) return 0
        if (action == context.last && action == context.beforeLast) return 0
        if (action == context.last && BIG_MOVES.contains(action)) return 0
        if (ORBS.contains(action) && context.last?.let { ORBS.contains(it) } == true) return 0
        if (action == D1000Action.SWEEP && !context.hard && context.last?.let { SLAMS.contains(it) } == true) return 0
        if (action == D1000Action.DIVE) return if (context.playerNearEye) DIVE_WEIGHT else 0
        var weight = action.weight(context.tier)
        if (weight == 0 && !CUE_ONLY.contains(action)) return 0
        for (entry in CUES) {
            if (!entry.key.active(context)) continue
            if (entry.value.excluded.contains(action)) return 0
            weight += entry.value.bonus.get(action, 0)
        }
        if (action == context.last) weight /= 2
        return weight
    }

    private fun pick(context: D1000Context): D1000Action? {
        val total = D1000Action.entries.sumOf { weight(it, context) }
        if (total <= 0) {
            if (context.refused.isNotEmpty()) return null
            throw IllegalStateException("D-1000 has no action for $context: check the weights")
        }
        val target = context.r.coerceIn(0, ROLLS - 1) * total / ROLLS
        var sum = 0
        for (action in D1000Action.entries) {
            sum += weight(action, context)
            if (target < sum) return action
        }
        throw IllegalStateException("D-1000 roll $target is outside the weight total $total")
    }
}
