package com.maverick.game.engine.world.contacts

import com.maverick.game.engine.common.enums.ProcessState
import com.maverick.game.engine.world.body.FixtureTypeSet
import com.maverick.game.engine.world.body.IFixtureType

class ContactRulesBuilder(private val types: FixtureTypeSet) {

    private val size = types.size
    private val rules = Array(ProcessState.entries.size) { arrayOfNulls<IContactRule>(size * size) }
    private val flip = BooleanArray(size * size)
    private val ids = arrayOfNulls<String>(size * size)

    fun pair(id: String, a: IFixtureType, b: IFixtureType, rule: IContactRule, vararg phases: ProcessState) {
        require(types.owns(a) && types.owns(b)) { "Contact rule '$id' uses a fixture type that is not in the set" }
        put(id, a.index * size + b.index, rule, phases, false)
        if (a !== b) put(id, b.index * size + a.index, rule, phases, true)
    }

    fun sets(id: String, a: Set<IFixtureType>, b: Set<IFixtureType>, rule: IContactRule, vararg phases: ProcessState) {
        require(a.intersect(b).isEmpty()) {
            "Contact rule '$id' has a fixture type on both sides, so its role order is not defined"
        }
        a.forEach { typeA -> b.forEach { typeB -> pair(id, typeA, typeB, rule, *phases) } }
    }

    fun wildcard(id: String, a: IFixtureType, rule: IContactRule, vararg phases: ProcessState) =
        types.forEach { pair(id, a, it, rule, *phases) }

    fun build() = ContactRuleTable(types, Array(rules.size) { rules[it].copyOf() }, flip.copyOf(), ids.copyOf())

    private fun put(id: String, slot: Int, rule: IContactRule, phases: Array<out ProcessState>, flipped: Boolean) {
        val owner = ids[slot]
        require(owner == null || owner == id) { "Contact rules '$owner' and '$id' register the same pair" }
        phases.forEach { phase ->
            require(rules[phase.ordinal][slot] == null) { "Contact rule '$id' registers a pair twice for $phase" }
            rules[phase.ordinal][slot] = rule
        }
        ids[slot] = id
        flip[slot] = flipped
    }
}
