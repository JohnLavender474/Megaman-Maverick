package com.megaman.maverick.game.entities.explosions

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.state.TagsComponent
import com.megaman.maverick.game.testServices
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class ExplosionOrbTest : DescribeSpec({

    lateinit var orb: ExplosionOrb

    beforeEach {
        orb = ExplosionOrb(testServices())
        orb.addComponent(BodyComponent(Body(BodyType.ABSTRACT)))
        orb.addComponent(TagsComponent(ExplosionOrb.TAG))
    }

    describe("the explosion orb speed") {
        it("uses the default speed when the spawn props have no speed") {
            orb.onSpawn(props(ConstKeys.POSITION pairTo Vector2(), ConstKeys.TRAJECTORY pairTo Vector2(-1f, 1f)))

            orb.body.physics.velocity shouldBe Vector2(-112f, 112f)
        }

        it("uses the speed in the spawn props") {
            orb.onSpawn(
                props(
                    ConstKeys.POSITION pairTo Vector2(),
                    ConstKeys.TRAJECTORY pairTo Vector2(-1f, 1f),
                    ConstKeys.SPEED pairTo 2f
                )
            )

            orb.body.physics.velocity shouldBe Vector2(-64f, 64f)
        }
    }
})
