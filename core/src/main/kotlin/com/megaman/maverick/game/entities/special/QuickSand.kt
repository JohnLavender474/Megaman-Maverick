package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.objects.Matrix
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType

class QuickSand(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity, IAnimatedEntity {

    companion object {
        const val TAG = "QuickSand"
        private const val ATLAS_KEY_SUFFIX = "_v2"
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private val matrix = Matrix<GameRectangle>()

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENVIRONS_1.source)
            Position.entries.forEach { position ->
                val key = position.name.lowercase()
                val regionKey = "${TAG}${ATLAS_KEY_SUFFIX}/$key"
                val region = atlas.findRegion(regionKey)
                regions.put(key, region)

                GameLogger.debug(TAG, "init(): putting regions: regionKey=$regionKey, key=$key, region=$region")
            }
        }
        super.init()
        addComponent(defineBodyComponent())
        addComponent(SpritesComponent())
        addComponent(AnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        body.set(bounds)
        body.forEachFixture { ((it as Fixture).rawShape as GameRectangle).set(bounds) }

        defineDrawables(bounds.splitByCellSize(ConstVals.PPM.toFloat(), matrix))
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        sprites.clear()
        animators.clear()
        clearSpritePreProcess()
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()

        val sandFixture = Fixture(body, FixtureType.SAND)
        body.addFixture(sandFixture)
        debugShapes.add { sandFixture }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineDrawables(cells: Matrix<GameRectangle>) {
        cells.forEach { x, y, bounds ->
            val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 20))
            sprite.setSize(ConstVals.PPM.toFloat())

            val key = "${x}_${y}"
            sprites.put(key, sprite)

            putSpritePreProcess(key) { _, _ -> sprite.setCenter(bounds!!.getCenter()) }

            val regionKey = when (x) {
                0 -> when (y) {
                    cells.rows - 1 -> Position.TOP_LEFT
                    0 -> Position.BOTTOM_LEFT
                    else -> Position.CENTER_LEFT
                }
                cells.columns - 1 -> when (y) {
                    cells.rows - 1 -> Position.TOP_RIGHT
                    0 -> Position.BOTTOM_RIGHT
                    else -> Position.CENTER_RIGHT
                }
                else -> when (y) {
                    cells.rows - 1 -> Position.TOP_CENTER
                    0 -> Position.BOTTOM_CENTER
                    else -> Position.CENTER
                }
            }.name.lowercase()

            val animator: Animator
            try {
                val region = regions[regionKey]
                val animation = Animation(region!!, 4, 2, 0.1f, true)
                animator = Animator(animation)
            } catch (e: Exception) {
                throw IllegalStateException("Failed to create animator for region $regionKey", e)
            }

            putAnimator(key, sprite, animator)
        }
    }

    override fun getType() = EntityType.SPECIAL

    override fun getTag() = TAG
}
