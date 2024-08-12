package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.ObjectMap
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.audio.AudioComponent
import com.engine.common.enums.Direction
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
import com.engine.drawables.sprites.setCenter
import com.engine.entities.contracts.*
import com.engine.updatables.UpdatablesComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.IDirectionRotatable
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.HazardsFactory
import com.megaman.maverick.game.entities.overlapsGameCamera
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.world.BodyComponentCreator

class LavaBeamer(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ICullableEntity, ISpritesEntity,
    IAnimatedEntity, IAudioEntity, IHazard, IDirectionRotatable {

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

    override var directionRotation: Direction?
        get() = body.cardinalRotation
        set(value) {
            body.cardinalRotation = value
        }

    private val loop = Loop(LavaBeamerState.values().toGdxArray())
    private val timers = objectMapOf(
        LavaBeamerState.IDLE to Timer(IDLE_DUR),
        LavaBeamerState.SWITCHING_ON to Timer(SWITCHING_ON_DUR),
        LavaBeamerState.FIRING to Timer(FIRING_DUR)
    )
    private lateinit var cullOnEvents: CullableOnEvent

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.HAZARDS_1.source)
            regions.put("on", atlas.findRegion("$TAG/On"))
            regions.put("off", atlas.findRegion("$TAG/Off"))
            regions.put("switch", atlas.findRegion("$TAG/SwitchingOn"))
        }
        super<MegaGameEntity>.init()
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

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        directionRotation =
            Direction.valueOf(spawnProps.getOrDefault(ConstKeys.DIRECTION, "up", String::class).uppercase())
        val position = when (directionRotation!!) {
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
        super<MegaGameEntity>.onDestroy()
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
        val spawn = when (directionRotation!!) {
            Direction.UP -> body.getTopCenterPoint()
            Direction.DOWN -> body.getBottomCenterPoint()
            Direction.LEFT -> body.getCenterLeftPoint()
            Direction.RIGHT -> body.getCenterRightPoint()
        }
        val lavaBeam = EntityFactories.fetch(EntityType.HAZARD, HazardsFactory.LAVA_BEAM)!!
        game.engine.spawn(
            lavaBeam,
            props(
                ConstKeys.POSITION to spawn,
                ConstKeys.DIRECTION to directionRotation,
                ConstKeys.SPEED to FIRE_SPEED * ConstVals.PPM
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
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setOriginCenter()
            _sprite.rotation = directionRotation!!.rotation
            _sprite.setCenter(body.getCenter())
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
            "off" to Animation(regions["off"]),
            "switch" to Animation(regions["switch"], 2, 1, 0.1f, true),
            "on" to Animation(regions["on"])
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}