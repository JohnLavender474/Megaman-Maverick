package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.utils.OrbitUtils
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.bosses.PreciousWoman.Companion.SHIELD_GEM_MAX_DIST_FROM_ROOM_CENTER
import com.megaman.maverick.game.entities.bosses.PreciousWoman.Companion.SHIELD_GEM_SPIN_SPEED
import com.megaman.maverick.game.entities.bosses.PreciousWoman.ShieldGemDef
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.overlaps
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter

class PreciousGemCluster(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity {

    companion object {
        const val TAG = "PreciousGemCluster"
        private const val DIST_FROM_CENTER_DELTA = 2f
    }

    private val gems = OrderedMap<PreciousGem, ShieldGemDef>()
    private val roomCenter = Vector2()

    override fun init() {
        GameLogger.debug(TAG, "init()")
        super.init()
        addComponent(defineUpdatablesComponent())
        addComponent(defineBodyComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")

        super.onSpawn(spawnProps)

        val position = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(position)

        val trajectory = spawnProps.get(ConstKeys.TRAJECTORY, Vector2::class)!!
        body.physics.velocity.set(trajectory)

        roomCenter.set(spawnProps.get("${ConstKeys.ROOM}_${ConstKeys.CENTER}", Vector2::class)!!)

        val gems = spawnProps.get(PreciousGem.TAG) as OrderedMap<PreciousGem, ShieldGemDef>
        this.gems.putAll(gems)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")

        super.onDestroy()

        gems.keys().forEach { it.destroy() }
        gems.clear()
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        gems.forEach { entry ->
            val gem = entry.key
            val def = entry.value

            var (angle, distance, released) = def

            if (released) distance += DIST_FROM_CENTER_DELTA * ConstVals.PPM * delta

            angle += SHIELD_GEM_SPIN_SPEED * 360f * delta
            val center = OrbitUtils.calculateOrbitalPosition(
                angle,
                distance,
                body.getCenter(),
                GameObjectPools.fetch(Vector2::class)
            )
            gem.body.setCenter(center)

            def.set(angle, distance, released)
        }

        val iter = gems.iterator()
        while (iter.hasNext) {
            val gem = iter.next().key

            when {
                gem.dead -> iter.remove()

                !game.getGameCamera().overlaps(gem.body.getBounds()) &&
                    gem.body.getCenter().dst(roomCenter) > SHIELD_GEM_MAX_DIST_FROM_ROOM_CENTER * ConstVals.PPM -> {
                    gem.destroy()
                    iter.remove()
                }
            }
        }

        if (gems.isEmpty) destroy()
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ConstVals.PPM.toFloat())
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun getType() = EntityType.PROJECTILE

    override fun getTag() = TAG
}
