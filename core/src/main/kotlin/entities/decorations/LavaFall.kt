package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimator
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.objects.Matrix
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GamePolygon
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setBounds
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IDrawableShapesEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.utils.extensions.splitIntoGameRectanglesBasedOnCenter

class LavaFall(game: MegamanMaverickGame) : MegaGameEntity(game), ISpritesEntity, IAnimatedEntity,
    IDrawableShapesEntity {

    companion object {
        const val TAG = "LavaFall"
        private var region: TextureRegion? = null
    }

    private val matrix = Matrix<GameRectangle>()
    private var polygon: GamePolygon? = null

    override fun init() {
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.DECORATIONS_1.source, "Lava")
        addComponent(SpritesComponent())
        addComponent(AnimationsComponent())
        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ polygon }), debug = true))
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)
        polygon = spawnProps.get(ConstKeys.POLYGON, GamePolygon::class)!!
        val cells = polygon!!.splitIntoGameRectanglesBasedOnCenter(
            3f * ConstVals.PPM, ConstVals.PPM.toFloat(), matrix
        )
        defineDrawables(cells)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
    }

    private fun defineDrawables(cells: Matrix<GameRectangle>) {
        val sprites = OrderedMap<Any, GameSprite>()
        val animators = OrderedMap<Any, IAnimator>()
        cells.forEach { x, y, bounds ->
            if (bounds == null) return@forEach

            val offset = 0.1f * ConstVals.PPM
            bounds.translateSize(0f, offset)
            bounds.translate(0f, offset)

            val key = "${x}_${y}"

            val lavaSprite = GameSprite(DrawingPriority(DrawingSection.BACKGROUND, 0))
            lavaSprite.setBounds(bounds)
            sprites.put(key, lavaSprite)

            val animation = Animation(region!!, 1, 3, 0.2f, true)
            val animator = Animator(animation)
            animators.put(key, animator)
        }
        addComponent(SpritesComponent(sprites))
        addComponent(AnimationsComponent(animators, sprites))
    }

    override fun getEntityType() = EntityType.DECORATION
}
