package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimator
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.objects.Matrix
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GamePolygon
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
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
import com.megaman.maverick.game.entities.contracts.IWater
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.utils.extensions.getBoundingRectangle
import com.megaman.maverick.game.utils.extensions.splitIntoGameRectanglesBasedOnCenter
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds

class PolygonWater(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity, IAnimatedEntity, IWater {

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

    private val matrix = Matrix<GameRectangle>()
    private lateinit var waterFixture: Fixture

    private var splashSound = false

    override fun init() {
        val atlas = game.assMan.getTextureAtlas(TextureAsset.ENVIRONS_1.source)
        if (blueReg == null) blueReg = atlas.findRegion(BLUE_REG)
        if (surfaceReg == null) surfaceReg = atlas.findRegion(SURFACE_REG)
        if (underReg == null) underReg = atlas.findRegion(UNDER_REG)
        addComponent(defineBodyComponent())
        addComponent(defineCullablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val polygon = spawnProps.get(ConstKeys.POLYGON, GamePolygon::class)!!
        body.set(polygon.getBoundingRectangle())
        waterFixture.setShape(polygon)
        GameLogger.debug(TAG, "body=${body.getBounds()}")
        GameLogger.debug(TAG, "polygon=$polygon")

        val matrix =
            polygon.splitIntoGameRectanglesBasedOnCenter(ConstVals.PPM.toFloat(), ConstVals.PPM.toFloat(), matrix)
        GameLogger.debug(TAG, "matrix=$matrix")
        defineDrawables(matrix)

        splashSound = spawnProps.getOrDefault(ConstKeys.SPLASH, true, Boolean::class)
    }

    override fun onDestroy() {
        GameLogger.debug(Water.TAG, "onDestroy()")
        super.onDestroy()
    }

    override fun doMakeSplashSound() = splashSound

    private fun defineDrawables(cells: Matrix<GameRectangle>) {
        val sprites = OrderedMap<Any, GameSprite>()
        val animators = OrderedMap<Any, IAnimator>()
        cells.forEach { x, y, bounds ->
            if (bounds == null) return@forEach

            val blueSprite = GameSprite(blueReg!!, DrawingPriority(DrawingSection.FOREGROUND, 10))
            blueSprite.setBounds(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight())
            blueSprite.setAlpha(WATER_ALPHA)
            sprites.put("blue_${x}_${y}", blueSprite)

            val waterKey = "water_${x}_${y}"
            val waterSprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 10))
            waterSprite.setBounds(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight())
            waterSprite.setAlpha(WATER_ALPHA)
            sprites.put(waterKey, waterSprite)

            val isSurface = try {
                cells[x, y + 1] == null
            } catch (_: IndexOutOfBoundsException) {
                true
            }

            val animation = Animation(if (isSurface) surfaceReg!! else underReg!!, 1, 2, 0.15f, true)
            val animator = Animator(animation)
            animators.put(waterKey, animator)
        }
        addComponent(SpritesComponent(sprites))
        addComponent(AnimationsComponent(animators, sprites))
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body }

        waterFixture = Fixture(body, FixtureType.WATER)
        waterFixture.attachedToBody = false
        body.addFixture(waterFixture)
        debugShapes.add { waterFixture }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineCullablesComponent(): CullablesComponent {
        val cullable = getGameCameraCullingLogic(this)
        return CullablesComponent(objectMapOf(ConstKeys.CULL_OUT_OF_BOUNDS pairTo cullable))
    }

    override fun getType() = EntityType.SPECIAL

    override fun getTag(): String = TAG
}
