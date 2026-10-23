package com.megaman.maverick.game.world.contacts

import com.maverick.game.engine.common.enums.ProcessState
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.maverick.game.engine.world.body.FixtureTypeSet
import com.maverick.game.engine.world.body.IFixture
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.world.body.FixtureType
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import java.io.File

class ContactRuleSelectionTest : DescribeSpec({

    describe("contact rule selection") {
        it("picks the same rule for every pair and phase as the reference file") {
            val actual = selectAll(tableSelector())

            if (System.getProperty(UPDATE_PROPERTY) != null) {
                File(REFERENCE_PATH).writeText(actual)
            } else {
                val expected = ContactRuleSelectionTest::class.java.getResource("/$REFERENCE_FILE")!!.readText()
                actual shouldBe expected
            }
        }

        it("owns every fixture type in the table") {
            val types = FixtureTypeSet.of(FixtureType.entries)

            FixtureType.entries.forEach { types.owns(it) shouldBe true }
        }
    }
}) {

    companion object {
        const val REFERENCE_FILE = "contact-rule-selection.txt"
        const val REFERENCE_PATH = "src/test/resources/$REFERENCE_FILE"
        const val UPDATE_PROPERTY = "updateContactRuleSelection"

        val PHASES = listOf("BEGIN", "CONTINUE", "END")
    }
}

private class Selection(val ruleId: String, val roleA: IFixture)

private fun interface Selector {
    fun select(phase: String, fixture1: IFixture, fixture2: IFixture): Selection?
}

private fun tableSelector(): Selector {
    val table = megaContactRules(mockk<MegamanMaverickGame>(relaxed = true)) { _, _ -> false }
    return Selector { phase, fixture1, fixture2 ->
        table.select(ProcessState.valueOf(phase), fixture1, fixture2)?.let { Selection(it.ruleId, it.roleA) }
    }
}

private fun selectAll(selector: Selector): String {
    val body = Body(BodyType.ABSTRACT)
    val builder = StringBuilder()
    for (phase in ContactRuleSelectionTest.PHASES) for (type1 in FixtureType.entries) for (type2 in FixtureType.entries) {
        val fixture1 = Fixture(body, type1)
        val fixture2 = Fixture(body, type2)
        val selection = selector.select(phase, fixture1, fixture2)

        builder.append("$phase $type1 $type2 -> ")
        when {
            selection == null -> builder.append("-")
            type1 == type2 -> builder.append("${selection.ruleId} ${if (selection.roleA === fixture1) "12" else "21"}")
            else -> builder.append(selection.ruleId)
        }
        builder.append('\n')
    }
    return builder.toString()
}
