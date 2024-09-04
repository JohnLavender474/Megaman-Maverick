package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.entities.GameEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.IChildEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.world.Body
import com.mega.game.engine.world.BodyComponent
import com.mega.game.engine.world.BodyType
import com.mega.game.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType

class Spike(game: MegamanMaverickGame) : MegaGameEntity(game), IChildEntity, IBodyEntity, ISpritesEntity {

    companion object {
        const val TAG = "Spike"
        private var atlas: TextureAtlas? = null
    }

    override var parent: GameEntity? = null

    private lateinit var bodyOffset: Vector2
    private var spriteRotation = 0f

    override fun getEntityType() = EntityType.HAZARD

    override fun init() {
        if (atlas == null) atlas = game.assMan.getTextureAtlas(TextureAsset.HAZARDS_1.source)
        addComponent(defineBodyComponent())
        addComponent(SpritesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        body.set(bounds)
        body.fixtures.forEach { entry ->
            val shape = (entry.second as Fixture).rawShape
            if (shape is GameRectangle) shape.set(bounds)
        }

        bodyOffset = Vector2()
        parent?.let { if (it is IBodyEntity) bodyOffset.set(body.getCenter().sub(it.body.getCenter())) }

        spriteRotation = spawnProps.getOrDefault(ConstKeys.ROTATION, 0f) as Float

        val regionKey = spawnProps.get(ConstKeys.REGION, String::class)!!
        val textureRegion = atlas!!.findRegion(regionKey)

        val cells = bounds.splitByCellSize(ConstVals.PPM.toFloat())
        cells.forEach { x, y, r ->
            val width = spawnProps.getOrDefault(ConstKeys.WIDTH, 1f, Float::class)
            val height = spawnProps.getOrDefault(ConstKeys.HEIGHT, 1f, Float::class)
            val offsetX = spawnProps.getOrDefault(ConstKeys.OFFSET_X, 0f, Float::class)
            val offsetY = spawnProps.getOrDefault(ConstKeys.OFFSET_Y, 0f, Float::class)

            val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 10))
            sprite.setSize(width * ConstVals.PPM, height * ConstVals.PPM)
            sprite.setCenter(r!!.getCenter())
            sprite.translate(offsetX, offsetY)
            sprite.setRegion(textureRegion)

            sprites.put("${x}_$y", sprite)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        parent = null
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)

        val deathFixture = Fixture(body, FixtureType.DEATH, GameRectangle())
        body.addFixture(deathFixture)

        body.preProcess.put(ConstKeys.DEFAULT, Updatable {
            parent?.let {
                if (it is IBodyEntity) {
                    val parentCenter = it.body.getCenter()
                    val newCenter = parentCenter.add(bodyOffset)
                    body.setCenter(newCenter)
                }
            }
        })

        return BodyComponentCreator.create(this, body)
    }
}
