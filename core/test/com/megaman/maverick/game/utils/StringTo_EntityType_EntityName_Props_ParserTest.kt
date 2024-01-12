package com.megaman.maverick.game.utils

import com.megaman.maverick.game.entities.EntityType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class StringTo_EntityType_EntityName_Props_ParserTest :
    DescribeSpec({
      describe("StringTo_EntityType_EntityName_Props_Parser") {
        it("should parse a valid string into ParsedResult") {
          val input = "BLOCK,EntityName,[key1=value1][key2=value2][key3=value3]"

          val result = StringTo_EntityType_EntityName_Props_Parser.parse(input)

          result.entityType shouldBe EntityType.BLOCK
          result.entityName shouldBe "EntityName"
          result.properties.get("key1") shouldBe "value1"
          result.properties.get("key2") shouldBe "value2"
          result.properties.get("key3") shouldBe "value3"
        }

        it("should handle empty key in property") {
          val input = "ENEMY,EntityName,[=value]"

          val exception =
              shouldThrow<IllegalArgumentException> {
                StringTo_EntityType_EntityName_Props_Parser.parse(input)
              }

          exception.message shouldBe "Empty key in property: =value"
        }

        it("should handle missing '=' in property") {
          val input = "ITEM,EntityName,[key1value1]"

          val exception =
              shouldThrow<IllegalArgumentException> {
                StringTo_EntityType_EntityName_Props_Parser.parse(input)
              }

          exception.message shouldBe "Missing '=' in property: key1value1"
        }

        it("should handle missing closing bracket ']' in properties") {
          val input = "HAZARD,EntityName,[key1=value1[key2=value2]"

          val result = StringTo_EntityType_EntityName_Props_Parser.parse(input)

          result.entityType shouldBe EntityType.HAZARD
          result.entityName shouldBe "EntityName"
          result.properties.get("key1") shouldBe "value1[key2=value2"
        }

        it("should handle multiple properties with missing closing bracket in between") {
          val input = "ENEMY,EntityName,[key1=value1[key2=value2][key3=value3]"

          val result = StringTo_EntityType_EntityName_Props_Parser.parse(input)

          result.entityType shouldBe EntityType.ENEMY
          result.entityName shouldBe "EntityName"
          result.properties.get("key1") shouldBe "value1[key2=value2"
        }

        it("should handle multiple properties with missing '=' in between") {
          val input = "HAZARD,EntityName,[key1=value1][key2value2][key3=value3]"

          val exception =
              shouldThrow<IllegalArgumentException> {
                StringTo_EntityType_EntityName_Props_Parser.parse(input)
              }

          exception.message shouldBe "Missing '=' in property: key2value2"
        }

        it("should parse a valid string with no properties into ParsedResult") {
          val input = "ENEMY,Bat"

          val result = StringTo_EntityType_EntityName_Props_Parser.parse(input)

          result.entityType shouldBe EntityType.ENEMY
          result.entityName shouldBe "Bat"
          result.properties.size shouldBe 0
        }
      }
    })
