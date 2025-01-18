package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.objects.Matrix
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GamePolygon
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setBounds
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.entities.contracts.IDrawableShapesEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.utils.extensions.splitIntoGameRectanglesBasedOnCenter
import com.megaman.maverick.game.utils.extensions.toGdxRectangle

class LavaFall(game: MegamanMaverickGame) : MegaGameEntity(game), ISpritesEntity, IAnimatedEntity,
    ICullableEntity, IDrawableShapesEntity {

    companion object {
        const val TAG = "LavaFall"
        private const val CELL_WIDTH = 1f
        private const val CELL_HEIGHT = 2f
        private var region: TextureRegion? = null
    }

    private val matrix = Matrix<GameRectangle>()
    private val polygon = GamePolygon()
    private val bounds = GameRectangle()

    override fun init() {
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.DECORATIONS_1.source, TAG)
        addComponent(defineCullablesComponent())
        addComponent(SpritesComponent())
        addComponent(AnimationsComponent())
        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ polygon }), debug = true))
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        polygon.set(spawnProps.get(ConstKeys.POLYGON, GamePolygon::class)!!)
        bounds.set(polygon.toGdxRectangle())

        val cells = polygon.splitIntoGameRectanglesBasedOnCenter(
            CELL_WIDTH * ConstVals.PPM, CELL_HEIGHT * ConstVals.PPM, matrix
        )
        defineDrawables(cells)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        sprites.clear()
        animators.clear()
    }

    private fun defineDrawables(cells: Matrix<GameRectangle>) = cells.forEach { x, y, bounds ->
        if (bounds == null) return@forEach

        val offset = 0.1f * ConstVals.PPM
        bounds.translateSize(0f, offset)
        bounds.translate(0f, offset)

        val key = "${x}_${y}"

        val sprite = GameSprite(DrawingPriority(DrawingSection.BACKGROUND, 0))
        sprite.setBounds(bounds)
        sprites.put(key, sprite)

        val animation = Animation(region!!, 3, 1, 0.1f, true)
        val animator = Animator(animation)
        putAnimator(key, sprite, animator)
    }

    private fun defineCullablesComponent() = CullablesComponent(
        objectMapOf(ConstKeys.CULL_OUT_OF_BOUNDS pairTo getGameCameraCullingLogic(game.getGameCamera(), { bounds }))
    )

    override fun getTag() = TAG

    override fun getType() = EntityType.DECORATION
}
