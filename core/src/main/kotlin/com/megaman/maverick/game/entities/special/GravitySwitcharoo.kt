package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.SmoothOscillationTimer
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.*
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.enemies.TellySaucer
import com.megaman.maverick.game.entities.megaman.constants.AButtonTask
import com.megaman.maverick.game.entities.utils.getStandardEventCullingLogic
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.screens.levels.spawns.SpawnType
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.*

class GravitySwitcharoo(game: MegamanMaverickGame) : Switch(game), IBodyEntity, ISpritesEntity, IAnimatedEntity,
    ICullableEntity, IAudioEntity, IDirectional {

    companion object {
        const val TAG = "GravitySwitcharoo"

        private const val BODY_SIZE = 1.75f

        private const val ARROW_SPRITE_SIZE = 2f
        private const val ARROW_ALPHA = 0.75f

        private const val AURA_SPRITE_SIZE = 2.5f
        private const val AURA_MIN_ALPHA = 0.25f
        private const val AURA_MAX_ALPHA = 0.5f
        private const val AURA_BLINK_DUR = 0.2f

        private val TRIGGER_ENTITY_TAGS = gdxArrayOf(TellySaucer.TAG)

        private val regions = ObjectMap<String, TextureRegion>()
    }

    override var direction: Direction
        get() = body.direction
        set(value) {
            body.direction = value
        }

    private val auraBlink =
        SmoothOscillationTimer(duration = AURA_BLINK_DUR, start = AURA_MIN_ALPHA, end = AURA_MAX_ALPHA)

    private lateinit var spawnRoom: String

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.SPECIALS_1.source)
            gdxArrayOf(ConstKeys.ARROW, ConstKeys.AURA, ConstKeys.DEACTIVATED).forEach {
                regions.put(it, atlas.findRegion("${TAG}/$it"))
            }
        }
        super.init()
        addComponent(AudioComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineCullablesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val center = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(center)

        direction =
            Direction.valueOf(spawnProps.getOrDefault(ConstKeys.DIRECTION, ConstKeys.UP, String::class).uppercase())

        auraBlink.reset()

        spawnRoom = spawnProps.get(SpawnType.SPAWN_ROOM, String::class)!!
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
    }

    override fun shouldBeginSwitchToDown(delta: Float) = direction != megaman.direction && isTriggerEntityInBounds()

    private fun isTriggerEntityInBounds(): Boolean {
        val bounds = body.getBounds()
        return megaman.body.getBounds().overlaps(bounds) || TRIGGER_ENTITY_TAGS.any any@{ tag ->
            val entities = MegaGameEntities.getOfTag(tag)
            return@any entities.any { entity -> entity is IBodyEntity && entity.body.getBounds().overlaps(bounds) }
        }
    }

    override fun shouldBeginSwitchToUp(delta: Float) = direction != megaman.direction

    override fun shouldFinishSwitchToDown(delta: Float) = true

    override fun shouldFinishSwitchToUp(delta: Float) = true

    override fun onFinishSwitchToDown() {
        GameLogger.debug(TAG, "onFinishSwitchToDown(): direction=$direction")

        megaman.direction = direction
        megaman.aButtonTask = when {
            megaman.body.isSensing(BodySense.FEET_ON_GROUND) -> AButtonTask.JUMP
            else -> AButtonTask.AIR_DASH
        }

        requestToPlaySound(SoundAsset.LIFT_OFF_SOUND, false)
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(BODY_SIZE * ConstVals.PPM)
        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body.getBounds() }), debug = true))
        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.BODY))
    }

    private fun defineCullablesComponent() = CullablesComponent(
        objectMapOf(
            ConstKeys.CULL_EVENTS pairTo getStandardEventCullingLogic(
                this, objectSetOf(EventType.END_ROOM_TRANS, EventType.SET_TO_ROOM_NO_TRANS), cull@{ event ->
                    val room = event.getProperty(ConstKeys.ROOM, RectangleMapObject::class)!!.name
                    return@cull room != spawnRoom
                }
            )
        )
    )

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(
            ConstKeys.ARROW, GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 1)).also {
                it.setSize(ARROW_SPRITE_SIZE * ConstVals.PPM)
                it.setAlpha(ARROW_ALPHA)
            }
        )
        .updatable { _, sprite ->
            sprite.setCenter(body.getCenter())
            sprite.setOriginCenter()
            sprite.rotation = direction.getOpposite().rotation
        }
        .sprite(
            ConstKeys.AURA,
            GameSprite(regions[ConstKeys.AURA], DrawingPriority(DrawingSection.FOREGROUND, 2))
                .also { it.setSize(AURA_SPRITE_SIZE * ConstVals.PPM) }
        )
        .updatable { delta, sprite ->
            sprite.setCenter(body.getCenter())
            auraBlink.update(if (megaman.direction == direction) delta / 2f else delta)
            val alpha = auraBlink.getValue()
            sprite.setAlpha(alpha)
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(ConstKeys.ARROW)
        .animator(
            AnimatorBuilder()
                .setKeySupplier { if (megaman.direction == direction) ConstKeys.DEACTIVATED else ConstKeys.ARROW }
                .applyToAnimations { animations ->
                    animations.put(ConstKeys.ARROW, Animation(regions[ConstKeys.ARROW], 3, 1, 0.1f, true))
                    animations.put(ConstKeys.DEACTIVATED, Animation(regions[ConstKeys.DEACTIVATED]))
                }
                .build()
        )
        .build()
}
