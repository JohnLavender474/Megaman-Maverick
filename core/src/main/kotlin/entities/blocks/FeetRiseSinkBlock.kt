package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.utils.OrderedSet
import com.mega.game.engine.common.extensions.isAny
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.world.body.getEntity
import kotlin.math.abs

class FeetRiseSinkBlock(game: MegamanMaverickGame) : Block(game) {

    companion object {
        const val TAG = "FeetRiseSinkBlock"
    }

    private val feetSet = OrderedSet<IFixture>()

    private var maxY = 0f
    private var minY = 0f
    private var fallingSpeed = 0f
    private var risingSpeed = 0f

    override fun init() {
        super.init()
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val max = abs(spawnProps.getOrDefault(ConstKeys.MAX, 0f, Float::class))
        maxY = body.getMaxY() + (max * ConstVals.PPM)

        val min = abs(spawnProps.getOrDefault(ConstKeys.MIN, 0f, Float::class))
        minY = body.y - (min * ConstVals.PPM)

        fallingSpeed = -abs(spawnProps.getOrDefault(ConstKeys.FALL, 0f, Float::class))
        risingSpeed = abs(spawnProps.getOrDefault(ConstKeys.RISE, 0f, Float::class))
    }

    private fun shouldListenToFeet(feetFixture: IFixture): Boolean {
        val entity = feetFixture.getEntity()
        return entity.isAny(Megaman::class, PushableBlock::class)
    }

    override fun hitByFeet(feetFixture: IFixture) {
        if (shouldListenToFeet(feetFixture)) feetSet.add(feetFixture)
    }

    override fun defineBodyComponent(): BodyComponent {
        val bodyComponent = super.defineBodyComponent()
        bodyComponent.body.preProcess.put(ConstKeys.MOVE) {
            when {
                !feetSet.isEmpty -> when {
                    body.y > minY -> body.physics.velocity.y = fallingSpeed * ConstVals.PPM
                    else -> body.physics.velocity.y = 0f
                }

                body.getMaxY() < maxY -> body.physics.velocity.y = risingSpeed * ConstVals.PPM
                else -> body.physics.velocity.y = 0f
            }

            when {
                body.getMaxY() > maxY -> body.setMaxY(maxY)
                body.y < minY -> body.y = minY
            }
        }
        return bodyComponent
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({
        val iter = feetSet.iterator()
        while (iter.hasNext) {
            val feet = iter.next()
            if (!feet.getShape().overlaps(body)) iter.remove()
        }
    })
}
