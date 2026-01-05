package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.utils.AnimationUtils

class KoopaShellTrailSprite(game: MegamanMaverickGame) : MegaGameEntity(game), ISpritesEntity {

    companion object {
        const val TAG = "KoopaShellTrailSprite"

        private const val FADE_DUR = 0.25f

        private val regions = ObjectMap<String, TextureRegion>()
    }

    enum class ShellColor { GREEN, RED }

    private lateinit var color: ShellColor
    private val position = Vector2()
    private val fadeTimer = Timer(FADE_DUR)

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.SMB3_EFFECTS.source)
            AnimationUtils.loadRegions(TAG, atlas, ShellColor.entries.map { it.name.lowercase() }, regions)
        }
        super.init()
        addComponent(defineSpritesComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        color = spawnProps.get(ConstKeys.COLOR, ShellColor::class)!!

        val position = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        this.position.set(position)

        fadeTimer.reset()
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        fadeTimer.update(delta)
        if (fadeTimer.isFinished()) destroy()
    })

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(
            TAG, GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, -1))
                .also { it.setSize(2f * ConstVals.PPM.toFloat()) }
        )
        .preProcess { _, sprite ->
            sprite.setRegion(regions[color.name.lowercase()])
            sprite.setPosition(position, Position.BOTTOM_CENTER)
            sprite.setAlpha(1f - fadeTimer.getRatio())
        }
        .build()

    override fun getType() = EntityType.DECORATION
}
