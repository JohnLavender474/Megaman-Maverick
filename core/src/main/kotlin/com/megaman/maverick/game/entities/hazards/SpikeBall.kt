package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.shapes.GameCircle
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.drawables.shapes.DrawableShapesComponent
import com.maverick.game.engine.drawables.shapes.IDrawableShape
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.drawables.sprites.setCenter
import com.maverick.game.engine.drawables.sprites.setSize
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.entities.contracts.IDrawableShapesEntity
import com.maverick.game.engine.entities.contracts.IMotionEntity
import com.maverick.game.engine.entities.contracts.ISpritesEntity
import com.maverick.game.engine.motion.MotionComponent
import com.maverick.game.engine.motion.MotionComponent.MotionDefinition
import com.maverick.game.engine.motion.Pendulum
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getCenter

class SpikeBall(services: MegaServices) : MegaGameEntity(services), IBodyEntity, ISpritesEntity, IMotionEntity,
    IDrawableShapesEntity, IDamagerEntity, IHazard {

    companion object {
        const val TAG = "SpikeBall"
        private const val PENDULUM_GRAVITY = 10f
        private var spikeRegion: TextureRegion? = null
        private var chainRegion: TextureRegion? = null
    }

    override fun getType() = EntityType.HAZARD

    override fun init(vararg params: Any) {
        if (spikeRegion == null || chainRegion == null) {
            val atlas = services.assets.getTextureAtlas(TextureAsset.HAZARDS_1.source)
            spikeRegion = atlas.findRegion("SpikeBall/SpikeBall")
            chainRegion = atlas.findRegion("SpikeBall/Chain")
        }
        addComponent(MotionComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        val length = spawnProps.get(ConstKeys.LENGTH, Float::class)!!
        val gravity = spawnProps.getOrDefault(ConstKeys.GRAVITY, PENDULUM_GRAVITY, Float::class)
        val pendulum =
            Pendulum(
                length * ConstVals.PPM, gravity * ConstVals.PPM, spawn, 1 / 60f
            )
        putMotionDefinition(
            ConstKeys.PENDULUM,
            MotionDefinition(motion = pendulum, function = { value, _ ->
                body.setCenter(value)
            })
        )

        var tempDistance = 0f
        var tempIndex = 0
        while (tempDistance < length) {
            val chainSprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 0))
            chainSprite.setSize(0.25f * ConstVals.PPM)
            chainSprite.setRegion(chainRegion!!)

            val key = "chain_$tempIndex"
            sprites.put(key, chainSprite)

            val chainDistance = tempDistance * ConstVals.PPM

            putSpritePreProcess(key) { _, sprite ->
                val center = pendulum.getPointFromAnchor(chainDistance)
                sprite.setCenter(center)
            }

            tempDistance += 0.25f
            tempIndex++
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        clearMotionDefinitions()
        val spritesIter = sprites.iterator()
        while (spritesIter.hasNext) {
            val spriteEntry = spritesIter.next()
            if (spriteEntry.key != TAG) spritesIter.remove()
        }
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(2f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameCircle().setRadius(0.75f * ConstVals.PPM))
        body.addFixture(damagerFixture)
        damagerFixture.drawingColor = Color.RED
        debugShapes.add { damagerFixture}

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val spikeSprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 0))
        spikeSprite.setSize(2f * ConstVals.PPM)
        spikeSprite.setRegion(spikeRegion!!)
        val spritesComponent = SpritesComponent(spikeSprite)
        spritesComponent.putPreProcess { _, sprite -> sprite.setCenter(body.getCenter()) }
        return spritesComponent
    }

}
