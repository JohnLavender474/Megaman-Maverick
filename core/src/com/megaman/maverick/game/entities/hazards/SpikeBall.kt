package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameCircle
import com.engine.common.shapes.GameRectangle
import com.engine.damage.IDamager
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sorting.DrawingPriority
import com.engine.drawables.sorting.DrawingSection
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setCenter
import com.engine.drawables.sprites.setSize
import com.engine.entities.contracts.IBodyEntity
import com.engine.entities.contracts.IDrawableShapesEntity
import com.engine.entities.contracts.IMotionEntity
import com.engine.entities.contracts.ISpritesEntity
import com.engine.motion.MotionComponent
import com.engine.motion.MotionComponent.MotionDefinition
import com.engine.motion.Pendulum
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
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType

class SpikeBall(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity, IMotionEntity,
    IDrawableShapesEntity, IDamager, IHazard {

    companion object {
        const val TAG = "SpikeBall"
        private const val PENDULUM_GRAVITY = 10f
        private var spikeRegion: TextureRegion? = null
        private var chainRegion: TextureRegion? = null
    }

    override fun getEntityType() = EntityType.HAZARD

    override fun init() {
        if (spikeRegion == null || chainRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.HAZARDS_1.source)
            spikeRegion = atlas.findRegion("SpikeBall/SpikeBall")
            chainRegion = atlas.findRegion("SpikeBall/Chain")
        }
        addComponent(MotionComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
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

            putUpdateFunction(key) { _, _sprite ->
                val center = pendulum.getPointFromAnchor(chainDistance)
                _sprite.setCenter(center)
            }

            tempDistance += 0.25f
            tempIndex++
        }
    }

    override fun onDestroy() {
        super<MegaGameEntity>.onDestroy()
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
        damagerFixture.getShape().color = Color.RED
        debugShapes.add { damagerFixture.getShape() }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val spikeSprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 0))
        spikeSprite.setSize(2f * ConstVals.PPM)
        spikeSprite.setRegion(spikeRegion!!)
        val spritesComponent = SpritesComponent(spikeSprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setCenter(body.getCenter())
        }
        return spritesComponent
    }

}