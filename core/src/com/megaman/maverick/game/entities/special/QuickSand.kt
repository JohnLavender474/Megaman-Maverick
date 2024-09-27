package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimator
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.interfaces.UpdateFunction
import com.mega.game.engine.common.objects.GamePair
import com.mega.game.engine.common.objects.Matrix
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
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
import com.mega.game.engine.entities.contracts.ICullableEntity
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
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType

class QuickSand(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity, IAnimatedEntity,
    ICullableEntity {

    companion object {
        const val TAG = "QuickSand"
        private val regions = ObjectMap<String, TextureRegion>()
    }

    override fun getEntityType() = EntityType.SPECIAL

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
        addComponent(defineBodyComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
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

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))
        return BodyComponentCreator.create(this, body)
    }

    private fun defineDrawables(cells: Matrix<GameRectangle>) {
        val sprites = OrderedMap<String, GameSprite>()
        val updateFunctions = ObjectMap<String, UpdateFunction<GameSprite>>()
        val animators = Array<GamePair<() -> GameSprite, IAnimator>>()

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
            animators.add({ sprite } pairTo animator)
        }

        addComponent(SpritesComponent(sprites, updateFunctions))
        addComponent(AnimationsComponent(animators))
    }
}