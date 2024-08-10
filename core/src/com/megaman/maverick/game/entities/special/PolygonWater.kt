package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.OrderedMap
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimator
import com.engine.common.GameLogger
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.objects.Matrix
import com.engine.common.objects.Properties
import com.engine.common.shapes.GamePolygon
import com.engine.common.shapes.GameRectangle
import com.engine.cullables.CullablesComponent
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sorting.DrawingPriority
import com.engine.drawables.sorting.DrawingSection
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.entities.contracts.IAnimatedEntity
import com.engine.entities.contracts.IBodyEntity
import com.engine.entities.contracts.ISpritesEntity
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaGameEntity
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.utils.splitIntoGameRectanglesBasedOnCenter
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType

class PolygonWater(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity, IAnimatedEntity {

    companion object {
        const val TAG = "PolygonWater"
        private const val BLUE_REG = "Water/Water"
        private const val SURFACE_REG = "Water/Surface"
        private const val UNDER_REG = "Water/Under"
        private var blueReg: TextureRegion? = null
        private var surfaceReg: TextureRegion? = null
        private var underReg: TextureRegion? = null
        private const val WATER_ALPHA = 0.35f
    }

    var splashSound = false

    private lateinit var waterFixture: Fixture

    override fun init() {
        super<MegaGameEntity>.init()
        val atlas = game.assMan.getTextureAtlas(TextureAsset.ENVIRONS_1.source)
        if (blueReg == null) blueReg = atlas.findRegion(BLUE_REG)
        if (surfaceReg == null) surfaceReg = atlas.findRegion(SURFACE_REG)
        if (underReg == null) underReg = atlas.findRegion(UNDER_REG)
        addComponent(defineBodyComponent())
        addComponent(defineCullablesComponent())
    }

    override fun spawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "spawn(): spawnProps = $spawnProps")
        super.spawn(spawnProps)
        val polygon = spawnProps.get(ConstKeys.POLYGON, GamePolygon::class)!!
        val bounds = polygon.getBoundingRectangle()
        body.set(bounds)
        waterFixture.rawShape = polygon
        defineDrawables(polygon.splitIntoGameRectanglesBasedOnCenter(ConstVals.PPM.toFloat(), ConstVals.PPM.toFloat()))
        splashSound = spawnProps.getOrDefault(ConstKeys.SPLASH, true, Boolean::class)
    }

    override fun onDestroy() {
        GameLogger.debug(Water.TAG, "Destroyed")
        super<MegaGameEntity>.onDestroy()
    }

    private fun defineDrawables(cells: Matrix<GameRectangle>) {
        val sprites = OrderedMap<String, GameSprite>()
        val animators = Array<Pair<() -> GameSprite, IAnimator>>()
        cells.forEach { x, y, bounds ->
            if (bounds == null) return@forEach

            val blueSprite = GameSprite(blueReg!!, DrawingPriority(DrawingSection.FOREGROUND, 10))
            blueSprite.setBounds(bounds.x, bounds.y, bounds.width, bounds.height)
            blueSprite.setAlpha(WATER_ALPHA)
            sprites.put("blue_${x}_${y}", blueSprite)

            val waterSprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 10))
            waterSprite.setBounds(bounds.x, bounds.y, bounds.width, bounds.height)
            waterSprite.setAlpha(WATER_ALPHA)
            sprites.put("water_${x}_${y}", waterSprite)

            val isSurface = try {
                cells[x, y + 1] == null
            } catch (e: IndexOutOfBoundsException) {
                true
            }
            val animation = Animation(if (isSurface) surfaceReg!! else underReg!!, 1, 2, 0.15f, true)
            val animator = Animator(animation)
            animators.add({ waterSprite } to animator)
        }
        addComponent(SpritesComponent(sprites))
        addComponent(AnimationsComponent(animators))
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        val shapes = Array<() -> IDrawableShape?>()
        waterFixture = Fixture(body, FixtureType.WATER, GamePolygon())
        waterFixture.attachedToBody = false
        body.addFixture(waterFixture)
        shapes.add { waterFixture.getShape() }
        addComponent(DrawableShapesComponent(debugShapeSuppliers = shapes, debug = true))
        return BodyComponentCreator.create(this, body)
    }

    private fun defineCullablesComponent(): CullablesComponent {
        val cullable = getGameCameraCullingLogic(this)
        return CullablesComponent(objectMapOf(ConstKeys.CULL_OUT_OF_BOUNDS to cullable))
    }
}