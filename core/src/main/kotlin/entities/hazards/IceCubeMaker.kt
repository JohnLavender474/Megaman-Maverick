package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.overlaps
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.shapes.toGameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.entities.contracts.IAudioEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.HazardsFactory
import com.megaman.maverick.game.world.body.BodyComponentCreator

class IceCubeMaker(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity, ICullableEntity,
    IAudioEntity {

    companion object {
        const val TAG = "IceCubeMaker"
        private const val DELAY_TIME = 1.5f
        private var region: TextureRegion? = null
    }

    private val delayTimer = Timer(DELAY_TIME)
    private var runBounds: GameRectangle? = null

    override fun getEntityType() = EntityType.HAZARD

    override fun init() {
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.HAZARDS_1.source, TAG)
        addComponent(defineUpdatablesComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineBodyComponent())
        addComponent(AudioComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getTopCenterPoint()
        body.setTopCenterToPoint(spawn)
        runBounds = spawnProps.get(
            "${ConstKeys.RUN}_${ConstKeys.BOUNDS}", RectangleMapObject::class
        )?.rectangle?.toGameRectangle()
        delayTimer.reset()
    }

    private fun dropIceCube() {
        val spawn = body.getBottomCenterPoint()
        val icecube = EntityFactories.fetch(EntityType.HAZARD, HazardsFactory.SMALL_ICE_CUBE)!!
        icecube.spawn(props(ConstKeys.POSITION pairTo spawn))
        if (overlapsGameCamera()) requestToPlaySound(SoundAsset.CHILL_SHOOT_SOUND, false)
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        if (runBounds != null && !game.getGameCamera().overlaps(runBounds!!)) return@UpdatablesComponent

        delayTimer.update(delta)
        if (delayTimer.isFinished()) {
            dropIceCube()
            delayTimer.reset()
        }
    })

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(region!!)
        sprite.setSize(4f * ConstVals.PPM, 2.5f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setPosition(body.getTopCenterPoint(), Position.TOP_CENTER)
        }
        return spritesComponent
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(1.5f * ConstVals.PPM, 2.5f * ConstVals.PPM)
        return BodyComponentCreator.create(this, body)
    }
}
