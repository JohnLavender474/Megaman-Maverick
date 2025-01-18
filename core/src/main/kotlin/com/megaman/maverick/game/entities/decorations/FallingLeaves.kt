package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods.getRandom
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.DecorationsFactory
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.utils.extensions.toGameRectangle
import com.megaman.maverick.game.utils.misc.CullBoundsType

class FallingLeaves(game: MegamanMaverickGame) : MegaGameEntity(game), ICullableEntity {

    companion object {
        const val TAG = "FallingLeaves"
        private const val MIN_SPAWN_DELAY = 0.75f
        private const val MAX_SPAWN_DELAY = 1.5f
    }

    private val bounds = GameRectangle()
    private val spawnTimer = Timer()

    override fun init() {
        super.init()
        addComponent(CullablesComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        this.bounds.set(bounds)

        val time = getRandom(MIN_SPAWN_DELAY, MAX_SPAWN_DELAY)
        spawnTimer.resetDuration(time)

        val cullBoundsType = CullBoundsType.valueOf(
            spawnProps.getOrDefault(CullBoundsType.KEY, CullBoundsType.BODY.name, String::class).uppercase()
        )
        when (cullBoundsType) {
            CullBoundsType.BODY -> putCullable(
                ConstKeys.CULL_OUT_OF_BOUNDS,
                getGameCameraCullingLogic(game.getGameCamera(), { bounds })
            )

            CullBoundsType.OTHER -> {
                val other = spawnProps.get(ConstKeys.OTHER, RectangleMapObject::class)!!.rectangle.toGameRectangle(false)
                putCullable(ConstKeys.CULL_OUT_OF_BOUNDS, getGameCameraCullingLogic(game.getGameCamera(), { other }))
            }
        }
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
    }

    private fun spawnLeaf() {
        val x = getRandom(bounds.getX(), bounds.getMaxX())
        val y = bounds.getMaxY()
        val leaf = EntityFactories.fetch(EntityType.DECORATION, DecorationsFactory.FALLING_LEAF)!!
        leaf.spawn(props(ConstKeys.X pairTo x, ConstKeys.Y pairTo y))
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        spawnTimer.update(delta)
        if (spawnTimer.isFinished()) {
            spawnLeaf()
            val time = getRandom(MIN_SPAWN_DELAY, MAX_SPAWN_DELAY)
            spawnTimer.resetDuration(time)
        }
    })

    override fun getType() = EntityType.DECORATION

    override fun getTag() = TAG
}
