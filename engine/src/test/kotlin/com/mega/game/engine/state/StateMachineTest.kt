package com.mega.game.engine.state

import com.mega.game.engine.common.objects.MutableArray
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class StateMachineTest : DescribeSpec({

    describe("State Machine with simple loop") {
        it("should loop through states in order") {
            val stateA = DefaultStateImpl("A")
            val stateB = DefaultStateImpl("B")
            val stateC = DefaultStateImpl("C")

            stateA.addTransition({ true }, stateB)
            stateB.addTransition({ true }, stateC)
            stateC.addTransition({ true }, stateA)

            val stateMachine = StateMachine(stateA)

            stateMachine.getCurrent() shouldBe "A"
            stateMachine.next() shouldBe "B"
            stateMachine.next() shouldBe "C"
            stateMachine.next() shouldBe "A"
        }
    }

    describe("State Machine with branching paths") {
        it("should follow different paths based on conditions") {
            val stateA = DefaultStateImpl("A")
            val stateB = DefaultStateImpl("B")
            val stateC = DefaultStateImpl("C")
            val stateD = DefaultStateImpl("D")

            var aGoesToC = false

            stateA.addTransition({ aGoesToC }, stateC)
            stateA.addTransition({ true }, stateB)
            stateB.addTransition({ true }, stateC)
            stateC.addTransition({ true }, stateD)
            stateD.addTransition({ true }, stateA)

            val stateMachine = StateMachine(stateA)

            stateMachine.getCurrent() shouldBe "A"
            stateMachine.next() shouldBe "B"
            stateMachine.next() shouldBe "C"
            stateMachine.next() shouldBe "D"
            stateMachine.next() shouldBe "A"

            aGoesToC = true

            stateMachine.next() shouldBe "C"
        }
    }

    describe("State Machine with manipulated transition order") {
        it("should check transitions in the specified order") {
            val stateA = DefaultStateImpl("A")
            val stateB = DefaultStateImpl("B")
            val stateC = DefaultStateImpl("C")

            stateA.addTransition({ true }, stateB)
            stateA.addTransition({ true }, stateC)

            (stateA.transitions as MutableArray).reverse()

            val stateMachine = StateMachine(stateA)

            stateMachine.getCurrent() shouldBe "A"
            stateMachine.next() shouldBe "C"
        }
    }

    describe("State Machine with complex loop and branch combination") {
        it("should handle complex scenarios with multiple branches and loops") {
            val stateA = DefaultStateImpl("A")
            val stateB = DefaultStateImpl("B")
            val stateC = DefaultStateImpl("C")
            val stateD = DefaultStateImpl("D")
            val stateE = DefaultStateImpl("E")

            stateA.addTransition({ true }, stateB)
            stateB.addTransition({ true }, stateC)
            stateC.addTransition({ true }, stateD)
            stateD.addTransition({ false }, stateE)
            stateD.addTransition({ true }, stateA)
            stateA.addTransition({ false }, stateE)

            val stateMachine = StateMachine(stateA)

            stateMachine.getCurrent() shouldBe "A"
            stateMachine.next() shouldBe "B"
            stateMachine.next() shouldBe "C"
            stateMachine.next() shouldBe "D"
            stateMachine.next() shouldBe "A"
            stateMachine.next() shouldBe "B"
        }
    }

    describe("Potential pitfalls with incorrect transition conditions") {
        it("should handle a scenario where no transitions are valid") {
            val stateA = DefaultStateImpl("A")
            stateA.addTransition({ false }, stateA)

            val stateMachine = StateMachine(stateA)

            stateMachine.getCurrent() shouldBe "A"
            stateMachine.next() shouldBe "A"
        }

        it("should handle a scenario where transitions form an unintended loop") {
            val stateA = DefaultStateImpl("A")
            val stateB = DefaultStateImpl("B")

            stateA.addTransition({ true }, stateB)
            stateB.addTransition({ true }, stateA)

            val stateMachine = StateMachine(stateA)

            stateMachine.getCurrent() shouldBe "A"
            stateMachine.next() shouldBe "B"
            stateMachine.next() shouldBe "A"
            stateMachine.next() shouldBe "B"
        }
    }

    describe("State Machine with dynamic condition changes") {
        it("should change behavior based on dynamic condition changes") {
            var conditionToB = false
            var conditionToC = true

            val stateA = DefaultStateImpl("A")
            val stateB = DefaultStateImpl("B")
            val stateC = DefaultStateImpl("C")

            stateA.addTransition({ conditionToB }, stateB)
            stateA.addTransition({ conditionToC }, stateC)

            val stateMachine = StateMachine(stateA)

            stateMachine.getCurrent() shouldBe "A"
            stateMachine.next() shouldBe "C"

            conditionToB = true
            conditionToC = false

            stateMachine.setState(stateA)
            stateMachine.next() shouldBe "B"
        }
    }

    describe("State Machine with backtracking") {
        it("should be able to move back to previous states based on conditions") {
            val stateA = DefaultStateImpl("A")
            val stateB = DefaultStateImpl("B")
            val stateC = DefaultStateImpl("C")

            stateA.addTransition({ true }, stateB)
            stateB.addTransition({ true }, stateC)
            stateC.addTransition({ true }, stateB)

            val stateMachine = StateMachine(stateA)

            stateMachine.getCurrent() shouldBe "A"
            stateMachine.next() shouldBe "B"
            stateMachine.next() shouldBe "C"
            stateMachine.next() shouldBe "B"
            stateMachine.next() shouldBe "C"
        }
    }

    describe("State Machine with mixed loops and conditional branches") {
        it("should handle mixed loops with conditional branches correctly") {
            var loopCondition = true

            val stateA = DefaultStateImpl("A")
            val stateB = DefaultStateImpl("B")
            val stateC = DefaultStateImpl("C")
            val stateD = DefaultStateImpl("D")
            val stateE = DefaultStateImpl("E")

            stateA.addTransition({ true }, stateB)
            stateB.addTransition({ loopCondition }, stateC)
            stateC.addTransition({ true }, stateD)
            stateD.addTransition({ true }, stateA)
            stateB.addTransition({ !loopCondition }, stateE)

            val stateMachine = StateMachine(stateA)

            stateMachine.getCurrent() shouldBe "A"
            stateMachine.next() shouldBe "B"
            stateMachine.next() shouldBe "C"
            stateMachine.next() shouldBe "D"
            stateMachine.next() shouldBe "A"

            loopCondition = false

            stateMachine.next() shouldBe "B"
            stateMachine.next() shouldBe "E"
        }
    }

    describe("State Machine with complex backtracking and dynamic branching") {
        it("should handle complex scenarios with backtracking and dynamic branching") {
            var conditionToD = false

            val stateA = DefaultStateImpl("A")
            val stateB = DefaultStateImpl("B")
            val stateC = DefaultStateImpl("C")
            val stateD = DefaultStateImpl("D")
            val stateE = DefaultStateImpl("E")
            val stateF = DefaultStateImpl("F")

            stateA.addTransition({ true }, stateB)
            stateB.addTransition({ true }, stateC)
            stateC.addTransition({ conditionToD }, stateD)
            stateC.addTransition({ !conditionToD }, stateE)
            stateD.addTransition({ true }, stateF)
            stateE.addTransition({ true }, stateB)

            val stateMachine = StateMachine(stateA)

            stateMachine.getCurrent() shouldBe "A"
            stateMachine.next() shouldBe "B"
            stateMachine.next() shouldBe "C"

            stateMachine.next() shouldBe "E"
            stateMachine.next() shouldBe "B"

            conditionToD = true

            stateMachine.next() shouldBe "C"
            stateMachine.next() shouldBe "D"
            stateMachine.next() shouldBe "F"
        }
    }

    describe("State Machine with nested loops and conditional resets") {
        it("should handle nested loops with conditional resets") {
            var innerLoopCondition = true

            val stateA = DefaultStateImpl("A")
            val stateB = DefaultStateImpl("B")
            val stateC = DefaultStateImpl("C")
            val stateD = DefaultStateImpl("D")

            stateA.addTransition({ true }, stateB)
            stateB.addTransition({ !innerLoopCondition }, stateC)
            stateC.addTransition({ true }, stateA)

            stateB.addTransition({ innerLoopCondition }, stateD)
            stateD.addTransition({ true }, stateB)

            val stateMachine = StateMachine(stateA)

            stateMachine.getCurrent() shouldBe "A"
            stateMachine.next() shouldBe "B"

            stateMachine.next() shouldBe "D"
            stateMachine.next() shouldBe "B"

            stateMachine.next() shouldBe "D"

            innerLoopCondition = false
            stateMachine.next() shouldBe "B"
            stateMachine.next() shouldBe "C"
            stateMachine.next() shouldBe "A"
        }
    }

    describe("State Machine with multiple active conditions and branching complexity") {
        it("should correctly evaluate multiple conditions and handle complex branching") {
            var conditionToC = true
            var conditionToD = true
            var conditionToE = false

            val stateA = DefaultStateImpl("A")
            val stateB = DefaultStateImpl("B")
            val stateC = DefaultStateImpl("C")
            val stateD = DefaultStateImpl("D")
            val stateE = DefaultStateImpl("E")
            val stateF = DefaultStateImpl("F")

            stateA.addTransition({ true }, stateB)
            stateB.addTransition({ conditionToC }, stateC)
            stateB.addTransition({ conditionToD }, stateD)
            stateB.addTransition({ conditionToE }, stateE)
            stateC.addTransition({ true }, stateF)
            stateD.addTransition({ true }, stateF)

            val stateMachine = StateMachine(stateA)

            stateMachine.getCurrent() shouldBe "A"
            stateMachine.next() shouldBe "B"
            stateMachine.next() shouldBe "C"

            conditionToC = false
            conditionToE = true

            stateMachine.setState(stateA)
            stateMachine.next() shouldBe "B"
            stateMachine.next() shouldBe "D"

            conditionToD = false
            stateMachine.setState(stateA)
            stateMachine.next() shouldBe "B"
            stateMachine.next() shouldBe "E"
        }
    }
})