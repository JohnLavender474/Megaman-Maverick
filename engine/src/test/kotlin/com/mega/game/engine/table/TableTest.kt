package com.mega.game.engine.table

import com.mega.game.engine.common.objects.table.TableBuilder
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class TableTest : DescribeSpec({

    describe("Table") {
        val table = TableBuilder<String>()
            .row("A1", "A2", "A3")
            .row("B1", "B2")
            .row("C1", "C2", "C3", "C4")
            .build()

        it("should return correct row and column counts") {
            table.rowCount() shouldBe 3
            table.columnCount(0) shouldBe 4
            table.columnCount(1) shouldBe 2
            table.columnCount(2) shouldBe 3
        }

        it("should return correct elements with get(row, column)") {
            table.get(0, 0).element shouldBe "C1"
            table.get(0, 1).element shouldBe "C2"
            table.get(1, 0).element shouldBe "B1"
            table.get(2, 2).element shouldBe "A3"
        }

        it("should throw IllegalArgumentException when getting out-of-bounds elements") {
            shouldThrow<IllegalArgumentException> { table.get(2, 5) } // out of bounds column
            shouldThrow<IllegalArgumentException> { table.get(5, 0) } // out of bounds row
            shouldThrow<IllegalArgumentException> { table.get(-1, 0) } // negative row index
            shouldThrow<IllegalArgumentException> { table.get(0, -1) } // negative column index
        }
    }

    describe("TableNode") {
        val table = TableBuilder<String>()
            .row("A1", "A2", "A3")
            .row("B1", "B2")
            .row("C1", "C2", "C3", "C4")
            .build()

        val node = table.get(0, 2)

        it("should move up correctly with cycling") {
            node.previousRow().element shouldBe "B2"
            node.previousRow().previousRow().element shouldBe "A2"
        }

        it("should move down correctly with cycling") {
            node.nextRow().element shouldBe "A3"
            node.nextRow().nextRow().element shouldBe "B2"
            node.nextRow().nextRow().nextRow().element shouldBe "C2"
        }

        it("should move left correctly with cycling") {
            node.previousColumn().element shouldBe "C2"
            node.previousColumn().previousColumn().element shouldBe "C1"
            node.previousColumn().previousColumn().previousColumn().element shouldBe "C4"
        }

        it("should move right correctly with cycling") {
            node.nextColumn().element shouldBe "C4"
            node.nextColumn().nextColumn().element shouldBe "C1"
            node.nextColumn().nextColumn().nextColumn().element shouldBe "C2"
        }
    }

    describe("TableBuilder") {
        it("should create a table with the correct structure") {
            val builder = TableBuilder<String>()
                .row("X1", "X2", "X3")
                .row("Y1", "Y2")
                .row("Z1", "Z2", "Z3", "Z4")
            val table = builder.build()

            table.rowCount() shouldBe 3
            table.columnCount(0) shouldBe 4
            table.columnCount(1) shouldBe 2
            table.columnCount(2) shouldBe 3

            table.get(0, 0).element shouldBe "Z1"
            table.get(1, 0).element shouldBe "Y1"
            table.get(2, 2).element shouldBe "X3"
        }
    }
})
