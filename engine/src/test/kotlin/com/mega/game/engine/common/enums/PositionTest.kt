package com.mega.game.engine.common.enums

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class PositionTest : DescribeSpec({
    describe("Position companion object") {
        it("should correctly retrieve positions based on x and y") {
            Position.get(0, 0) shouldBe Position.BOTTOM_LEFT
            Position.get(1, 1) shouldBe Position.CENTER
            Position.get(2, 2) shouldBe Position.TOP_RIGHT
        }

        it("should throw IndexOutOfBoundsException for invalid x or y") {
            shouldThrow<IndexOutOfBoundsException> { Position.get(-1, 0) }
            shouldThrow<IndexOutOfBoundsException> { Position.get(0, -1) }
            shouldThrow<IndexOutOfBoundsException> { Position.get(3, 0) }
            shouldThrow<IndexOutOfBoundsException> { Position.get(0, 3) }
        }
    }

    describe("Position instance methods") {
        it("should correctly find the left position") {
            Position.CENTER.left() shouldBe Position.CENTER_LEFT
            Position.TOP_LEFT.left() shouldBe Position.TOP_RIGHT
            Position.BOTTOM_CENTER.left() shouldBe Position.BOTTOM_LEFT
        }

        it("should correctly find the right position") {
            Position.CENTER.right() shouldBe Position.CENTER_RIGHT
            Position.TOP_RIGHT.right() shouldBe Position.TOP_LEFT
            Position.BOTTOM_LEFT.right() shouldBe Position.BOTTOM_CENTER
        }

        it("should correctly find the up position") {
            Position.CENTER.up() shouldBe Position.TOP_CENTER
            Position.BOTTOM_RIGHT.up() shouldBe Position.CENTER_RIGHT
            Position.TOP_LEFT.up() shouldBe Position.BOTTOM_LEFT
        }

        it("should correctly find the down position") {
            Position.CENTER.down() shouldBe Position.BOTTOM_CENTER
            Position.TOP_RIGHT.down() shouldBe Position.CENTER_RIGHT
            Position.BOTTOM_LEFT.down() shouldBe Position.TOP_LEFT
        }

        it("should correctly find the opposite position") {
            Position.BOTTOM_LEFT.opposite() shouldBe Position.TOP_RIGHT
            Position.CENTER.opposite() shouldBe Position.CENTER
            Position.TOP_CENTER.opposite() shouldBe Position.BOTTOM_CENTER
        }

        it("should move to the correct position") {
            Position.CENTER.move(Direction.LEFT) shouldBe Position.CENTER_LEFT
            Position.TOP_RIGHT.move(Direction.RIGHT).move(Direction.UP) shouldBe Position.BOTTOM_LEFT
            Position.BOTTOM_LEFT.move(Direction.LEFT).move(Direction.UP).move(Direction.LEFT) shouldBe Position.CENTER
        }
    }
})
