package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.Vector2
import com.engine.common.enums.Position
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.interfaces.Updatable
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.drawables.sorting.DrawingPriority
import com.engine.drawables.sorting.DrawingSection
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.drawables.sprites.setSize
import com.engine.entities.IGameEntity
import com.engine.entities.contracts.IBodyEntity
import com.engine.entities.contracts.IChildEntity
import com.engine.entities.contracts.ISpritesEntity
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaGameEntity
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType

class Spike(game: MegamanMaverickGame) : MegaGameEntity(game), IChildEntity, IBodyEntity, ISpritesEntity {

    companion object {
        const val TAG = "Spike"
        private var atlas: TextureAtlas? = null
    }

    override var parent: IGameEntity? = null

    private lateinit var bodyOffset: Vector2

    private var spriteRotation = 0f
    private var spriteRegion = ""
    private lateinit var spritePosition: Position
    private lateinit var spriteOffset: Vector2

    override fun getEntityType() = EntityType.HAZARD

    override fun init() {
        if (atlas == null) atlas = game.assMan.getTextureAtlas(TextureAsset.HAZARDS_1.source)
        addComponent(defineBodyComponent())
        addComponent(defineSpriteComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)

        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        body.set(bounds)
        body.fixtures.forEach { entry ->
            val shape = (entry.second as Fixture).rawShape
            if (shape is GameRectangle) shape.set(bounds)
        }

        bodyOffset = Vector2()
        parent?.let { if (it is IBodyEntity) bodyOffset.set(body.getCenter().sub(it.body.getCenter())) }

        spriteRotation = spawnProps.getOrDefault(ConstKeys.ROTATION, 0f) as Float

        spriteRegion = spawnProps.get(ConstKeys.REGION, String::class)!!
        val textureRegion = atlas!!.findRegion(spriteRegion)
        firstSprite!!.setRegion(textureRegion)

        spritePosition =
            Position.valueOf(spawnProps.getOrDefault(ConstKeys.POSITION, "BOTTOM_CENTER") as String)

        spriteOffset = Vector2()
        spriteOffset.x = spawnProps.getOrDefault(ConstKeys.OFFSET_X, 0f) as Float
        spriteOffset.y = spawnProps.getOrDefault(ConstKeys.OFFSET_Y, 0f) as Float
    }

    override fun onDestroy() {
        super<MegaGameEntity>.onDestroy()
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

    private fun defineSpriteComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 5))
        sprite.setSize(ConstVals.PPM.toFloat())

        val spriteComponent = SpritesComponent(sprite)
        spriteComponent.putUpdateFunction { _, _sprite ->
            _sprite.rotation = spriteRotation

            val position = body.getPositionPoint(spritePosition)
            _sprite.setPosition(position, spritePosition)

            _sprite.translate(spriteOffset.x * ConstVals.PPM, spriteOffset.y * ConstVals.PPM)
        }

        return spriteComponent
    }
}
