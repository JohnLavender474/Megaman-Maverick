package com.megaman.maverick.game.utils

import com.mega.game.engine.world.body.*;
import com.mega.game.engine.world.collisions.*;
import com.mega.game.engine.world.contacts.*;
import com.mega.game.engine.world.pathfinding.*;

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class StringToPropertiesParserTest :
    DescribeSpec({
      describe("StringToPropertiesParser") {
        it("should parse a valid string into Properties") {
          val input = "[key1=value1][key2=value2][key3=value3]"

          val result = StringToPropertiesParser.parse(input)

          result.get("key1") shouldBe "value1"
          result.get("key2") shouldBe "value2"
          result.get("key3") shouldBe "value3"
        }

        it("should handle empty key in property") {
          val input = "[=value]"

          val exception =
              shouldThrow<IllegalArgumentException> { StringToPropertiesParser.parse(input) }

          exception.message shouldBe "Empty key in property: =value"
        }

        it("should handle missing '=' in property") {
          val input = "[key1value1]"

          val exception =
              shouldThrow<IllegalArgumentException> { StringToPropertiesParser.parse(input) }

          exception.message shouldBe "Missing '=' in property: key1value1"
        }

        it(
            "should parse value despite missing closing bracket (up to user to verify result is correct)") {
              val input = "[key1=value1[key2=value2]"

              val result = StringToPropertiesParser.parse(input)

              result.get("key1") shouldBe "value1[key2=value2"
            }

        it("should handle missing closing bracket ']' at the end") {
          val input = "[key1=value1][key2=value2"

          val exception =
              shouldThrow<IllegalArgumentException> { StringToPropertiesParser.parse(input) }

          exception.message shouldBe "Missing closing bracket ']'. Property begins at index 13."
        }

        it("should handle multiple properties with missing closing bracket in between") {
          val input = "[key1=value1[key2=value2][key3=value3]"

          val result = StringToPropertiesParser.parse(input)

          result.get("key1") shouldBe "value1[key2=value2"
        }

        it("should handle multiple properties with missing '=' in between") {
          val input = "[key1=value1][key2value2][key3=value3]"

          val exception =
              shouldThrow<IllegalArgumentException> { StringToPropertiesParser.parse(input) }

          exception.message shouldBe "Missing '=' in property: key2value2"
        }
      }
    })
