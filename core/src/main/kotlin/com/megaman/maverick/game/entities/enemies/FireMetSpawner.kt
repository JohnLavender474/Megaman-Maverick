package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.extensions.toGdxArray
import com.mega.game.engine.common.objects.Loop
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.cullables.CullableOnEvent
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.GameEntity
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.entities.contracts.*
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.EnemiesFactory
import com.megaman.maverick.game.events.EventType

import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.getCenter
import com.megaman.maverick.game.world.body.getPositionPoint

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

    private val loop = Loop(FireMetSpawnerState.entries.toTypedArray().toGdxArray())
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
        addComponent(defineBodyComponent())
        addComponent(defineUpdatablesComponent())
        addComponent(defineCullablesComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.TOP_CENTER)
        body.setTopCenterToPoint(spawn)

        maxToSpawn = spawnProps.getOrDefault(ConstKeys.MAX, DEFAULT_MAX_SPAWNED, Int::class)

        loop.reset()
        closedTimer.reset()
        transTimer.reset()
        openTimer.reset()
    }

    override fun onDestroy() {
        super.onDestroy()
        children.forEach { (it as GameEntity).destroy() }
        children.clear()
    }

    private fun spawnFireMet() {
        val fireMet = EntityFactories.fetch(EntityType.ENEMY, EnemiesFactory.FIRE_MET)!!
        fireMet.spawn(props(ConstKeys.POSITION pairTo body.getCenter()))
        children.add(fireMet)
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        val iter = children.iterator()
        while (iter.hasNext()) {
            val child = iter.next() as MegaGameEntity
            if (child.dead) iter.remove()
        }

        if (children.size < maxToSpawn) {
            val fireMetSpawnerState = loop.getCurrent()
            val timer = when (fireMetSpawnerState) {
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
        return CullablesComponent(objectMapOf(ConstKeys.CULL_EVENTS pairTo cullOnEvents))
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 0))
        sprite.setSize(1.5f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.setPosition(body.getPositionPoint(Position.TOP_CENTER), Position.TOP_CENTER)
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
            "closed" pairTo Animation(regions.get("closed")),
            "opening" pairTo Animation(regions.get("opening"), 2, 2, 0.1f, false),
            "open" pairTo Animation(regions.get("open")),
            "closing" pairTo Animation(regions.get("opening"), 2, 2, 0.1f, false).reversed()
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}
