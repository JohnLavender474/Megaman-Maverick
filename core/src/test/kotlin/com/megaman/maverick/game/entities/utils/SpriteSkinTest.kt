package com.megaman.maverick.game.entities.utils

import com.maverick.game.engine.common.extensions.objectSetOf
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.megaman.maverick.game.ConstKeys
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class SpriteSkinTest : DescribeSpec({

    val skins = objectSetOf(SpriteSkin.DEFAULT, SpriteSkin.LAB)

    describe("of") {

        it("gives DEFAULT when the property is absent") {
            SpriteSkin.of("TestEntity", props(), skins) shouldBe SpriteSkin.DEFAULT
        }

        it("gives a skin of the set in any case") {
            SpriteSkin.of("TestEntity", props(ConstKeys.COLOR pairTo "Lab"), skins) shouldBe SpriteSkin.LAB
        }

        it("throws for a skin that is not in the set and names the class") {
            val error = shouldThrow<IllegalArgumentException> {
                SpriteSkin.of("TestEntity", props(ConstKeys.COLOR pairTo "wily"), skins)
            }
            error.message shouldContain "'wily'"
            error.message shouldContain "default, lab"
            error.message shouldContain "TestEntity"
        }

        it("throws for an unknown value") {
            shouldThrow<IllegalArgumentException> {
                SpriteSkin.of("TestEntity", props(ConstKeys.COLOR pairTo "blue"), skins)
            }
        }
    }
})
