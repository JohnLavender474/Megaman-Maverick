package com.maverick.game.engine.world.contacts

import com.maverick.game.engine.common.enums.ProcessState
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.maverick.game.engine.world.body.FixtureTypeSet
import com.maverick.game.engine.world.body.IFixture
import com.maverick.game.engine.world.body.IFixtureType
import com.maverick.game.engine.world.body.TestFixtureType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class ContactRuleTableTest : DescribeSpec({

    val types = FixtureTypeSet.of(TestFixtureType.entries)
    val body = Body(BodyType.ABSTRACT)

    fun fixture(type: IFixtureType) = Fixture(body, type)

    class Call(val phase: ProcessState, val a: IFixture, val b: IFixture)

    class RecordingRule : IContactRule {
        val calls = mutableListOf<Call>()

        override fun onBegin(a: IFixture, b: IFixture, delta: Float) {
            calls.add(Call(ProcessState.BEGIN, a, b))
        }

        override fun onContinue(a: IFixture, b: IFixture, delta: Float) {
            calls.add(Call(ProcessState.CONTINUE, a, b))
        }

        override fun onEnd(a: IFixture, b: IFixture, delta: Float) {
            calls.add(Call(ProcessState.END, a, b))
        }
    }

    describe("dispatch") {
        it("passes the fixtures in the rule's role order for both contact orders") {
            val rule = RecordingRule()
            val table = ContactRulesBuilder(types).apply {
                pair("f1-f2", TestFixtureType.F1, TestFixtureType.F2, rule, ProcessState.BEGIN)
            }.build()
            val f1 = fixture(TestFixtureType.F1)
            val f2 = fixture(TestFixtureType.F2)

            table.dispatch(ProcessState.BEGIN, f1, f2, 0f)
            table.dispatch(ProcessState.BEGIN, f2, f1, 0f)

            rule.calls.map { it.a to it.b } shouldBe listOf(f1 to f2, f1 to f2)
        }

        it("keeps the contact order for a same-type pair") {
            val rule = RecordingRule()
            val table = ContactRulesBuilder(types).apply {
                pair("f-f", TestFixtureType.F, TestFixtureType.F, rule, ProcessState.BEGIN)
            }.build()
            val first = fixture(TestFixtureType.F)
            val second = fixture(TestFixtureType.F)

            table.dispatch(ProcessState.BEGIN, first, second, 0f)
            table.dispatch(ProcessState.BEGIN, second, first, 0f)

            rule.calls.map { it.a to it.b } shouldBe listOf(first to second, second to first)
        }

        it("calls only the registered phases") {
            val rule = RecordingRule()
            val table = ContactRulesBuilder(types).apply {
                pair("f1-f2", TestFixtureType.F1, TestFixtureType.F2, rule, ProcessState.BEGIN, ProcessState.END)
            }.build()
            val f1 = fixture(TestFixtureType.F1)
            val f2 = fixture(TestFixtureType.F2)

            ProcessState.entries.forEach { table.dispatch(it, f1, f2, 0f) }

            rule.calls.map { it.phase } shouldBe listOf(ProcessState.BEGIN, ProcessState.END)
        }

        it("does nothing for a pair with no rule") {
            val rule = RecordingRule()
            val table = ContactRulesBuilder(types).apply {
                pair("f1-f2", TestFixtureType.F1, TestFixtureType.F2, rule, ProcessState.BEGIN)
            }.build()

            table.dispatch(ProcessState.BEGIN, fixture(TestFixtureType.KEEP), fixture(TestFixtureType.SKIP), 0f)

            rule.calls.size shouldBe 0
        }

        it("throws for a fixture type that is not in the set") {
            class StrayType(override val index: Int) : IFixtureType

            val table = ContactRulesBuilder(types).build()

            shouldThrow<IllegalStateException> {
                table.dispatch(ProcessState.BEGIN, fixture(StrayType(0)), fixture(TestFixtureType.F), 0f)
            }
        }
    }

    describe("wildcard") {
        it("gives the wildcard type the first role with every type, and fixture1 when both have it") {
            val rule = RecordingRule()
            val table = ContactRulesBuilder(types).apply {
                wildcard("consumer", TestFixtureType.KEEP, rule, ProcessState.BEGIN)
            }.build()
            val keep = fixture(TestFixtureType.KEEP)

            TestFixtureType.entries.filter { it != TestFixtureType.KEEP }.forEach {
                val other = fixture(it)
                table.select(ProcessState.BEGIN, other, keep)!!.roleA shouldBe keep
            }
            val keep2 = fixture(TestFixtureType.KEEP)
            table.select(ProcessState.BEGIN, keep2, keep)!!.roleA shouldBe keep2
        }
    }

    describe("sets") {
        it("registers every pair of the two sets") {
            val table = ContactRulesBuilder(types).apply {
                sets(
                    "death",
                    setOf(TestFixtureType.F),
                    setOf(TestFixtureType.F1, TestFixtureType.F2),
                    RecordingRule(),
                    ProcessState.BEGIN
                )
            }.build()

            table.ruleId(ProcessState.BEGIN, fixture(TestFixtureType.F2), fixture(TestFixtureType.F)) shouldBe "death"
            table.ruleId(ProcessState.BEGIN, fixture(TestFixtureType.F1), fixture(TestFixtureType.F)) shouldBe "death"
            table.ruleId(ProcessState.BEGIN, fixture(TestFixtureType.F1), fixture(TestFixtureType.F2)) shouldBe null
        }
    }

    describe("build checks") {
        it("rejects two rules for the same pair") {
            shouldThrow<IllegalArgumentException> {
                ContactRulesBuilder(types).apply {
                    pair("one", TestFixtureType.F1, TestFixtureType.F2, RecordingRule(), ProcessState.BEGIN)
                    pair("two", TestFixtureType.F2, TestFixtureType.F1, RecordingRule(), ProcessState.END)
                }
            }
        }

        it("rejects a rule that registers the same pair twice for a phase") {
            shouldThrow<IllegalArgumentException> {
                ContactRulesBuilder(types).apply {
                    pair("one", TestFixtureType.F1, TestFixtureType.F2, RecordingRule(), ProcessState.BEGIN)
                    pair("one", TestFixtureType.F1, TestFixtureType.F2, RecordingRule(), ProcessState.BEGIN)
                }
            }
        }

        it("rejects a set rule with a type on both sides") {
            shouldThrow<IllegalArgumentException> {
                ContactRulesBuilder(types).sets(
                    "bad",
                    setOf(TestFixtureType.F, TestFixtureType.F1),
                    setOf(TestFixtureType.F1),
                    RecordingRule(),
                    ProcessState.BEGIN
                )
            }
        }

        it("rejects a wildcard that clashes with an explicit pair") {
            shouldThrow<IllegalArgumentException> {
                ContactRulesBuilder(types).apply {
                    pair("explicit", TestFixtureType.KEEP, TestFixtureType.F, RecordingRule(), ProcessState.BEGIN)
                    wildcard("consumer", TestFixtureType.KEEP, RecordingRule(), ProcessState.BEGIN)
                }
            }
        }

        it("rejects a fixture type that is not in the set") {
            class StrayType(override val index: Int) : IFixtureType

            shouldThrow<IllegalArgumentException> {
                ContactRulesBuilder(types).pair("stray", StrayType(0), TestFixtureType.F, RecordingRule())
            }
        }
    }
})
