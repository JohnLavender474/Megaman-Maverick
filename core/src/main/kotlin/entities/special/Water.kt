package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimator
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
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
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType

class Water(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity, IAnimatedEntity, IWater {

    companion object {
        const val TAG = "Water"

        private const val WATER_REG = "Water/Water"
        private const val SURFACE_REG = "Water/Surface"
        private const val UNDER_REG = "Water/Under"

        private var waterReg: TextureRegion? = null
        private var surfaceReg: TextureRegion? = null
        private var underReg: TextureRegion? = null

        private const val WATER_ALPHA = 0.2f
    }

    private var splashSound = true

    override fun init() {
        GameLogger.debug(TAG, "init()")

        val atlas = game.assMan.getTextureAtlas(TextureAsset.ENVIRONS_1.source)
        if (waterReg == null) waterReg = atlas.findRegion(WATER_REG)
        if (surfaceReg == null) surfaceReg = atlas.findRegion(SURFACE_REG)
        if (underReg == null) underReg = atlas.findRegion(UNDER_REG)

        addComponent(defineBodyComponent())
        addComponent(defineCullablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        body.set(bounds)

        body.fixtures.forEach { (_, fixture) ->
            val shape = (fixture as Fixture).rawShape
            if (shape is GameRectangle) shape.set(bounds)
        }

        val hidden = spawnProps.getOrDefault(ConstKeys.HIDDEN, false, Boolean::class)
        if (hidden) {
            removeComponent(SpritesComponent::class)
            removeComponent(AnimationsComponent::class)
        } else defineDrawables(bounds)

        splashSound = spawnProps.getOrDefault(ConstKeys.SPLASH, true, Boolean::class)
    }

    override fun onDestroy() {
        super.onDestroy()
        GameLogger.debug(TAG, "onDestroy()")
    }

    override fun doMakeSplashSound() = splashSound

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body }

        val waterFixture = Fixture(body, FixtureType.WATER)
        body.addFixture(waterFixture)
        debugShapes.add { waterFixture }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineCullablesComponent(): CullablesComponent {
        val cullable = getGameCameraCullingLogic(this)
        return CullablesComponent(objectMapOf(ConstKeys.CULL_OUT_OF_BOUNDS pairTo cullable))
    }

    private fun defineDrawables(bounds: GameRectangle) {
        val sprites = OrderedMap<Any, GameSprite>()
        val animators = OrderedMap<Any, IAnimator>()

        val waterSprite = GameSprite(waterReg!!, DrawingPriority(DrawingSection.FOREGROUND, 10))
        waterSprite.setBounds(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight())
        waterSprite.setAlpha(WATER_ALPHA)
        sprites.put("water", waterSprite)

        val rows = (bounds.getHeight() / ConstVals.PPM).toInt()
        val columns = (bounds.getWidth() / ConstVals.PPM).toInt()

        for (x in 0 until columns) for (y in 0 until rows) {
            val pos = GameObjectPools.fetch(Vector2::class)
                .set(bounds.getX() + x * ConstVals.PPM, bounds.getY() + y * ConstVals.PPM)

            val region = if (y == rows - 1) surfaceReg!! else underReg!!
            val animation = Animation(region, 1, 2, 0.15f, true)

            val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 11))
            sprite.setBounds(pos.x, pos.y, ConstVals.PPM.toFloat(), ConstVals.PPM.toFloat())
            sprite.setAlpha(WATER_ALPHA)

            val key = "animated_water_${x}_${y}"
            sprites.put(key, sprite)
            val animator = Animator(animation)
            animators.put(key, animator)
        }

        addComponent(SpritesComponent(sprites))
        addComponent(AnimationsComponent(animators, sprites))
    }

    override fun getEntityType() = EntityType.SPECIAL

    override fun getTag() = TAG
}
