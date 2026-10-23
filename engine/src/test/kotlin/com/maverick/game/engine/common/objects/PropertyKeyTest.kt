package com.maverick.game.engine.common.objects

import com.maverick.game.engine.common.enums.ProcessState
import com.maverick.game.engine.common.interfaces.IPropertizable
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

class PropertyKeyTest : DescribeSpec({

    class Holder : IPropertizable {
        override val properties = Properties()
    }

    describe("PropertyKey") {
        it("stores and reads a typed value") {
            val key = PropertyKey<(Int) -> Int>("double")
            val holder = Holder()

            holder.putProperty(key) { it * 2 }

            holder.getProperty(key)!!.invoke(4) shouldBe 8
        }

        it("reads null when nothing is stored") {
            Holder().getProperty(PropertyKey<String>("missing")) shouldBe null
        }

        it("treats two keys with the same name as different keys") {
            val key1 = PropertyKey<String>("same")
            val key2 = PropertyKey<String>("same")
            val holder = Holder()

            holder.putProperty(key1, "one")

            holder.getProperty(key2) shouldBe null
            holder.hasProperty(key2) shouldBe false
        }

        it("does not collide with a string key of the same text") {
            val key = PropertyKey<String>("text")
            val holder = Holder()

            holder.putProperty("text", "string key")
            holder.putProperty(key, "typed key")

            holder.getProperty("text") shouldBe "string key"
            holder.getProperty(key) shouldBe "typed key"
        }

        it("checks and removes a value") {
            val key = PropertyKey<String>("removable")
            val holder = Holder()
            holder.putProperty(key, "value")

            holder.hasProperty(key) shouldBe true
            holder.removeProperty(key) shouldBe "value"
            holder.hasProperty(key) shouldBe false
        }

        it("uses its name as its text") {
            PropertyKey<String>("named").toString() shouldBe "named"
        }
    }

    describe("PhasedPropertyKey") {
        it("gives the same key object for the same state") {
            val key = PhasedPropertyKey<String>("phased")

            key[ProcessState.BEGIN] shouldBeSameInstanceAs key[ProcessState.BEGIN]
        }

        it("gives a different key for each state") {
            val key = PhasedPropertyKey<String>("phased")
            val holder = Holder()

            holder.putProperty(key[ProcessState.BEGIN], "begin")
            holder.putProperty(key[ProcessState.END], "end")

            holder.getProperty(key[ProcessState.BEGIN]) shouldBe "begin"
            holder.getProperty(key[ProcessState.CONTINUE]) shouldBe null
            holder.getProperty(key[ProcessState.END]) shouldBe "end"
        }

        it("names each state's key after the phased key and the state") {
            PhasedPropertyKey<String>("hit")[ProcessState.CONTINUE].name shouldBe "hit_CONTINUE"
        }
    }
})
