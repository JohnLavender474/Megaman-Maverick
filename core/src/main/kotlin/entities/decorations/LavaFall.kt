package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimator
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.objects.GamePair
import com.mega.game.engine.common.objects.Matrix
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GamePolygon
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setBounds
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.utils.splitIntoGameRectanglesBasedOnCenter

class LavaFall(game: MegamanMaverickGame) : MegaGameEntity(game), ISpritesEntity, IAnimatedEntity {

    companion object {
        const val TAG = "LavaFall"
        private var region: TextureRegion? = null
    }

    override fun getEntityType() = EntityType.DECORATION

    override fun init() {
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.DECORATIONS_1.source, "Lava")
        addComponent(SpritesComponent())
        addComponent(AnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        val polygon = spawnProps.get(ConstKeys.POLYGON, GamePolygon::class)!!
        val cells = polygon.splitIntoGameRectanglesBasedOnCenter(3f * ConstVals.PPM, ConstVals.PPM.toFloat())
        defineDrawables(cells)
    }

    private fun defineDrawables(cells: Matrix<GameRectangle>) {
        val sprites = OrderedMap<String, GameSprite>()
        val animators = Array<GamePair<() -> GameSprite, IAnimator>>()
        cells.forEach { x, y, gameRectangle ->
            if (gameRectangle == null) return@forEach

            val bounds = gameRectangle.copy()
            val offset = 0.1f * ConstVals.PPM
            bounds.height += offset
            bounds.y -= offset

            val lavaSprite = GameSprite(DrawingPriority(DrawingSection.BACKGROUND, 0))
            lavaSprite.setBounds(bounds)
            sprites.put("${x}_${y}", lavaSprite)

            val animation = Animation(region!!, 1, 3, 0.2f, true)
            val animator = Animator(animation)
            animators.add({ lavaSprite } pairTo animator)
        }
        addComponent(SpritesComponent(sprites))
        addComponent(AnimationsComponent(animators))
    }
}
