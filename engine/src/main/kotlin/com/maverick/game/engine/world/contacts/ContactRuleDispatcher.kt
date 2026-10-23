package com.maverick.game.engine.world.contacts

import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.ProcessState
import com.maverick.game.engine.world.body.IFixture

class ContactRuleDispatcher(
    private val table: ContactRuleTable,
    private val debugFilter: ((IFixture, IFixture) -> Boolean)? = null
) : IContactListener {

    companion object {
        const val TAG = "ContactRuleDispatcher"
    }

    override fun beginContact(fixture1: IFixture, fixture2: IFixture, delta: Float) =
        dispatch(ProcessState.BEGIN, fixture1, fixture2, delta)

    override fun continueContact(fixture1: IFixture, fixture2: IFixture, delta: Float) =
        dispatch(ProcessState.CONTINUE, fixture1, fixture2, delta)

    override fun endContact(fixture1: IFixture, fixture2: IFixture, delta: Float) =
        dispatch(ProcessState.END, fixture1, fixture2, delta)

    private fun dispatch(phase: ProcessState, fixture1: IFixture, fixture2: IFixture, delta: Float) {
        val filter = debugFilter
        if (filter != null && filter.invoke(fixture1, fixture2)) GameLogger.debug(
            TAG,
            "$phase ${table.ruleId(phase, fixture1, fixture2)}: fixture1=$fixture1, fixture2=$fixture2"
        )
        table.dispatch(phase, fixture1, fixture2, delta)
    }
}
