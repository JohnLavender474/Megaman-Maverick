package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.entities.EntityExistenceState
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.enemies.Met
import com.megaman.maverick.game.entities.hazards.MetHelmet
import com.megaman.maverick.game.entities.state.mapObjectId

class TestHelmetMover(services: MegaServices) : MegaGameEntity(services) {

    companion object {
        const val TAG = "TestHelmetMover"
    }

    private val drop = Vector2()
    private var targetId = 0
    private var lastMoved: MetHelmet? = null

    override fun getType() = EntityType.SPECIAL

    override fun init(vararg params: Any) {
        super.init()
        addComponent(UpdatablesComponent({ update() }))
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)
        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        drop.set(bounds.getX() + bounds.getWidth() / 2f, bounds.getY())
        val target = spawnProps.get(ConstKeys.TARGET, RectangleMapObject::class)!!
        if (target.name != Met.TAG) throw IllegalArgumentException(
            "$TAG (id=${spawnProps.get(ConstKeys.ID)}) has ${ConstKeys.TARGET} -> ${target.name}. " +
                "Point ${ConstKeys.TARGET} at a ${Met.TAG} map object."
        )
        targetId = target.properties.get(ConstKeys.ID, Int::class.java)
        lastMoved = null
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        lastMoved = null
    }

    private fun update() {
        val last = lastMoved
        if (last != null &&
            (last.existenceState != EntityExistenceState.LIVE || last.status == MetHelmet.HelmetStatus.SEATED)
        ) lastMoved = null

        for (entity in MegaGameEntities.getOfTag(MetHelmet.TAG)) {
            if (entity !is MetHelmet || entity === lastMoved) continue
            if (entity.status != MetHelmet.HelmetStatus.FLYING || entity.owner?.mapObjectId != targetId) continue
            GameLogger.debug(TAG, "update(): move helmet to drop=$drop")
            entity.body.setBottomCenterToPoint(drop.x, drop.y)
            entity.body.physics.velocity.setZero()
            lastMoved = entity
        }
    }
}
