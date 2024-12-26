package com.mega.game.engine.state

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class StateMachineBuilderTest : DescribeSpec({

    describe("StateMachineBuilder basic functionality") {

        it("should correctly build a state machine with defined states and transitions") {
            val stateMachine = StateMachineBuilder<String>()
                .state("A", "State A")
                .state("B", "State B")
                .state("C", "State C")
                .transition("A", "B") { true }
                .transition("B", "C") { true }
                .initialState("A")
                .build()

            stateMachine.getCurrent() shouldBe "State A"
            stateMachine.next()
            stateMachine.getCurrent() shouldBe "State B"
            stateMachine.next()
            stateMachine.getCurrent() shouldBe "State C"
        }
    }

    describe("StateMachineBuilder with deferred state creation") {

        it("should allow transitions to be defined before states are added") {
            val stateMachine = StateMachineBuilder<String>()
                .transition("A", "B") { true }
                .state("A", "State A")
                .state("B", "State B")
                .initialState("A")
                .build()

            stateMachine.getCurrent() shouldBe "State A"
            stateMachine.next()
            stateMachine.getCurrent() shouldBe "State B"
        }

        it("should throw an exception if a state is referenced in a transition but not defined") {
            shouldThrow<IllegalArgumentException> {
                StateMachineBuilder<String>()
                    .state("A", "State A")
                    .transition("A", "B") { true }
                    .initialState("A")
                    .build()
            }
        }

        it("should throw an exception if the initial state is not defined") {
            shouldThrow<IllegalStateException> {
                StateMachineBuilder<String>()
                    .state("A", "State A")
                    .build()
            }
        }
    }

    describe("StateMachineBuilder with complex state machine setups") {

        it("should handle complex setups with multiple states and transitions") {
            val stateMachine = StateMachineBuilder<String>()
                .state("A", "State A")
                .state("B", "State B")
                .state("C", "State C")
                .state("D", "State D")
                .state("E", "State E")
                .transition("A", "B") { true }
                .transition("B", "C") { true }
                .transition("C", "D") { true }
                .transition("D", "A") { true }
                .transition("B", "E") { false }
                .initialState("A")
                .build()

            stateMachine.getCurrent() shouldBe "State A"
            stateMachine.next()
            stateMachine.getCurrent() shouldBe "State B"
            stateMachine.next()
            stateMachine.getCurrent() shouldBe "State C"
            stateMachine.next()
            stateMachine.getCurrent() shouldBe "State D"
            stateMachine.next()
            stateMachine.getCurrent() shouldBe "State A"
        }

        it("should throw an exception if any transition references an undefined state in complex setups") {
            shouldThrow<IllegalArgumentException> {
                StateMachineBuilder<String>()
                    .state("A", "State A")
                    .state("B", "State B")
                    .transition("A", "B") { true }
                    .transition("B", "C") { true }
                    .initialState("A")
                    .build()
            }
        }
    }

    describe("StateMachineBuilder with edge cases") {

        it("should handle an empty state machine definition with an error") {
            shouldThrow<IllegalStateException> {
                StateMachineBuilder<String>().build()
            }
        }

        it("should handle transitions that are never used") {
            val stateMachine = StateMachineBuilder<String>()
                .state("A", "State A")
                .state("B", "State B")
                .state("C", "State C")
                .transition("A", "B") { true }
                .transition("B", "C") { false }
                .initialState("A")
                .build()

            stateMachine.getCurrent() shouldBe "State A"
            stateMachine.next()
            stateMachine.getCurrent() shouldBe "State B"
            stateMachine.next()
            stateMachine.getCurrent() shouldBe "State B"
        }
    }
})
