package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.targetPlayer
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getRotatedBounds

class TestChargedShooter(services: MegaServices) : MegaGameEntity(services) {

    companion object {
        const val TAG = "TestChargedShooter"
        private const val DEFAULT_COUNT = 1
        private const val DEFAULT_INTERVAL = 1.5f
    }

    private val bounds = GameRectangle()
    private val timer = Timer(DEFAULT_INTERVAL)
    private var fullyCharged = false
    private var count = DEFAULT_COUNT
    private var shotsFired = 0
    private var armed = true
    private var velocityX = 0f

    override fun getType() = EntityType.SPECIAL

    override fun init(vararg params: Any) {
        super.init()
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)
        bounds.set(spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!)
        fullyCharged = spawnProps.getOrDefault(ConstKeys.FULL, false, Boolean::class)
        count = spawnProps.getOrDefault(ConstKeys.COUNT, DEFAULT_COUNT, Int::class)
        timer.resetDuration(spawnProps.getOrDefault(ConstKeys.INTERVAL, DEFAULT_INTERVAL, Float::class))
        val speed = spawnProps.getOrDefault(ConstKeys.SPEED, MegamanValues.BULLET_VEL, Float::class)
        velocityX = speed * ConstVals.PPM * directionSign(spawnProps)
        shotsFired = 0
        armed = true
    }

    private fun directionSign(spawnProps: Properties): Float {
        val direction = spawnProps.get(ConstKeys.DIRECTION, String::class)
        return when (direction) {
            "left" -> -1f
            "right" -> 1f
            else -> throw IllegalArgumentException(
                "$TAG (id=${spawnProps.get(ConstKeys.ID)}) has ${ConstKeys.DIRECTION}=$direction. " +
                    "Set it to left or right."
            )
        }
    }

    private fun fire() {
        GameLogger.debug(TAG, "fire(): shot ${shotsFired + 1} of $count, fullyCharged=$fullyCharged")
        MegaEntityFactory.fetch(ChargedShot::class)!!.spawn(
            props(
                ConstKeys.OWNER pairTo targetPlayer,
                ConstKeys.POSITION pairTo bounds.getCenter(),
                ConstKeys.TRAJECTORY pairTo GameObjectPools.fetch(Vector2::class).set(velocityX, 0f),
                ConstKeys.BOOLEAN pairTo fullyCharged
            )
        )
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        if (!services.gameCamera.getRotatedBounds().overlaps(bounds)) {
            armed = true
            shotsFired = 0
            timer.reset()
            return@UpdatablesComponent
        }
        if (!armed) return@UpdatablesComponent

        timer.update(delta)
        if (!timer.isFinished()) return@UpdatablesComponent

        fire()
        shotsFired++
        timer.reset()
        if (shotsFired >= count) armed = false
    })
}
