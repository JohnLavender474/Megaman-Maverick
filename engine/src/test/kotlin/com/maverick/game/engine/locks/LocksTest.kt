package com.maverick.game.engine.locks

import com.maverick.game.engine.MockGameEntity
import com.maverick.game.engine.components.MissingComponentException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

private enum class TestGroup(override val index: Int) : LockGroup { A(0), B(1) }

private enum class TestReason(override val index: Int) : LockReason { X(0), Y(1), LAST(31) }

private data class OutOfRange(override val index: Int) : LockGroup, LockReason

class LocksTest : DescribeSpec({

    describe("LocksComponent") {
        it("is locked while any reason holds the group, and each group is separate") {
            val locks = LocksComponent(2)

            locks.add(TestGroup.A, TestReason.X)
            locks.add(TestGroup.A, TestReason.LAST)

            locks.isLocked(TestGroup.A) shouldBe true
            locks.isLocked(TestGroup.B) shouldBe false

            locks.remove(TestGroup.A, TestReason.X)

            locks.isLocked(TestGroup.A) shouldBe true
            locks.getLockGroup(TestGroup.A).isLockedBy(TestReason.LAST) shouldBe true

            locks.remove(TestGroup.A, TestReason.LAST)

            locks.isLocked(TestGroup.A) shouldBe false
        }

        it("holds a reason once: a second add needs only one remove") {
            val locks = LocksComponent(2)

            locks.add(TestGroup.B, TestReason.Y)
            locks.add(TestGroup.B, TestReason.Y)
            locks.remove(TestGroup.B, TestReason.Y)

            locks.isLocked(TestGroup.B) shouldBe false
        }

        it("ignores the remove of a reason that does not hold the group") {
            val locks = LocksComponent(2)
            locks.add(TestGroup.A, TestReason.X)

            locks.remove(TestGroup.A, TestReason.Y)

            locks.isLocked(TestGroup.A) shouldBe true
        }

        it("clear frees one group, and reset frees all") {
            val locks = LocksComponent(2)
            locks.add(TestGroup.A, TestReason.X)
            locks.add(TestGroup.B, TestReason.Y)

            locks.clear(TestGroup.A)

            locks.isLocked(TestGroup.A) shouldBe false
            locks.isLocked(TestGroup.B) shouldBe true

            locks.reset()

            locks.isLocked(TestGroup.B) shouldBe false
        }

        it("refuses a group or a reason index out of range") {
            val locks = LocksComponent(2)

            shouldThrow<IllegalArgumentException> { locks.add(OutOfRange(2), TestReason.X) }
            shouldThrow<IllegalArgumentException> { locks.add(OutOfRange(-1), TestReason.X) }
            shouldThrow<IllegalArgumentException> { locks.add(TestGroup.A, OutOfRange(LocksComponent.MAX_REASONS)) }
            shouldThrow<IllegalArgumentException> { locks.isLocked(OutOfRange(5)) }
            shouldThrow<IllegalArgumentException> { locks.getLockGroup(OutOfRange(2)) }
            shouldThrow<IllegalArgumentException> { locks.getLockGroup(OutOfRange(-1)) }
            shouldThrow<IllegalArgumentException> {
                locks.getLockGroup(TestGroup.A).isLockedBy(OutOfRange(LocksComponent.MAX_REASONS))
            }
        }

        it("getLockGroup returns the same object on each call, one per group") {
            val locks = LocksComponent(2)
            val a = locks.getLockGroup(TestGroup.A)

            locks.add(TestGroup.A, TestReason.X)
            locks.reset()

            locks.getLockGroup(TestGroup.A) shouldBeSameInstanceAs a
            (locks.getLockGroup(TestGroup.B) === a) shouldBe false
        }

        it("the group object follows add, remove, clear and reset") {
            val locks = LocksComponent(2)
            val a = locks.getLockGroup(TestGroup.A)
            val b = locks.getLockGroup(TestGroup.B)

            a.isLocked() shouldBe false
            a.isLockedBy(TestReason.X) shouldBe false

            locks.add(TestGroup.A, TestReason.X)
            locks.add(TestGroup.A, TestReason.Y)

            a.isLocked() shouldBe true
            a.isLockedBy(TestReason.X) shouldBe true
            a.isLockedBy(TestReason.Y) shouldBe true
            a.isLockedBy(TestReason.LAST) shouldBe false
            b.isLocked() shouldBe false
            b.isLockedBy(TestReason.X) shouldBe false

            locks.remove(TestGroup.A, TestReason.X)

            a.isLocked() shouldBe true
            a.isLockedBy(TestReason.X) shouldBe false
            a.isLockedBy(TestReason.Y) shouldBe true

            locks.clear(TestGroup.A)

            a.isLocked() shouldBe false
            a.isLockedBy(TestReason.Y) shouldBe false

            locks.add(TestGroup.A, TestReason.LAST)
            locks.add(TestGroup.B, TestReason.Y)
            locks.reset()

            a.isLocked() shouldBe false
            a.isLockedBy(TestReason.LAST) shouldBe false
            b.isLocked() shouldBe false
            b.isLockedBy(TestReason.Y) shouldBe false
        }

        it("calls the hook on each add, and with false when a group becomes free") {
            val calls = ArrayList<Pair<LockGroup, Boolean>>()
            val locks = LocksComponent(2) { group, locked -> calls.add(group to locked) }

            locks.add(TestGroup.A, TestReason.X)
            locks.add(TestGroup.A, TestReason.Y)
            locks.remove(TestGroup.A, TestReason.X)
            locks.remove(TestGroup.A, TestReason.Y)
            locks.add(TestGroup.B, TestReason.X)
            locks.clear(TestGroup.B)
            locks.clear(TestGroup.B)
            locks.add(TestGroup.A, TestReason.X)
            locks.reset()

            calls shouldBe listOf(
                TestGroup.A to true,
                TestGroup.A to true,
                TestGroup.A to false,
                TestGroup.B to true,
                TestGroup.B to false,
                TestGroup.A to true
            )
        }
    }

    describe("lock functions") {
        it("are not locked for an entity without a locks component, and cannot lock it") {
            val entity = MockGameEntity()

            entity.isLocked(TestGroup.A) shouldBe false
            shouldThrow<MissingComponentException> { entity.lock(TestGroup.A, TestReason.X) }
        }

        it("lock, unlock and clearLocks change the entity's locks component") {
            val entity = MockGameEntity().also { it.addComponent(LocksComponent(2)) }

            entity.lock(TestGroup.A, TestReason.X)
            entity.isLocked(TestGroup.A) shouldBe true

            entity.unlock(TestGroup.A, TestReason.X)
            entity.isLocked(TestGroup.A) shouldBe false

            entity.lock(TestGroup.A, TestReason.X)
            entity.clearLocks(TestGroup.A)
            entity.isLocked(TestGroup.A) shouldBe false
        }
    }
})
