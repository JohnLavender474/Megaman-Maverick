package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.enemies.Starkner.StarknerState

class StarknerTrailSprite(game: MegamanMaverickGame) : MegaGameEntity(game), ISpritesEntity {

    companion object {
        const val TAG = "StarknerTrailSprite"
        private const val DUR = 0.25f
        private val regions = ObjectMap<StarknerState, TextureRegion>()
    }

    private val position = Vector2()
    private var rotation = 0f

    private val timer = Timer(DUR)

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.DECORATIONS_1.source)
            gdxArrayOf(StarknerState.BLUE, StarknerState.YELLOW, StarknerState.RED).forEach { state ->
                val region = atlas.findRegion("$TAG/${state.name.lowercase()}")
                if (region == null) throw IllegalStateException("Region is null for state=$state")
                regions.put(state, region)
            }
            GameLogger.debug(TAG, "init(): build regions: ${regions.map { it.key }}")
        }
        super.init()
        addComponent(defineSpritesComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val position = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        this.position.set(position)

        rotation = spawnProps.getOrDefault(ConstKeys.ROTATION, 0f, Float::class)

        val state = spawnProps.get(ConstKeys.STATE, StarknerState::class)!!
        val region = regions[state] ?: throw IllegalStateException(
            "Region is null for state=$state"
        )
        sprites[TAG]!!.setRegion(region)

        timer.reset()
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        timer.update(delta)
        if (timer.isFinished()) destroy()
    })

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().also { sprite -> sprite.setSize(2.5f * ConstVals.PPM) })
        .preProcess { _, sprite ->
            sprite.setCenter(position)
            sprite.setOriginCenter()
            sprite.rotation = rotation
            sprite.setAlpha(1f - timer.getRatio())
        }
        .build()

    override fun getType() = EntityType.DECORATION

    override fun getTag() = TAG
}
