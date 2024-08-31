package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.engine.audio.AudioComponent
import com.engine.common.enums.Position
import com.engine.common.extensions.getTextureRegion
import com.engine.common.extensions.objectMapOf
import com.engine.common.extensions.objectSetOf
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.cullables.CullableOnEvent
import com.engine.cullables.CullablesComponent
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.entities.contracts.IAudioEntity
import com.engine.entities.contracts.IBodyEntity
import com.engine.entities.contracts.ISpritesEntity
import com.engine.updatables.UpdatablesComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.HazardsFactory
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.world.BodyComponentCreator

class IceCubeMaker(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity, IAudioEntity {

    companion object {
        const val TAG = "IceCubeMaker"
        private const val DELAY_TIME = 1.5f
        private var region: TextureRegion? = null
    }

    private val delayTimer = Timer(DELAY_TIME)

    override fun getEntityType() = EntityType.HAZARD

    override fun init() {
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.HAZARDS_1.source, TAG)
        super.init()
        addComponent(defineUpdatablesComponent())
        addComponent(defineCullablesComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineBodyComponent())
        addComponent(AudioComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getTopCenterPoint()
        body.setTopCenterToPoint(spawn)
        delayTimer.reset()
    }

    private fun dropIceCube() {
        val spawn = body.getBottomCenterPoint()
        val icecube = EntityFactories.fetch(EntityType.HAZARD, HazardsFactory.FRAGILE_ICE_CUBE)!!
        game.engine.spawn(icecube, props(ConstKeys.POSITION to spawn))
        if (overlapsGameCamera()) requestToPlaySound(SoundAsset.CHILL_SHOOT_SOUND, false)
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        delayTimer.update(delta)
        if (delayTimer.isFinished()) {
            dropIceCube()
            delayTimer.reset()
        }
    })

    private fun defineCullablesComponent(): CullablesComponent {
        val cullEvents =
            objectSetOf<Any>(EventType.PLAYER_SPAWN, EventType.BEGIN_ROOM_TRANS, EventType.GATE_INIT_OPENING)
        val cullOnEvents = CullableOnEvent({ cullEvents.contains(it) }, cullEvents)
        runnablesOnSpawn.add { game.eventsMan.addListener(cullOnEvents) }
        runnablesOnDestroy.add { game.eventsMan.removeListener(cullOnEvents) }
        return CullablesComponent(objectMapOf(ConstKeys.CULL_EVENTS to cullOnEvents))
    }

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