package com.maverick.game.engine.world.contacts

import com.maverick.game.engine.common.enums.ProcessState
import com.maverick.game.engine.world.body.FixtureTypeSet
import com.maverick.game.engine.world.body.IFixture

class ContactRuleTable internal constructor(
    private val types: FixtureTypeSet,
    private val rules: Array<Array<IContactRule?>>,
    private val flip: BooleanArray,
    private val ids: Array<String?>
) {

    class Selection(val ruleId: String, val roleA: IFixture, val roleB: IFixture)

    fun dispatch(phase: ProcessState, fixture1: IFixture, fixture2: IFixture, delta: Float) {
        val slot = slot(fixture1, fixture2)
        val rule = rules[phase.ordinal][slot] ?: return
        if (flip[slot]) call(rule, phase, fixture2, fixture1, delta) else call(rule, phase, fixture1, fixture2, delta)
    }

    fun ruleId(phase: ProcessState, fixture1: IFixture, fixture2: IFixture): String? {
        val slot = slot(fixture1, fixture2)
        return if (rules[phase.ordinal][slot] == null) null else ids[slot]
    }

    fun select(phase: ProcessState, fixture1: IFixture, fixture2: IFixture): Selection? {
        val slot = slot(fixture1, fixture2)
        if (rules[phase.ordinal][slot] == null) return null
        return if (flip[slot]) Selection(ids[slot]!!, fixture2, fixture1) else Selection(ids[slot]!!, fixture1, fixture2)
    }

    private fun slot(fixture1: IFixture, fixture2: IFixture): Int {
        val type1 = fixture1.getType()
        val type2 = fixture2.getType()
        check(types.owns(type1) && types.owns(type2)) {
            "A fixture type is not in the contact rule table: fixture1=$fixture1, fixture2=$fixture2"
        }
        return type1.index * types.size + type2.index
    }

    private fun call(rule: IContactRule, phase: ProcessState, a: IFixture, b: IFixture, delta: Float) =
        when (phase) {
            ProcessState.BEGIN -> rule.onBegin(a, b, delta)
            ProcessState.CONTINUE -> rule.onContinue(a, b, delta)
            ProcessState.END -> rule.onEnd(a, b, delta)
        }
}
