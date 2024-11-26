package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.extensions.equalsAny
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IAudioEntity
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.events.Event
import com.mega.game.engine.events.IEventListener
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.events.EventType

class SwitchGate(game: MegamanMaverickGame) : Block(game), ISpritesEntity, IAnimatedEntity, IEventListener,
    IAudioEntity, ICullableEntity {

    companion object {
        const val TAG = "SwitchGate"
        private const val SWITCH_DUR = 0.5f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class SwitchGateState { OPENING, OPEN, CLOSING, CLOSED }

    override val eventKeyMask = objectSetOf<Any>(EventType.ACTIVATE_SWITCH, EventType.DEACTIVATE_SWITCH)

    private val switchTimer = Timer(SWITCH_DUR)
    private lateinit var state: SwitchGateState
    private var key = -1

    private val open: Boolean
        get() = state.equalsAny(SwitchGateState.OPENING, SwitchGateState.OPEN)
    private val closed: Boolean
        get() = state.equalsAny(SwitchGateState.CLOSING, SwitchGateState.CLOSED)

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.GATES.source)
            SwitchGateState.entries.forEach {
                val key = it.name.lowercase()
                regions.put(key, atlas.findRegion(key))
            }
        }
        super.init()
        addComponent(defineCullablesComponent())
        addComponent(defineUpdatablesComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
        addComponent(AudioComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        game.eventsMan.addListener(this)
        state = SwitchGateState.CLOSED
        switchTimer.setToEnd()
        key = spawnProps.get(ConstKeys.KEY, Int::class)!!
    }

    override fun onDestroy() {
        super.onDestroy()
        game.eventsMan.removeListener(this)
    }

    override fun onEvent(event: Event) {
        when (event.key) {
            EventType.ACTIVATE_SWITCH -> {
                val key = event.getProperty(ConstKeys.KEY, Int::class)
                if (closed && this.key == key) {
                    state = SwitchGateState.OPENING
                    switchTimer.reset()

                    requestToPlaySound(SoundAsset.BOSS_DOOR_SOUND, false)
                }
            }

            EventType.DEACTIVATE_SWITCH -> {
                val key = event.getProperty(ConstKeys.KEY, Int::class)
                if (open && this.key == key) {
                    state = SwitchGateState.CLOSING
                    switchTimer.reset()

                    requestToPlaySound(SoundAsset.BOSS_DOOR_SOUND, false)
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val bodyComponent = super.defineBodyComponent()
        bodyComponent.body.preProcess.put(ConstKeys.ON) {
            body.physics.collisionOn = closed
            body.fixtures.forEach { (it.second as Fixture).active = closed }
        }
        return bodyComponent
    }

    private fun defineCullablesComponent() = CullablesComponent(
        objectMapOf(ConstKeys.CULL_OUT_OF_BOUNDS pairTo getGameCameraCullingLogic(this))
    )

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        if (state.equalsAny(SwitchGateState.OPENING, SwitchGateState.CLOSING)) {
            switchTimer.update(delta)
            if (switchTimer.isFinished()) state = when (state) {
                SwitchGateState.OPENING -> SwitchGateState.OPEN
                else -> SwitchGateState.CLOSED
            }
        }
    })

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 1))
        sprite.setSize(2f * ConstVals.PPM, 3f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.hidden = state.equalsAny(SwitchGateState.OPEN)
            sprite.setCenter(body.getCenter())
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = { if (state == SwitchGateState.OPEN) null else state.name.lowercase() }
        val animation = objectMapOf<String, IAnimation>(
            "closed" pairTo Animation(regions["closed"]),
            "opening" pairTo Animation(regions["opening"], 1, 5, 0.125f, false),
            "closing" pairTo Animation(regions["opening"], 1, 5, 0.125f, false).reversed(),
        )
        val animator = Animator(keySupplier, animation)
        return AnimationsComponent(this, animator)
    }
}
