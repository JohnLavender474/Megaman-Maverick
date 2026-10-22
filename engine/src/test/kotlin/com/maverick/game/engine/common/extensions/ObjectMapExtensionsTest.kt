package com.maverick.game.engine.common.extensions

import com.badlogic.gdx.utils.OrderedMap
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class ObjectMapExtensionsTest :
    DescribeSpec({
        describe("indexOfKey") {
            fun mapOfKeys(vararg keys: String): OrderedMap<String, Int> {
                val map = OrderedMap<String, Int>()
                keys.forEachIndexed { index, key -> map.put(key, index * 10) }
                return map
            }

            it("should return the insertion index of each key") {
                val map = mapOfKeys("c", "a", "b")
                map.indexOfKey("c") shouldBe 0
                map.indexOfKey("a") shouldBe 1
                map.indexOfKey("b") shouldBe 2
            }

            it("should return -1 for a key that is not in the map") {
                mapOfKeys("a", "b").indexOfKey("z") shouldBe -1
            }

            it("should return -1 for an empty map") {
                OrderedMap<String, Int>().indexOfKey("a") shouldBe -1
            }

            it("should compare keys by equality, not identity") {
                val map = mapOfKeys("blocks")
                val key = StringBuilder("blo").append("cks").toString()
                map.indexOfKey(key) shouldBe 0
            }

            it("should keep the index when an existing key is put again") {
                val map = mapOfKeys("a", "b", "c")
                map.put("a", 99)
                map.indexOfKey("a") shouldBe 0
            }

            it("should shift the indexes after a key is removed") {
                val map = mapOfKeys("a", "b", "c")
                map.remove("a")
                map.indexOfKey("a") shouldBe -1
                map.indexOfKey("b") shouldBe 0
                map.indexOfKey("c") shouldBe 1
            }
        }
    })
