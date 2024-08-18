package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.enums.Position
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.extensions.objectSetOf
import com.engine.common.extensions.toGdxArray
import com.engine.common.objects.Loop
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.cullables.CullableOnEvent
import com.engine.cullables.CullablesComponent
import com.engine.drawables.sorting.DrawingPriority
import com.engine.drawables.sorting.DrawingSection
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.drawables.sprites.setSize
import com.engine.entities.IGameEntity
import com.engine.entities.contracts.*
import com.engine.updatables.UpdatablesComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.EnemiesFactory
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.world.BodyComponentCreator

class FireMetSpawner(game: MegamanMaverickGame) : MegaGameEntity(game), IParentEntity, IBodyEntity, ICullableEntity,
    ISpritesEntity, IAnimatedEntity, IHazard {

    companion object {
        const val TAG = "FireMetSpawner"
        private const val CLOSED_DUR = 0.75f
        private const val TRANS_DUR = 0.45f
        private const val OPEN_DUR = 0.25f
        private const val DEFAULT_MAX_SPAWNED = 3
        private val regions = ObjectMap<String, TextureRegion>()
    }

    enum class FireMetSpawnerState { CLOSED, OPENING, SPAWNING, CLOSING }

    override var children = Array<IGameEntity>()

    private val loop = Loop(FireMetSpawnerState.values().toGdxArray())
    private val closedTimer = Timer(CLOSED_DUR)
    private val transTimer = Timer(TRANS_DUR)
    private val openTimer = Timer(OPEN_DUR)
    private var maxToSpawn = DEFAULT_MAX_SPAWNED

    override fun getEntityType() = EntityType.HAZARD

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            regions.put("closed", atlas.findRegion("$TAG/Closed"))
            regions.put("opening", atlas.findRegion("$TAG/Opening"))
            regions.put("open", atlas.findRegion("$TAG/Open"))
        }
        super<MegaGameEntity>.init()
        addComponent(defineBodyComponent())
        addComponent(defineUpdatablesComponent())
        addComponent(defineCullablesComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getTopCenterPoint()
        body.setTopCenterToPoint(spawn)

        maxToSpawn = spawnProps.getOrDefault(ConstKeys.MAX, DEFAULT_MAX_SPAWNED, Int::class)

        loop.reset()
        closedTimer.reset()
        transTimer.reset()
        openTimer.reset()
    }

    override fun onDestroy() {
        super<MegaGameEntity>.onDestroy()
        children.forEach { it.kill() }
        children.clear()
    }

    private fun spawnFireMet() {
        val fireMet = EntityFactories.fetch(EntityType.ENEMY, EnemiesFactory.FIRE_MET)!!
        game.engine.spawn(fireMet, props(ConstKeys.POSITION to body.getCenter()))
        children.add(fireMet)
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        val iter = children.iterator()
        while (iter.hasNext()) {
            val child = iter.next()
            if (child.dead) iter.remove()
        }

        if (children.size < maxToSpawn) {
            val state = loop.getCurrent()
            val timer = when (state) {
                FireMetSpawnerState.CLOSED -> closedTimer
                FireMetSpawnerState.OPENING, FireMetSpawnerState.CLOSING -> transTimer
                FireMetSpawnerState.SPAWNING -> openTimer
            }
            timer.update(delta)
            if (timer.isFinished()) {
                timer.reset()
                loop.next()
                if (loop.getCurrent() == FireMetSpawnerState.SPAWNING) spawnFireMet()
            }
        }
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(2f * ConstVals.PPM)
        return BodyComponentCreator.create(this, body)
    }

    private fun defineCullablesComponent(): CullablesComponent {
        val eventsToCullOn = objectSetOf<Any>(
            EventType.GAME_OVER, EventType.PLAYER_SPAWN, EventType.BEGIN_ROOM_TRANS, EventType.GATE_INIT_OPENING
        )
        val cullOnEvents = CullableOnEvent({ eventsToCullOn.contains(it.key) }, eventsToCullOn)
        return CullablesComponent(objectMapOf(ConstKeys.CULL_EVENTS to cullOnEvents))
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 0))
        sprite.setSize(1.5f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setPosition(body.getTopCenterPoint(), Position.TOP_CENTER)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = {
            if (children.size < maxToSpawn) when (loop.getCurrent()) {
                FireMetSpawnerState.CLOSED -> "closed"
                FireMetSpawnerState.OPENING -> "opening"
                FireMetSpawnerState.SPAWNING -> "open"
                FireMetSpawnerState.CLOSING -> "closing"
            } else "closed"
        }
        val animations = objectMapOf<String, IAnimation>(
            "closed" to Animation(regions.get("closed")),
            "opening" to Animation(regions.get("opening"), 2, 2, 0.1f, false),
            "open" to Animation(regions.get("open")),
            "closing" to Animation(regions.get("opening"), 2, 2, 0.1f, false).reversed()
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}