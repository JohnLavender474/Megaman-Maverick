package com.maverick.game.engine.state

import com.badlogic.gdx.utils.Array
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

private class Log {
    val calls = ArrayList<String>()
}

private class TestStep(val name: String, var done: Boolean = true) : Step<Log> {

    override fun begin(context: Log) {
        context.calls.add("begin:$name")
    }

    override fun isFinished(context: Log) = done

    override fun end(context: Log, interrupted: Boolean) {
        context.calls.add("end:$name:$interrupted")
    }
}

private fun routine(vararg steps: Step<Log>): Routine<Log> {
    val array = Array<Step<Log>>()
    steps.forEach { array.add(it) }
    return Routine(array)
}

class RoutineTest : DescribeSpec({

    describe("Routine") {
        it("runs through each finished step in one update") {
            val log = Log()
            val routine = routine(TestStep("a"), TestStep("b"))

            routine.update(log)

            log.calls.shouldContainExactly("begin:a", "end:a:false", "begin:b", "end:b:false")
            routine.finished shouldBe true
        }

        it("stops at a step that is not finished, and does not begin it again") {
            val log = Log()
            val wait = TestStep("wait", done = false)
            val routine = routine(TestStep("a"), wait, TestStep("b"))

            routine.update(log)
            routine.update(log)

            log.calls.shouldContainExactly("begin:a", "end:a:false", "begin:wait")
            routine.finished shouldBe false

            wait.done = true
            routine.update(log)

            log.calls.shouldContainExactly(
                "begin:a", "end:a:false", "begin:wait", "end:wait:false", "begin:b", "end:b:false"
            )
            routine.finished shouldBe true
        }

        it("ends a started step as interrupted on a cancel, and starts no other step") {
            val log = Log()
            val routine = routine(TestStep("wait", done = false), TestStep("b"))

            routine.update(log)
            routine.cancel(log)
            routine.update(log)

            log.calls.shouldContainExactly("begin:wait", "end:wait:true")
            routine.finished shouldBe true
        }

        it("calls nothing on a cancel with no started step") {
            val log = Log()
            val routine = routine(TestStep("a"))

            routine.cancel(log)

            log.calls.isEmpty() shouldBe true
            routine.finished shouldBe true
        }

        it("calls nothing on a cancel after the last step") {
            val log = Log()
            val routine = routine(TestStep("a"))

            routine.update(log)
            routine.cancel(log)

            log.calls.shouldContainExactly("begin:a", "end:a:false")
        }
    }
})
