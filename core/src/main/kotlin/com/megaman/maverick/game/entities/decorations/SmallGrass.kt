package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.common.enums.Position
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponentBuilder
import com.maverick.game.engine.drawables.sprites.setPosition
import com.maverick.game.engine.drawables.sprites.setSize
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.entities.contracts.ISpritesEntity
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.Receivers
import com.megaman.maverick.game.world.body.getPosition
import com.megaman.maverick.game.world.body.setReceiver

class SmallGrass(services: MegaServices) : MegaGameEntity(services), IBodyEntity, ISpritesEntity {

    companion object {
        const val TAG = "SmallGrass"
        private const val BRUSH_DUR = 0.1f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private val brushTimer = Timer(BRUSH_DUR)
    private var hitByBeam = false

    override fun init(vararg params: Any) {
        if (regions.isEmpty) {
            val atlas = services.assets.getTextureAtlas(TextureAsset.DECORATIONS_1.source)
            gdxArrayOf("still", "brush").forEach {
                val region = atlas.findRegion("$TAG/$it")
                regions.put(it, region)
            }
        }
        super.init()
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val spawn = when {
            spawnProps.containsKey(ConstKeys.POSITION) -> spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
            else -> spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        }
        body.setBottomCenterToPoint(spawn)

        brushTimer.setToEnd()

        hitByBeam = false
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ConstVals.PPM.toFloat(), 0.25f * ConstVals.PPM)

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle(body))
        bodyFixture.setReceiver(Receivers.HIT_BY_PLAYER) { brushTimer.reset() }
        body.addFixture(bodyFixture)

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 5)))
        .preProcess { delta, sprite ->
            brushTimer.update(delta)
            val brush = !brushTimer.isFinished()
            val regionKey = if (brush) "brush" else "still"
            sprite.setRegion(regions[regionKey])
            sprite.setSize(ConstVals.PPM.toFloat())
            sprite.setPosition(body.getPosition())
        }
        .build()

    override fun getType() = EntityType.DECORATION

    override fun getTag() = TAG
}
