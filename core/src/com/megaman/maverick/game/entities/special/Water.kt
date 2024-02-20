package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.OrderedMap
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimator
import com.engine.common.GameLogger
import com.engine.common.extensions.gdxArrayOf
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.cullables.CullablesComponent
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sorting.DrawingPriority
import com.engine.drawables.sorting.DrawingSection
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.ISprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.entities.GameEntity
import com.engine.entities.contracts.IBodyEntity
import com.engine.entities.contracts.ISpriteEntity
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType

class Water(game: MegamanMaverickGame) : GameEntity(game), IBodyEntity, ISpriteEntity {

    companion object {
        const val TAG = "Water"

        private const val WATER_REG = "Water/Water"
        private const val SURFACE_REG = "Water/Surface"
        private const val UNDER_REG = "Water/Under"

        private var waterReg: TextureRegion? = null
        private var surfaceReg: TextureRegion? = null
        private var underReg: TextureRegion? = null

        private const val WATER_ALPHA = 0.35f
    }

    private lateinit var waterFixture: Fixture

    var splashSound = true

    override fun init() {
        GameLogger.debug(TAG, "Initializing...")

        val atlas = game.assMan.getTextureAtlas(TextureAsset.ENVIRONS_1.source)
        if (waterReg == null) waterReg = atlas.findRegion(WATER_REG)
        if (surfaceReg == null) surfaceReg = atlas.findRegion(SURFACE_REG)
        if (underReg == null) underReg = atlas.findRegion(UNDER_REG)

        addComponent(defineBodyComponent())
        addComponent(defineCullablesComponent())
    }

    override fun spawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "Spawning")
        super.spawn(spawnProps)

        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        body.set(bounds)
        body.fixtures.forEach { (_, fixture) ->
            val shape = fixture.shape
            if (shape is GameRectangle) shape.set(bounds)
        }

        defineDrawables(bounds)

        splashSound = spawnProps.getOrDefault(ConstKeys.SPLASH, true, Boolean::class)
    }

    override fun onDestroy() {
        super<GameEntity>.onDestroy()
        GameLogger.debug(TAG, "Destroyed")
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        val shapes = Array<() -> IDrawableShape?>()

        // water fixture
        waterFixture = Fixture(GameRectangle(), FixtureType.WATER)
        body.addFixture(waterFixture)
        waterFixture.shape.color = Color.BLUE
        shapes.add { waterFixture.shape }

        addComponent(DrawableShapesComponent(this, debugShapeSuppliers = shapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineCullablesComponent(): CullablesComponent {
        val cullable = getGameCameraCullingLogic(this)
        return CullablesComponent(this, gdxArrayOf(cullable))
    }

    private fun defineDrawables(bounds: GameRectangle) {
        val sprites = OrderedMap<String, ISprite>()

        val waterSprite = GameSprite(waterReg!!, DrawingPriority(DrawingSection.FOREGROUND, 10))
        waterSprite.setBounds(bounds.x, bounds.y, bounds.width, bounds.height)
        waterSprite.setAlpha(WATER_ALPHA)
        sprites.put("water", waterSprite)

        val rows = (bounds.height / ConstVals.PPM).toInt()
        val columns = (bounds.width / ConstVals.PPM).toInt()

        val animators = Array<Pair<() -> ISprite, IAnimator>>()

        for (x in 0 until columns) {
            for (y in 0 until rows) {
                val pos = Vector2(bounds.x + x * ConstVals.PPM, bounds.y + y * ConstVals.PPM)

                val region = if (y == rows - 1) surfaceReg!! else underReg!!
                val animation = Animation(region, 1, 2, 0.15f, true)

                val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 10))
                sprite.setBounds(pos.x, pos.y, ConstVals.PPM.toFloat(), ConstVals.PPM.toFloat())
                sprite.setAlpha(WATER_ALPHA)

                sprites.put("animated_water_${x}_${y}", sprite)

                val animator = Animator(animation)
                animators.add({ sprite } to animator)
            }
        }

        addComponent(SpritesComponent(this, sprites))
        addComponent(AnimationsComponent(this, animators))
    }
}
