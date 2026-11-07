package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.players.forEach
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.entities.state.CrusherComponent
import com.megaman.maverick.game.entities.state.worldToBodySide
import com.megaman.maverick.game.players.asPlayer
import com.megaman.maverick.game.world.body.FixtureLabel
import com.megaman.maverick.game.world.body.addBlockFilter
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.removeFixtureLabel

class CameraEdgeBlock(services: MegaServices) : AbstractBlock(services) {

    companion object {
        const val TAG = "CameraEdgeBlock"
    }

    var crushing = false
    var crushSurfaces = Array<GameRectangle>()

    private lateinit var side: Direction

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        super.init()

        val crusher = CrusherComponent(
            { body.getBounds() },
            { worldToBodySide(side, it.body.direction) },
            { crushing },
            { shape -> crushSurfaces.any { shape.overlaps(it) } }
        )
        addComponent(crusher)

        body.preProcess.put(ConstKeys.DEATH) {
            players.forEach { if (it.body.physics.collisionOn) crusher.crush(it) }
        }
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")

        side = spawnProps.get(ConstKeys.DIRECTION, Direction::class)!!
        crushing = false
        crushSurfaces = Array()

        spawnProps.put(ConstKeys.CULL_OUT_OF_BOUNDS, false)
        spawnProps.put(ConstKeys.DRAW, false)
        spawnProps.put(ConstKeys.DO_UPDATE, { true })

        super.onSpawn(spawnProps)

        if (spawnProps.getOrDefault(ConstKeys.FEET, false, Boolean::class))
            blockFixture.removeFixtureLabel(FixtureLabel.NO_FEET_TOUCHIE)

        body.addBlockFilter { entity, _ -> entity.asPlayer == null }
    }

    override fun getTag() = TAG
}
