package com.megaman.maverick.game.entities.megaman.sprites

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.events.Event
import com.mega.game.engine.events.IEventListener
import com.mega.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.megaman.components.*
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.misc.DirectionPositionMapper
import com.megaman.maverick.game.world.body.getPositionPoint

class MegamanTrailSprite(game: MegamanMaverickGame) : MegaGameEntity(game), ISpritesEntity, IEventListener {

    companion object {
        const val TAG = "MegamanTrailingSprite"

        const val AIR_DASH = "airdash"

        const val GROUND_SLIDE = "groundslide"
        const val GROUND_SLIDE_SHOOT = "groundslide_shoot"

        private const val FADE_DUR = 0.25f

        private val regions = ObjectMap<String, TextureRegion>()
    }

    override val eventKeyMask = objectSetOf<Any>(EventType.BEGIN_ROOM_TRANS, EventType.END_ROOM_TRANS)

    private val fadeTimer = Timer(FADE_DUR)

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.MEGAMAN_TRAIL_SPRITE.source)
            gdxArrayOf(AIR_DASH, GROUND_SLIDE, GROUND_SLIDE_SHOOT).forEach { regions.put(it, atlas.findRegion(it)) }
        }
        super.init()
        addComponent(defineUpdatablesComponent())
        addComponent(defineSpritesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        game.eventsMan.addListener(this)

        super.onSpawn(spawnProps)

        val type = spawnProps.get(ConstKeys.TYPE, String::class)!!
        defaultSprite.setRegion(regions[type])

        defaultSprite.setFlip(megaman.shouldFlipSpriteX(), megaman.shouldFlipSpriteY())
        defaultSprite.setOriginCenter()
        defaultSprite.rotation = megaman.getSpriteRotation()

        val position = DirectionPositionMapper.getInvertedPosition(megaman.getSpriteDirection())
        defaultSprite.setPosition(megaman.body.getPositionPoint(position), position)
        defaultSprite.translateX(megaman.getSpriteXTranslation() * ConstVals.PPM)
        defaultSprite.translateY(megaman.getSpriteYTranslation() * ConstVals.PPM)

        fadeTimer.reset()
    }

    override fun onDestroy() {
        super.onDestroy()
        game.eventsMan.removeListener(this)
    }

    override fun onEvent(event: Event) {
        when (event.key) {
            EventType.BEGIN_ROOM_TRANS, EventType.END_ROOM_TRANS-> destroy()
        }
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        fadeTimer.update(delta)
        if (fadeTimer.isFinished()) destroy()
    })

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, -1))
        sprite.setSize(MEGAMAN_SPRITE_SIZE * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ -> sprite.setAlpha(1f - fadeTimer.getRatio()) }
        return spritesComponent
    }

    override fun getEntityType() = EntityType.DECORATION
}
