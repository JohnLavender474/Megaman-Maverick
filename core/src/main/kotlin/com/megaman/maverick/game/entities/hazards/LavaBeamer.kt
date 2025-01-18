package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.extensions.toGdxArray
import com.mega.game.engine.common.interfaces.IDirectional
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
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.entities.contracts.*
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.HazardsFactory
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.getCenter
import com.megaman.maverick.game.world.body.getPositionPoint

class LavaBeamer(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ICullableEntity, ISpritesEntity,
    IAnimatedEntity, IAudioEntity, IHazard, IDirectional {

    companion object {
        const val TAG = "LavaBeamer"
        private const val IDLE_DUR = 1.25f
        private const val SWITCHING_ON_DUR = 0.5f
        private const val FIRING_DUR = 0.25f
        private const val FIRE_SPEED = 12f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class LavaBeamerState {
        IDLE,
        SWITCHING_ON,
        FIRING
    }

    override var direction: Direction
        get() = body.direction
        set(value) {
            body.direction = value
        }

    private val loop = Loop(LavaBeamerState.entries.toTypedArray().toGdxArray())
    private val timers = objectMapOf(
        LavaBeamerState.IDLE pairTo Timer(IDLE_DUR),
        LavaBeamerState.SWITCHING_ON pairTo Timer(SWITCHING_ON_DUR),
        LavaBeamerState.FIRING pairTo Timer(FIRING_DUR)
    )
    private lateinit var cullOnEvents: CullableOnEvent

    override fun getType() = EntityType.HAZARD

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.HAZARDS_1.source)
            regions.put("on", atlas.findRegion("$TAG/On"))
            regions.put("off", atlas.findRegion("$TAG/Off"))
            regions.put("switch", atlas.findRegion("$TAG/SwitchingOn"))
        }
        addComponent(defineUpdatablesComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
        addComponent(AudioComponent())
        addComponent(CullablesComponent())

        val cullEvents = objectSetOf<Any>(
            EventType.GAME_OVER,
            EventType.PLAYER_SPAWN,
            EventType.BEGIN_ROOM_TRANS,
            EventType.GATE_INIT_OPENING
        )
        cullOnEvents = CullableOnEvent({ cullEvents.contains(it.key) }, cullEvents)
        putCullable(ConstKeys.CULL_EVENTS, cullOnEvents)
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        direction =
            Direction.valueOf(spawnProps.getOrDefault(ConstKeys.DIRECTION, "up", String::class).uppercase())
        val position = when (direction) {
            Direction.UP -> Position.TOP_CENTER
            Direction.DOWN -> Position.BOTTOM_CENTER
            Direction.LEFT -> Position.CENTER_LEFT
            Direction.RIGHT -> Position.CENTER_RIGHT
        }
        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        body.positionOnPoint(bounds.getPositionPoint(position), position)
        loop.reset()
        timers.values().forEach { it.reset() }
        game.eventsMan.addListener(cullOnEvents)
    }

    override fun onDestroy() {
        super.onDestroy()
        game.eventsMan.removeListener(cullOnEvents)
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        val timer = timers.get(loop.getCurrent())
        timer.update(delta)
        if (timer.isFinished()) {
            loop.next()
            if (loop.getCurrent() == LavaBeamerState.FIRING) fireLava()
            timer.reset()
        }
    })

    private fun fireLava() {
        val spawn = when (direction) {
            Direction.UP -> body.getPositionPoint(Position.TOP_CENTER)
            Direction.DOWN -> body.getPositionPoint(Position.BOTTOM_CENTER)
            Direction.LEFT -> body.getPositionPoint(Position.CENTER_LEFT)
            Direction.RIGHT -> body.getPositionPoint(Position.CENTER_RIGHT)
        }
        val lavaBeam = EntityFactories.fetch(EntityType.HAZARD, HazardsFactory.LAVA_BEAM)!!
        lavaBeam.spawn(
            props(
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.DIRECTION pairTo direction,
                ConstKeys.SPEED pairTo FIRE_SPEED * ConstVals.PPM
            )
        )
        if (overlapsGameCamera()) requestToPlaySound(SoundAsset.WHEE_SOUND, false)
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(3f * ConstVals.PPM, 2f * ConstVals.PPM)
        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 10))
        sprite.setSize(3f * ConstVals.PPM, 2f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.setOriginCenter()
            sprite.rotation = direction.rotation
            sprite.setCenter(body.getCenter())
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = {
            when (loop.getCurrent()) {
                LavaBeamerState.IDLE -> "off"
                LavaBeamerState.SWITCHING_ON -> "switch"
                LavaBeamerState.FIRING -> "on"
            }
        }
        val animations = objectMapOf<String, IAnimation>(
            "off" pairTo Animation(regions["off"]),
            "switch" pairTo Animation(regions["switch"], 2, 1, 0.1f, true),
            "on" pairTo Animation(regions["on"])
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}
