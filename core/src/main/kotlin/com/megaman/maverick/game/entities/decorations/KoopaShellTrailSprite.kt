package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Position
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponentBuilder
import com.maverick.game.engine.drawables.sprites.setPosition
import com.maverick.game.engine.drawables.sprites.setSize
import com.maverick.game.engine.entities.contracts.ISpritesEntity
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.utils.AnimationUtils

class KoopaShellTrailSprite(game: MegaServices) : MegaGameEntity(game), ISpritesEntity {

    companion object {
        const val TAG = "KoopaShellTrailSprite"

        private const val FADE_DUR = 0.25f

        private val regions = ObjectMap<String, TextureRegion>()
    }

    enum class ShellColor { GREEN, RED }

    private lateinit var color: ShellColor
    private val position = Vector2()
    private val fadeTimer = Timer(FADE_DUR)

    override fun init(vararg params: Any) {
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
