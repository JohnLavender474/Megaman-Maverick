package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.*
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.events.Event
import com.mega.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.utils.getStandardEventCullingLogic
import com.megaman.maverick.game.events.EventType

class LightSource(game: MegamanMaverickGame) : MegaGameEntity(game), ISpritesEntity, IAnimatedEntity, ICullableEntity {

    companion object {
        const val TAG = "LightSource"
        const val LANTERN_TYPE = "Lantern"
        const val CANDLE_TYPE = "Candle"
        private const val LIGHT_RADIUS = 6
        private const val RADIANCE_FACTOR = 1f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private lateinit var type: String
    private lateinit var bounds: GameRectangle
    private lateinit var keys: ObjectSet<Int>
    private lateinit var spritePos: Position

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.DECORATIONS_1.source)
            regions.put(LANTERN_TYPE, atlas.findRegion(LANTERN_TYPE))
            regions.put(CANDLE_TYPE, atlas.findRegion(CANDLE_TYPE))
        }
        super.init()
        addComponent(defineUpdatablesComponent())
        addComponent(defineCullablesComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        keys = spawnProps.get(ConstKeys.KEYS, String::class)!!
            .replace("\\s+", "")
            .split(",")
            .map { it.toInt() }
            .toObjectSet()
        type = spawnProps.getOrDefault(ConstKeys.TYPE, LANTERN_TYPE, String::class)

        val rawSpritePos = spawnProps.get("${ConstKeys.SPRITE}_${ConstKeys.POSITION}")
        spritePos = when (rawSpritePos) {
            is Position -> rawSpritePos
            is String -> Position.valueOf(rawSpritePos.uppercase())
            else -> Position.BOTTOM_CENTER
        }
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        sendEvent(false)
    }

    fun sendEvent(light: Boolean) {
        GameLogger.debug(TAG, "sendEvent(): light=$light")
        game.eventsMan.submitEvent(
            Event(
                EventType.ADJUST_DARKNESS, props(
                    ConstKeys.KEYS pairTo keys,
                    ConstKeys.LIGHT pairTo light,
                    ConstKeys.CENTER pairTo bounds.getCenter(),
                    ConstKeys.RADIUS pairTo LIGHT_RADIUS,
                    ConstKeys.RADIANCE pairTo RADIANCE_FACTOR
                )
            )
        )
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ sendEvent(true) })

    private fun defineCullablesComponent() = CullablesComponent(
        objectMapOf(
            ConstKeys.CULL_EVENTS pairTo getStandardEventCullingLogic(
                this,
                objectSetOf(EventType.BEGIN_ROOM_TRANS)
            )
        )
    )

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, -1))
        sprite.setSize(1.65f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setPosition(
                bounds.getPositionPoint(spritePos),
                spritePos
            )
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = { type }
        val animations = objectMapOf<String, IAnimation>(
            CANDLE_TYPE pairTo Animation(regions[CANDLE_TYPE], 1, 3, 0.1f, true),
            LANTERN_TYPE pairTo Animation(regions[LANTERN_TYPE], 2, 1, 0.2f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    override fun getEntityType() = EntityType.DECORATION

    override fun getTag() = TAG
}
