package com.megaman.maverick.game.world.contacts.rules

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectSet
import com.maverick.game.engine.common.enums.ProcessState
import com.maverick.game.engine.common.objects.GamePair
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.shapes.GameLine
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.common.shapes.ShapeUtils
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.IFixture
import com.maverick.game.engine.world.contacts.IContactRule
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.contracts.ILaserEntity
import com.megaman.maverick.game.utils.extensions.getBoundingRectangle
import com.megaman.maverick.game.world.body.*

class LaserBlockRule : IContactRule {

    private val tempVec2Set = ObjectSet<Vector2>()

    override fun onBegin(a: IFixture, b: IFixture, delta: Float) = begin(a, b)

    override fun onContinue(a: IFixture, b: IFixture, delta: Float) = continueContact(a, b)

    private fun begin(laserFixture: IFixture, blockFixture: IFixture) {
        val blockBody = blockFixture.getBody() as Body
        // If the block's body also has a shield fixture, then ignore the block. This assumes that the
        // block's shield fixture and block fixture are the same size and there is only one shield fixture.
        if (blockBody.fixtures.containsKey(FixtureType.SHIELD)) return

        val laserEntity = laserFixture.getEntity() as ILaserEntity
        val blockEntity = blockFixture.getEntity() as Block

        if (laserEntity != blockEntity && !laserEntity.isLaserIgnoring(blockEntity)) {
            val blockRectangle = blockFixture.getShape() as GameRectangle
            val laserLine = laserFixture.getShape() as GameLine

            val intersections =
                laserFixture.getProperty(ConstKeys.COLLECTION) as MutableCollection<GamePair<Vector2, IFixture>>

            if (ShapeUtils.intersectRectangleAndLine(blockRectangle, laserLine, tempVec2Set))
                tempVec2Set.forEach { intersections.add(it pairTo blockFixture) }

            tempVec2Set.clear()
        }
    }

    private fun continueContact(laserFixture: IFixture, blockFixture: IFixture) {
        val blockBody = blockFixture.getBody() as Body
        // If the block's body also has a shield fixture, then ignore the block. This assumes that the
        // block's shield fixture and block fixture are the same size and there is only one shield fixture.
        if (blockBody.fixtures.containsKey(FixtureType.SHIELD)) return

        val laserEntity = laserFixture.getEntity() as ILaserEntity
        val blockEntity = blockFixture.getEntity() as Block

        if (laserEntity != blockEntity && !laserEntity.isLaserIgnoring(blockEntity)) {
            val blockRectangle = blockFixture.getShape().getBoundingRectangle()
            val laserLine = laserFixture.getShape() as GameLine

            val intersections =
                laserFixture.getProperty(ConstKeys.COLLECTION) as MutableCollection<GamePair<Vector2, IFixture>>

            if (ShapeUtils.intersectRectangleAndLine(blockRectangle, laserLine, tempVec2Set))
                tempVec2Set.forEach { intersections.add(it pairTo blockFixture) }

            tempVec2Set.clear()
        }
    }
}

object LaserBodyRule : IContactRule {

    override fun onBegin(a: IFixture, b: IFixture, delta: Float) = hit(ProcessState.BEGIN, a, b)

    override fun onContinue(a: IFixture, b: IFixture, delta: Float) = hit(ProcessState.CONTINUE, a, b)

    private fun hit(state: ProcessState, laserFixture: IFixture, bodyFixture: IFixture) {
        val laserEntity = laserFixture.getEntity() as ILaserEntity
        val bodyEntity = bodyFixture.getEntity() as IBodyEntity

        if (laserEntity != bodyEntity && !laserEntity.isLaserIgnoring(bodyEntity)) {
            laserFixture.receiver(Receivers.HIT_BY_BODY)?.invoke(bodyEntity, state)
        }
    }
}

class LaserShieldRule : IContactRule {

    private val tempVec2Set = ObjectSet<Vector2>()

    override fun onBegin(a: IFixture, b: IFixture, delta: Float) = hit(a, b)

    override fun onContinue(a: IFixture, b: IFixture, delta: Float) = hit(a, b)

    private fun hit(laserFixture: IFixture, shieldFixture: IFixture) {
        val laserEntity = laserFixture.getEntity() as ILaserEntity
        val shieldEntity = shieldFixture.getEntity()

        if (laserEntity != shieldEntity && !laserEntity.isLaserIgnoring(shieldEntity)) {
            val shieldRectangle = shieldFixture.getShape().getBoundingRectangle()
            val laserLine = laserFixture.getShape() as GameLine

            val intersections =
                laserFixture.getProperty(ConstKeys.COLLECTION) as MutableCollection<GamePair<Vector2, IFixture>>

            if (ShapeUtils.intersectRectangleAndLine(shieldRectangle, laserLine, tempVec2Set))
                tempVec2Set.forEach { intersections.add(it pairTo shieldFixture) }

            tempVec2Set.clear()
        }
    }
}
