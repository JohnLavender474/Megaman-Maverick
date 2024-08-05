package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimator
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.interfaces.UpdateFunction
import com.engine.common.objects.Matrix
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sorting.DrawingPriority
import com.engine.drawables.sorting.DrawingSection
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setCenter
import com.engine.drawables.sprites.setSize
import com.engine.entities.GameEntity
import com.engine.entities.contracts.IAnimatedEntity
import com.engine.entities.contracts.IBodyEntity
import com.engine.entities.contracts.ICullableEntity
import com.engine.entities.contracts.ISpritesEntity
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType

class QuickSand(game: MegamanMaverickGame) : GameEntity(game), IBodyEntity, ISpritesEntity, IAnimatedEntity,
    ICullableEntity {

    companion object {
        const val TAG = "QuickSand"
        private val regions = ObjectMap<String, TextureRegion>()
    }

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENVIRONS_1.source)
            regions.put("TopCenter", atlas.findRegion("$TAG/TopCenter"))
            regions.put("TopLeft", atlas.findRegion("$TAG/TopLeft"))
            regions.put("TopRight", atlas.findRegion("$TAG/TopRight"))
            regions.put("Center", atlas.findRegion("$TAG/Center"))
            regions.put("Left", atlas.findRegion("$TAG/Left"))
            regions.put("Right", atlas.findRegion("$TAG/Right"))
        }
        super<GameEntity>.init()
        addComponent(defineBodyComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        body.set(bounds)
        body.fixtures.forEach { ((it.second as Fixture).rawShape as GameRectangle).set(bounds) }
        defineDrawables(bounds.splitByCellSize(ConstVals.PPM.toFloat()))
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        val debugShapes = Array<() -> IDrawableShape?>()

        val sandFixture = Fixture(body, FixtureType.SAND, GameRectangle())
        body.addFixture(sandFixture)
        sandFixture.rawShape.color = Color.RED
        debugShapes.add { sandFixture.getShape() }

        addComponent(DrawableShapesComponent(this, debugShapeSuppliers = debugShapes, debug = true))
        return BodyComponentCreator.create(this, body)
    }

    private fun defineDrawables(cells: Matrix<GameRectangle>) {
        val sprites = OrderedMap<String, GameSprite>()
        val updateFunctions = ObjectMap<String, UpdateFunction<GameSprite>>()
        val animators = Array<Pair<() -> GameSprite, IAnimator>>()

        cells.forEach { x, y, bounds ->
            val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 10))
            sprite.setSize(ConstVals.PPM.toFloat())

            val key = "sand_${x}_${y}"
            sprites.put(key, sprite)

            updateFunctions.put(key, UpdateFunction { _, _sprite ->
                _sprite.setCenter(bounds!!.getCenter())
            })

            var regionKey = if (x % 3 == 0) "Left" else if (x % 3 == 2) "Right" else "Center"
            if (y == cells.rows - 1) regionKey = "Top$regionKey"

            val region = regions.get(regionKey)
            val animation = Animation(region!!, 1, 2, 0.25f, true)
            val animator = Animator(animation)
            animators.add({ sprite } to animator)
        }

        addComponent(SpritesComponent(this, sprites, updateFunctions))
        addComponent(AnimationsComponent(this, animators))
    }
}