package com.maverick.game.engine.events

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class SignalTest : DescribeSpec({

    describe("Signal") {
        it("calls its listeners at once, in the order they connected") {
            val signal = Signal<Int>()
            val calls = ArrayList<String>()
            signal.connect { calls.add("a$it") }
            signal.connect { calls.add("b$it") }

            signal.emit(1)

            calls shouldBe listOf("a1", "b1")
        }

        it("stops calling a disconnected listener") {
            val signal = Signal<Int>()
            val calls = ArrayList<Int>()
            val listener: (Int) -> Unit = { calls.add(it) }
            signal.connect(listener)

            signal.emit(1)
            signal.disconnect(listener)
            signal.emit(2)

            calls shouldBe listOf(1)
        }

        it("applies a connect or disconnect made during an emit after that emit") {
            val signal = Signal<Int>()
            val calls = ArrayList<String>()
            val late: (Int) -> Unit = { calls.add("late$it") }
            lateinit var self: (Int) -> Unit
            self = {
                calls.add("self$it")
                signal.disconnect(self)
                signal.connect(late)
            }
            signal.connect(self)

            signal.emit(1)
            signal.emit(2)

            calls shouldBe listOf("self1", "late2")
        }

        it("refuses an emit from one of its own listeners") {
            val signal = Signal<Int>()
            signal.connect { if (it == 1) signal.emit(2) }

            shouldThrow<IllegalStateException> { signal.emit(1) }

            signal.connect { }
            signal.emit(3)
        }
    }
})
