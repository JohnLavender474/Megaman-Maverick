package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.IDrawableShapesEntity
import com.mega.game.engine.entities.contracts.IMotionEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.motion.MotionComponent
import com.mega.game.engine.motion.MotionComponent.MotionDefinition
import com.mega.game.engine.motion.Pendulum
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType

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

            putUpdateFunction(key) { _, _sprite ->
                val center = pendulum.getPointFromAnchor(chainDistance)
                _sprite.setCenter(center)
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