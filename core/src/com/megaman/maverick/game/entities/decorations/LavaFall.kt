package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.OrderedMap
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimator
import com.engine.common.extensions.getTextureRegion
import com.engine.common.objects.Matrix
import com.engine.common.objects.Properties
import com.engine.common.shapes.GamePolygon
import com.engine.common.shapes.GameRectangle
import com.engine.drawables.sorting.DrawingPriority
import com.engine.drawables.sorting.DrawingSection
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setBounds
import com.engine.entities.GameEntity
import com.engine.entities.contracts.IAnimatedEntity
import com.engine.entities.contracts.ISpritesEntity
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.utils.splitIntoGameRectanglesBasedOnCenter

class LavaFall(game: MegamanMaverickGame): GameEntity(game), ISpritesEntity, IAnimatedEntity {

    companion object {
        const val TAG = "LavaFall"
        private var region: TextureRegion? = null
    }

    override fun init() {
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.DECORATIONS_1.source, "Lava")
        addComponent(SpritesComponent(this))
        addComponent(AnimationsComponent(this))
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        val polygon = spawnProps.get(ConstKeys.POLYGON, GamePolygon::class)!!
        val cells = polygon.splitIntoGameRectanglesBasedOnCenter(3f * ConstVals.PPM, ConstVals.PPM.toFloat())
        defineDrawables(cells)
    }

    private fun defineDrawables(cells: Matrix<GameRectangle>) {
        val sprites = OrderedMap<String, GameSprite>()
        val animators = Array<Pair<() -> GameSprite, IAnimator>>()
        cells.forEach { x, y, gameRectangle ->
            if (gameRectangle == null) return@forEach

            val lavaSprite = GameSprite(DrawingPriority(DrawingSection.BACKGROUND, 0))
            lavaSprite.setBounds(gameRectangle)
            sprites.put("lavafall_${x}_${y}", lavaSprite)

            val animation = Animation(region!!, 1, 3, 0.2f, true)
            val animator = Animator(animation)
            animators.add({ lavaSprite } to animator)
        }
        addComponent(SpritesComponent(this, sprites))
        addComponent(AnimationsComponent(this, animators))
    }
}