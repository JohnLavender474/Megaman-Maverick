package com.megaman.maverick.game.entities.blocks

import com.engine.common.objects.Properties
import com.engine.world.BodyComponent
import com.engine.world.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame

import com.megaman.maverick.game.world.FixtureType

class FeetRiseSinkBlock(game: MegamanMaverickGame) : Block(game) {

    companion object {
        const val TAG = "FeetRiseSinkBlock"
    }

    private lateinit var megamanFeet: IFixture

    private var maxY = 0f
    private var minY = 0f

    private var fallingSpeed = 0f
    private var risingSpeed = 0f

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)

        val max = spawnProps.getOrDefault(ConstKeys.MAX, 0f, Float::class)
        maxY = body.getMaxY() + (max * ConstVals.PPM)

        val min = spawnProps.getOrDefault(ConstKeys.MIN, 0f, Float::class)
        minY = body.y - (min * ConstVals.PPM)

        fallingSpeed = spawnProps.getOrDefault(ConstKeys.FALL, 0f, Float::class)
        risingSpeed = spawnProps.getOrDefault(ConstKeys.RISE, 0f, Float::class)

        megamanFeet = getMegaman().body.fixtures.first { it.second.getFixtureType() == FixtureType.FEET }.second
    }

    override fun defineBodyComponent(): BodyComponent {
        val bodyComponent = super.defineBodyComponent()
        bodyComponent.body.preProcess.put(ConstKeys.MOVE) {
            if (megamanFeet.getShape().overlaps(body)) {
                if (body.y > minY) body.physics.velocity.y = fallingSpeed * ConstVals.PPM
                else body.physics.velocity.y = 0f
            } else if (body.getMaxY() < maxY) body.physics.velocity.y = risingSpeed * ConstVals.PPM
            else body.physics.velocity.y = 0f

            if (body.getMaxY() > maxY) body.setMaxY(maxY)
            else if (body.y < minY) body.y = minY
        }
        return bodyComponent
    }
}