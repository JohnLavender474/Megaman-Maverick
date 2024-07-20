package com.megaman.maverick.game.entities.hazards

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
import com.engine.drawables.sorting.DrawingPriority
import com.engine.drawables.sorting.DrawingSection
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setBounds
import com.engine.entities.GameEntity
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
import com.megaman.maverick.game.utils.split
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType

class Lava(game: MegamanMaverickGame) : GameEntity(game), IBodyEntity, ISpritesEntity, IAnimatedEntity {

    companion object {
        const val TAG = "Lava"
        private const val FLOW = "Flow"
        private const val FALL = "Fall"
        private var regions: ObjectMap<String, TextureRegion>? = null
    }

    private lateinit var type: String
    private var left = false

    override fun init() {
        if (regions == null) {
            regions = ObjectMap()
            val atlas = game.assMan.getTextureAtlas(TextureAsset.HAZARDS_1.source)
            regions!!.put(FLOW, atlas.findRegion("Lava/${FLOW}"))
            regions!!.put(FALL, atlas.findRegion("Lava/${FALL}"))
        }
        addComponent(defineBodyComponent())
        addComponent(SpritesComponent(this))
        addComponent(AnimationsComponent(this))
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        body.set(bounds)
        type = spawnProps.getOrDefault(ConstKeys.TYPE, FLOW, String::class)
        val cells = bounds.split(ConstVals.PPM.toFloat(), ConstVals.PPM.toFloat())
        left = spawnProps.getOrDefault(ConstKeys.LEFT, false, Boolean::class)
        defineDrawables(cells)
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)

        val deathFixture = Fixture(body, FixtureType.DEATH, GameRectangle())
        deathFixture.putProperty(ConstKeys.INSTANT, true)
        body.addFixture(deathFixture)

        body.preProcess.put(ConstKeys.DEFAULT) {
            (deathFixture.rawShape as GameRectangle).set(body)
        }

        return BodyComponentCreator.create(this, body)
    }

    private fun defineDrawables(cells: Matrix<GameRectangle>) {
        val sprites = OrderedMap<String, GameSprite>()
        val updateFunctions = ObjectMap<String, UpdateFunction<GameSprite>>()
        val animators = Array<Pair<() -> GameSprite, IAnimator>>()
        cells.forEach { x, y, gameRectangle ->
            if (gameRectangle == null) return@forEach

            val lavaSprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 0))
            lavaSprite.setBounds(gameRectangle)
            val key = "lava_${x}_${y}"
            sprites.put(key, lavaSprite)
            if (type == FALL) updateFunctions.put(key, UpdateFunction { _, _sprite ->
                _sprite.setFlip(left, false)
            })

            val region = regions!!.get(type)
            val animation = Animation(region!!, 1, 3, 0.1f, true)
            val animator = Animator(animation)
            animators.add({ lavaSprite } to animator)
        }
        addComponent(SpritesComponent(this, sprites, updateFunctions))
        addComponent(AnimationsComponent(this, animators))
    }
}