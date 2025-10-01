package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.entities.contracts.IMotionEntity
import com.mega.game.engine.motion.MotionComponent
import com.mega.game.engine.motion.MotionComponent.MotionDefinition
import com.mega.game.engine.motion.Trajectory
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter

class PreciousBlock(game: MegamanMaverickGame) : AnimatedBlock(game), IMotionEntity {

    companion object {
        const val TAG = "PreciousBlock"
    }

    private var spawn: Vector2? = null

    override fun init() {
        GameLogger.debug(TAG, "init()")
        super.init()
        addComponent(MotionComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.ANIMATION, TAG)
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")

        super.onSpawn(spawnProps)

        if (spawnProps.containsKey("${ConstKeys.TRAJECTORY}_${ConstKeys.DEF}")) {
            val trajectoryDefinition = spawnProps.get("${ConstKeys.TRAJECTORY}_${ConstKeys.DEF}", String::class)!!

            val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
            spawn = bounds.getCenter(false)
            body.setCenter(spawn!!)

            val trajectory = Trajectory(trajectoryDefinition, ConstVals.PPM)
            putMotionDefinition(
                ConstKeys.TRAJECTORY, MotionDefinition(
                    motion = trajectory,
                    onReset = { spawn?.let { body.setCenter(it) } },
                    function = { value, _ -> body.physics.velocity.set(value) }
                )
            )
        }
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        if (this.spawn != null) {
            GameObjectPools.free(this.spawn!!)
            this.spawn = null
        }
    }
}
