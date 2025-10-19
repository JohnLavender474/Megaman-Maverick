package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.utils.OrbitUtils
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.events.Event
import com.mega.game.engine.events.IEventListener
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.bosses.PreciousWoman.Companion.SHIELD_GEM_MAX_DIST_FROM_ORIGIN
import com.megaman.maverick.game.entities.bosses.PreciousWoman.ShieldGemDef
import com.megaman.maverick.game.entities.contracts.IOwnable
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.overlaps
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter

class PreciousGemCluster(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, IEventListener,
    IOwnable<IGameEntity> {

    companion object {
        const val TAG = "PreciousGemCluster"
        const val DEFAULT_SHIELD_GEM_SPIN_SPEED = 0.25f
        private const val DEFAULT_DIST_FROM_CENTER_DELTA = 3f
    }

    override val eventKeyMask = objectSetOf<Any>(EventType.PLAYER_JUST_DIED)
    override var owner: IGameEntity? = null

    val gems = OrderedMap<PreciousGem, ShieldGemDef>()
    var spinSpeed = DEFAULT_SHIELD_GEM_SPIN_SPEED
    var distDeltaOnRelease = 0f
    val origin = Vector2()

    private var maxDistFromOrigin = 0f

    override fun init() {
        GameLogger.debug(TAG, "init()")
        super.init()
        addComponent(defineBodyComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        game.eventsMan.addListener(this)

        owner = spawnProps.get(ConstKeys.OWNER, IGameEntity::class)

        val position = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(position)

        val trajectory = spawnProps.getOrDefault(ConstKeys.TRAJECTORY, Vector2.Zero, Vector2::class)
        body.physics.velocity.set(trajectory)

        origin.set(spawnProps.get(ConstKeys.ORIGIN, Vector2::class)!!)

        val gems = spawnProps.get(PreciousGem.TAG) as OrderedMap<PreciousGem, ShieldGemDef>
        this.gems.putAll(gems)

        maxDistFromOrigin = spawnProps.getOrDefault(
            "${ConstKeys.MAX}_${ConstKeys.DISTANCE}",
            SHIELD_GEM_MAX_DIST_FROM_ORIGIN * ConstVals.PPM,
            Float::class
        )

        distDeltaOnRelease = spawnProps.getOrDefault(
            "${ConstKeys.DISTANCE}_${ConstKeys.DELTA}",
            DEFAULT_DIST_FROM_CENTER_DELTA * ConstVals.PPM,
            Float::class
        )

        spinSpeed =
            spawnProps.getOrDefault("${ConstKeys.SPIN}_${ConstKeys.SPEED}", DEFAULT_SHIELD_GEM_SPIN_SPEED, Float::class)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        game.eventsMan.removeListener(this)

        gems.keys().forEach { it.destroy() }
        gems.clear()
    }

    override fun onEvent(event: Event) {
        GameLogger.debug(TAG, "onEvent(): event=$event")
        if (owner == megaman && event.key == EventType.PLAYER_JUST_DIED) destroy()
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        gems.forEach { entry ->
            val gem = entry.key
            val def = entry.value
            var (angle, distance, released) = def

            if (released) distance += distDeltaOnRelease * delta

            angle += spinSpeed * 360f * delta

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
                gem.dead -> {
                    GameLogger.debug(TAG, "defineUpdatablesComponent(): gem dead, removing: $gem")
                    iter.remove()
                }
                !game.getGameCamera().overlaps(gem.body.getBounds()) &&
                    gem.body.getCenter().dst(origin) > maxDistFromOrigin -> {
                    GameLogger.debug(TAG, "defineUpdatablesComponent(): gem out of cam bounds: $gem")
                    gem.destroy()
                    iter.remove()
                }
            }
        }

        if (gems.isEmpty && !dead) destroy()
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
